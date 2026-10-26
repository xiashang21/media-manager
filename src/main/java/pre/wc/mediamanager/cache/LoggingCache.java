package pre.wc.mediamanager.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

import java.util.concurrent.Callable;

/**
 * 在 Spring Cache 外层增加统一日志，便于在日志中直接看到缓存 key 与 HIT/MISS。
 */
@Slf4j
public class LoggingCache implements Cache {

    private final Cache delegate;

    public LoggingCache(Cache delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Object getNativeCache() {
        return delegate.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper wrapper = delegate.get(key);
        log.info("CACHE {} name={} key={}", wrapper == null ? "MISS" : "HIT", getName(), key);
        return wrapper;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        T value = delegate.get(key, type);
        log.info("CACHE {} name={} key={}", value == null ? "MISS" : "HIT", getName(), key);
        return value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        T value = delegate.get(key, valueLoader);
        log.info("CACHE LOAD name={} key={}", getName(), key);
        return value;
    }

    @Override
    public void put(Object key, Object value) {
        delegate.put(key, value);
        log.info("CACHE PUT name={} key={}", getName(), key);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper wrapper = delegate.putIfAbsent(key, value);
        if (wrapper == null) {
            log.info("CACHE PUT_IF_ABSENT name={} key={}", getName(), key);
        } else {
            log.info("CACHE HIT name={} key={}", getName(), key);
        }
        return wrapper;
    }

    @Override
    public void evict(Object key) {
        delegate.evict(key);
        log.info("CACHE EVICT name={} key={}", getName(), key);
    }

    @Override
    public boolean evictIfPresent(Object key) {
        boolean removed = delegate.evictIfPresent(key);
        log.info("CACHE EVICT name={} key={} removed={}", getName(), key, removed);
        return removed;
    }

    @Override
    public void clear() {
        delegate.clear();
        log.info("CACHE CLEAR name={}", getName());
    }

    @Override
    public boolean invalidate() {
        boolean invalidated = delegate.invalidate();
        log.info("CACHE INVALIDATE name={} invalidated={}", getName(), invalidated);
        return invalidated;
    }
}
