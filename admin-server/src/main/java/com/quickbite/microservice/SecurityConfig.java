package com.quickbite.microservice;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    private static final Path DEFAULT_STORE = Paths.get(System.getProperty("user.dir"), "admin-store.properties");

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        Path store = Paths.get(System.getenv().getOrDefault("ADMIN_STORE_PATH", DEFAULT_STORE.toString()));
        String email = null;
        String passwordHash = null;

        try {
            if (Files.exists(store)) {
                Properties p = new Properties();
                try (InputStream in = Files.newInputStream(store)) {
                    p.load(in);
                }
                email = p.getProperty("email");
                passwordHash = p.getProperty("passwordHash");
            } else {
                email = System.getenv("ADMIN_EMAIL");
                String envPass = System.getenv("ADMIN_PASSWORD");
                if (email != null && envPass != null) {
                    // do not persist here; encoding in memory for this run
                    passwordHash = encoder.encode(envPass);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to read admin store: " + e.getMessage());
        }

        if (email == null || passwordHash == null) {
            String generated = UUID.randomUUID().toString().substring(0, 8);
            System.out.println("No admin configured; created temporary user 'admin' with password: " + generated);
            UserDetails user = User.withUsername("admin").password(encoder.encode(generated)).roles("ADMIN").build();
            return new InMemoryUserDetailsManager(user);
        }

        UserDetails user = User.withUsername(email).password(passwordHash).roles("ADMIN").build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");

        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/assets/**", "/login").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login").permitAll().successHandler(successHandler))
            .logout(logout -> logout.logoutUrl("/logout"))
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
