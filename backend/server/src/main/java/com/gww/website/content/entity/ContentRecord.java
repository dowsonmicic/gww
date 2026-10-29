package com.gww.website.content.entity;

import lombok.*;

/** 内容持久化实体，不直接作为接口入参/出参。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentRecord {
  private String id;
  private String kind;
  private String name;
  private String nameEn;
  private String category;
  private String brand;
  private String status;
  private String description;
  private String descriptionEn;
  private String imageId;
  /** JSON 字符串，MyBatis 直接读写；Jackson 在接口层处理 ↔ List 互转。 */
  private String sections;
}
