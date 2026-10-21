package com.gww.website.media.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "媒体信息；私有资源仅通过认证接口下载")
public record MediaVo(
    String id,
    String originalName,
    String contentType,
    long sizeBytes,
    boolean published,
    String url) {}
