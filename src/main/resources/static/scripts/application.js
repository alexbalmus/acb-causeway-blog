// Spring Security protects Wicket AJAX, ordinary forms and multipart submissions.
(function () {
    function token() {
        var cookie = document.cookie.split('; ').find(function (value) { return value.startsWith('XSRF-TOKEN='); });
        return cookie ? decodeURIComponent(cookie.substring('XSRF-TOKEN='.length)) : '';
    }
    function protectForm(form) {
        if (new URL(form.action || location.href, location.href).origin !== location.origin) return;
        var input = form.querySelector('input[name="_csrf"]');
        if (!input) {
            input = document.createElement('input');
            input.type = 'hidden';
            input.name = '_csrf';
            form.appendChild(input);
        }
        if (input.value !== token()) input.value = token();
    }
    function protectForms() { document.querySelectorAll('form').forEach(protectForm); }
    jQuery.ajaxPrefilter(function (options, originalOptions, xhr) {
        if (new URL(options.url, location.href).origin === location.origin) {
            xhr.setRequestHeader('X-XSRF-TOKEN', token());
        }
    });
    jQuery(function () {
        protectForms();
        new MutationObserver(protectForms).observe(document.body, { childList: true, subtree: true });
        document.addEventListener('submit', function (event) { protectForm(event.target); }, true);
    });
})();
