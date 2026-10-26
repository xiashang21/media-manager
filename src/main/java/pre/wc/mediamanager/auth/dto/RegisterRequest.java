package pre.wc.mediamanager.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "请输入用户名")
        @Size(min = 3, max = 32, message = "用户名长度必须为 3-32 个字符")
        String username,
        @NotBlank(message = "请输入密码")
        @Size(min = 6, max = 64, message = "密码长度必须为 6-64 个字符")
        String password,
        @Size(max = 50, message = "昵称不能超过 50 个字符")
        String nickname
) {
}
