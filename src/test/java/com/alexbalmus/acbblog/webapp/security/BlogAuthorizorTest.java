package com.alexbalmus.acbblog.webapp.security;

import org.apache.causeway.applib.Identifier;
import org.apache.causeway.applib.id.LogicalType;
import org.apache.causeway.applib.services.iactn.InteractionContext;
import org.apache.causeway.applib.services.user.UserMemento;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class BlogAuthorizorTest {
    final BlogAuthorizor authorizor = new BlogAuthorizor();
    final CausewayPrincipalConverter converter = new CausewayPrincipalConverter();

    @Test void convertsOnlyAuthenticatedPrincipalsAndPreservesRoles() {
        assertThat(converter.convert(null)).isNull();
        assertThat(converter.convert(new UsernamePasswordAuthenticationToken("alice", "secret"))).isNull();
        assertThat(converter.convert(new AnonymousAuthenticationToken("key", "anonymous",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))))).isNull();
        var user = converter.convert(UsernamePasswordAuthenticationToken.authenticated("alice", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        assertThat(user.name()).isEqualTo("alice");
        assertThat(user.streamRoleNames()).contains("ROLE_USER", UserMemento.AUTHORIZED_USER_ROLE);
        assertThat(user.authenticationSource()).isEqualTo(UserMemento.AuthenticationSource.EXTERNAL);
    }

    @Test void grantsBlogAndPresentationAccessWithoutGrantingAdministration() {
        var user = InteractionContext.ofUserWithSystemDefaults(UserMemento.ofNameAndRoleNames("alice", "ROLE_USER"));
        var admin = InteractionContext.ofUserWithSystemDefaults(UserMemento.ofNameAndRoleNames("admin", "ROLE_ADMIN"));
        assertThat(authorizor.isUsable(user, id("blog.Blog"))).isTrue();
        assertThat(authorizor.isVisible(user, id("causeway.applib.DomainObjectList"))).isTrue();
        assertThat(authorizor.isVisible(user, id("causeway.conf.ConfigurationMenu"))).isFalse();
        assertThat(authorizor.isVisible(admin, id("causeway.conf.ConfigurationMenu"))).isTrue();
        assertThat(authorizor.isUsable(admin, id("causeway.security.LogoutMenu"))).isFalse();
        assertThat(authorizor.isUsable(null, id("blog.Blog"))).isFalse();
    }

    private Identifier id(String name) {
        return Identifier.classIdentifier(LogicalType.eager(Object.class, name));
    }
}
