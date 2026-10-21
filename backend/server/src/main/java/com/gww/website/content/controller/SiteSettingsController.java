package com.gww.website.content.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gww.website.common.vo.Result;
import com.gww.website.content.dto.SiteSettings;
import com.gww.website.content.service.SiteSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** 后台保存和官网读取使用同一配置模型，权限由Spring Security统一控制。 */
@RestController
public class SiteSettingsController {
  private final SiteSettingsService service;

  public SiteSettingsController(SiteSettingsService service) { this.service = service; }

  @GetMapping({"/api/admin/site-settings", "/api/public/site-settings"})
  @Operation(summary = "读取网站基础设置")
  public Result<SiteSettings> get() { return Result.ok(service.get()); }

  @PutMapping("/api/admin/site-settings")
  @Operation(summary = "保存网站基础设置")
  public Result<SiteSettings> save(@Valid @RequestBody SiteSettings dto)
      throws JsonProcessingException {
    return Result.ok(service.save(dto));
  }
}
