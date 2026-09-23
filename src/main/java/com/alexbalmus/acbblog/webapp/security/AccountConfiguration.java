package com.alexbalmus.acbblog.webapp.security;

import java.util.Set;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EntityScan(basePackageClasses = Account.class)
@EnableJpaRepositories(basePackageClasses = AccountRepository.class)
public class AccountConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService accountUserDetailsService(AccountRepository accounts) {
        return username -> {
            var account = accounts.findById(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
            return User.withUsername(account.username()).password(account.passwordHash())
                    .disabled(!account.enabled()).authorities(account.authorities().toArray(String[]::new)).build();
        };
    }

    @Bean
    @Profile("Dev")
    ApplicationRunner seedAccounts(AccountRepository accounts, PasswordEncoder encoder, Environment environment) {
        return args -> {
            for (var username : new String[] {"sven", "dick", "bob", "joe"}) {
                if (accounts.existsById(username)) continue;
                var password = environment.getProperty("acb.security.seed." + username + ".password");
                if (password == null || password.isBlank()) continue;
                var roles = username.equals("sven") ? Set.of("ROLE_USER", "ROLE_ADMIN") : Set.of("ROLE_USER");
                accounts.saveAndFlush(new Account(username, encoder.encode(password), true, roles));
            }
        };
    }
}
