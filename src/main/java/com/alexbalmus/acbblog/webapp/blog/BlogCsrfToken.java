package com.alexbalmus.acbblog.webapp.blog;

/** Synchronizer token kept in the server session, exposed only in rendered forms. */
public record BlogCsrfToken(String token) {
    public String getParameterName() { return "_csrf"; }
    public String getToken() { return token; }
}
