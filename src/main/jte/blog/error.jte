@import com.alexbalmus.acbblog.webapp.blog.BlogCsrfToken
@param String message
@param String back
@param boolean fragment
@param BlogCsrfToken csrf
@param String username = null
@if(fragment)
    @template.blog.error-body(message = message, back = back)
@else
    @template.blog.layout(title = "Request failed", username = username, csrf = csrf, body = @`
        @template.blog.error-body(message = message, back = back)
    `)
@endif
