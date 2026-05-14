package haonguyen.taskflow_be.security;

import haonguyen.taskflow_be.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {
    private final Long id;
    private final String email;
    private final String actualUsername;
    private final String password;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.actualUsername = user.getUsername();
        this.password = user.getPassword();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getActualUsername() { return actualUsername; }

    // Spring Security uses getUsername() for the principal name — we use email
    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return password; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
