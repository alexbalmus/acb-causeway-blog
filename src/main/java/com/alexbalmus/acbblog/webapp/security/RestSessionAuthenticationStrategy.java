package com.alexbalmus.acbblog.webapp.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.causeway.applib.services.iactn.InteractionContext;
import org.apache.causeway.viewer.restfulobjects.viewer.webmodule.auth.AuthenticationStrategy;
import org.springframework.security.core.context.SecurityContextHolder;

/** Constructed by Causeway's REST filter; Spring has already validated the session. */
public class RestSessionAuthenticationStrategy implements AuthenticationStrategy {
    @Override public InteractionContext lookupValid(HttpServletRequest request, HttpServletResponse response) {
        var user = new CausewayPrincipalConverter().convert(SecurityContextHolder.getContext().getAuthentication());
        return user == null ? null : InteractionContext.ofUserWithSystemDefaults(user);
    }
    @Override public void bind(HttpServletRequest request, HttpServletResponse response, InteractionContext context) {
        // Never cache a second identity alongside Spring's SecurityContext.
    }
    @Override public void invalidate(HttpServletRequest request, HttpServletResponse response) {
        // Legacy GET logout must not mutate authentication. Logout is handled by Spring's POST endpoints.
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
