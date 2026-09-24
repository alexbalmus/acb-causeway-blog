package com.alexbalmus.acbblog.webapp.blog;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import jakarta.persistence.EntityManagerFactory;
import org.apache.causeway.applib.services.iactn.InteractionContext;
import org.apache.causeway.applib.services.iactn.InteractionService;
import org.apache.causeway.applib.services.wrapper.WrapperFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import com.alexbalmus.acbblog.modules.blog.domain.blog.*;
import com.alexbalmus.acbblog.modules.blog.domain.post.*;
import com.alexbalmus.acbblog.modules.blog.domain.mixins.blog.*;
import com.alexbalmus.acbblog.modules.blog.domain.mixins.post.Post_delete;
import com.alexbalmus.acbblog.modules.blog.domain.userhandle.UserHandlesRepository;
import com.alexbalmus.acbblog.webapp.security.CausewayPrincipalConverter;
import static com.alexbalmus.acbblog.webapp.blog.BlogForm.text;

@Service
public class BlogClientService {
    private final BlogsRepository blogs;
    private final PostsRepository posts;
    private final UserHandlesRepository handles;
    private final Blogs actions;
    private final WrapperFactory wrappers;
    private final InteractionService interactions;
    private final CausewayPrincipalConverter converter;
    private final EntityManagerFactory entities;
    private final PlatformTransactionManager transactions;
    private final BlogImages images;

    public BlogClientService(BlogsRepository blogs, PostsRepository posts, UserHandlesRepository handles,
            Blogs actions, WrapperFactory wrappers, InteractionService interactions,
            CausewayPrincipalConverter converter, EntityManagerFactory entities,
            PlatformTransactionManager transactions, BlogImages images) {
        this.blogs = blogs; this.posts = posts; this.handles = handles; this.actions = actions;
        this.wrappers = wrappers; this.interactions = interactions; this.converter = converter;
        this.entities = entities; this.transactions = transactions; this.images = images;
    }
    private <T> T work(boolean readOnly, Supplier<T> work) {
        var user = converter.convert(SecurityContextHolder.getContext().getAuthentication());
        var transaction = new TransactionTemplate(transactions);
        transaction.setReadOnly(readOnly);
        if (user == null) {
            if (!readOnly) throw new AccessDeniedException("Sign in to edit.");
            return interactions.callAnonymous(() -> transaction.execute(status -> work.get()));
        }
        return interactions.call(InteractionContext.ofUserWithSystemDefaults(user),
                () -> transaction.execute(status -> work.get()));
    }
    private String username() {
        var user = converter.convert(SecurityContextHolder.getContext().getAuthentication());
        return user == null ? null : user.name();
    }
    private String handle() {
        var username = username();
        return username == null ? null : handles.findByUsername(username).map(h -> h.getHandle()).orElse(null);
    }
    private long id(Object entity) { return ((Number) entities.getPersistenceUnitUtil().getIdentifier(entity)).longValue(); }
    private Blog blog(long id) { return blogs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    private Post post(long id) { return posts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    private boolean owned(Blog blog) { return blog.getHandle().equals(handle()); }
    private void owner(Blog blog) {
        if (!owned(blog)) throw new AccessDeniedException(BlogOwnershipGuard.ONLY_THE_BLOG_OWNER_CAN_DO_THIS);
    }
    private BlogPage.BlogRow row(Blog blog) { return new BlogPage.BlogRow(id(blog), blog.getName(), blog.getHandle(), owned(blog)); }

    public BlogPage page(String kind, Long id, boolean editing, String panel, String query,
            Map<String, String> values, Map<String, String> errors, String message) {
        return work(true, () -> {
            Blog blog = kind.equals("blog") ? blog(id) : null;
            Post post = kind.equals("post") ? post(id) : null;
            if (post != null) blog = post.getBlog();
            if (editing && blog != null) owner(blog);
            List<BlogPage.BlogRow> rows = List.of();
            if (kind.equals("directory")) {
                rows = (text(query) == null ? blogs.findAll(Sort.by("name", "handle", "id"))
                        : blogs.findByNameContainingOrderByNameAsc(query.trim())).stream().map(this::row).toList();
            } else if (kind.equals("my")) {
                rows = (handle() == null ? List.<Blog>of() : blogs.findAllByHandleOrderByNameAsc(handle()))
                        .stream().map(this::row).toList();
            }
            var postRows = blog != null && post == null ? posts.findByBlogOrderByTitleAsc(blog).stream()
                    .map(p -> new BlogPage.PostRow(id(p), p.getTitle())).toList() : List.<BlogPage.PostRow>of();
            var view = post == null ? null : new BlogPage.PostView(id(post), post.getTitle(), post.getContent(),
                    post.getPictureDescription(), post.getPicture() != null,
                    BlogPage.generatedHtml(post.getPicturePreview().html()),
                    BlogPage.generatedHtml(post.getContentPreview().html()));
            String title = post != null ? post.getTitle() : blog != null ? blog.getName()
                    : kind.equals("my") ? "My Blogs" : "Explore Blogs";
            return new BlogPage(kind, title, username(), handle(), editing, panel == null ? "" : panel,
                    query == null ? "" : query, rows, blog == null ? null : row(blog), view, postRows,
                    values, errors, message);
        });
    }

    public String mutate(String kind, Long id, String action, BlogForm form) {
        return work(false, () -> {
            // Ownership is checked before uploads or expensive AI calls, and again by the wrappers.
            Blog blog = kind.equals("blog") ? blog(id) : null;
            Post post = kind.equals("post") ? post(id) : null;
            if (post != null) blog = post.getBlog();
            if (blog != null) owner(blog);
            if (kind.equals("my")) {
                if (action.equals("create")) {
                    required("name", form.getName(), 50); required("handle", form.getHandle(), 40);
                    var created = wrappers.wrap(actions).create(text(form.getName()), text(form.getHandle()));
                    return "/blog/blogs/" + id(wrappers.unwrap(created)) + "/edit";
                }
                if (action.equals("handle")) {
                    required("handle", form.getHandle(), 40);
                    wrappers.wrap(actions).changeHandle(text(form.getHandle()));
                    return "/blog/my";
                }
            }
            if (kind.equals("blog")) {
                switch (action) {
                    case "rename" -> {
                        required("name", form.getName(), 50);
                        var duplicate = blogs.findByNameAndHandle(text(form.getName()), blog.getHandle());
                        if (duplicate != null && !duplicate.equals(blog)) throw new BlogFormException("name", "A blog with this name already exists for your handle.");
                        wrappers.wrap(blog).updateName(text(form.getName()));
                    }
                    case "create-post" -> {
                        required("name", form.getName(), 50); optional("content", form.getContent(), 4000);
                        optional("description", form.getDescription(), 500);
                        var created = wrappers.wrapMixin(Blog_createPost.class, blog).act(text(form.getName()),
                                text(form.getContent()), images.decode(form.getPicture()), text(form.getDescription()));
                        return "/blog/posts/" + id(wrappers.unwrap(created)) + "/edit";
                    }
                    case "delete" -> { wrappers.wrapMixin(Blog_delete.class, blog).act(); return "/blog/my"; }
                    default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                }
                return "/blog/blogs/" + id + "/edit";
            }
            if (kind.equals("post")) {
                switch (action) {
                    case "rename" -> { required("name", form.getName(), 50); wrappers.wrap(post).updateTitle(text(form.getName())); }
                    case "content" -> { optional("content", form.getContent(), 4000); wrappers.wrap(post).setContent(text(form.getContent())); }
                    case "description" -> { optional("description", form.getDescription(), 500); wrappers.wrap(post).setPictureDescription(text(form.getDescription())); }
                    case "picture" -> {
                        optional("description", form.getDescription(), 500);
                        wrappers.wrap(post).updatePicture(images.decode(form.getPicture()), text(form.getDescription()));
                    }
                    case "clear-picture" -> wrappers.wrap(post).clearPicture();
                    case "delete" -> {
                        long blogId = id(blog);
                        wrappers.wrapMixin(Post_delete.class, post).act();
                        return "/blog/blogs/" + blogId + "/edit";
                    }
                    default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                }
                return "/blog/posts/" + id + "/edit";
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        });
    }
    private void required(String field, String value, int max) {
        if (text(value) == null) throw new BlogFormException(field, "This field is required.");
        optional(field, value, max);
    }
    private void optional(String field, String value, int max) {
        if (value != null && value.length() > max) throw new BlogFormException(field, "Use at most " + max + " characters.");
    }
}
