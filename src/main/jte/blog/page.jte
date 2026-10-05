@import com.alexbalmus.acbblog.webapp.blog.BlogPage
@import com.alexbalmus.acbblog.webapp.blog.BlogCsrfToken
@param BlogPage page
@param BlogCsrfToken csrf
@template.blog.layout(title = page.title(), username = page.username(), csrf = csrf, body = @`
    @template.blog.content(page = page, csrf = csrf)
`)
