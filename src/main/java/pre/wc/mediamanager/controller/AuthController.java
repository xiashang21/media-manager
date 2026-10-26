package pre.wc.mediamanager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pre.wc.mediamanager.auth.dto.AuthResponse;
import pre.wc.mediamanager.auth.dto.LoginRequest;
import pre.wc.mediamanager.auth.dto.RegisterRequest;
import pre.wc.mediamanager.auth.dto.UserVO;
import pre.wc.mediamanager.common.Result;
import pre.wc.mediamanager.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(authService.currentUser());
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT 为无状态令牌，客户端删除本地凭证即可完成退出。
        return Result.success();
    }
}
