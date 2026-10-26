package pre.wc.mediamanager.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LoggingCacheManager implements CacheManager {

    private final CaffeineCacheManager delegate;
    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>();

    public LoggingCacheManager(CaffeineCacheManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, key -> {
            Cache cache = delegate.getCache(key);
            return cache == null ? null : new LoggingCache(cache);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }
}
