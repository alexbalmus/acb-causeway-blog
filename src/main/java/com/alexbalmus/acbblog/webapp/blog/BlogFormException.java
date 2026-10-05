package com.alexbalmus.acbblog.webapp.blog;

import java.util.Map;

public class BlogFormException extends RuntimeException {
    private final Map<String, String> errors;
    public BlogFormException(String field, String message) {
        super(message);
        errors = Map.of(field, message);
    }
    public Map<String, String> errors() { return errors; }
}
