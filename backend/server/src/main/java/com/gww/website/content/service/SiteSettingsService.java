package com.gww.website.content.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gww.website.common.exception.BusinessException;
import com.gww.website.content.dto.SiteSettings;
import com.gww.website.content.entity.ContentRecord;
import com.gww.website.content.mapper.ContentMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 固定设置记录以JSON保存，避免将公开网站配置混入服务器环境变量。 */
@Service
public class SiteSettingsService {
  private static final String ID = "site-basic";
  private final ContentMapper mapper;
  private final ObjectMapper json;

  public SiteSettingsService(ContentMapper mapper, ObjectMapper json) {
    this.mapper = mapper;
    this.json = json;
  }

  public SiteSettings get() {
    var row = mapper.find(ID, "settings");
    if (row == null) return null;
    try {
      return json.readValue(row.getDescription(), SiteSettings.class);
    } catch (JsonProcessingException ex) {
      throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "网站设置读取失败");
    }
  }

  @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED,
      rollbackFor = Exception.class)
  public SiteSettings save(SiteSettings settings) throws JsonProcessingException {
    // upsert保证首次并发保存不会创建多个设置记录。
    mapper.saveSettings(new ContentRecord(ID, "settings", "网站基础设置", "Site settings",
        "", "", "PUBLISHED", json.writeValueAsString(settings), "", null));
    return settings;
  }
}
