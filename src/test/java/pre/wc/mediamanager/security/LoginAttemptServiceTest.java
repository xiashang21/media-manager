package pre.wc.mediamanager.security;

import org.junit.jupiter.api.Test;
import pre.wc.mediamanager.common.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {

    @Test
    void blocksAfterMaxAttempts() {
        LoginAttemptService service = new LoginAttemptService();
        for (int i = 0; i < 5; i++) {
            service.recordFailure("alice|127.0.0.1");
        }

        assertThatThrownBy(() -> service.checkAllowed("alice|127.0.0.1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(429));
    }

    @Test
    void clearResetsCounterAfterSuccessfulLogin() {
        LoginAttemptService service = new LoginAttemptService();
        for (int i = 0; i < 5; i++) {
            service.recordFailure("alice|127.0.0.1");
        }
        service.clear("alice|127.0.0.1");

        service.checkAllowed("alice|127.0.0.1");
    }

    @Test
    void differentKeysDoNotInterfere() {
        LoginAttemptService service = new LoginAttemptService();
        for (int i = 0; i < 5; i++) {
            service.recordFailure("alice|127.0.0.1");
        }

        service.checkAllowed("bob|127.0.0.1");
    }
}
