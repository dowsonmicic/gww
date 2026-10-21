package com.gww.website;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/** 官网业务服务，独立于Nuxt与媒体文件目录。 */
@EnableCaching
@SpringBootApplication
public class WebsiteApplication {
  public static void main(String[] args) {
    SpringApplication.run(WebsiteApplication.class, args);
  }
}
