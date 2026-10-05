/* HTMX only enhances normal forms; all domain rules and rendering stay on the server. */
document.addEventListener('htmx:configRequest', event => {
  const token = document.querySelector('meta[name="blog-csrf"]')?.content;
  if (token) event.detail.headers['X-Blog-CSRF'] = token;
  document.getElementById('network-error').hidden = true;
});
document.addEventListener('htmx:beforeSwap', event => {
  if ([400, 403, 404, 409, 413, 422, 500].includes(event.detail.xhr.status)) {
    event.detail.shouldSwap = true;
    event.detail.isError = false;
  }
});
document.addEventListener('htmx:afterSwap', () => {
  const main = document.getElementById('blog-content');
  if (main?.dataset.title) document.title = main.dataset.title;
  const focus = main?.querySelector('.is-invalid, form input:not([type="hidden"]):not([readonly]), form textarea');
  (focus || main)?.focus({ preventScroll: true });
});
document.addEventListener('htmx:sendError', () => { document.getElementById('network-error').hidden = false; });
document.addEventListener('htmx:timeout', () => { document.getElementById('network-error').hidden = false; });
