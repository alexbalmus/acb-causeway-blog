@import gg.jte.Content
@import org.springframework.security.web.csrf.CsrfToken
@param String title
@param String username
@param CsrfToken csrf
@param Content body
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="htmx-config" content='{"historyCacheSize":0,"historyRestoreAsHxRequest":false,"allowEval":false,"allowScriptTags":false}'>
    <title>${title} · ACB Blog</title>
    <link rel="icon" href="/images/favicon.png">
    <link rel="stylesheet" href="/webjars/bootswatch/5.3.8/dist/litera/bootstrap.min.css">
    <link rel="stylesheet" href="/blog/assets/blog.css">
    <script defer src="/webjars/htmx.org/2.0.8/dist/htmx.min.js"></script>
    <script defer src="/blog/assets/blog.js"></script>
</head>
<body hx-history="false">
<a class="visually-hidden-focusable" href="#blog-content">Skip to content</a>
<nav class="navbar navbar-expand bg-primary" data-bs-theme="dark" aria-label="Main navigation">
    <div class="container flex-wrap gap-2">
        <a class="navbar-brand fw-bold" href="/blog">✉ ACB Blog</a>
        <div class="d-flex flex-wrap align-items-center gap-3">
            <a class="nav-link text-white" href="/blog">Explore</a>
            @if(username != null)
                <a class="nav-link text-white" href="/blog/my">My Blogs</a>
                <span class="navbar-text">${username}</span>
                <form action="/blog/logout" method="post">
                    <input type="hidden" name="${csrf.getParameterName()}" value="${csrf.getToken()}">
                    <button class="btn btn-sm btn-outline-light">Sign out</button>
                </form>
            @else
                <a class="btn btn-sm btn-outline-light" href="/blog/login">Sign in</a>
            @endif
        </div>
    </div>
</nav>
${body}
<div id="network-error" class="alert alert-danger network-error" role="alert" hidden>Unable to reach the server. Your form is still here; please try again.</div>
</body>
</html>
