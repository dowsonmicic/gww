package com.gww.website.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gww.website.common.vo.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/** Session认证与CSRF保护；生产域名应经Nginx同源转发。 */
@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder encoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService users(
      @Value("${app.admin.username}") String username,
      @Value("${app.admin.password-hash}") String hash,
      @Value("${app.admin.password:}") String password,
      PasswordEncoder encoder,
      Environment environment) {
    if (hash.isBlank() && !password.isBlank()
        && (environment.acceptsProfiles(Profiles.of("dev", "local"))
            && !environment.acceptsProfiles(Profiles.of("prod")))) {
      hash = encoder.encode(password);
    }
    if (!hash.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}")) {
      throw new IllegalStateException("请配置ADMIN_PASSWORD_HASH；dev环境也可配置ADMIN_PASSWORD");
    }
    return new InMemoryUserDetailsManager(
        User.withUsername(username).password(hash).roles("ADMIN").build());
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, ObjectMapper mapper) throws Exception {
    http.csrf(csrf -> csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository()));
    http.authorizeHttpRequests(
        auth ->
            auth.requestMatchers("/api/auth/csrf", "/api/auth/login", "/api/public/**")
                .permitAll()
                .requestMatchers("/doc.html", "/webjars/**", "/v3/api-docs/**", "/swagger-ui/**")
                .hasRole("ADMIN")
                .anyRequest()
                .hasRole("ADMIN"));
    http.exceptionHandling(
        errors ->
            errors
                .authenticationEntryPoint(
                    (req, res, ex) -> {
                      res.setStatus(401);
                      res.setContentType("application/json;charset=UTF-8");
                      mapper.writeValue(res.getWriter(), Result.error(401, "请先登录"));
                    })
                .accessDeniedHandler(
                    (req, res, ex) -> {
                      res.setStatus(403);
                      res.setContentType("application/json;charset=UTF-8");
                      mapper.writeValue(res.getWriter(), Result.error(403, "权限不足或CSRF令牌无效"));
                    }));
    http.logout(
        logout ->
            logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(
                    (req, res, auth) -> {
                      res.setContentType("application/json;charset=UTF-8");
                      mapper.writeValue(res.getWriter(), Result.ok(null));
                    })
                .invalidateHttpSession(true)
                .deleteCookies("GWWSESSION"));
    return http.build();
  }
}
