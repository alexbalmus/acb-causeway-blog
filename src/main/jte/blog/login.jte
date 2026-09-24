@import org.springframework.security.web.csrf.CsrfToken
@param CsrfToken csrf
@param String error
@template.blog.layout(title = "Sign in", username = null, csrf = csrf, body = @`
<main id="blog-content" class="container py-5">
    <div class="card login-card mx-auto"><div class="card-body p-4">
        <h1 class="h3 mb-4">Sign in</h1>
        @if(!error.isEmpty())
            <div class="alert alert-danger" role="alert">${error}</div>
        @endif
        <form method="post" action="/blog/login">
            <input type="hidden" name="${csrf.getParameterName()}" value="${csrf.getToken()}">
            <div class="mb-3"><label class="form-label" for="username">Username</label><input class="form-control" id="username" name="username" autocomplete="username" required autofocus></div>
            <div class="mb-4"><label class="form-label" for="password">Password</label><input class="form-control" type="password" id="password" name="password" autocomplete="current-password" required></div>
            <button class="btn btn-primary w-100">Sign in</button>
        </form>
        <a class="d-block mt-3" href="/blog">Continue reading without signing in</a>
    </div></div>
</main>
`)
