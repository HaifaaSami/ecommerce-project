package ecommerce.security;

import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** الهوية المصادَق عليها (تؤخذ من التوكن فقط، لا من جسم الطلب). */
public record AuthUser(Long id, String username, String role) {
    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
    public boolean isAdmin() { return "ADMIN".equals(role); }
}
