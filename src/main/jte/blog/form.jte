@import com.alexbalmus.acbblog.webapp.blog.BlogPage
@import org.springframework.security.web.csrf.CsrfToken
@param BlogPage page
@param CsrfToken csrf
@param String action
@param String title
@param String target
<section class="card border-primary mb-4" aria-label="${title}">
<div class="card-body">
    <h2 class="h5">${title}</h2>
    <form method="post" action="${target}" hx-post="${target}" hx-target="#blog-content" hx-swap="outerHTML" enctype="multipart/form-data" hx-encoding="multipart/form-data" hx-disabled-elt="find button">
        <input type="hidden" name="${csrf.getParameterName()}" value="${csrf.getToken()}">
        @if(!page.error("general").isEmpty())
            <div class="alert alert-danger" role="alert">${page.error("general")}</div>
        @endif
        @if(action.equals("create") || action.equals("rename") || action.equals("create-post"))
            @template.blog.field(page = page, name = "name", label = action.equals("create-post") || page.post() != null ? "Title" : "Name", required = true, value = action.equals("rename") ? page.title() : "")
        @endif
        @if(action.equals("create") || action.equals("handle"))
            @template.blog.field(page = page, name = "handle", label = "Handle", max = 40, required = true, value = page.handle(), readonly = action.equals("create") && page.handle() != null)
            @if(action.equals("handle"))
                <p class="text-secondary">This updates the handle on all your blogs.</p>
            @elseif(page.handle() != null)
                <p class="text-secondary">Use Change Handle to change your existing handle.</p>
            @endif
        @endif
        @if(action.equals("content") || action.equals("create-post"))
            @template.blog.field(page = page, name = "content", label = "Content", max = 4000, multiline = true, value = page.post() == null ? "" : page.post().content())
        @endif
        @if(action.equals("picture") || action.equals("create-post"))
            <div class="mb-3">
                <label class="form-label" for="field-picture">Picture (optional)</label>
                <input id="field-picture" class="form-control ${page.error("picture").isEmpty() ? "" : "is-invalid"}" type="file" name="picture" accept="image/*" aria-describedby="error-picture">
                <div id="error-picture" class="invalid-feedback">${page.error("picture")}</div>
                <div class="form-text">Choose a supported image such as PNG or JPEG. Leaving this empty keeps an existing picture.</div>
            </div>
        @endif
        @if(action.equals("description") || action.equals("picture") || action.equals("create-post"))
            @template.blog.field(page = page, name = "description", label = "Picture description", max = 500, multiline = true, value = page.post() == null ? "" : page.post().description())
        @endif
        @if(action.equals("delete"))
            <p>Delete <strong>${page.title()}</strong>${page.kind().equals("blog") ? " and all its posts" : ""}? This cannot be undone.</p>
        @elseif(action.equals("clear-picture"))
            <p>Remove this post’s picture and its description?</p>
        @endif
        <div class="d-flex gap-2 align-items-center">
            <button type="submit" class="btn ${action.equals("delete") || action.equals("clear-picture") ? "btn-danger" : "btn-primary"}">${action.equals("delete") ? "Delete" : action.equals("clear-picture") ? "Remove" : "Save"}</button>
            <a class="btn btn-outline-secondary" href="${page.url()}" hx-get="${page.url()}" hx-target="#blog-content" hx-swap="outerHTML">Cancel</a>
            <span class="htmx-indicator text-secondary" role="status">Saving…</span>
        </div>
    </form>
</div>
</section>
