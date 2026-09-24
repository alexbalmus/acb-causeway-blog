@import com.alexbalmus.acbblog.webapp.blog.BlogPage
@import org.springframework.security.web.csrf.CsrfToken
@param BlogPage page
@param CsrfToken csrf
<main id="blog-content" class="container py-4" tabindex="-1" data-title="${page.title()} · ACB Blog">
    @if(page.blog() != null)
        <nav aria-label="breadcrumb"><ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="/blog">Home</a></li>
            @if(page.post() != null)
                <li class="breadcrumb-item"><a href="/blog/blogs/${page.blog().id()}">${page.blog().name()}</a></li>
            @endif
            <li class="breadcrumb-item active" aria-current="page">${page.title()}</li>
        </ol></nav>
    @endif
    @if(page.message() != null)
        <div class="alert alert-info" role="status">${page.message()}</div>
    @endif
    <div class="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4">
        <div>
            <h1 class="h3 mb-1">${page.title()}</h1>
            @if(page.blog() != null)
                <p class="text-secondary mb-0">@${page.blog().handle()}</p>
            @elseif(page.kind().equals("my") && page.handle() != null)
                <p class="text-secondary mb-0">Signed-in handle: <strong>@${page.handle()}</strong></p>
            @elseif(page.kind().equals("directory"))
                <p class="text-secondary mb-0">Read stories from our community. Sign in to create and edit your own.</p>
            @endif
        </div>
        <div class="d-flex flex-wrap gap-2">
            @if(page.kind().equals("my"))
                @template.blog.panel-link(url = "/blog/my?panel=create", label = "+ New Blog")
                @if(page.handle() != null)
                    @template.blog.panel-link(url = "/blog/my?panel=handle", label = "@ Change Handle")
                @else
                    <button class="btn btn-outline-secondary" disabled title="Create a blog to associate a handle with your account">@ Change Handle</button>
                @endif
                <a class="btn btn-outline-secondary" href="/blog">Find Blogs</a>
            @elseif(page.blog() != null)
                @if(page.editing())
                    <a class="btn btn-outline-secondary" href="${page.url().replace("/edit", "")}">Read</a>
                    @template.blog.panel-link(url = page.url() + "?panel=rename", label = "Rename")
                    @if(page.post() == null)
                        @template.blog.panel-link(url = page.url() + "?panel=create-post", label = "+ New Post")
                    @else
                        @template.blog.panel-link(url = page.url() + "?panel=content", label = "Edit Content")
                        @template.blog.panel-link(url = page.url() + "?panel=picture", label = "Update Picture")
                        @template.blog.panel-link(url = page.url() + "?panel=description", label = "Edit Description")
                        @if(page.post().hasPicture() || page.post().description() != null)
                            @template.blog.panel-link(url = page.url() + "?panel=clear-picture", label = "Clear Picture", danger = true)
                        @endif
                    @endif
                    @template.blog.panel-link(url = page.url() + "?panel=delete", label = page.post() == null ? "Delete Blog" : "Delete Post", danger = true)
                @elseif(page.username() == null || page.blog().owned())
                    <a class="btn btn-primary" href="${page.url()}/edit">Edit</a>
                @else
                    <span class="text-secondary">Only the blog owner can edit.</span>
                @endif
            @endif
        </div>
    </div>
    @if(page.editing() && !page.panel().isEmpty())
        @if(page.kind().equals("my") && (page.panel().equals("create") || page.panel().equals("handle")))
            @template.blog.form(page = page, csrf = csrf, action = page.panel(), title = page.panel().equals("create") ? "New Blog" : "Change Handle", target = "/blog/my/" + page.panel())
        @elseif(page.blog() != null && java.util.Set.of("rename", "create-post", "content", "picture", "description", "clear-picture", "delete").contains(page.panel()))
            @template.blog.form(page = page, csrf = csrf, action = page.panel(), title = switch (page.panel()) { case "rename" -> "Rename"; case "create-post" -> "New Post"; case "content" -> "Edit Content"; case "picture" -> "Update Picture"; case "description" -> "Edit Description"; case "clear-picture" -> "Clear Picture"; default -> "Confirm Deletion"; }, target = page.url().replace("/edit", "") + "/" + page.panel())
        @endif
    @endif
    @if(page.kind().equals("directory"))
        <form class="card card-body mb-4" action="/blog" method="get" hx-get="/blog" hx-target="#blog-content" hx-swap="outerHTML" hx-push-url="true">
            <label class="form-label" for="search">Find Blogs</label>
            <div class="d-flex gap-2"><input id="search" name="q" class="form-control" placeholder="Name contains…" value="${page.query()}" maxlength="50"><button class="btn btn-primary">Search</button><a class="btn btn-outline-secondary" href="/blog">Clear</a></div>
        </form>
    @endif
    @if(page.blog() == null)
        <section class="card"><div class="card-body">
            <h2 class="h5">${page.kind().equals("my") ? "My Blogs" : "Blogs"} (${page.blogs().size()})</h2>
            @if(page.blogs().isEmpty())
                <p class="text-secondary mb-0">${page.kind().equals("my") ? "You have no blogs yet — create one with New Blog." : "No blogs found."}</p>
            @else
                <div class="table-responsive"><table class="table table-hover align-middle mb-0"><thead><tr><th>Name</th><th>Handle</th><th><span class="visually-hidden">Actions</span></th></tr></thead><tbody>
                @for(var blog : page.blogs())
                    <tr><td><a class="fw-semibold text-decoration-none" href="/blog/blogs/${blog.id()}">${blog.name()}</a></td><td>@${blog.handle()}</td><td class="text-end text-nowrap">
                        <a class="btn btn-sm btn-outline-primary" href="/blog/blogs/${blog.id()}">Open</a>
                        @if(page.kind().equals("my"))
                            <a class="btn btn-sm btn-outline-danger" href="/blog/blogs/${blog.id()}/edit?panel=delete">Delete</a>
                        @endif
                    </td></tr>
                @endfor
                </tbody></table></div>
            @endif
        </div></section>
    @elseif(page.post() == null)
        <section class="card"><div class="card-body"><h2 class="h5">Posts (${page.posts().size()})</h2>
            @if(page.posts().isEmpty())
                <p class="text-secondary mb-0">No posts yet.</p>
            @else
                <div class="table-responsive"><table class="table table-hover align-middle mb-0"><thead><tr><th>Title</th><th><span class="visually-hidden">Actions</span></th></tr></thead><tbody>
                @for(var post : page.posts())
                    <tr><td><a class="fw-semibold text-decoration-none" href="/blog/posts/${post.id()}">${post.title()}</a></td><td class="text-end text-nowrap"><a class="btn btn-sm btn-outline-primary" href="/blog/posts/${post.id()}">Open</a>
                        @if(page.editing())
                            <a class="btn btn-sm btn-outline-danger" href="/blog/posts/${post.id()}/edit?panel=delete">Delete</a>
                        @endif
                    </td></tr>
                @endfor
                </tbody></table></div>
            @endif
        </div></section>
    @else
        <article class="card"><div class="card-body article-preview">
            ${page.post().pictureHtml()}
            <div class="post-content mx-auto mt-3">${page.post().contentHtml()}</div>
            @if(!page.post().hasPicture() && (page.post().content() == null || page.post().content().isBlank()))
                <p class="text-secondary text-center">This post has no content yet.</p>
            @endif
        </div></article>
    @endif
</main>
