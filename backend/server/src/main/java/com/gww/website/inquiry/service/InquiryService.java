package com.gww.website.inquiry.service;

import com.gww.website.common.exception.BusinessException;
import com.gww.website.inquiry.entity.InquiryRecord;
import com.gww.website.inquiry.mapper.InquiryMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Slf4j
@Service
public class InquiryService {
  @Schema(description = "询盘参数")
  public record Request(
      @NotBlank @Size(max = 80) String name,
      @NotBlank @Email @Size(max = 180) String email,
      @NotBlank @Pattern(regexp = "[+0-9 ()-]{1,40}") String phone,
      @NotBlank @Size(max = 4000) String message,
      @NotBlank @Pattern(regexp = "zh|en") String locale,
      @Size(max = 80) String country,
      @Size(max = 80) String industry,
      @Size(max = 180) String product,
      @Size(max = 500) @Pattern(regexp = "^$|https?://[^\\s]+") String website,
      @Size(max = 180) String company) {}

  public record Vo(
      String id,
      String name,
      String email,
      String phone,
      String message,
      String locale,
      String status,
      String country,
      String industry,
      String product,
      String website,
      String company,
      @Schema(description = "提交时间，按服务器数据库时间显示") LocalDateTime createdAt) {}

  private final InquiryMapper mapper;

  public InquiryService(InquiryMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(
      propagation = Propagation.REQUIRED,
      isolation = Isolation.READ_COMMITTED,
      rollbackFor = Exception.class)
  public String create(Request dto) {
    String id = UUID.randomUUID().toString();
    mapper.insert(
        new InquiryRecord(
            id, dto.name(), dto.email(), dto.phone(), dto.message(), dto.locale(), "NEW",
            value(dto.country()), value(dto.industry()), value(dto.product()),
            value(dto.website()), value(dto.company()), null));
    log.info("询盘保存 id={} locale={}", id, dto.locale());
    return id;
  }

  public List<Vo> list(LocalDate startDate, LocalDate endDate, String locale) {
    validateRange(startDate, endDate, locale);
    return mapper.list(
        startDate == null ? null : startDate.atStartOfDay(),
        endDate == null ? null : endDate.plusDays(1).atStartOfDay(), locale)
        .stream().map(this::toVo).toList();
  }

  /** 仅由管理员接口调用；联系方式原值只在授权后台返回。 */
  public Vo detail(String id) {
    InquiryRecord row = mapper.find(id);
    if (row == null) throw new BusinessException(HttpStatus.NOT_FOUND, "询盘不存在");
    log.info("管理员读取询盘详情 id={}", id);
    return toVo(row);
  }

  public void validateRange(LocalDate startDate, LocalDate endDate, String locale) {
    if (startDate != null && endDate != null && startDate.isAfter(endDate))
      throw new BusinessException(HttpStatus.BAD_REQUEST, "开始日期不能晚于结束日期");
    if (locale != null && !Set.of("zh", "en").contains(locale))
      throw new BusinessException(HttpStatus.BAD_REQUEST, "语言不支持");
    if (LocalDate.MAX.equals(endDate))
      throw new BusinessException(HttpStatus.BAD_REQUEST, "结束日期超出支持范围");
  }

  /** 在事务内消费数据库游标，流式生成Excel；结束日采用次日零点排他边界。 */
  @Transactional(readOnly = true)
  public void export(LocalDate startDate, LocalDate endDate, String locale, OutputStream output)
      throws IOException {
    validateRange(startDate, endDate, locale);
    try (var rows = mapper.export(
        startDate == null ? null : startDate.atStartOfDay(),
        endDate == null ? null : endDate.plusDays(1).atStartOfDay(), locale)) {
      long count = InquiryExcel.write(rows, output);
      log.info("管理员导出询盘 start={} end={} locale={} count={}", startDate, endDate, locale, count);
    }
  }

  private Vo toVo(InquiryRecord r) {
    return new Vo(r.getId(), r.getName(), r.getEmail(), r.getPhone(), r.getMessage(),
        r.getLocale(), r.getStatus(), r.getCountry(), r.getIndustry(), r.getProduct(),
        r.getWebsite(), r.getCompany(), r.getCreatedAt());
  }

  @Transactional(
      propagation = Propagation.REQUIRED,
      isolation = Isolation.READ_COMMITTED,
      rollbackFor = Exception.class)
  public void update(String id, String status) {
    if (!Set.of("NEW", "PROCESSING", "CLOSED").contains(status))
      throw new BusinessException(HttpStatus.BAD_REQUEST, "状态不支持");
    if (mapper.updateStatus(id, status) == 0)
      throw new BusinessException(HttpStatus.NOT_FOUND, "询盘不存在");
    log.info("询盘状态更新 id={} status={}", id, status);
  }

  private String value(String value) {
    return value == null ? "" : value.trim();
  }

}
