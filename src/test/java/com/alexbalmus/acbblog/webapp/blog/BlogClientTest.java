package com.alexbalmus.acbblog.webapp.blog;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.alexbalmus.acbblog.webapp.ACBBlogApp;
import com.alexbalmus.acbblog.webapp.security.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.alexbalmus.acbblog.modules.blog.common.post.safety.*;
import com.alexbalmus.acbblog.modules.blog.common.post.picture.PictureDescriptionGenerator;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = ACBBlogApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.datasource.url=jdbc:h2:mem:blogclient;DATABASE_TO_UPPER=false",
                "eclipselink.application-location=target", "spring.ai.openai.api-key=test-only"})
@ActiveProfiles("Ai")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class BlogClientTest {
    @LocalServerPort int port;
    @Autowired AccountRepository accounts;
    @Autowired PasswordEncoder passwords;
    @MockitoBean PostSafetyChecker safety;
    @MockitoBean PictureDescriptionGenerator descriptions;
    @BeforeEach void seed() {
        when(safety.assess(any(), any())).thenReturn(SafetyAssessment.allowed());
        when(descriptions.generateFor(any())).thenReturn(Optional.of("Generated picture caption"));
        for (String name : List.of("writer", "reader", "administrator")) {
            if (!accounts.existsById(name)) accounts.saveAndFlush(new Account(name, passwords.encode("test-password"), true,
                    name.equals("administrator") ? Set.of("ROLE_USER", "ROLE_ADMIN") : Set.of("ROLE_USER")));
        }
    }
    @Test void completeWorkflowPublicReadingAndOwnership() throws Exception {
        var owner = new Browser();
        owner.login("writer");
        String unique = "Blog-" + UUID.randomUUID();
        var created = owner.post("/blog/my/create", Map.of("name", unique, "handle", "writer"), true, false);
        assertThat(created.statusCode()).as(created.body()).isEqualTo(303);
        String edit = created.headers().firstValue("Location").orElseThrow();
        String blog = edit.replace("/edit", "");
        assertThat(owner.get(edit).body()).contains(unique, "New Post");
        var anonymous = new Browser();
        var directory = anonymous.get("/blog");
        assertThat(directory.statusCode()).as(directory.body()).isEqualTo(200);
        assertThat(directory.body()).contains(unique, "<!doctype html>").doesNotContain("passwordHash");
        assertThat(anonymous.get(blog).body()).contains("Edit").doesNotContain("Delete Blog");
        assertThat(anonymous.get("/restful/services/blog.Blogs").statusCode()).isEqualTo(401);
        assertThat(anonymous.post(blog + "/rename", Map.of("name", "hacked"), true, false).statusCode()).isEqualTo(302);
        assertThat(owner.post(blog + "/rename", Map.of("name", "missing-csrf"), false, false).statusCode()).isEqualTo(403);
        for (String user : List.of("reader", "administrator")) {
            var other = new Browser(); other.login(user);
            assertThat(other.post(blog + "/rename", Map.of("name", "hacked"), true, true).statusCode()).isEqualTo(403);
            assertThat(other.get(edit).body()).contains("Only the blog owner");
        }
        var invalid = owner.post(blog + "/rename", Map.of("name", ""), true, true);
        assertThat(invalid.statusCode()).isEqualTo(422);
        assertThat(invalid.body()).contains("This field is required.").doesNotContain("<!doctype html>");
        var postCreated = owner.post(blog + "/create-post", Map.of("name", "First post", "content", "<script>alert(1)</script>\n\nSecond paragraph"), true, false);
        assertThat(postCreated.statusCode()).as(postCreated.body()).isEqualTo(303);
        String postEdit = postCreated.headers().firstValue("Location").orElseThrow();
        String post = postEdit.replace("/edit", "");
        var duplicate = owner.post(blog + "/create-post", Map.of("name", "First post"), true, true);
        assertThat(duplicate.statusCode()).as(duplicate.body()).isEqualTo(422);
        var article = anonymous.get(post);
        assertThat(article.body()).contains("&lt;script&gt;", "Second paragraph").doesNotContain("<script>alert(1)</script>");
        for (var operation : Map.of("rename", Map.of("name", "Renamed post"), "content", Map.of("content", "Updated body"),
                "description", Map.of("description", "Updated caption")).entrySet()) {
            var response = owner.post(post + "/" + operation.getKey(), operation.getValue(), true, true);
            assertThat(response.statusCode()).as(response.body()).isEqualTo(204);
            assertThat(response.headers().firstValue("HX-Location")).isPresent();
        }
        assertThat(anonymous.get(post).body()).contains("Renamed post", "Updated body");
        when(safety.assess(any(), eq("Blocked content"))).thenReturn(SafetyAssessment.blocked("Test moderation rejection"));
        var blocked = owner.post(post + "/content", Map.of("content", "Blocked content"), true, true);
        assertThat(blocked.statusCode()).as(blocked.body()).isEqualTo(422);
        assertThat(blocked.body()).contains("Test moderation rejection", "Blocked content");
        var blockedCreation = owner.post(blog + "/create-post", Map.of("name", "Rejected draft", "content", "Blocked content"), true, true);
        assertThat(blockedCreation.statusCode()).as(blockedCreation.body()).isEqualTo(422);
        assertThat(blockedCreation.body()).contains("Test moderation rejection", "Rejected draft");
        assertThat(anonymous.get(post).body()).contains("Updated body").doesNotContain("Blocked content");
        byte[] png = png();
        assertThat(owner.upload(post + "/picture", png).statusCode()).isEqualTo(303);
        assertThat(anonymous.get(post).body()).contains("data:image/png;base64,", "Generated picture caption");
        assertThat(owner.upload(post + "/picture", "invalid image".getBytes(StandardCharsets.UTF_8)).statusCode()).isEqualTo(422);
        assertThat(owner.upload(post + "/picture", new byte[6 * 1024 * 1024]).statusCode()).isEqualTo(413);
        assertThat(owner.post(post + "/picture", Map.of("description", "Caption without new image"), true, false).statusCode()).isEqualTo(303);
        assertThat(anonymous.get(post).body()).contains("data:image/png;base64,", "Caption without new image");
        assertThat(owner.post(post + "/clear-picture", Map.of(), true, false).statusCode()).isEqualTo(303);
        assertThat(owner.post(post + "/delete", Map.of(), true, false).statusCode()).isEqualTo(303);
        assertThat(anonymous.get(post).statusCode()).isEqualTo(404);
        assertThat(owner.post(blog + "/delete", Map.of(), true, false).statusCode()).isEqualTo(303);
        assertThat(anonymous.get(blog).statusCode()).isEqualTo(404);
    }
    @Test void handleChangesAndNormalForms() throws Exception {
        var browser = new Browser(); browser.login("reader");
        var newBlog = browser.get("/blog/my?panel=create");
        var created = browser.post("/blog/my/create", Map.of("name", "Reader blog", "handle", "reader", "_csrf", csrf(newBlog.body())), false, false);
        assertThat(created.statusCode()).as(created.body()).isEqualTo(303);
        String blog = URI.create(created.headers().firstValue("Location").orElseThrow()).getPath().replace("/edit", "");
        assertThat(browser.post("/blog/my/handle", Map.of("handle", "reader-new"), true, false).statusCode()).isEqualTo(303);
        assertThat(new Browser().get(blog).body()).contains("reader-new");
        var createAgain = browser.get("/blog/my?panel=create");
        assertThat(createAgain.body()).contains("value=\"reader-new\"", "readonly");
        assertThat(browser.post("/blog/my/create", Map.of("name", "Reader blog", "handle", "reader-new"), true, true).statusCode()).isEqualTo(422);
        assertThat(browser.post(blog + "/delete", Map.of(), true, false).statusCode()).isEqualTo(303);
        assertThat(browser.get("/blog/my?panel=create").body()).contains("value=\"reader-new\"");
    }
    private static byte[] png() throws Exception {
        var output = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(3, 2, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", output);
        return output.toByteArray();
    }
    @Test void loginReturnsToEditorAndSessionsAreShared() throws Exception {
        var browser = new Browser();
        assertThat(browser.get("/blog/my?panel=create").statusCode()).isEqualTo(302);
        var loginPage = browser.get("/blog/login");
        String oldSession = browser.cookie("JSESSIONID");
        String token = csrf(loginPage.body());
        var login = browser.post("/blog/login", Map.of("username", "reader", "password", "test-password", "_csrf", token), false, false);
        assertThat(login.statusCode()).isEqualTo(302);
        assertThat(URI.create(login.headers().firstValue("Location").orElseThrow()).getPath()).isEqualTo("/blog/my");
        assertThat(browser.cookie("JSESSIONID")).isNotEqualTo(oldSession);
        assertThat(browser.get("/api/auth/me").body()).contains("reader");
        assertThat(browser.get("/restful/services/blog.Blogs").statusCode()).isEqualTo(200);
        assertThat(browser.get("/wicket/").statusCode()).isIn(200, 302);
        browser.get("/blog/my");
        assertThat(browser.post("/blog/logout", Map.of(), true, false).statusCode()).isEqualTo(302);
        assertThat(browser.get("/api/auth/me").statusCode()).isEqualTo(401);
        var expired = browser.getHx("/blog/my");
        assertThat(expired.statusCode()).isEqualTo(401);
        assertThat(expired.headers().firstValue("HX-Redirect")).contains("/blog/login");
    }
    @Test void fullPagesFragmentsAssetsAndSearch() throws Exception {
        var browser = new Browser();
        assertThat(browser.get("/blog").body()).contains("<html", "htmx.min.js", "bootstrap.min.css");
        var fragment = browser.getHx("/blog?q=nonexistent");
        assertThat(fragment.statusCode()).isEqualTo(200);
        assertThat(fragment.body()).contains("No blogs found.").doesNotContain("<html");
        assertThat(fragment.headers().firstValue("Vary")).contains("HX-Request");
        assertThat(browser.get("/webjars/htmx.org/2.0.8/dist/htmx.min.js").statusCode()).isEqualTo(200);
        assertThat(browser.get("/webjars/bootswatch/5.3.8/dist/litera/bootstrap.min.css").statusCode()).isEqualTo(200);
        assertThat(browser.get("/blog/assets/blog.js").statusCode()).isEqualTo(200);
    }
    private static String csrf(String html) {
        var match = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(html);
        assertThat(match.find()).as(html).isTrue();
        return match.group(1);
    }
    private class Browser {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();
        String cookie(String name) { return cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals(name)).map(HttpCookie::getValue).findFirst().orElse(""); }
        HttpResponse<String> get(String path) throws Exception { return send("GET", path, "", false, false); }
        HttpResponse<String> getHx(String path) throws Exception { return send("GET", path, "", false, true); }
        void login(String name) throws Exception {
            get("/api/auth/csrf");
            assertThat(post("/api/auth/login", Map.of("username", name, "password", "test-password"), true, false).statusCode()).isEqualTo(204);
            get("/api/auth/csrf");
        }
        HttpResponse<String> post(String path, Map<String, String> fields, boolean csrf, boolean hx) throws Exception {
            String body = fields.entrySet().stream().map(e -> enc(e.getKey()) + "=" + enc(e.getValue())).collect(java.util.stream.Collectors.joining("&"));
            return send("POST", path, body, csrf, hx);
        }
        HttpResponse<String> send(String method, String path, String body, boolean csrf, boolean hx) throws Exception {
            var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
            if (csrf) builder.header("X-XSRF-TOKEN", cookie("XSRF-TOKEN"));
            if (hx) builder.header("HX-Request", "true");
            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        }
        HttpResponse<String> upload(String path, byte[] image) throws Exception {
            String boundary = "BlogTestBoundary";
            var bytes = new java.io.ByteArrayOutputStream();
            bytes.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"picture\"; filename=\"picture.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            bytes.write(image);
            bytes.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("X-XSRF-TOKEN", cookie("XSRF-TOKEN"))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bytes.toByteArray())).build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    }
}
