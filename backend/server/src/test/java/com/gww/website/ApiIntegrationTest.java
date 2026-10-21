package com.gww.website;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** H2与本机会话的接口集成测试，不依赖Redis。 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired org.springframework.security.core.userdetails.UserDetailsService users;
  @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;

  @Test
  void authenticationAndCsrfAreRequired() throws Exception {
    mvc.perform(get("/api/admin/content/products")).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/admin/content/products").with(user("admin").roles("ADMIN")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/content/products").with(user("guest").roles("USER")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/auth/csrf"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.token").isNotEmpty());
  }

  @Test
  void loginCreatesSessionAndLogoutRevokesIt() throws Exception {
    // 测试中动态设置密码哈希，不为生产提供默认密码。
    ((org.springframework.security.provisioning.InMemoryUserDetailsManager) users)
        .updateUser(
            org.springframework.security.core.userdetails.User.withUsername("admin")
                .password(encoder.encode("integration-test-only"))
                .roles("ADMIN")
                .build());
    mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized());
    var result =
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"admin\",\"password\":\"integration-test-only\"}"))
            .andExpect(status().isOk())
            .andReturn();
    var session = (org.springframework.mock.web.MockHttpSession) result.getRequest().getSession();
    mvc.perform(get("/api/auth/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.username").value("admin"));
    mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isOk());
    org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
    mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void contentDraftPublicationValidationAndDeletion() throws Exception {
    String draft =
        """
        {"name":"测试型号","nameEn":"Test model","category":"POE","brand":"DOW",
         "status":"DRAFT","description":"说明","descriptionEn":"Description"}
        """;
    String body =
        mvc.perform(
                post("/api/admin/content/products")
                    .with(user("admin").roles("ADMIN"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(draft))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = json.readTree(body).path("data").path("id").asText();
    mvc.perform(get("/api/public/content/products/" + id)).andExpect(status().isNotFound());
    mvc.perform(
            put("/api/admin/content/products/" + id)
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(draft.replace("DRAFT", "PUBLISHED")))
        .andExpect(status().isOk());
    mvc.perform(get("/api/public/content/products/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.nameEn").value("Test model"));
    mvc.perform(get("/api/admin/content/products?page=0").with(user("admin").roles("ADMIN")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/admin/content/products")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            delete("/api/admin/content/products/" + id)
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
        .andExpect(status().isOk());
    mvc.perform(get("/api/public/content/products/" + id)).andExpect(status().isNotFound());
  }

  @Test
  void articleUsesOneRequiredTitle() throws Exception {
    var result = mvc.perform(
            post("/api/admin/content/articles")
                .with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"单一标题\",\"status\":\"PUBLISHED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("单一标题"))
        .andExpect(jsonPath("$.data.name").doesNotExist())
        .andExpect(jsonPath("$.data.nameEn").doesNotExist())
        .andReturn();
    String id = json.readTree(result.getResponse().getContentAsString()).path("data").path("id").asText();
    mvc.perform(get("/api/public/content/articles/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("单一标题"));
    mvc.perform(post("/api/admin/content/articles")
            .with(user("admin").roles("ADMIN")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\" \",\"status\":\"DRAFT\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(delete("/api/admin/content/articles/" + id)
            .with(user("admin").roles("ADMIN")).with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  void inquiriesValidateAndShowAdminContacts() throws Exception {
    mvc.perform(
            post("/api/public/inquiries")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"客户","email":"customer@example.com","phone":"+8613812345678",
                     "message":"Please contact me","locale":"en","country":"VN",
                     "industry":"PLASTICS","product":"","website":"https://example.com","company":"Test company"}
                    """))
        .andExpect(status().isOk());
    mvc.perform(get("/api/admin/inquiries").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].email").value("customer@example.com"))
        .andExpect(jsonPath("$.data[0].phone").value("+8613812345678"))
        .andExpect(jsonPath("$.data[0].createdAt").isNotEmpty())
        .andExpect(jsonPath("$.data[0].country").value("VN"))
        .andExpect(jsonPath("$.data[0].company").value("Test company"));
    mvc.perform(
            post("/api/public/inquiries")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void websiteSettingsPersistAndRejectUnsafeValues() throws Exception {
    String settings = """
        {"companyName":"测试企业","companyNameEn":"Test Company","companyIntro":"介绍",
         "companyIntroEn":"Introduction","address":"地址","addressEn":"Address",
         "contactEmail":"info@example.com","contactPhone":"+84123456789",
         "defaultLocale":"en","supportUrl":"https://example.com/support"}
        """;
    for (int i = 0; i < 2; i++) {
      mvc.perform(put("/api/admin/site-settings").with(user("admin").roles("ADMIN"))
              .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(settings))
          .andExpect(status().isOk());
    }
    mvc.perform(get("/api/public/site-settings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.companyName").value("测试企业"))
        .andExpect(jsonPath("$.data.defaultLocale").value("en"));
    mvc.perform(put("/api/admin/site-settings").with(user("admin").roles("ADMIN"))
            .with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(settings.replace("https://example.com/support", "javascript:alert(1)")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void exportsActualOpenApiWhenRequested() throws Exception {
    String spec =
        mvc.perform(get("/v3/api-docs").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    org.junit.jupiter.api.Assertions.assertTrue(json.readTree(spec).path("paths").size() > 0);
    String destination = System.getProperty("openapi.export");
    if (destination != null) Files.writeString(Path.of(destination), spec);
  }
}
