package com.alexbalmus.acbblog.webapp.security;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.causeway.applib.services.iactn.InteractionContext;
import org.apache.causeway.applib.services.iactn.InteractionService;
import org.apache.causeway.viewer.graphql.applib.auth.UserMementoProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class CausewaySessionConfiguration {
    private static final String GRAPHQL_USER = "acb.authenticatedUser";

    @Bean
    FilterRegistrationBean<OncePerRequestFilter> causewaySessionBridge(
            InteractionService interactions, CausewayPrincipalConverter converter) {
        var filter = new OncePerRequestFilter() {
            @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                    FilterChain chain) throws IOException, ServletException {
                var user = converter.convert(SecurityContextHolder.getContext().getAuthentication());
                if (user == null) { response.setStatus(401); return; }
                interactions.runAndCatch(InteractionContext.ofUserWithSystemDefaults(user),
                        () -> chain.doFilter(request, response)).ifFailureFail();
            }
        };
        var registration = new FilterRegistrationBean<OncePerRequestFilter>(filter);
        registration.setName("acbCausewaySessionBridge");
        registration.setOrder(-90); // After Spring Security (-100), before Causeway viewer filters.
        registration.addUrlPatterns("/wicket/*");
        return registration;
    }

    @Bean
    WebGraphQlInterceptor graphqlIdentity(CausewayPrincipalConverter converter) {
        return (request, chain) -> {
            var user = converter.convert(SecurityContextHolder.getContext().getAuthentication());
            if (user == null) return reactor.core.publisher.Mono.error(new IllegalStateException("Unauthenticated GraphQL request"));
            request.configureExecutionInput((input, builder) -> builder.graphQLContext(context ->
                    context.put(GRAPHQL_USER, user)).build());
            return chain.next(request);
        };
    }

    @Bean
    @org.springframework.context.annotation.Primary
    UserMementoProvider graphqlUserMementoProvider() {
        return (context, parameters) -> {
            org.apache.causeway.applib.services.user.UserMemento user = context.getGraphQLContext().get(GRAPHQL_USER);
            if (user == null) throw new IllegalStateException("Missing authenticated GraphQL identity");
            return user;
        };
    }
}
