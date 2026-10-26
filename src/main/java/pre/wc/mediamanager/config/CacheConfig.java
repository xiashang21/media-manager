package pre.wc.mediamanager.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pre.wc.mediamanager.cache.LoggingCacheManager;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String VIDEO_PAGE = "videoPage";
    public static final String VIDEO_DETAIL = "videoDetail";
    public static final String VIDEO_CATEGORIES = "videoCategories";
    public static final String VIDEO_HOT = "videoHot";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager delegate = new CaffeineCacheManager(
                VIDEO_PAGE, VIDEO_DETAIL, VIDEO_CATEGORIES, VIDEO_HOT);
        delegate.setCaffeine(Caffeine.newBuilder()
                .maximumSize(2_000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats());
        delegate.setAllowNullValues(false);
        return new LoggingCacheManager(delegate);
    }
}
