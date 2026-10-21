package com.gww.website.media.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 磁盘路径仅服务端读取，未配置时不可上传，不能落入构建目录。 */
@Data
@Component
@ConfigurationProperties("app.media")
public class MediaProperties {
  private String publicRoot = "";
  private String privateRoot = "";
  private String publicUrlPrefix = "/media/";
  private long maxUploadBytes = 20971520;
}
