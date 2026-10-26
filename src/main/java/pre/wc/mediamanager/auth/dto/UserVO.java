package pre.wc.mediamanager.auth.dto;

import pre.wc.mediamanager.entity.User;

import java.time.LocalDateTime;

public record UserVO(
        Long id,
        String username,
        String nickname,
        String role,
        LocalDateTime lastLoginTime
) {
    public static UserVO from(User user) {
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(),
                user.getRole(), user.getLastLoginTime());
    }
}
