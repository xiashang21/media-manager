package pre.wc.mediamanager.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;
import pre.wc.mediamanager.common.BusinessException;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于 Caffeine 的登录失败计数，防止同一用户名/IP 被暴力破解。
 * 10 分钟内连续失败达到 5 次后，后续登录直接返回 429。
 */
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final Cache<String, AtomicInteger> attempts = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(WINDOW)
            .build();

    public void checkAllowed(String key) {
        AtomicInteger counter = attempts.getIfPresent(key);
        if (counter != null && counter.get() >= MAX_ATTEMPTS) {
            throw new BusinessException(429, "登录失败次数过多，请 10 分钟后再试");
        }
    }

    public void recordFailure(String key) {
        attempts.get(key, k -> new AtomicInteger()).incrementAndGet();
    }

    public void clear(String key) {
        attempts.invalidate(key);
    }
}
