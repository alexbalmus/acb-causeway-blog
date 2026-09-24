@import com.alexbalmus.acbblog.webapp.blog.BlogPage
@param BlogPage page
@param String name
@param String label
@param String value = ""
@param int max = 50
@param boolean required = false
@param boolean multiline = false
@param boolean readonly = false
<div class="mb-3">
    <label class="form-label" for="field-${name}">${label}</label>
    @if(multiline)
        <textarea id="field-${name}" name="${name}" class="form-control ${page.error(name).isEmpty() ? "" : "is-invalid"}" rows="${name.equals("content") ? 10 : 3}" maxlength="${max}" aria-describedby="error-${name}">${page.value(name, value)}</textarea>
    @else
        <input id="field-${name}" name="${name}" class="form-control ${page.error(name).isEmpty() ? "" : "is-invalid"}" value="${page.value(name, value)}" maxlength="${max}" required="${required}" readonly="${readonly}" aria-describedby="error-${name}">
    @endif
    <div id="error-${name}" class="invalid-feedback">${page.error(name)}</div>
</div>
