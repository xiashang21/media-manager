package pre.wc.mediamanager.auth.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresAt,
        UserVO user
) {
}
