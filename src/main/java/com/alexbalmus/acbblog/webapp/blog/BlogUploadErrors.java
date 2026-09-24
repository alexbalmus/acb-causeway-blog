package com.alexbalmus.acbblog.webapp.blog;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.ModelAndView;

/** Multipart parsing can fail before MVC has selected a controller. */
@ControllerAdvice
public class BlogUploadErrors {
    @ExceptionHandler(MultipartException.class)
    ModelAndView upload(MultipartException exception, HttpServletRequest request, HttpServletResponse response) {
        if (!request.getServletPath().startsWith("/blog/")) throw exception;
        boolean size = exception instanceof org.springframework.web.multipart.MaxUploadSizeExceededException;
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            size |= cause.getClass().getSimpleName().contains("SizeLimitExceeded");
        }
        response.setStatus(size ? 413 : 400);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Vary", "HX-Request");
        var view = new ModelAndView("blog/error");
        view.addObject("message", size ? "The upload is too large. Choose a smaller picture and try again."
                : "The upload could not be read. Please select the picture again.");
        String path = request.getServletPath();
        view.addObject("back", path.matches("/blog/(blogs|posts)/[0-9]+/.*")
                ? path.substring(0, path.lastIndexOf('/')) : "/blog/my");
        view.addObject("fragment", "true".equals(request.getHeader("HX-Request")));
        view.addObject("csrf", request.getAttribute("_csrf"));
        return view;
    }
}
