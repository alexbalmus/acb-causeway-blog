package com.alexbalmus.acbblog.webapp.blog;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.ModelAndView;

/** Security belongs only to this MVC viewer; Causeway's Wicket/REST filters stay untouched. */
@Configuration
public class BlogWebConfiguration implements WebMvcConfigurer {
    private final BlogSession session;
    public BlogWebConfiguration(BlogSession session) { this.session = session; }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                response.setHeader("Cache-Control", "no-store");
                response.setHeader("Vary", "HX-Request");
                response.setHeader("X-Content-Type-Options", "nosniff");
                response.setHeader("X-Frame-Options", "SAMEORIGIN");
                String path = request.getServletPath();
                boolean read = request.getMethod().equals("GET") || request.getMethod().equals("HEAD");
                boolean publicPage = path.equals("/blog/login") || (read && (path.equals("/blog") || path.equals("/blog/")
                        || path.matches("/blog/(blogs|posts)/[0-9]+")));
                if (!publicPage && session.current() == null) {
                    session.rememberEditor();
                    loginRedirect(request, response, "/blog/login", 401);
                    return false;
                }
                if (!read && !session.validCsrf()) {
                    if ("true".equals(request.getHeader("HX-Request"))) {
                        loginRedirect(request, response, "/blog/login?expired", 403);
                    } else {
                        response.setStatus(403);
                        response.setContentType("text/html;charset=UTF-8");
                        response.getWriter().write("<!doctype html><html lang=\"en\"><title>Form expired</title><p>This form expired or its CSRF token is missing. <a href=\"/blog/login?expired\">Sign in and try again</a>.</p></html>");
                    }
                    return false;
                }
                request.setAttribute("_csrf", session.csrf());
                return true;
            }
            @Override public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView view) {
                if (view != null) view.addObject("csrf", session.csrf());
            }
        }).addPathPatterns("/blog", "/blog/**").excludePathPatterns("/blog/assets/**");
    }
    private static void loginRedirect(HttpServletRequest request, HttpServletResponse response, String target, int status) throws java.io.IOException {
        if ("true".equals(request.getHeader("HX-Request"))) {
            response.setHeader("HX-Redirect", target);
            response.setStatus(status);
        } else response.sendRedirect(target);
    }
}
