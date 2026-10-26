package pre.wc.mediamanager.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pre.wc.mediamanager.auth.dto.AuthResponse;
import pre.wc.mediamanager.auth.dto.LoginRequest;
import pre.wc.mediamanager.auth.dto.RegisterRequest;
import pre.wc.mediamanager.common.UnauthorizedException;
import pre.wc.mediamanager.entity.User;
import pre.wc.mediamanager.mapper.UserMapper;
import pre.wc.mediamanager.security.LoginAttemptService;
import pre.wc.mediamanager.security.TokenService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final TokenService tokenService = mock(TokenService.class);
    private final LoginAttemptService loginAttemptService = mock(LoginAttemptService.class);
    private final AuthServiceImpl service = new AuthServiceImpl(userMapper, passwordEncoder, tokenService, loginAttemptService);

    @Test
    void registerStoresBcryptHashAndReturnsToken() {
        when(userMapper.selectByUsername("alice")).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(5L);
            return 1;
        });
        when(tokenService.issue(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(new TokenService.IssuedToken("signed-token", 123456789L));

        AuthResponse response = service.register(new RegisterRequest("Alice", "secret123", "Alice"));

        assertThat(response.token()).isEqualTo("signed-token");
        assertThat(response.user().id()).isEqualTo(5L);
        assertThat(response.user().username()).isEqualTo("alice");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches("secret123", captor.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = new User();
        user.setId(2L);
        user.setUsername("alice");
        user.setNickname("Alice");
        user.setRole("USER");
        user.setStatus(1);
        user.setPasswordHash(passwordEncoder.encode("correct-password"));
        when(userMapper.selectByUsername("alice")).thenReturn(user);

        assertThatThrownBy(() -> service.login(new LoginRequest("alice", "wrong-password")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("用户名或密码错误");
    }
}
