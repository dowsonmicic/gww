package com.gww.website.inquiry.controller;

import com.gww.website.common.exception.BusinessException;
import com.gww.website.common.vo.Result;
import com.gww.website.inquiry.service.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import com.gww.website.common.config.LocalRateLimiter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.bind.annotation.*;

@RestController
public class InquiryController {
  private final InquiryService service;
  private final LocalRateLimiter limiter;

  public InquiryController(InquiryService service, LocalRateLimiter limiter) {
    this.service = service;
    this.limiter = limiter;
  }

  public record StatusRequest(@NotBlank String status) {}

  @PostMapping("/api/public/inquiries")
  @Operation(summary = "保存客户询盘，需CSRF令牌及限流")
  public Result<String> create(
      @Valid @RequestBody InquiryService.Request dto, HttpServletRequest req) {
    String key = "inquiry:rate:" + req.getRemoteAddr();
    if (!limiter.allow(key, 5, Duration.ofHours(1)))
      throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "提交过于频繁");
    return Result.ok(service.create(dto));
  }

  @GetMapping("/api/admin/inquiries")
  @Operation(summary = "管理员查询完整询盘，按提交时间倒序",
      description = "按数据库created_at查询；startDate/endDate格式yyyy-MM-dd，包含起止日全天；locale可选zh/en，不传条件查询全部。")
  public Result<List<InquiryService.Vo>> list(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(required = false) String locale) {
    return Result.ok(service.list(startDate, endDate, locale));
  }

  @GetMapping("/api/admin/inquiries/{id}")
  @Operation(summary = "管理员查看单条询盘详情")
  public Result<InquiryService.Vo> detail(@PathVariable String id) {
    return Result.ok(service.detail(id));
  }

  @GetMapping(value = "/api/admin/inquiries/export",
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  @Operation(summary = "管理员按日期批量导出询盘Excel",
      description = "日期格式yyyy-MM-dd，包含开始及结束日全天；不指定日期导出全部，locale可选zh或en，不指定则全部语言。按时间倒序，不限制100条。日期使用数据库服务器时间。")
  public ResponseEntity<StreamingResponseBody> export(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(required = false) String locale) {
    service.validateRange(startDate, endDate, locale);
    String filename = "inquiries_" + (startDate == null ? "all" : startDate) + "_"
        + (endDate == null ? "all" : endDate) + ".xlsx";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(output -> service.export(startDate, endDate, locale, output));
  }

  @PutMapping("/api/admin/inquiries/{id}/status")
  @Operation(summary = "修改询盘跟进状态")
  public Result<Void> update(@PathVariable String id, @Valid @RequestBody StatusRequest dto) {
    service.update(id, dto.status());
    return Result.ok(null);
  }
}
