package com.gww.website.content.service;

import com.gww.website.common.exception.BusinessException;
import com.gww.website.content.dto.ContentRequest;
import com.gww.website.content.entity.ContentRecord;
import com.gww.website.content.mapper.ContentMapper;
import com.gww.website.content.vo.ContentVo;
import com.gww.website.media.service.MediaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Slf4j
@Service
public class ContentService {
  private final ContentMapper mapper;
  private final MediaService media;
  private final ObjectMapper om;

  public ContentService(ContentMapper mapper, MediaService media, ObjectMapper om) {
    this.mapper = mapper;
    this.media = media;
    this.om = om;
  }

  public List<ContentVo> list(String kind, int page, int size) {
    checkKind(kind);
    return mapper.list(kind, size, (page - 1) * size).stream().map(this::vo).toList();
  }

  @Cacheable(value = "content:published", key = "#kind")
  public List<ContentVo> published(String kind) {
    checkKind(kind);
    return mapper.published(kind).stream().map(this::vo).toList();
  }

  public ContentVo detail(String kind, String id, boolean publicOnly) {
    checkKind(kind);
    var row = mapper.find(id, kind);
    if (row == null || publicOnly && !"PUBLISHED".equals(row.getStatus()))
      throw new BusinessException(HttpStatus.NOT_FOUND, "内容不存在");
    return vo(row);
  }

  @Transactional(
      propagation = Propagation.REQUIRED,
      isolation = Isolation.READ_COMMITTED,
      rollbackFor = Exception.class)
  @CacheEvict(value = "content:published", allEntries = true)
  public ContentVo save(String kind, String id, ContentRequest dto) {
    checkKind(kind);
    boolean article = "articles".equals(kind);
    String name = article ? dto.title() : dto.name();
    if (name == null || name.isBlank())
      throw new BusinessException(HttpStatus.BAD_REQUEST, article ? "标题不能为空" : "名称不能为空");
    if (!article && (dto.nameEn() == null || dto.nameEn().isBlank()))
      throw new BusinessException(HttpStatus.BAD_REQUEST, "英文名称不能为空");
    if (dto.imageId() != null) media.publishedUrl(dto.imageId());
    var row =
        new ContentRecord(
            id == null ? UUID.randomUUID().toString() : id,
            kind,
            name.trim(),
            article ? existingEnglishName(id) : Optional.ofNullable(dto.nameEn()).orElse(""),
            Optional.ofNullable(dto.category()).orElse(""),
            Optional.ofNullable(dto.brand()).orElse(""),
            dto.status(),
            Optional.ofNullable(dto.description()).orElse(""),
            Optional.ofNullable(dto.descriptionEn()).orElse(""),
            dto.imageId(),
            toSectionsJson(dto.sections()));
    if (id == null) mapper.insert(row);
    else if (mapper.update(row) == 0) throw new BusinessException(HttpStatus.NOT_FOUND, "内容不存在");
    log.info("内容保存 kind={} id={} status={}", kind, row.getId(), row.getStatus());
    return vo(row);
  }

  @Transactional(
      propagation = Propagation.REQUIRED,
      isolation = Isolation.READ_COMMITTED,
      rollbackFor = Exception.class)
  @CacheEvict(value = "content:published", allEntries = true)
  public void delete(String kind, String id) {
    checkKind(kind);
    if (mapper.delete(id, kind) == 0) throw new BusinessException(HttpStatus.NOT_FOUND, "内容不存在");
    log.info("内容删除 kind={} id={}", kind, id);
  }

  private void checkKind(String kind) {
    if (!Set.of("products", "categories", "brands", "articles", "settings").contains(kind))
      throw new BusinessException(HttpStatus.BAD_REQUEST, "内容类型不支持");
  }

  /** 共用表保留历史英文名称，但文章接口不再编辑或展示它。 */
  private String existingEnglishName(String id) {
    var existing = id == null ? null : mapper.find(id, "articles");
    return existing == null ? "" : existing.getNameEn();
  }

  private ContentVo vo(ContentRecord r) {
    boolean article = "articles".equals(r.getKind());
    return new ContentVo(
        r.getId(),
        r.getKind(),
        article ? r.getName() : null,
        article ? null : r.getName(),
        article ? null : r.getNameEn(),
        r.getCategory(),
        r.getBrand(),
        r.getStatus(),
        r.getDescription(),
        r.getDescriptionEn(),
        r.getImageId(),
        media.publishedUrl(r.getImageId()),
        fromSectionsJson(r.getSections()));
  }

  private String toSectionsJson(List<ContentRequest.SectionItem> sections) {
    if (sections == null || sections.isEmpty()) return null;
    try {
      return om.writeValueAsString(sections);
    } catch (JsonProcessingException e) {
      throw new BusinessException(HttpStatus.BAD_REQUEST, "产品详情段落格式错误");
    }
  }

  private List<ContentVo.SectionItem> fromSectionsJson(String json) {
    if (json == null || json.isBlank()) return null;
    try {
      return om.readValue(
          json, new TypeReference<List<ContentVo.SectionItem>>() {});
    } catch (JsonProcessingException e) {
      log.warn("sections JSON 解析失败，忽略");
      return null;
    }
  }
}
