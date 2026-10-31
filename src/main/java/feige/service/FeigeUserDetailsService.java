package feige.service;

import feige.domain.UserAccount;
import feige.domain.FeigeUser;
import feige.repo.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class FeigeUserDetailsService implements UserDetailsService {

    private final UserAccountRepository users;

    public FeigeUserDetailsService(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        if (username == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        UserAccount account = users.findByUsernameKey(username.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("用户名或密码错误"));
        return new FeigeUser(account);
    }
}
