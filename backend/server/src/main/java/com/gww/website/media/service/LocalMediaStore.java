package com.gww.website.media.service;

import com.gww.website.common.exception.BusinessException;
import com.gww.website.media.config.MediaProperties;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** 可替换的本地存储边界；从不采用用户文件名作为路径。 */
@Component
public class LocalMediaStore {
  private final MediaProperties properties;

  public LocalMediaStore(MediaProperties properties) {
    this.properties = properties;
  }

  public record Stored(String key, String contentType, long size) {}

  public Stored save(MultipartFile file) throws IOException {
    Path root = root(false);
    if (file.isEmpty() || file.getSize() > properties.getMaxUploadBytes()) {
      throw new BusinessException(HttpStatus.BAD_REQUEST, "文件为空或超过大小限制");
    }
    String name = Optional.ofNullable(file.getOriginalFilename()).orElse("");
    String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    String type =
        Map.of(
                "jpg",
                "image/jpeg",
                "jpeg",
                "image/jpeg",
                "png",
                "image/png",
                "webp",
                "image/webp",
                "pdf",
                "application/pdf",
                "mp4",
                "video/mp4")
            .get(ext);
    if (type == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "不支持的文件类型");
    String key = UUID.randomUUID() + "." + ext;
    Path target = resolve(root, key);
    try (InputStream input = file.getInputStream()) {
      byte[] header = input.readNBytes(16);
      if (!signature(header, ext)) throw new BusinessException(HttpStatus.BAD_REQUEST, "文件内容与类型不符");
      try (OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
        output.write(header);
        byte[] buffer = new byte[8192];
        long total = header.length;
        int read;
        while ((read = input.read(buffer)) != -1) {
          total += read;
          if (total > properties.getMaxUploadBytes())
            throw new BusinessException(HttpStatus.BAD_REQUEST, "文件超过大小限制");
          output.write(buffer, 0, read);
        }
      }
      return new Stored(key, type, Files.size(target));
    } catch (IOException | RuntimeException ex) {
      Files.deleteIfExists(target);
      throw ex;
    }
  }

  public Path file(String key, boolean published) throws IOException {
    Path path = resolve(root(published), key);
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
      throw new BusinessException(HttpStatus.NOT_FOUND, "文件不存在");
    return path;
  }

  public void publish(String key) throws IOException {
    Path target = resolve(root(true), key);
    Path source = file(key, false);
    // CREATE_NEW确保不覆盖已有文件；仅清理由本次调用创建的未完成副本。
    boolean created = false;
    try (InputStream input = Files.newInputStream(source)) {
      try (OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
        created = true;
        input.transferTo(output);
      }
    } catch (FileAlreadyExistsException ex) {
      throw new BusinessException(HttpStatus.CONFLICT, "公开文件已存在");
    } catch (IOException | RuntimeException ex) {
      if (created) Files.deleteIfExists(target);
      throw ex;
    }
  }

  public void remove(String key, boolean published) throws IOException {
    Files.deleteIfExists(resolve(root(published), key));
  }

  private Path root(boolean published) throws IOException {
    if (properties.getPublicRoot().isBlank() || properties.getPrivateRoot().isBlank()) {
      throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "媒体存储目录尚未配置");
    }
    Path publicPath = checkedRoot(properties.getPublicRoot());
    Path privatePath = checkedRoot(properties.getPrivateRoot());
    if (publicPath.startsWith(privatePath) || privatePath.startsWith(publicPath)) {
      throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "公开和私有目录必须独立");
    }
    return published ? publicPath : privatePath;
  }

  private Path checkedRoot(String value) throws IOException {
    Path path = Path.of(value);
    if (!path.isAbsolute())
      throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "存储目录必须为绝对路径");
    for (Path part : path)
      if (".output".equals(part.toString()))
        throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "存储目录不能位于构建目录");
    Files.createDirectories(path);
    Path real = path.toRealPath();
    for (Path part : real)
      if (".output".equals(part.toString()))
        throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "存储目录不能位于构建目录");
    return real;
  }

  private Path resolve(Path root, String key) {
    if (!key.matches("[0-9a-f-]{36}\\.(jpg|jpeg|png|webp|pdf|mp4)"))
      throw new BusinessException(HttpStatus.BAD_REQUEST, "无效媒体标识");
    return root.resolve(key);
  }

  private boolean signature(byte[] h, String ext) {
    if (h.length < 12) return false;
    return switch (ext) {
      case "jpg", "jpeg" -> (h[0] & 255) == 255 && (h[1] & 255) == 216 && (h[2] & 255) == 255;
      case "png" ->
          Arrays.equals(Arrays.copyOf(h, 8), new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
      case "webp" ->
          new String(h, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
              && new String(h, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
      case "pdf" -> new String(h, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-");
      case "mp4" -> new String(h, 4, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("ftyp");
      default -> false;
    };
  }
}
