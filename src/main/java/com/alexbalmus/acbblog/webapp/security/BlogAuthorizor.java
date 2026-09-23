package com.alexbalmus.acbblog.webapp.security;

import java.util.Set;
import org.apache.causeway.applib.Identifier;
import org.apache.causeway.applib.services.iactn.InteractionContext;
import org.apache.causeway.core.security.authorization.Authorizor;
import org.springframework.stereotype.Component;

@Component
public class BlogAuthorizor implements Authorizor {
    private static final Set<String> PRESENTATION_TYPES = Set.of(
            "causeway.applib.DomainObjectList", "causeway.applib.UserMenu",
            "causeway.applib.UserMemento", "causeway.applib.RoleMemento",
            "causeway.security.LoginRedirect", "acb.security.SessionMenu");

    @Override public boolean isVisible(InteractionContext context, Identifier identifier) {
        return allowed(context, identifier);
    }
    @Override public boolean isUsable(InteractionContext context, Identifier identifier) {
        return allowed(context, identifier);
    }
    private boolean allowed(InteractionContext context, Identifier identifier) {
        if (context == null || context.getUser() == null) return false;
        var type = identifier.logicalType().logicalName();
        // The built-in SAFE logout action mutates a session through GET. Use our POST action instead.
        if (type.equals("causeway.security.LogoutMenu") || type.contains("SecurityAccount")) return false;
        var roles = context.getUser().streamRoleNames().collect(java.util.stream.Collectors.toSet());
        if (roles.contains("ROLE_ADMIN")) return true;
        return roles.contains("ROLE_USER") && (type.startsWith("blog.") || PRESENTATION_TYPES.contains(type));
    }
}
