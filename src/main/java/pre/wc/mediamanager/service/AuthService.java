package pre.wc.mediamanager.service;

import pre.wc.mediamanager.auth.dto.AuthResponse;
import pre.wc.mediamanager.auth.dto.LoginRequest;
import pre.wc.mediamanager.auth.dto.RegisterRequest;
import pre.wc.mediamanager.auth.dto.UserVO;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserVO currentUser();
}
