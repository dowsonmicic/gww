package com.gww.website.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/** 描述按纯文本处理，前端不得未经清洗使用v-html。 */
@Schema(description = "内容编辑参数")
public record ContentRequest(
    @Schema(description = "文章标题；articles必填，文章不使用name/nameEn")
    @Size(max = 120) String title,
    @Size(max = 120) String name,
    @Schema(description = "非文章内容的英文名称")
    @Size(max = 180) String nameEn,
    @Size(max = 80) String category,
    @Size(max = 80) String brand,
    @NotBlank @Pattern(regexp = "DRAFT|PUBLISHED|ARCHIVED") String status,
    @Schema(description = "内容，纯文本，选填") @Size(max = 20000) String description,
    @Schema(description = "英文内容，选填") @Size(max = 20000) String descriptionEn,
    @Pattern(regexp = "[0-9a-fA-F-]{36}") String imageId) {}
