package pre.wc.mediamanager.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import pre.wc.mediamanager.auth.dto.AuthResponse;
import pre.wc.mediamanager.auth.dto.LoginRequest;
import pre.wc.mediamanager.auth.dto.RegisterRequest;
import pre.wc.mediamanager.auth.dto.UserVO;
import pre.wc.mediamanager.common.BusinessException;
import pre.wc.mediamanager.common.UnauthorizedException;
import pre.wc.mediamanager.entity.User;
import pre.wc.mediamanager.mapper.UserMapper;
import pre.wc.mediamanager.security.LoginAttemptService;
import pre.wc.mediamanager.security.TokenService;
import pre.wc.mediamanager.security.UserContext;
import pre.wc.mediamanager.security.UserPrincipal;
import pre.wc.mediamanager.service.AuthService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Marker SECURITY = MarkerFactory.getMarker("SECURITY");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,32}$");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LoginAttemptService loginAttemptService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        validateUsername(username);
        String password = request.password();
        if (password == null || password.length() < 6 || password.length() > 64) {
            throw new BusinessException("密码长度必须为 6-64 个字符");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException("密码内容过长，请使用不超过 72 字节的密码");
        }
        if (userMapper.selectByUsername(username) != null) {
            throw new BusinessException(409, "用户名已存在，请更换一个");
        }

        String nickname = StringUtils.hasText(request.nickname())
                ? request.nickname().trim() : username;
        if (nickname.length() > 50) {
            throw new BusinessException("昵称不能超过 50 个字符");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setRole("USER");
        user.setStatus(1);
        try {
            if (userMapper.insert(user) != 1) {
                throw new BusinessException("注册失败，请稍后重试");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(409, "用户名已存在，请更换一个");
        }
        log.info(SECURITY, "用户注册成功 userId={} username={} role={}",
                user.getId(), user.getUsername(), user.getRole());
        return authResponse(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse login(LoginRequest request) {
        String username = normalizeUsername(request.username());
        String attemptKey = username + "|" + clientIp();
        try {
            loginAttemptService.checkAllowed(attemptKey);
        } catch (BusinessException e) {
            log.warn(SECURITY, "登录被限流 username={} reason=连续失败次数过多", username);
            throw e;
        }

        User user = userMapper.selectByUsername(username);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(attemptKey);
            log.warn(SECURITY, "用户登录失败 username={} reason=用户名或密码错误", username);
            throw new UnauthorizedException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            loginAttemptService.recordFailure(attemptKey);
            log.warn(SECURITY, "用户登录失败 userId={} username={} reason=账号已禁用",
                    user.getId(), username);
            throw new UnauthorizedException("账号已被禁用");
        }
        loginAttemptService.clear(attemptKey);
        userMapper.updateLastLogin(user.getId());
        user.setLastLoginTime(LocalDateTime.now());
        log.info(SECURITY, "用户登录成功 userId={} username={} role={}",
                user.getId(), user.getUsername(), user.getRole());
        return authResponse(user);
    }

    private String clientIp() {
        String ip = MDC.get("clientIp");
        return ip == null || ip.isBlank() ? "unknown" : ip;
    }

    @Override
    public UserVO currentUser() {
        UserPrincipal principal = UserContext.requireUser();
        User user = userMapper.selectById(principal.id());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new UnauthorizedException("登录用户不存在或已被禁用");
        }
        return UserVO.from(user);
    }

    private AuthResponse authResponse(User user) {
        TokenService.IssuedToken token = tokenService.issue(
                user.getId(), user.getUsername(), user.getNickname(), user.getRole());
        return new AuthResponse(token.value(), "Bearer", token.expiresAt(), UserVO.from(user));
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            throw new BusinessException("请输入用户名");
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private void validateUsername(String username) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BusinessException("用户名只能包含字母、数字、下划线或短横线，长度为 3-32 个字符");
        }
    }
}
