package com.gww.website.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/** 官网公开配置；不包含磁盘路径、管理员密码或其他敏感配置。 */
@Schema(description = "网站基础设置，中英文信息分区，联系方式共用")
public record SiteSettings(
    @NotNull @Size(max = 120) String companyName,
    @NotNull @Size(max = 180) String companyNameEn,
    @NotNull @Size(max = 4000) String companyIntro,
    @NotNull @Size(max = 4000) String companyIntroEn,
    @NotNull @Size(max = 300) String address,
    @NotNull @Size(max = 300) String addressEn,
    @NotNull @Email @Size(max = 180) String contactEmail,
    @NotNull @Pattern(regexp = "[+0-9 ()-]{0,40}") String contactPhone,
    @NotBlank @Pattern(regexp = "zh|en") String defaultLocale,
    @NotNull @Size(max = 500) @Pattern(regexp = "^$|https://[^\\s]+") String supportUrl) {}
