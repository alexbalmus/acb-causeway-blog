package com.alexbalmus.acbblog.webapp.security;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthenticationController {
    @GetMapping("/api/auth/csrf")
    public void csrf(CsrfToken token) { token.getToken(); }

    public record Identity(String userName, List<String> roles) {}

    @GetMapping("/api/auth/me")
    public Identity me(Authentication authentication) {
        return new Identity(authentication.getName(),
                authentication.getAuthorities().stream().map(a -> a.getAuthority()).sorted().toList());
    }
}
