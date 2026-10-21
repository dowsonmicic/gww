package com.gww.website.content.controller;

import com.gww.website.common.vo.Result;
import com.gww.website.content.dto.ContentRequest;
import com.gww.website.content.service.ContentService;
import com.gww.website.content.vo.ContentVo;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
public class ContentController {
  private final ContentService service;

  public ContentController(ContentService service) {
    this.service = service;
  }

  @GetMapping("/api/admin/content/{kind}")
  @Operation(summary = "管理内容分页列表")
  public Result<List<ContentVo>> list(
      @PathVariable String kind,
      @RequestParam(defaultValue = "1") @Min(1) @Max(10000) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return Result.ok(service.list(kind, page, size));
  }

  @PostMapping("/api/admin/content/{kind}")
  @Operation(summary = "新建内容")
  public Result<ContentVo> create(
      @PathVariable String kind, @Valid @RequestBody ContentRequest dto) {
    return Result.ok(service.save(kind, null, dto));
  }

  @PutMapping("/api/admin/content/{kind}/{id}")
  @Operation(summary = "修改内容")
  public Result<ContentVo> update(
      @PathVariable String kind, @PathVariable String id, @Valid @RequestBody ContentRequest dto) {
    return Result.ok(service.save(kind, id, dto));
  }

  @DeleteMapping("/api/admin/content/{kind}/{id}")
  @Operation(summary = "删除内容")
  public Result<Void> delete(@PathVariable String kind, @PathVariable String id) {
    service.delete(kind, id);
    return Result.ok(null);
  }

  @GetMapping("/api/public/content/{kind}")
  @Operation(summary = "已发布内容列表，中英文完整资料")
  public Result<List<ContentVo>> published(@PathVariable String kind) {
    return Result.ok(service.published(kind));
  }

  @GetMapping("/api/public/content/{kind}/{id}")
  @Operation(summary = "已发布内容详情")
  public Result<ContentVo> detail(@PathVariable String kind, @PathVariable String id) {
    return Result.ok(service.detail(kind, id, true));
  }
}
