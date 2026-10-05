package com.alexbalmus.acbblog.webapp.blog;

public class BlogAccessDeniedException extends RuntimeException {
    public BlogAccessDeniedException(String message) { super(message); }
}
