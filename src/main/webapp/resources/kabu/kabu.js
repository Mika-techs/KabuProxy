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

    // ---- countdown to the next (not cancelled) lesson of today, and to the end of the running one ----
    var nextup = document.getElementById('nextup');
    if (nextup) {
        var endRow = document.getElementById('nextup-end');
        var endTimerEl = document.getElementById('nextup-end-timer');
        var nextRow = document.getElementById('nextup-next');
        var timerEl = document.getElementById('nextup-timer');
        var whatEl = document.getElementById('nextup-what');
        // desktop grid only - the mobile list holds the same lessons
        var lessons = [];
        document.querySelectorAll('.timetable__day.is-today .lesson:not(.lesson--cancelled)').forEach(function (el) {
            var range = (el.dataset.time || '').split('–');
            if (range.length !== 2) {
                return;
            }
            var text = function (selector) {
                var found = el.querySelector(selector);
                return found ? found.textContent.trim() : '';
            };
            lessons.push({
                start: toMinutes(range[0]) * 60,
                end: toMinutes(range[1]) * 60,
                subject: text('.lesson__subject'),
                teacher: text('.lesson__meta span:first-child'),
                room: text('.lesson__room')
            });
        });
        var pad = function (n) { return (n < 10 ? '0' : '') + n; };
        var format = function (left) {
            var h = Math.floor(left / 3600);
            var m = Math.floor(left % 3600 / 60);
            return (h > 0 ? h + ':' + pad(m) : m) + ':' + pad(left % 60);
        };
        var tick = function () {
            var now = new Date();
            var seconds = now.getHours() * 3600 + now.getMinutes() * 60 + now.getSeconds();
            var next = null;
            var currentEnd = null;
            lessons.forEach(function (l) {
                if (l.start > seconds && (next === null || l.start < next)) {
                    next = l.start;
                }
                // parallel lessons may end at different times - count until the last one is over
                if (l.start <= seconds && l.end > seconds && (currentEnd === null || l.end > currentEnd)) {
                    currentEnd = l.end;
                }
            });
            // the end only matters when the next lesson doesn't follow right away (break, free period, end of day)
            var showEnd = currentEnd !== null && currentEnd !== next;
            endRow.hidden = !showEnd;
            if (showEnd) {
                endTimerEl.textContent = format(currentEnd - seconds);
            }
            nextRow.hidden = next === null;
            if (next !== null) {
                timerEl.textContent = format(next - seconds);
                // parallel lessons (groups) share a start time - name the teacher to tell them apart
                var parallel = lessons.filter(function (l) { return l.start === next; });
                var labels = parallel.map(function (l) {
                    var details = [l.room, parallel.length > 1 ? l.teacher : ''].filter(Boolean).join(', ');
                    return l.subject + (details ? ' (' + details + ')' : '');
                });
                whatEl.textContent = labels.filter(function (s, i) { return s && labels.indexOf(s) === i; }).join(' / ');
            }
            nextup.hidden = !showEnd && next === null;
        };
        tick();
        setInterval(tick, 1000);
    }

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
