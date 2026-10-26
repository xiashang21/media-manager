package pre.wc.mediamanager.security;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pre.wc.mediamanager.common.UnauthorizedException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 轻量级 JWT（HS256）实现。令牌包含用户身份与过期时间，服务端只保存签名密钥，
 * 不保存会话，适合当前单体视频服务的无状态鉴权场景。
 */
@Component
public class TokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HEADER = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;

    public TokenService(ObjectMapper objectMapper,
                        @Value("${app.jwt.secret}") String secret,
                        @Value("${app.jwt.expiration-minutes:120}") long expirationMinutes) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret 至少需要 32 字节");
        }
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = Math.max(1, expirationMinutes) * 60;
    }

    public IssuedToken issue(Long userId, String username, String nickname, String role) {
        long now = Instant.now().getEpochSecond();
        long expiresAt = now + expirationSeconds;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", String.valueOf(userId));
        payload.put("username", username);
        payload.put("nickname", nickname);
        payload.put("role", role);
        payload.put("iat", now);
        payload.put("exp", expiresAt);
        payload.put("jti", UUID.randomUUID().toString().replace("-", ""));

        try {
            String body = encode(objectMapper.writeValueAsBytes(payload));
            String content = HEADER + "." + body;
            String signature = sign(content);
            return new IssuedToken(content + "." + signature, expiresAt);
        } catch (Exception e) {
            throw new IllegalStateException("登录令牌生成失败", e);
        }
    }

    public UserPrincipal parse(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("登录凭证缺失");
        }
        String[] segments = token.split("\\.");
        if (segments.length != 3) {
            throw new UnauthorizedException("登录凭证格式错误");
        }
        String content = segments[0] + "." + segments[1];
        if (!MessageDigest.isEqual(sign(content).getBytes(StandardCharsets.US_ASCII),
                segments[2].getBytes(StandardCharsets.US_ASCII))) {
            throw new UnauthorizedException("登录凭证签名无效");
        }
        try {
            JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(segments[1]));
            long exp = payload.path("exp").asLong(0);
            if (exp <= Instant.now().getEpochSecond()) {
                throw new UnauthorizedException("登录已过期，请重新登录");
            }
            Long userId = Long.valueOf(payload.path("sub").asText());
            String username = payload.path("username").asText();
            String nickname = payload.path("nickname").asText(null);
            String role = payload.path("role").asText("USER");
            return new UserPrincipal(userId, username, nickname, role);
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("登录凭证无效");
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return encode(mac.doFinal(content.getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception e) {
            throw new IllegalStateException("登录令牌签名失败", e);
        }
    }

    private static String encode(String value) {
        return encode(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    public record IssuedToken(String value, long expiresAt) {
    }
}
