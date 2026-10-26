package pre.wc.mediamanager.security;

public record UserPrincipal(Long id, String username, String nickname, String role) {

    public String displayName() {
        return nickname == null || nickname.isBlank() ? username : nickname;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
