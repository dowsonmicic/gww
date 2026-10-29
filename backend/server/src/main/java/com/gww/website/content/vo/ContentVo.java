package com.gww.website.content.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;

@Schema(description = "内容管理信息，媒体仅返回URL不返回磁盘路径")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContentVo(
    String id,
    String kind,
    @Schema(description = "文章标题；仅articles返回") String title,
    String name,
    String nameEn,
    String category,
    String brand,
    String status,
    String description,
    String descriptionEn,
    String imageId,
    String imageUrl,
    java.util.List<SectionItem> sections)
    implements java.io.Serializable {

  public record SectionItem(String title, String titleEn, String body, String bodyEn) {}
}
