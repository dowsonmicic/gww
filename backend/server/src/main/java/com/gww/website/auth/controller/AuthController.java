package com.gww.website.auth.controller;

import com.gww.website.common.exception.BusinessException;
import com.gww.website.common.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import com.gww.website.common.config.LocalRateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final UserDetailsService users;
  private final PasswordEncoder encoder;
  private final LocalRateLimiter limiter;

  public AuthController(
      UserDetailsService users, PasswordEncoder encoder, LocalRateLimiter limiter) {
    this.users = users;
    this.encoder = encoder;
    this.limiter = limiter;
  }

  @Schema(description = "登录参数")
  public record LoginRequest(
      @NotBlank @Size(max = 80) String username, @NotBlank @Size(max = 200) String password) {}

  @Schema(description = "CSRF请求头及令牌")
  public record TokenVo(String headerName, String token) {}

  public record UserVo(String username, String role) {}

  @GetMapping("/csrf")
  @Operation(summary = "获取CSRF令牌，写请求需携带此请求头和Session Cookie")
  public Result<TokenVo> csrf(CsrfToken token) {
    return Result.ok(new TokenVo(token.getHeaderName(), token.getToken()));
  }

  @PostMapping("/login")
  @Operation(summary = "管理员登录")
  public Result<UserVo> login(
      @Valid @RequestBody LoginRequest dto, HttpServletRequest req, HttpServletResponse res) {
    // 限流使用真实连接IP；不信任任意客户端X-Forwarded-For。
    String key = "auth:attempt:" + req.getRemoteAddr();
    if (!limiter.allow(key, 10, Duration.ofMinutes(10)))
      throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "登录尝试过于频繁");
    try {
      var user = users.loadUserByUsername(dto.username());
      if (!encoder.matches(dto.password(), user.getPassword()))
        throw new BadCredentialsException("invalid");
      req.getSession();
      req.changeSessionId();
      var context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(
          UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
      // 登录提升权限后废弃旧CSRF令牌，前端须重新获取。
      new CsrfAuthenticationStrategy(new HttpSessionCsrfTokenRepository())
          .onAuthentication(context.getAuthentication(), req, res);
      SecurityContextHolder.setContext(context);
      new HttpSessionSecurityContextRepository().saveContext(context, req, res);
      limiter.reset(key);
      log.info("管理员登录成功 username={}", user.getUsername());
      return Result.ok(new UserVo(user.getUsername(), "ADMIN"));
    } catch (org.springframework.security.core.AuthenticationException ex) {
      log.warn("管理员登录失败");
      throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
    }
  }

  @GetMapping("/me")
  @Operation(summary = "查询当前管理员")
  public Result<UserVo> me(java.security.Principal principal) {
    return Result.ok(new UserVo(principal.getName(), "ADMIN"));
  }
}
