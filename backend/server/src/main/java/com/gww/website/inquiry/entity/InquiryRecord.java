package com.gww.website.inquiry.entity;

import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InquiryRecord {
  private String id;
  private String name;
  private String email;
  private String phone;
  private String message;
  private String locale;
  private String status;
  private String country;
  private String industry;
  private String product;
  private String website;
  private String company;
  /** 数据库生成的提交时间，管理员列表和导出使用同一时间字段。 */
  private LocalDateTime createdAt;
}
