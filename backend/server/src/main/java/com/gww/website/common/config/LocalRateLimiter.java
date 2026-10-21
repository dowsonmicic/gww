package com.gww.website.common.config;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 单机固定窗口限流；重启会清空，多实例部署需切换回共享限流。 */
@Component
public class LocalRateLimiter {
  private record Window(long expiresAt, int count) {}
  private final Map<String, Window> windows = new HashMap<>();
  private final Clock clock;

  public LocalRateLimiter() {
    this(Clock.systemUTC());
  }

  LocalRateLimiter(Clock clock) {
    this.clock = clock;
  }

  public synchronized boolean allow(String key, int limit, Duration duration) {
    long now = clock.millis();
    windows.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
    Window current = windows.get(key);
    // 限制活跃IP占用的内存，超限时拒绝新key而非无限增长。
    if (current == null && windows.size() >= 10000) return false;
    if (current == null) current = new Window(now + duration.toMillis(), 0);
    if (current.count() >= limit) return false;
    windows.put(key, new Window(current.expiresAt(), current.count() + 1));
    return true;
  }

  public synchronized void reset(String key) {
    windows.remove(key);
  }
}
