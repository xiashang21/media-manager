package pre.wc.mediamanager.security;

import pre.wc.mediamanager.common.UnauthorizedException;

public final class UserContext {

    private static final ThreadLocal<UserPrincipal> CURRENT = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(UserPrincipal principal) {
        CURRENT.set(principal);
    }

    public static UserPrincipal get() {
        return CURRENT.get();
    }

    public static UserPrincipal requireUser() {
        UserPrincipal principal = CURRENT.get();
        if (principal == null) {
            throw new UnauthorizedException("请先登录后再进行该操作");
        }
        return principal;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
