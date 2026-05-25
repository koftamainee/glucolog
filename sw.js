const CACHE = 'glucolog-v1';

const STATIC_ASSETS = [
    '/',
    '/index.html',
    '/site.webmanifest',
    '/css/reset.css',
    '/css/variables.css',
    '/css/base.css',
    '/css/header.css',
    '/css/layout.css',
    '/css/forms.css',
    '/css/chart.css',
    '/css/insulin.css',
    '/css/meals.css',
    '/css/water.css',
    '/css/sport.css',
    '/css/stool.css',
    '/css/sleep.css',
    '/css/notes.css',
    '/css/calendar.css',
    '/css/sidebar.css',
    '/css/journal.css',
    '/css/tutorial.css',
    '/css/journal-delete.css',
    '/css/offline.css',
    '/js/storage.js',
    '/js/chart.js',
    '/js/render.js',
    '/js/calendar.js',
    '/js/sidebar.js',
    '/js/tutorial.js',
    '/js/drive.js',
    '/js/app.js',
    '/icons/favicon.ico',
    '/icons/favicon-16x16.png',
    '/icons/favicon-32x32.png',
    '/icons/apple-touch-icon.png',
    '/icons/android-chrome-192x192.png',
    '/icons/android-chrome-512x512.png',
];

self.addEventListener('install', event => {
    event.waitUntil(
        caches.open(CACHE).then(cache => cache.addAll(STATIC_ASSETS))
    );
});

self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(keys =>
            Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k)))
        )
    );
});

self.addEventListener('fetch', event => {
    event.respondWith(
        caches.match(event.request).then(cached => cached || fetch(event.request))
    );
});
