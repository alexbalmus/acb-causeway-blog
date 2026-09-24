package com.alexbalmus.acbblog.webapp.blog;

import java.util.List;
import java.util.Map;
import gg.jte.Content;
import gg.jte.html.HtmlContent;

/** Presentation-only values; templates never receive managed entities or account records. */
public record BlogPage(String kind, String title, String username, String handle, boolean editing,
        String panel, String query, List<BlogRow> blogs, BlogRow blog, PostView post,
        List<PostRow> posts, Map<String, String> values, Map<String, String> errors, String message) {
    public record BlogRow(long id, String name, String handle, boolean owned) {}
    public record PostRow(long id, String title) {}
    public record PostView(long id, String title, String content, String description,
            boolean hasPicture, Content pictureHtml, Content contentHtml) {}
    public String value(String name, String fallback) { return values.getOrDefault(name, fallback == null ? "" : fallback); }
    public String error(String name) { return errors.getOrDefault(name, ""); }
    public String url() {
        return switch (kind) {
            case "my" -> "/blog/my";
            case "blog" -> "/blog/blogs/" + blog.id() + (editing ? "/edit" : "");
            case "post" -> "/blog/posts/" + post.id() + (editing ? "/edit" : "");
            default -> "/blog";
        };
    }
    // Only call with HTML from our escaping MarkupSupport/ImageSupport builders.
    static Content generatedHtml(String html) { return (HtmlContent) output -> output.writeContent(html); }
}
