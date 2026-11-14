package feige.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

public class FeigeUser implements UserDetails, Serializable {

    private static final long serialVersionUID = 2L;

    private final long id;
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final String nickname;
    private final String avatar;

    public FeigeUser(UserAccount account) {
        this.id = account.getId();
        this.username = account.getUsername();
        this.passwordHash = account.getPasswordHash();
        this.role = account.getRole();
        this.nickname = account.getNickname();
        this.avatar = account.getAvatar();
    }

    public long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof FeigeUser)) {
            return false;
        }
        return username.equalsIgnoreCase(((FeigeUser) other).username);
    }

    @Override
    public int hashCode() {
        return username.toLowerCase(Locale.ROOT).hashCode();
    }
}
