package com.gww.website;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** 仅询盘后台相关集成测试，使用隔离H2数据库，不写入本地MySQL客户数据。 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:inquiry-admin;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InquiryAdminTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void seed() {
    jdbc.update("DELETE FROM inquiry_record");
    insert("old", "2026-10-19 23:59:59", "zh");
    insert("start", "2026-10-20 00:00:00", "zh");
    insert("end", "2026-10-20 23:59:59", "zh");
    insert("english", "2026-10-20 12:00:00", "en");
    insert("new", "2026-10-21 00:00:00", "zh");
  }

  private void insert(String id, String time, String locale) {
    jdbc.update("INSERT INTO inquiry_record(id,name,email,phone,message,locale,status,created_at,country,company)"
        + " VALUES(?,?,?,?,?,?,'NEW',?,'VN','Test Company')",
        id, "客户-" + id, "customer@example.com", "+84123456789",
        "=HYPERLINK(\"https://example.com\")\n<&>\u0001", locale, time);
  }

  @Test
  void contactsDetailsAndNewestFirst() throws Exception {
    mvc.perform(get("/api/admin/inquiries").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value("new"))
        .andExpect(jsonPath("$.data[1].id").value("end"))
        .andExpect(jsonPath("$.data[0].email").value("customer@example.com"))
        .andExpect(jsonPath("$.data[0].phone").value("+84123456789"))
        .andExpect(jsonPath("$.data[0].createdAt").value("2026-10-21T00:00:00"));
    mvc.perform(get("/api/admin/inquiries/end").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.company").value("Test Company"))
        .andExpect(jsonPath("$.data.email").value("customer@example.com"));
    mvc.perform(get("/api/admin/inquiries/missing").with(user("admin").roles("ADMIN")))
        .andExpect(status().isNotFound());
  }

  @Test
  void listQueriesDatabaseCreatedAtWithInclusiveDates() throws Exception {
    mvc.perform(get("/api/admin/inquiries?startDate=2026-10-20&endDate=2026-10-20&locale=zh")
            .with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].id").value("end"))
        .andExpect(jsonPath("$.data[1].id").value("start"));
    mvc.perform(get("/api/admin/inquiries?endDate=2026-10-20")
            .with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(4));
    mvc.perform(get("/api/admin/inquiries?startDate=2026-10-21")
            .with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value("new"))
        .andExpect(jsonPath("$.data.length()").value(1));
    // 修改数据库提交时间后，列表必须以数据库新值为准。
    jdbc.update("UPDATE inquiry_record SET created_at='2026-10-22 10:00:00' WHERE id='old'");
    mvc.perform(get("/api/admin/inquiries?startDate=2026-10-22&endDate=2026-10-22")
            .with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value("old"));
    for (String query : new String[] {
        "startDate=2026-10-21&endDate=2026-10-20", "locale=fr", "endDate=invalid"}) {
      mvc.perform(get("/api/admin/inquiries?" + query).with(user("admin").roles("ADMIN")))
          .andExpect(status().isBadRequest());
    }
  }

  @Test
  void adminPermissionIsRequired() throws Exception {
    for (String path : new String[] {
        "/api/admin/inquiries", "/api/admin/inquiries/end", "/api/admin/inquiries/export"}) {
      mvc.perform(get(path)).andExpect(status().isUnauthorized());
      mvc.perform(get(path).with(user("guest").roles("USER"))).andExpect(status().isForbidden());
    }
  }

  @Test
  void exportIncludesBothDateBoundariesAndLiteralText() throws Exception {
    String xml = worksheet("/api/admin/inquiries/export?startDate=2026-10-20&endDate=2026-10-20&locale=zh");
    assertTrue(xml.contains("客户-end"));
    assertTrue(xml.contains("客户-start"));
    assertFalse(xml.contains("客户-new"));
    assertFalse(xml.contains("客户-old"));
    assertFalse(xml.contains("客户-english"));
    assertTrue(xml.indexOf("客户-end") < xml.indexOf("客户-start"));
    assertTrue(xml.contains("customer@example.com"));
    assertTrue(xml.contains("+84123456789"));
    assertTrue(xml.contains("=HYPERLINK"));
    assertFalse(xml.contains("<f>"));
    assertFalse(xml.contains("\u0001"));
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    var document = factory.newDocumentBuilder().parse(
        new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    assertEquals(3, document.getElementsByTagName("row").getLength());
  }

  @Test
  void exportAllExceeds100AndHandlesEmptyResults() throws Exception {
    for (int i = 0; i < 105; i++) insert("bulk-" + i, "2026-10-20 12:30:00", "zh");
    String xml = worksheet("/api/admin/inquiries/export");
    assertEquals(111, xml.split("<row r=").length - 1);
    assertTrue(xml.contains("客户-english"));
    String empty = worksheet("/api/admin/inquiries/export?startDate=2030-01-01");
    assertEquals(1, empty.split("<row r=").length - 1);
  }

  @Test
  void invalidExportFiltersFailBeforeDownload() throws Exception {
    for (String query : new String[] {
        "startDate=2026-10-21&endDate=2026-10-20", "locale=fr", "startDate=invalid"}) {
      mvc.perform(get("/api/admin/inquiries/export?" + query).with(user("admin").roles("ADMIN")))
          .andExpect(status().isBadRequest());
    }
  }

  private String worksheet(String url) throws Exception {
    var pending = mvc.perform(get(url).with(user("admin").roles("ADMIN")))
        .andExpect(request().asyncStarted()).andReturn();
    var response = mvc.perform(asyncDispatch(pending)).andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(header().string("Content-Type",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .andReturn().getResponse();
    assertTrue(response.getHeader("Content-Disposition").contains(".xlsx"));
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(response.getContentAsByteArray()))) {
      java.util.zip.ZipEntry entry;
      String worksheet = null;
      int parts = 0;
      while ((entry = zip.getNextEntry()) != null) {
        String content = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
        if (entry.getName().equals("xl/worksheets/sheet1.xml")) worksheet = content;
        parts++;
      }
      assertEquals(5, parts);
      assertNotNull(worksheet);
      return worksheet;
    }
  }
}
