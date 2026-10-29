package com.gww.website.media.service;

import com.gww.website.common.exception.BusinessException;
import com.gww.website.content.entity.ContentRecord;
import com.gww.website.content.mapper.ContentMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 官网固定媒体位置与上传记录关联；不提供位置新增或删除。 */
@Slf4j
@Service
public class SiteMediaService {
  private static final List<String> SLOTS = List.of("carousel-1", "carousel-2", "carousel-3", "partners");
  private final ContentMapper mapper;
  private final MediaService media;

  public SiteMediaService(ContentMapper mapper, MediaService media) {
    this.mapper = mapper;
    this.media = media;
  }

  public record Request(@NotBlank @Size(max = 36) String imageId) {}
  public record Vo(
      @Schema(description = "固定位置：carousel-1/2/3或partners") String id,
      String name, String group, String imageId, String url) {}

  public List<Vo> list() {
    return SLOTS.stream().map(this::get).toList();
  }

  private Vo get(String slot) {
    var row = mapper.find("site-media-" + slot, "settings");
    String imageId = row == null ? null : row.getImageId();
    boolean partner = slot.equals("partners");
    return new Vo(slot, partner ? "合作商标" : "轮播图 " + slot.substring(9),
        partner ? "partners" : "carousel", imageId,
        imageId == null ? (partner ? "/images/brands/brands.webp"
            : "/images/lb/block_" + slot.substring(9) + ".webp")
            : "/api/public/media/" + imageId + "/image");
  }

  /** 只绑定已发布的图片，使用单位置upsert确保首次保存与并发保存一致。 */
  @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED,
      rollbackFor = Exception.class)
  public Vo save(String slot, Request request) {
    if (!SLOTS.contains(slot))
      throw new BusinessException(HttpStatus.BAD_REQUEST, "官网媒体位置不支持");
    var image = media.required(request.imageId());
    if (!image.isPublished()
        || !Set.of("image/jpeg", "image/png", "image/webp").contains(image.getContentType()))
      throw new BusinessException(HttpStatus.BAD_REQUEST, "请选择已发布的JPG/PNG/WebP图片");
    mapper.saveSiteMedia(new ContentRecord("site-media-" + slot, "settings", slot, slot,
        "", "", "PUBLISHED", "", "", image.getId(), null));
    log.info("官网媒体替换 slot={} imageId={}", slot, image.getId());
    return get(slot);
  }
}
