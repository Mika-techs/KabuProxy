(function () {
    'use strict';

    // ---- theme toggle (light / dark / system) ----
    var root = document.documentElement;
    var toggle = document.getElementById('theme-toggle');
    if (toggle) {
        toggle.addEventListener('click', function () {
            var dark = root.dataset.theme
                ? root.dataset.theme === 'dark'
                : window.matchMedia('(prefers-color-scheme: dark)').matches;
            root.dataset.theme = dark ? 'light' : 'dark';
            try { localStorage.setItem('kabu-theme', root.dataset.theme); } catch (e) { /* private mode */ }
        });
    }

    // ---- mark the lesson running right now ----
    function toMinutes(hhmm) {
        var p = hhmm.split(':');
        return parseInt(p[0], 10) * 60 + parseInt(p[1], 10);
    }
    function markNow() {
        var now = new Date();
        var minutes = now.getHours() * 60 + now.getMinutes();
        document.querySelectorAll('.lesson.is-now').forEach(function (el) { el.classList.remove('is-now'); });
        document.querySelectorAll('.timetable__day.is-today .lesson, .daycard.is-today .lesson').forEach(function (el) {
            var range = (el.dataset.time || '').split('–');
            if (range.length === 2 && minutes >= toMinutes(range[0]) && minutes < toMinutes(range[1])) {
                el.classList.add('is-now');
            }
        });
    }
    markNow();
    setInterval(markNow, 60 * 1000);

    // ---- mobile: jump to today's card ----
    var todayCard = document.querySelector('.daycard.is-today');
    if (todayCard && window.matchMedia('(max-width: 860px)').matches && !location.hash) {
        todayCard.scrollIntoView({ block: 'start' });
    }

    // ---- keep an open tab fresh: reload (GET, never re-POST) after 15 min when visible ----
    if (!document.querySelector('.ui-datatable')) {
        var loadedAt = Date.now();
        var FRESH_MS = 15 * 60 * 1000;
        var refreshIfStale = function () {
            if (document.visibilityState === 'visible' && Date.now() - loadedAt > FRESH_MS) {
                location.href = location.pathname + location.search;
            }
        };
        document.addEventListener('visibilitychange', refreshIfStale);
        setInterval(refreshIfStale, 60 * 1000);
    }
})();
