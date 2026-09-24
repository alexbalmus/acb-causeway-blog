/* HTMX only enhances normal forms; all domain rules and rendering stay on the server. */
document.addEventListener('htmx:configRequest', event => {
  const token = document.cookie.split('; ').find(value => value.startsWith('XSRF-TOKEN='));
  if (token) event.detail.headers['X-XSRF-TOKEN'] = decodeURIComponent(token.substring(11));
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
