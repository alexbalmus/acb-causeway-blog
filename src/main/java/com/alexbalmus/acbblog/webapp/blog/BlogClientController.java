package com.alexbalmus.acbblog.webapp.blog;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.causeway.applib.services.wrapper.InvalidException;
import org.apache.causeway.applib.services.wrapper.DisabledException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/blog")
public class BlogClientController {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(BlogClientController.class);
    private final BlogClientService service;
    public BlogClientController(BlogClientService service) { this.service = service; }
    @ModelAttribute("csrf")
    org.springframework.security.web.csrf.CsrfToken csrf(HttpServletRequest request) {
        return (org.springframework.security.web.csrf.CsrfToken) request.getAttribute("_csrf");
    }

    @GetMapping({"", "/"})
    String directory(@RequestParam(defaultValue = "") String q, Model model, HttpServletRequest request,
            HttpServletResponse response) {
        return page("directory", null, false, "", q, Map.of(), Map.of(), null, model, request, response);
    }
    @GetMapping("/my")
    String my(@RequestParam(defaultValue = "") String panel, Model model, HttpServletRequest request,
            HttpServletResponse response) {
        return page("my", null, true, panel, "", Map.of(), Map.of(), null, model, request, response);
    }
    @GetMapping({"/blogs/{id}", "/blogs/{id}/edit"})
    String blog(@PathVariable long id, @RequestParam(defaultValue = "") String panel, Model model,
            HttpServletRequest request, HttpServletResponse response) {
        return page("blog", id, request.getServletPath().endsWith("/edit"), panel, "", Map.of(), Map.of(), null, model, request, response);
    }
    @GetMapping({"/posts/{id}", "/posts/{id}/edit"})
    String post(@PathVariable long id, @RequestParam(defaultValue = "") String panel, Model model,
            HttpServletRequest request, HttpServletResponse response) {
        return page("post", id, request.getServletPath().endsWith("/edit"), panel, "", Map.of(), Map.of(), null, model, request, response);
    }
    @GetMapping("/login")
    String login(Model model, HttpServletRequest request, HttpServletResponse response) {
        model.addAttribute("error", request.getParameter("error") != null ? "Invalid username or password."
                : request.getParameter("expired") != null ? "Your session or form expired. Sign in and try again." : "");
        response.setHeader("Cache-Control", "no-store");
        return "blog/login";
    }
    @PostMapping("/my/{action}")
    String changeMy(@PathVariable String action, @ModelAttribute BlogForm form, Model model,
            HttpServletRequest request, HttpServletResponse response) {
        return change("my", null, action, form, model, request, response);
    }
    @PostMapping("/blogs/{id}/{action}")
    String changeBlog(@PathVariable long id, @PathVariable String action, @ModelAttribute BlogForm form,
            Model model, HttpServletRequest request, HttpServletResponse response) {
        return change("blog", id, action, form, model, request, response);
    }
    @PostMapping("/posts/{id}/{action}")
    String changePost(@PathVariable long id, @PathVariable String action, @ModelAttribute BlogForm form,
            Model model, HttpServletRequest request, HttpServletResponse response) {
        return change("post", id, action, form, model, request, response);
    }
    private String change(String kind, Long id, String action, BlogForm form, Model model,
            HttpServletRequest request, HttpServletResponse response) {
        try {
            String destination = service.mutate(kind, id, action, form);
            request.getSession().setAttribute("blog.message", "Changes saved.");
            if (htmx(request)) {
                response.setHeader("HX-Location", "{\"path\":\"" + destination + "\",\"target\":\"#blog-content\",\"swap\":\"outerHTML\"}");
                response.setStatus(204);
                return null;
            }
            response.setStatus(303);
            response.setHeader("Location", destination);
            return null;
        } catch (BlogFormException ex) {
            return invalid(kind, id, action, form, ex.errors(), model, request, response);
        } catch (InvalidException ex) {
            String field = switch (action) {
                case "rename", "create-post" -> "name";
                case "handle" -> "handle";
                case "content" -> "content";
                default -> "general";
            };
            return invalid(kind, id, action, form, Map.of(field, ex.getMessage()), model, request, response);
        } catch (DataIntegrityViolationException ex) {
            return invalid(kind, id, action, form, Map.of("general", "This name or handle is already in use."), model, request, response);
        } catch (RuntimeException ex) {
            // Domain action bodies use IllegalArgumentException for business-rule vetoes,
            // sometimes nested in a Causeway invocation exception (as in the other viewers).
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (cause instanceof IllegalArgumentException && cause.getMessage() != null) {
                    return invalid(kind, id, action, form, Map.of("general", cause.getMessage()), model, request, response);
                }
            }
            throw ex;
        }
    }
    private String invalid(String kind, Long id, String action, BlogForm form, Map<String, String> errors,
            Model model, HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(422);
        String message = form.getPicture() != null && !form.getPicture().isEmpty()
                ? "Please select your picture again before submitting." : null;
        return page(kind, id, true, action, "", form.values(), errors, message, model, request, response);
    }
    private String page(String kind, Long id, boolean editing, String panel, String query,
            Map<String, String> values, Map<String, String> errors, String message, Model model,
            HttpServletRequest request, HttpServletResponse response) {
        if (message == null && request.getSession(false) != null) {
            message = (String) request.getSession().getAttribute("blog.message");
            request.getSession().removeAttribute("blog.message");
        }
        model.addAttribute("page", service.page(kind, id, editing, panel, query, values, errors, message));
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Vary", "HX-Request");
        return htmx(request) ? "blog/content" : "blog/page";
    }
    private boolean htmx(HttpServletRequest request) {
        return "true".equals(request.getHeader("HX-Request"))
                && !"true".equals(request.getHeader("HX-History-Restore-Request"));
    }
    @ExceptionHandler(Exception.class)
    String failure(Exception ex, Model model, HttpServletRequest request, HttpServletResponse response) {
        int status = 500;
        String message = "The request could not be completed. Please try again.";
        if (ex instanceof AccessDeniedException || ex instanceof DisabledException) {
            status = 403; message = "Only the blog owner can do this.";
        } else if (ex instanceof org.springframework.web.method.annotation.MethodArgumentTypeMismatchException) {
            status = 404; message = "This page could not be found.";
        } else if (ex instanceof ResponseStatusException error) {
            status = error.getStatusCode().value(); message = "This page could not be found.";
        } else if (ex instanceof MaxUploadSizeExceededException) {
            status = 413; message = "The upload is too large. Choose a smaller picture and try again.";
        } else if (ex instanceof ObjectOptimisticLockingFailureException) {
            status = 409; message = "This item changed while you were editing. Reload it before trying again.";
        } else LOG.error("Blog client request failed", ex);
        response.setStatus(status);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Vary", "HX-Request");
        model.addAttribute("message", message);
        String path = request.getServletPath();
        String back = path.matches("/blog/(blogs|posts)/[0-9]+/.*")
                ? path.substring(0, path.lastIndexOf('/')) : "/blog";
        model.addAttribute("back", back);
        model.addAttribute("fragment", htmx(request));
        return "blog/error";
    }
}
