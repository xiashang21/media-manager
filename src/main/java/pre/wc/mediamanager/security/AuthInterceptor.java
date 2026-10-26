package pre.wc.mediamanager.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import pre.wc.mediamanager.common.UnauthorizedException;
import pre.wc.mediamanager.entity.User;
import pre.wc.mediamanager.mapper.UserMapper;

import java.time.Duration;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final Pattern NUMERIC_ID_PATH = Pattern.compile("^/api/video/\\d+/?$");
    private static final Pattern PUBLIC_PLAY_PATH = Pattern.compile("^/api/video/\\d+/play/?$");

    private final TokenService tokenService;
    private final UserMapper userMapper;

    /** 用户状态短缓存，避免每个请求都回查数据库，又能让禁用状态在 30 秒内生效。 */
    private final Cache<Long, Boolean> userStatusCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofSeconds(30))
            .build();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        UserPrincipal principal = null;
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization)) {
            if (!authorization.startsWith("Bearer ")) {
                throw new UnauthorizedException("Authorization 必须使用 Bearer 令牌");
            }
            principal = tokenService.parse(authorization.substring(7).trim());
            assertUserActive(principal.id());
            UserContext.set(principal);
            MDC.put("userId", String.valueOf(principal.id()));
        }

        if (principal == null && !isPublicRequest(request)) {
            throw new UnauthorizedException("请先登录后再进行该操作");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // userId 的 MDC 清理交给 TraceIdFilter 在写完请求汇总日志后统一执行，
        // 这样 “HTTP xxx” 汇总行也能保留用户身份，形成完整链路。
        UserContext.clear();
    }

    private void assertUserActive(Long userId) {
        Boolean active = userStatusCache.getIfPresent(userId);
        if (active == null) {
            User user = userMapper.selectById(userId);
            active = user != null && user.getStatus() != null && user.getStatus() == 1;
            userStatusCache.put(userId, active);
        }
        if (!active) {
            throw new UnauthorizedException("账号不存在或已被禁用，请重新登录");
        }
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return true;
        }
        if ("POST".equals(method)
                && (path.equals("/api/auth/login") || path.equals("/api/auth/register"))) {
            return true;
        }
        // 播放计数不涉及用户数据修改，允许匿名播放视频时累加播放量。
        if ("POST".equals(method) && PUBLIC_PLAY_PATH.matcher(path).matches()) {
            return true;
        }
        if (!"GET".equals(method)) {
            return false;
        }
        if (path.equals("/api/auth/me") || path.equals("/api/auth/logout")) {
            return false;
        }
        if (path.equals("/api/v1/info")
                || path.equals("/api/video/page")
                || path.equals("/api/video/hot")
                || path.equals("/api/video/categories")) {
            return true;
        }
        if (path.startsWith("/api/video/stream/") || path.startsWith("/api/video/cover/")) {
            return true;
        }
        return NUMERIC_ID_PATH.matcher(path).matches();
    }
}
