@param String url
@param String label
@param boolean danger = false
<a class="btn ${danger ? "btn-outline-danger" : "btn-outline-primary"}" href="${url}" hx-get="${url}" hx-target="#blog-content" hx-swap="outerHTML">${label}</a>
