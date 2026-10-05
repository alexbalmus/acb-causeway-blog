package com.alexbalmus.acbblog.webapp.blog;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;
import org.apache.causeway.applib.services.iactnlayer.InteractionContext;
import org.apache.causeway.core.security.authentication.AuthenticationRequestPassword;
import org.apache.causeway.core.security.authentication.manager.AuthenticationManager;
import org.springframework.stereotype.Component;

/** A browser session for the MVC viewer, authenticated by the configured Causeway realm. */
@Component
public class BlogSession {
    private static final String IDENTITY = BlogSession.class.getName() + ".identity";
    private static final String CSRF = BlogSession.class.getName() + ".csrf";
    private static final String RETURN_TO = BlogSession.class.getName() + ".returnTo";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AuthenticationManager authentication;
    private final HttpServletRequest request;

    public BlogSession(AuthenticationManager authentication, HttpServletRequest request) {
        this.authentication = authentication;
        this.request = request;
    }
    public InteractionContext current() {
        var session = request.getSession(false);
        if (session == null) return null;
        var identity = (Identity) session.getAttribute(IDENTITY);
        if (identity == null) return null;
        if (authentication.isSessionValid(identity.context)) return identity.context;
        session.removeAttribute(IDENTITY);
        return null;
    }
    public boolean login(String username, String password) {
        var context = authentication.authenticate(new AuthenticationRequestPassword(username, password));
        if (context == null) return false;
        var session = request.getSession();
        request.changeSessionId();
        session.setAttribute(IDENTITY, new Identity(context, authentication));
        session.removeAttribute(CSRF);
        return true;
    }
    public void logout() {
        var session = request.getSession(false);
        if (session == null) return;
        session.removeAttribute(IDENTITY); // closes the Causeway authentication code too
        session.removeAttribute(CSRF);
        session.removeAttribute(RETURN_TO);
        session.removeAttribute("blog.message");
        request.changeSessionId();
        // Wicket may have its own state in this servlet session. Do not sign it out implicitly.
    }
    public BlogCsrfToken csrf() {
        var session = request.getSession();
        synchronized (session) {
            var token = (BlogCsrfToken) session.getAttribute(CSRF);
            if (token == null) {
                byte[] bytes = new byte[32];
                RANDOM.nextBytes(bytes);
                token = new BlogCsrfToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
                session.setAttribute(CSRF, token);
            }
            return token;
        }
    }
    public boolean validCsrf() {
        var session = request.getSession(false);
        var expected = session == null ? null : (BlogCsrfToken) session.getAttribute(CSRF);
        String supplied = request.getHeader("X-Blog-CSRF");
        if (supplied == null) supplied = request.getParameter("_csrf");
        return expected != null && supplied != null && MessageDigest.isEqual(
                expected.token().getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }
    public void rememberEditor() {
        String path = request.getServletPath();
        if (!request.getMethod().equals("GET") || !safeEditor(path)) return;
        String panel = request.getParameter("panel");
        String target = path + (panel != null && panel.matches("[a-z-]{1,30}") ? "?panel=" + panel : "");
        request.getSession().setAttribute(RETURN_TO, target);
    }
    public String takeReturnTo() {
        var session = request.getSession();
        String target = (String) session.getAttribute(RETURN_TO);
        session.removeAttribute(RETURN_TO);
        return target == null ? "/blog/my" : target;
    }
    private static boolean safeEditor(String path) {
        return path.equals("/blog/my") || path.matches("/blog/(blogs|posts)/[0-9]+/edit");
    }
    private static final class Identity implements HttpSessionBindingListener {
        private final InteractionContext context;
        private final AuthenticationManager authentication;
        private Identity(InteractionContext context, AuthenticationManager authentication) {
            this.context = context; this.authentication = authentication;
        }
        @Override public void valueUnbound(HttpSessionBindingEvent event) {
            authentication.closeSession(context.getUser());
        }
    }
}
