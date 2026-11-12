package feige.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers(HttpMethod.POST, "/api/register").permitAll()
                .antMatchers(HttpMethod.GET, "/api/theme").permitAll()
                .antMatchers("/login", "/register", "/css/**", "/js/**", "/favicon.svg", "/favicon.png", "/hero-mark.png", "/hero-mark.svg", "/dove-icon.svg").permitAll()
                .antMatchers("/admin", "/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
                .and()
                .formLogin()
                .loginPage("/login")
                .defaultSuccessUrl("/chat", true)
                .failureUrl("/login?error")
                .permitAll()
                .and()
                .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .and()
                .csrf()
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .and()
                .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) -> {
                    String uri = request.getRequestURI();
                    if (uri.startsWith("/api/") || "/ws".equals(uri)) {
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED).commence(request, response, authException);
                    } else {
                        new LoginUrlAuthenticationEntryPoint("/login").commence(request, response, authException);
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        writeJson(response, 403, "没有权限");
                        return;
                    }
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                });
        return http.build();
    }

    private static void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
