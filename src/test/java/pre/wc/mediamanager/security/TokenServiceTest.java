package pre.wc.mediamanager.security;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import pre.wc.mediamanager.common.UnauthorizedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private TokenService tokenService() {
        return new TokenService(new ObjectMapper(), "0123456789abcdef0123456789abcdef", 120);
    }

    @Test
    void issuesAndParsesSignedToken() {
        TokenService service = tokenService();

        TokenService.IssuedToken issued = service.issue(7L, "alice", "Alice", "USER");
        UserPrincipal principal = service.parse(issued.value());

        assertThat(principal.id()).isEqualTo(7L);
        assertThat(principal.username()).isEqualTo("alice");
        assertThat(principal.displayName()).isEqualTo("Alice");
        assertThat(principal.isAdmin()).isFalse();
        assertThat(issued.expiresAt()).isGreaterThan(System.currentTimeMillis() / 1000);
    }

    @Test
    void rejectsTamperedToken() {
        TokenService service = tokenService();
        String token = service.issue(7L, "alice", "Alice", "USER").value();
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        assertThatThrownBy(() -> service.parse(tampered))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("签名");
    }

    @Test
    void rejectsMalformedToken() {
        assertThatThrownBy(() -> tokenService().parse("not-a-jwt"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("格式");
    }
}
