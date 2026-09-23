package com.alexbalmus.acbblog.webapp.security;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.time.Duration;

import com.alexbalmus.acbblog.webapp.ACBBlogApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.assertThat;

/** Real servlet requests exercise Spring Security, Causeway filters and EclipseLink together. */
@SpringBootTest(classes = ACBBlogApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.datasource.url=jdbc:h2:mem:security;DATABASE_TO_UPPER=false",
                "eclipselink.application-location=target", "spring.ai.openai.api-key=test-only"})
class SessionSecurityTest {
    @LocalServerPort int port;
    @Autowired AccountRepository accounts;
    @Autowired PasswordEncoder encoder;

    @BeforeEach void seed() {
        for (var name : new String[] {"alice", "bob", "admin", "disabled"}) {
            if (!accounts.existsById(name)) accounts.saveAndFlush(new Account(name, encoder.encode("test-password"),
                    !name.equals("disabled"), name.equals("admin") ? Set.of("ROLE_USER", "ROLE_ADMIN") : Set.of("ROLE_USER")));
        }
    }

    @Test void loginIdentityLogoutAndCsrf() throws Exception {
        var browser = new Browser();
        assertThat(browser.get("/api/auth/me").statusCode()).isEqualTo(401);
        assertThat(browser.get("/restful/user/").statusCode()).isEqualTo(401);
        assertThat(browser.get("/login").statusCode()).isEqualTo(200);
        var oldSession = browser.cookie("JSESSIONID");
        assertThat(browser.post("/api/auth/login", "username=alice&password=test-password", false).statusCode()).isEqualTo(403);
        assertThat(browser.post("/api/auth/login", "username=alice&password=wrong", true).statusCode()).isEqualTo(401);
        assertThat(browser.post("/api/auth/login", "username=disabled&password=test-password", true).statusCode()).isEqualTo(401);
        assertThat(browser.login("alice").statusCode()).isEqualTo(204);
        assertThat(browser.cookie("JSESSIONID")).isNotBlank().isNotEqualTo(oldSession);
        assertThat(browser.get("/api/auth/me").body()).contains("alice", "ROLE_USER").doesNotContain("password");
        var user = browser.get("/restful/user/");
        assertThat(user.statusCode()).as(user.body()).isEqualTo(200);
        assertThat(user.body()).contains("alice", "ROLE_USER");
        assertThat(browser.get("/restful/services/blog.Blogs").statusCode()).isEqualTo(200);
        assertThat(browser.get("/actuator/health").statusCode()).isEqualTo(403);
        assertThat(browser.post("/api/auth/logout", "", false).statusCode()).isEqualTo(403);
        assertThat(browser.post("/api/auth/logout", "", true).statusCode()).isEqualTo(204);
        assertThat(browser.get("/api/auth/me").statusCode()).isEqualTo(401);
        assertThat(browser.get("/restful/user/").statusCode()).isEqualTo(401);
        assertThat(browser.get("/wicket/").statusCode()).isEqualTo(302);
    }

    @Test void restMutationsAndGraphqlRequireCsrfAndAuthentication() throws Exception {
        var browser = new Browser();
        browser.login("alice");
        var payload = "{\"name\":{\"value\":\"Security test\"},\"handle\":{\"value\":\"alice\"}}";
        assertThat(browser.json("POST", "/restful/services/blog.Blogs/actions/create/invoke", payload, false).statusCode()).isEqualTo(403);
        var created = browser.json("POST", "/restful/services/blog.Blogs/actions/create/invoke", payload, true);
        assertThat(created.statusCode()).as(created.body()).isEqualTo(200);
        String id = com.jayway.jsonpath.JsonPath.read(created.body(), "$.instanceId");
        var objectUrl = "/restful/objects/blog.Blog/" + id;
        var renameUrl = objectUrl + "/actions/updateName/invoke";
        var rename = "{\"name\":{\"value\":\"Owner renamed\"}}";
        assertThat(browser.json("PUT", renameUrl, rename, true).statusCode()).isEqualTo(200);
        for (var otherName : new String[] {"bob", "admin"}) {
            var other = new Browser();
            other.login(otherName);
            assertThat(other.get(objectUrl).statusCode()).isEqualTo(200);
            var denied = other.json("PUT", renameUrl, rename, true);
            assertThat(denied.statusCode()).as(denied.body()).isIn(403, 422);
            assertThat(denied.body()).contains("Only the blog owner");
        }
        var query = "{\"query\":\"{ __typename }\"}";
        assertThat(browser.json("POST", "/graphql", query, false).statusCode()).isEqualTo(403);
        var graphql = browser.json("POST", "/graphql", query, true);
        assertThat(graphql.statusCode()).as(graphql.body()).isEqualTo(200);
        assertThat(graphql.body()).contains("data").doesNotContain("errors");
        var anonymous = new Browser();
        anonymous.get("/api/auth/csrf");
        assertThat(anonymous.json("POST", "/graphql", query, true).statusCode()).isEqualTo(401);
    }

    @Test void passwordHashesAndAuthenticationRecordsAreNotExposed() throws Exception {
        var account = accounts.findById("alice").orElseThrow();
        assertThat(account.passwordHash()).startsWith("$2").isNotEqualTo("test-password");
        assertThat(encoder.matches("test-password", account.passwordHash())).isTrue();
        var browser = new Browser();
        browser.login("admin");
        var types = browser.get("/restful/domain-types/");
        assertThat(types.statusCode()).as(types.body()).isEqualTo(200);
        assertThat(types.body()).doesNotContain("SecurityAccount", "webapp.security.Account", "passwordHash");
        assertThat(browser.get("/restful/user/logout").statusCode()).isEqualTo(403);
        assertThat(browser.get("/api/auth/me").statusCode()).isEqualTo(200);
    }

    @Test void wicketUsesTheSameSessionAndRejectsUnprotectedPosts() throws Exception {
        var browser = new Browser();
        browser.login("bob");
        var page = browser.get("/wicket/");
        assertThat(page.statusCode()).as(page.body()).isIn(200, 302);
        for (int redirects = 0; redirects < 5 && page.statusCode() == 302; redirects++) {
            page = browser.get(page.headers().firstValue("location").orElseThrow());
        }
        assertThat(page.statusCode()).as(page.body()).isEqualTo(200);
        assertThat(page.body()).contains("application.js").doesNotContain("name=\"password\"");
        assertThat(browser.post("/wicket/", "", false).statusCode()).isEqualTo(403);
        assertThat(browser.get("/logout").statusCode()).isEqualTo(200);
        // Visiting the confirmation page must not log out through GET.
        assertThat(browser.get("/api/auth/me").statusCode()).isEqualTo(200);
        assertThat(browser.post("/logout", "", true).statusCode()).isEqualTo(302);
        assertThat(browser.get("/api/auth/me").statusCode()).isEqualTo(401);
    }

    private class Browser {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).build();
        String cookie(String name) {
            return cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals(name))
                    .map(c -> c.getValue()).findFirst().orElse("");
        }
        HttpResponse<String> get(String path) throws Exception { return request("GET", path, "", "application/json", false); }
        HttpResponse<String> post(String path, String body, boolean csrf) throws Exception {
            return request("POST", path, body, "application/x-www-form-urlencoded", csrf);
        }
        HttpResponse<String> json(String method, String path, String body, boolean csrf) throws Exception {
            return request(method, path, body, "application/json", csrf);
        }
        HttpResponse<String> login(String username) throws Exception {
            get("/api/auth/csrf");
            var response = post("/api/auth/login", "username=" + username + "&password=test-password", true);
            get("/api/auth/csrf");
            return response;
        }
        HttpResponse<String> request(String method, String path, String body, String contentType, boolean csrf) throws Exception {
            var uri = URI.create("http://127.0.0.1:" + port).resolve(path);
            var builder = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30))
                    .header("Content-Type", contentType).header("Accept", "*/*")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
            if (csrf) builder.header("X-XSRF-TOKEN", cookie("XSRF-TOKEN"));
            return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
