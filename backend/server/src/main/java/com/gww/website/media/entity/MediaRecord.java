package com.gww.website.media.entity;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaRecord {
  private String id;
  private String objectKey;
  private String originalName;
  private String contentType;
  private long sizeBytes;
  private boolean published;
}
