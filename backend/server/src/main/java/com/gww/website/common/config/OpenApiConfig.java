package com.gww.website.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Session认证说明及由安全过滤器实现的退出接口。 */
@Configuration
public class OpenApiConfig {
  @Bean
  OpenAPI definition() {
    return new OpenAPI()
        .info(
            new Info().title("官网管理后台").version("1.0.0").description("同源Session认证；所有写请求需携带CSRF请求头。"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "session",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("GWWSESSION")));
  }

  @Bean
  OpenApiCustomizer securityDocumentation() {
    return api -> {
      api.path(
          "/api/auth/logout",
          new PathItem()
              .post(
                  new Operation()
                      .operationId("logout")
                      .summary("退出登录并失效Session")
                      .responses(
                          new ApiResponses()
                              .addApiResponse(
                                  "200", new ApiResponse().description("统一Result响应，data为null")))));
      api.getPaths()
          .forEach(
              (path, item) ->
                  item.readOperations()
                      .forEach(
                          operation -> {
                            if (path.startsWith("/api/admin/")
                                || path.equals("/api/auth/me")
                                || path.equals("/api/auth/logout")) {
                              operation.addSecurityItem(
                                  new SecurityRequirement().addList("session"));
                            }
                            operation
                                .getResponses()
                                .addApiResponse("400", new ApiResponse().description("参数校验失败"));
                            operation
                                .getResponses()
                                .addApiResponse("401", new ApiResponse().description("未登录或凭证错误"));
                            operation
                                .getResponses()
                                .addApiResponse(
                                    "403", new ApiResponse().description("权限不足或CSRF无效"));
                            operation
                                .getResponses()
                                .addApiResponse("500", new ApiResponse().description("服务异常"));
                          }));
    };
  }
}
