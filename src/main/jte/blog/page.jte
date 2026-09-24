@import com.alexbalmus.acbblog.webapp.blog.BlogPage
@import org.springframework.security.web.csrf.CsrfToken
@param BlogPage page
@param CsrfToken csrf
@template.blog.layout(title = page.title(), username = page.username(), csrf = csrf, body = @`
    @template.blog.content(page = page, csrf = csrf)
`)
