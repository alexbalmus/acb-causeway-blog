package com.alexbalmus.acbblog.webapp;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.causeway.security.simple.realm.SimpleRealm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercises the real HTTP viewer, metamodel, authorization and H2 persistence together. */
@SpringBootTest(classes = ACBBlogApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:restcontract;DATABASE_TO_UPPER=false")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RestfulObjectsIntegrationTest {
    private static final String SERVICE = "/restful/services/blog.Blogs";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    @LocalServerPort int port;
    @Autowired SimpleRealm realm;
    @Autowired PasswordEncoder passwords;
    private String owner;
    private String other;

    @BeforeEach void users() {
        owner = "owner-" + UUID.randomUUID().toString().substring(0, 12);
        other = "other-" + UUID.randomUUID().toString().substring(0, 12);
        // Both have full realm permissions: ownership must still be enforced.
        for (String user : List.of(owner, other)) {
            realm.addUser(user, passwords.encode("pass"), List.of("admin_role"));
        }
    }

    @Test void authenticationIsRequiredAndBadCredentialsDoNotAuthenticate() throws Exception {
        assertThat(send(null, "GET", SERVICE, null).statusCode()).isEqualTo(401);
        assertThat(send("unknown", "GET", "/restful/user/", null).statusCode()).isEqualTo(401);
        assertThat(json(send(owner, "GET", "/restful/user/", null), 200).path("userName").asText()).isEqualTo(owner);
        // A previous successful Basic request must not authenticate the next anonymous request.
        assertThat(send(null, "GET", SERVICE, null).statusCode()).isEqualTo(401);
    }

    @Test void crudPropertiesListsSearchAndCascadeDeletion() throws Exception {
        String blog = createBlog(owner, "REST journal", owner);
        String second = createBlog(owner, "Second journal", owner);
        JsonNode ownList = action(owner, "GET", SERVICE, "listAll", Map.of(), 200);
        assertThat(list(owner, ownList).toString()).contains("REST journal", "Second journal");
        JsonNode empty = action(other, "GET", SERVICE, "listAll", Map.of(), 200);
        assertThat(list(other, empty).size()).isZero();
        JsonNode search = action(other, "GET", SERVICE, "findByNameContaining", Map.of("name", "REST journal"), 200);
        assertThat(list(other, search).toString()).contains("REST journal");

        action(owner, "PUT", blog, "updateName", Map.of("name", "Renamed journal"), 200);
        assertThat(object(owner, blog).path("members").path("name").path("value").asText()).isEqualTo("Renamed journal");
        String post = createPost(blog, "First post", "Initial content");
        assertThat(json(send(owner, "GET", blog + "/collections/posts", null), 200).path("value").toString()).contains("First post");
        json(send(owner, "PUT", post + "/properties/content", Map.of("value", "Updated content")), 200);
        action(owner, "PUT", post, "updateTitle", Map.of("name", "Renamed post"), 200);
        assertThat(object(owner, post).path("members").path("content").path("value").asText()).isEqualTo("Updated content");
        assertThat(object(owner, post).path("members").path("title").path("value").asText()).isEqualTo("Renamed post");

        action(owner, "POST", SERVICE, "changeHandle", Map.of("handle", owner + "-new"), 200);
        for (String href : List.of(blog, second)) {
            assertThat(object(owner, href).path("members").path("handle").path("value").asText()).isEqualTo(owner + "-new");
        }
        action(owner, "POST", blog, "delete", Map.of(), 200);
        assertThat(send(owner, "GET", post, null).statusCode()).isEqualTo(404);
        assertThat(send(owner, "GET", blog, null).statusCode()).isEqualTo(404);
        assertThat(send(owner, "GET", second, null).statusCode()).isEqualTo(200);
    }

    @Test void duplicateAndInvalidInputLeavePersistedStateUnchanged() throws Exception {
        String blog = createBlog(owner, "Unique journal", owner);
        action(owner, "POST", SERVICE, "create", Map.of("name", "Unique journal", "handle", owner), 422);
        action(other, "POST", SERVICE, "create", Map.of("name", "Stolen handle", "handle", owner), 422);
        action(owner, "PUT", blog, "updateName", Map.of("name", "x".repeat(51)), 422);
        assertThat(object(owner, blog).path("members").path("name").path("value").asText()).isEqualTo("Unique journal");
        String first = createPost(blog, "One", "Original");
        String second = createPost(blog, "Two", "Second");
        action(owner, "PUT", second, "updateTitle", Map.of("name", "One"), 422);
        action(owner, "POST", blog, "createPost", postArgs("One", "Duplicate"), 422);
        assertThat(object(owner, first).path("members").path("content").path("value").asText()).isEqualTo("Original");
        assertThat(object(owner, second).path("members").path("title").path("value").asText()).isEqualTo("Two");
    }

    @Test void anotherAdministratorCannotMutateOwnedBlogsOrPosts() throws Exception {
        String blog = createBlog(owner, "Private ownership", owner);
        String post = createPost(blog, "Protected", "Original");
        assertThat(object(other, blog).path("members").path("updateName").path("disabledReason").asText()).contains("owner");
        action(other, "PUT", blog, "updateName", Map.of("name", "Hacked"), 403);
        action(other, "POST", blog, "createPost", postArgs("Hacked", "Bad"), 403);
        action(other, "POST", blog, "delete", Map.of(), 403);
        action(other, "PUT", post, "updateTitle", Map.of("name", "Hacked"), 403);
        assertThat(send(other, "PUT", post + "/properties/content", Map.of("value", "Hacked")).statusCode()).isEqualTo(403);
        assertThat(send(other, "PUT", post + "/properties/pictureDescription", Map.of("value", "Hacked")).statusCode()).isEqualTo(403);
        var pictureArgs = new LinkedHashMap<String, Object>();
        pictureArgs.put("picture", null); pictureArgs.put("pictureDescription", "Hacked");
        action(other, "PUT", post, "updatePicture", pictureArgs, 403);
        action(other, "PUT", post, "clearPicture", Map.of(), 403);
        action(other, "POST", post, "delete", Map.of(), 403);
        assertThat(object(owner, post).path("members").path("content").path("value").asText()).isEqualTo("Original");
        assertThat(json(send(owner, "GET", blog + "/collections/posts", null), 200).path("value").size()).isEqualTo(1);
    }

    @Test void deletingByTitleAndDeletingThePostHaveTheSamePersistenceEffect() throws Exception {
        String blog = createBlog(owner, "Deletion", owner);
        String first = createPost(blog, "Delete by title", null);
        String second = createPost(blog, "Delete directly", null);
        action(owner, "POST", blog, "deletePost", Map.of("title", "Delete by title"), 200);
        action(owner, "POST", second, "delete", Map.of(), 200);
        assertThat(send(owner, "GET", first, null).statusCode()).isEqualTo(404);
        assertThat(send(owner, "GET", second, null).statusCode()).isEqualTo(404);
        assertThat(json(send(owner, "GET", blog + "/collections/posts", null), 200).path("value").size()).isZero();
    }

    private String createBlog(String user, String name, String handle) throws Exception {
        return self(action(user, "POST", SERVICE, "create", Map.of("name", name, "handle", handle), 200));
    }
    private String createPost(String blog, String title, String content) throws Exception {
        return self(action(owner, "POST", blog, "createPost", postArgs(title, content), 200));
    }
    private Map<String, Object> postArgs(String title, String content) {
        var args = new LinkedHashMap<String, Object>();
        args.put("title", title); args.put("content", content);
        args.put("picture", null); args.put("pictureDescription", null);
        return args;
    }
    private JsonNode object(String user, String href) throws Exception { return json(send(user, "GET", href, null), 200); }
    private JsonNode list(String user, JsonNode result) throws Exception {
        assertThat(result.path("domainType").asText()).isEqualTo("causeway.applib.DomainObjectList");
        return json(send(user, "GET", self(result) + "/collections/objects", null), 200).path("value");
    }
    private String self(JsonNode repr) {
        for (JsonNode link : repr.path("links")) {
            if (link.path("rel").asText().equals("self")) return URI.create(link.path("href").asText()).getPath();
        }
        throw new AssertionError("Missing self link: " + repr);
    }
    private JsonNode action(String user, String method, String href, String action, Map<String, ?> args, int status) throws Exception {
        var wrapped = new LinkedHashMap<String, Object>();
        args.forEach((key, value) -> { var param = new LinkedHashMap<String, Object>(); param.put("value", value); wrapped.put(key, param); });
        String path = href + "/actions/" + action + "/invoke";
        if (method.equals("GET")) path += "?x-causeway-querystring=" + URLEncoder.encode(JSON.writeValueAsString(wrapped), StandardCharsets.UTF_8);
        return json(send(user, method, path, method.equals("GET") ? null : wrapped), status);
    }
    private JsonNode json(HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        return response.body().isBlank() ? JSON.nullNode() : JSON.readTree(response.body());
    }
    private HttpResponse<String> send(String user, String method, String path, Object body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(30)).header("Accept", "application/json").header("Content-Type", "application/json");
        if (user != null) request.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((user + ":pass").getBytes(StandardCharsets.UTF_8)));
        return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
    }
}
