package com.alexbalmus.acbblog.webapp.security;

import jakarta.servlet.DispatcherType;
import org.apache.causeway.security.spring.authentication.AuthenticatorSpring;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

@Configuration
// Import only the authenticator: the stock module installs a /* interaction filter and disables CSRF.
@Import(AuthenticatorSpring.class)
public class SecurityConfiguration {
    @Bean
    DaoAuthenticationProvider authenticationProvider(UserDetailsService users, PasswordEncoder passwords) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(passwords);
        return provider;
    }

    private void configureSession(HttpSecurity http, boolean secureCookies) throws Exception {
        var repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.path("/").sameSite("Lax").secure(secureCookies));
        var xor = new XorCsrfTokenRequestAttributeHandler();
        var plain = new CsrfTokenRequestAttributeHandler();
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId()))
                .csrf(csrf -> csrf.csrfTokenRepository(repository)
                        .csrfTokenRequestHandler(new org.springframework.security.web.csrf.CsrfTokenRequestHandler() {
                            @Override public void handle(jakarta.servlet.http.HttpServletRequest request,
                                    jakarta.servlet.http.HttpServletResponse response,
                                    java.util.function.Supplier<org.springframework.security.web.csrf.CsrfToken> token) {
                                xor.handle(request, response, token);
                                token.get(); // Materialize the cookie for Angular and Wicket AJAX.
                            }
                            @Override public String resolveCsrfTokenValue(jakarta.servlet.http.HttpServletRequest request,
                                    org.springframework.security.web.csrf.CsrfToken token) {
                                return request.getHeader(token.getHeaderName()) != null || request.getServletPath().startsWith("/wicket")
                                        ? plain.resolveCsrfTokenValue(request, token)
                                        : xor.resolveCsrfTokenValue(request, token);
                            }
                        }))
                .httpBasic(basic -> basic.disable())
                .rememberMe(remember -> remember.disable());
    }

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurity(HttpSecurity http,
            @Value("${acb.security.secure-cookies:false}") boolean secureCookies) throws Exception {
        configureSession(http, secureCookies);
        http.securityMatcher("/api/**", "/restful", "/restful/**", "/graphql", "/graphql/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/restful/user/logout", "/restful/user/logout/").denyAll()
                        .requestMatchers("/restful/swagger/**", "/graphql/schema/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .requestCache(cache -> cache.disable())
                .formLogin(login -> login.loginPage("/login").loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) -> response.setStatus(204))
                        .failureHandler((request, response, exception) -> response.setStatus(401)))
                .logout(logout -> logout.logoutUrl("/api/auth/logout")
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> response.setStatus(401))
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write(exception instanceof org.springframework.security.web.csrf.CsrfException
                                    ? "{\"error\":\"csrf\"}" : "{\"error\":\"forbidden\"}");
                        }));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain blogSecurity(HttpSecurity http,
            @Value("${acb.security.secure-cookies:false}") boolean secureCookies) throws Exception {
        configureSession(http, secureCookies);
        var cache = new org.springframework.security.web.savedrequest.HttpSessionRequestCache();
        cache.setRequestMatcher(request -> request.getMethod().equals("GET")
                && request.getServletPath().startsWith("/blog/")
                && !request.getServletPath().equals("/blog/login"));
        http.securityMatcher("/blog", "/blog/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/blog", "/blog/", "/blog/login", "/blog/assets/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/blog/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/blog/blogs/{id}", "/blog/posts/{id}").permitAll()
                        .anyRequest().authenticated())
                .requestCache(requests -> requests.requestCache(cache))
                .formLogin(login -> login.loginPage("/blog/login").loginProcessingUrl("/blog/login")
                        .failureUrl("/blog/login?error")
                        .successHandler((request, response, authentication) -> {
                            var saved = cache.getRequest(request, response);
                            String target = "/blog/my";
                            if (saved != null) {
                                var uri = java.net.URI.create(saved.getRedirectUrl());
                                String path = uri.getRawPath();
                                if (path.startsWith("/blog/") && !path.contains("%") && !path.contains("\\"))
                                    target = path + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
                            }
                            cache.removeRequest(request, response);
                            response.sendRedirect(target);
                        }))
                .logout(logout -> logout.logoutUrl("/blog/logout")
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN").logoutSuccessUrl("/blog"))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            if ("true".equals(request.getHeader("HX-Request"))) {
                                response.setHeader("HX-Redirect", "/blog/login");
                                response.setStatus(401);
                            } else response.sendRedirect("/blog/login");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            if ("true".equals(request.getHeader("HX-Request"))
                                    && exception instanceof org.springframework.security.web.csrf.CsrfException) {
                                response.setHeader("HX-Redirect", "/blog/login?expired");
                                response.setStatus(403);
                            } else response.sendError(403);
                        }));
        return http.build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain browserSecurity(HttpSecurity http,
            @Value("${acb.security.secure-cookies:false}") boolean secureCookies) throws Exception {
        configureSession(http, secureCookies);
        http.authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/", "/index.html", "/login", "/logout", "/favicon.ico",
                                "/images/**", "/css/**", "/scripts/**", "/webjars/**").permitAll()
                        .requestMatchers("/wicket", "/wicket/**").authenticated()
                        .requestMatchers("/actuator/**", "/graphiql", "/graphiql/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .formLogin(login -> login.defaultSuccessUrl("/wicket/", true).permitAll())
                .logout(logout -> logout.deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .logoutSuccessUrl("/login?logout").permitAll());
        return http.build();
    }
}
