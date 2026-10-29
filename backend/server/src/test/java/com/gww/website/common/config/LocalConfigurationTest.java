package com.gww.website.common.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class LocalConfigurationTest {
  @Test
  void adminPasswordAutoDetectsPlainOrBcrypt() {
    var encoder = new BCryptPasswordEncoder();
    // 明文 → 自动 BCrypt 编码
    var plainUsers = new SecurityConfig().users("admin", "test-only", encoder);
    assertTrue(encoder.matches("test-only", plainUsers.loadUserByUsername("admin").getPassword()));

    // BCrypt 哈希 → 原样通过
    var hash = encoder.encode("already-hashed");
    var hashUsers = new SecurityConfig().users("admin", hash, encoder);
    assertEquals(hash, hashUsers.loadUserByUsername("admin").getPassword());
  }

  @Test
  void localLimiterRejectsAtLimitAndResetRestoresAccess() {
    var limiter = new LocalRateLimiter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    assertTrue(limiter.allow("ip", 2, Duration.ofMinutes(1)));
    assertTrue(limiter.allow("ip", 2, Duration.ofMinutes(1)));
    assertFalse(limiter.allow("ip", 2, Duration.ofMinutes(1)));
    limiter.reset("ip");
    assertTrue(limiter.allow("ip", 2, Duration.ofMinutes(1)));
    assertTrue(limiter.allow("expired", 1, Duration.ZERO));
    assertTrue(limiter.allow("expired", 1, Duration.ZERO));
  }
}
