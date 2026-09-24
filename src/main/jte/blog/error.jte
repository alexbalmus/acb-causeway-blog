@import org.springframework.security.web.csrf.CsrfToken
@param String message
@param String back
@param boolean fragment
@param CsrfToken csrf
@if(fragment)
    @template.blog.error-body(message = message, back = back)
@else
    @template.blog.layout(title = "Request failed", username = null, csrf = csrf, body = @`
        @template.blog.error-body(message = message, back = back)
    `)
@endif
