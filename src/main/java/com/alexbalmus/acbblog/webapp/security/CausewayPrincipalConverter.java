package com.alexbalmus.acbblog.webapp.security;

import org.apache.causeway.applib.services.user.UserMemento;
import org.apache.causeway.security.spring.authconverters.AuthenticationConverter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class CausewayPrincipalConverter implements AuthenticationConverter {
    @Override
    public UserMemento convert(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) return null;
        return UserMemento.ofNameAndRoleNames(authentication.getName(),
                        authentication.getAuthorities().stream().map(a -> a.getAuthority()).toArray(String[]::new))
                .withRoleAdded(UserMemento.AUTHORIZED_USER_ROLE)
                .withAuthenticationSource(UserMemento.AuthenticationSource.EXTERNAL);
    }
}
