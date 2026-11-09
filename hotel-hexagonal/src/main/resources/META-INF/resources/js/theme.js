// Modo claro / oscuro con Bootstrap 5.3 (atributo data-bs-theme).
// La preferencia del usuario se recuerda en el navegador (localStorage).
(function () {
    var KEY = 'hotel-theme';

    function stored() {
        try { return localStorage.getItem(KEY); } catch (e) { return null; }
    }

    function save(value) {
        try { localStorage.setItem(KEY, value); } catch (e) { /* sin almacenamiento: se ignora */ }
    }

    function preferred() {
        var value = stored();
        if (value === 'light' || value === 'dark') {
            return value;
        }
        return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }

    function apply(theme) {
        document.documentElement.setAttribute('data-bs-theme', theme);
        var icon = document.getElementById('theme-icon');
        var label = document.getElementById('theme-label');
        if (icon) {
            icon.className = theme === 'dark' ? 'bi bi-sun-fill' : 'bi bi-moon-stars-fill';
        }
        if (label) {
            label.textContent = theme === 'dark' ? 'Modo claro' : 'Modo oscuro';
        }
    }

    // Se aplica antes de pintar la página para evitar el parpadeo
    apply(preferred());

    document.addEventListener('DOMContentLoaded', function () {
        apply(preferred());
        var button = document.getElementById('theme-toggle');
        if (button) {
            button.addEventListener('click', function () {
                var current = document.documentElement.getAttribute('data-bs-theme');
                var next = current === 'dark' ? 'light' : 'dark';
                save(next);
                apply(next);
            });
        }
    });
})();
