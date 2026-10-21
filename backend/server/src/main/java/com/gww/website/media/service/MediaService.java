package com.gww.website.media.service;

import com.gww.website.common.exception.BusinessException;
import com.gww.website.media.config.MediaProperties;
import com.gww.website.media.entity.MediaRecord;
import com.gww.website.media.mapper.MediaMapper;
import com.gww.website.media.vo.MediaVo;
import java.io.IOException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class MediaService {
  private final MediaMapper mapper;
  private final LocalMediaStore store;
  private final MediaProperties properties;

  public MediaService(MediaMapper mapper, LocalMediaStore store, MediaProperties properties) {
    this.mapper = mapper;
    this.store = store;
    this.properties = properties;
  }

  public List<MediaVo> list() {
    return mapper.list().stream().map(this::vo).toList();
  }

  public MediaRecord required(String id) {
    MediaRecord row = mapper.find(id);
    if (row == null) throw new BusinessException(HttpStatus.NOT_FOUND, "媒体不存在");
    return row;
  }

  public String publishedUrl(String id) {
    if (id == null) return null;
    MediaRecord row = required(id);
    if (!row.isPublished()) throw new BusinessException(HttpStatus.BAD_REQUEST, "内容只能引用已发布媒体");
    String prefix = properties.getPublicUrlPrefix();
    if (!prefix.matches("/[a-zA-Z0-9/_-]+/"))
      throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "媒体URL配置无效");
    return prefix + row.getObjectKey();
  }

  @Transactional(
      propagation = Propagation.REQUIRED,
      isolation = Isolation.READ_COMMITTED,
      rollbackFor = Exception.class)
  public MediaVo upload(MultipartFile file) throws IOException {
    var saved = store.save(file);
    rollbackFile(saved.key(), false);
    String name = Optional.ofNullable(file.getOriginalFilename()).orElse("media");
    name = name.replaceAll("[\\p{Cntrl}/\\\\]", "_");
    if (name.length() > 180) name = name.substring(0, 180);
    var row =
        new MediaRecord(
            UUID.randomUUID().toString(),
            saved.key(),
            name,
            saved.contentType(),
            saved.size(),
            false);
    mapper.insert(row);
    log.info("媒体上传 id={} bytes={}", row.getId(), row.getSizeBytes());
    return vo(row);
  }

  @Transactional(
      propagation = Propagation.REQUIRED,
      isolation = Isolation.READ_COMMITTED,
      rollbackFor = Exception.class)
  public MediaVo publish(String id) throws IOException {
    MediaRecord row = mapper.lock(id);
    if (row == null) throw new BusinessException(HttpStatus.NOT_FOUND, "媒体不存在");
    if (!row.isPublished()) {
      // 成功创建公开副本后才注册补偿，避免冲突时删除已有文件。
      store.publish(row.getObjectKey());
      rollbackFile(row.getObjectKey(), true);
      mapper.publish(id);
      row.setPublished(true);
      log.info("媒体公开发布 id={}", id);
    }
    return vo(row);
  }

  private MediaVo vo(MediaRecord row) {
    return new MediaVo(
        row.getId(),
        row.getOriginalName(),
        row.getContentType(),
        row.getSizeBytes(),
        row.isPublished(),
        row.isPublished()
            ? publishedUrl(row.getId())
            : "/api/admin/media/" + row.getId() + "/file");
  }

  private void rollbackFile(String key, boolean published) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED)
              try {
                store.remove(key, published);
              } catch (IOException ex) {
                log.error("媒体回滚清理失败 key={}", key, ex);
              }
          }
        });
  }
}
