package com.gww.website.media.controller;

import com.gww.website.common.vo.Result;
import com.gww.website.media.service.*;
import com.gww.website.media.vo.MediaVo;
import io.swagger.v3.oas.annotations.Operation;
import java.io.IOException;
import java.util.List;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class MediaController {
  private final MediaService service;
  private final LocalMediaStore store;

  public MediaController(MediaService service, LocalMediaStore store) {
    this.service = service;
    this.store = store;
  }

  @GetMapping("/api/admin/media")
  @Operation(summary = "媒体列表")
  public Result<List<MediaVo>> list() {
    return Result.ok(service.list());
  }

  @PostMapping(value = "/api/admin/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "上传媒体到私有目录")
  public Result<MediaVo> upload(@RequestPart("file") MultipartFile file) throws IOException {
    return Result.ok(service.upload(file));
  }

  @PostMapping("/api/admin/media/{id}/publish")
  @Operation(summary = "确认公开发布媒体")
  public Result<MediaVo> publish(@PathVariable String id) throws IOException {
    return Result.ok(service.publish(id));
  }

  @GetMapping("/api/admin/media/{id}/file")
  @Operation(summary = "管理员下载私有媒体")
  public ResponseEntity<Resource> file(@PathVariable String id) throws IOException {
    var row = service.required(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(row.getContentType()))
        .header("X-Content-Type-Options", "nosniff")
        .header("Cache-Control", "private, no-store")
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(row.getOriginalName(), java.nio.charset.StandardCharsets.UTF_8)
                .build()
                .toString())
        .body(new FileSystemResource(store.file(row.getObjectKey(), false)));
  }

  /** 本地联调公开图片；只读已发布目录，私有资源及PDF/视频仍走既有策略。 */
  @GetMapping("/api/public/media/{id}/image")
  @Operation(summary = "读取已发布图片")
  public ResponseEntity<Resource> publicImage(@PathVariable String id) throws IOException {
    var row = service.required(id);
    if (!row.isPublished() || !java.util.Set.of("image/jpeg", "image/png", "image/webp").contains(row.getContentType())) {
      throw new com.gww.website.common.exception.BusinessException(HttpStatus.NOT_FOUND, "公开图片不存在");
    }
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(row.getContentType()))
        .header("X-Content-Type-Options", "nosniff")
        .header("Cache-Control", "no-cache")
        .body(new FileSystemResource(store.file(row.getObjectKey(), true)));
  }
}
