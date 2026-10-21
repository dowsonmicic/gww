package com.gww.website.common.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class LocalConfigurationTest {
  @Test
  void plainPasswordIsEncodedOnlyInDev() {
    var encoder = new BCryptPasswordEncoder();
    var environment = new MockEnvironment();
    environment.setActiveProfiles("dev");
    var users = new SecurityConfig().users("admin", "", "test-only", encoder, environment);
    assertTrue(encoder.matches("test-only", users.loadUserByUsername("admin").getPassword()));
    environment.setActiveProfiles("prod");
    assertThrows(IllegalStateException.class,
        () -> new SecurityConfig().users("admin", "", "test-only", encoder, environment));
    environment.setActiveProfiles("dev", "prod");
    assertThrows(IllegalStateException.class,
        () -> new SecurityConfig().users("admin", "", "test-only", encoder, environment));
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
