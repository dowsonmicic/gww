package com.gww.website.media.controller;

import com.gww.website.common.vo.Result;
import com.gww.website.media.service.SiteMediaService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** 管理员替换固定位置，官网匿名读取相同数据库记录。 */
@RestController
public class SiteMediaController {
  private final SiteMediaService service;

  public SiteMediaController(SiteMediaService service) {
    this.service = service;
  }

  @GetMapping({"/api/admin/site-media", "/api/public/site-media"})
  @Operation(summary = "读取官网轮播图和合作商标",
      description = "固定3张轮播及1张商标，未替换时使用原图，已替换位置返回公开图片接口地址。")
  public Result<List<SiteMediaService.Vo>> list() {
    return Result.ok(service.list());
  }

  @PutMapping("/api/admin/site-media/{slot}")
  @Operation(summary = "替换官网固定媒体位置",
      description = "先上传并发布图片，再绑定imageId。slot仅允许carousel-1/2/3和partners，需要管理员及CSRF。")
  public Result<SiteMediaService.Vo> save(
      @PathVariable String slot, @Valid @RequestBody SiteMediaService.Request request) {
    return Result.ok(service.save(slot, request));
  }
}
