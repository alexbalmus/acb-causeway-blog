package com.alexbalmus.acbblog.webapp.blog;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

/** Deliberately excludes owner, entity ID and all persistence fields from form binding. */
public class BlogForm {
    private String name;
    private String handle;
    private String content;
    private String description;
    private MultipartFile picture;
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getHandle() { return handle; }
    public void setHandle(String value) { handle = value; }
    public String getContent() { return content; }
    public void setContent(String value) { content = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public MultipartFile getPicture() { return picture; }
    public void setPicture(MultipartFile value) { picture = value; }
    public Map<String, String> values() {
        var values = new LinkedHashMap<String, String>();
        if (name != null) values.put("name", name);
        if (handle != null) values.put("handle", handle);
        if (content != null) values.put("content", content);
        if (description != null) values.put("description", description);
        return values;
    }
    public static String text(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
