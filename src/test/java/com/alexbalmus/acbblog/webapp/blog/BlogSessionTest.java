package com.alexbalmus.acbblog.webapp.blog;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.apache.causeway.applib.services.iactn.InteractionContext;
import org.apache.causeway.applib.services.user.UserMemento;
import org.apache.causeway.core.security.authentication.manager.AuthenticationManager;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class BlogSessionTest {
    @Test void sessionExpiryClosesCausewayAuthenticationAndLoginRotatesSecrets() {
        var request = new MockHttpServletRequest();
        var authentication = mock(AuthenticationManager.class);
        var context = InteractionContext.ofUserWithSystemDefaults(UserMemento.ofName("writer"));
        when(authentication.authenticate(any())).thenReturn(context);
        when(authentication.isSessionValid(context)).thenReturn(true);
        var session = new BlogSession(authentication, request);
        String csrf = session.csrf().token();
        String sessionId = request.getSession().getId();
        assertThat(session.login("writer", "test-password")).isTrue();
        assertThat(request.getSession().getId()).isNotEqualTo(sessionId);
        assertThat(session.csrf().token()).isNotEqualTo(csrf);
        assertThat(session.current()).isSameAs(context);
        ((MockHttpSession) request.getSession()).invalidate();
        verify(authentication).closeSession(context.getUser());
        assertThat(session.current()).isNull();
    }
    @Test void invalidCausewaySessionIsNotTrustedAndReturnTargetsAreLocalGetEditors() {
        var request = new MockHttpServletRequest();
        var authentication = mock(AuthenticationManager.class);
        var context = InteractionContext.ofUserWithSystemDefaults(UserMemento.ofName("writer"));
        when(authentication.authenticate(any())).thenReturn(context);
        var session = new BlogSession(authentication, request);
        session.login("writer", "test-password");
        assertThat(session.current()).isNull();
        verify(authentication).closeSession(context.getUser());
        request.setMethod("GET"); request.setServletPath("/blog/posts/42/edit");
        request.setParameter("panel", "content"); session.rememberEditor();
        assertThat(session.takeReturnTo()).isEqualTo("/blog/posts/42/edit?panel=content");
        request.setServletPath("//example.com"); session.rememberEditor();
        assertThat(session.takeReturnTo()).isEqualTo("/blog/my");
        request.setMethod("POST"); request.setServletPath("/blog/posts/42/edit"); session.rememberEditor();
        assertThat(session.takeReturnTo()).isEqualTo("/blog/my");
    }
    @Test void logoutPreservesOtherViewersStateAndCsrfNeedsTheSameSession() {
        var request = new MockHttpServletRequest();
        var session = new BlogSession(mock(AuthenticationManager.class), request);
        request.getSession().setAttribute("wicket-state", "untouched");
        String token = session.csrf().token();
        assertThat(session.validCsrf()).isFalse();
        request.setParameter("_csrf", token);
        assertThat(session.validCsrf()).isTrue();
        session.logout();
        assertThat(session.validCsrf()).isFalse();
        assertThat(request.getSession().getAttribute("wicket-state")).isEqualTo("untouched");
        var other = new MockHttpServletRequest(); other.setParameter("_csrf", token);
        assertThat(new BlogSession(mock(AuthenticationManager.class), other).validCsrf()).isFalse();
    }
}
