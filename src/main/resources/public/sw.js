const CACHE = 'eosa-admin-v1';

const STATIC_ASSETS = [
    '/htmx.min.js',
    '/tailwind.min.js',
    '/tailwind.config.js',
    '/fonts.css',
    '/favicon.jpg',
];

self.addEventListener('install', event => {
    event.waitUntil(
        caches.open(CACHE).then(cache => cache.addAll(STATIC_ASSETS))
    );
    self.skipWaiting();
});

self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(keys =>
            Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k)))
        )
    );
    self.clients.claim();
});

self.addEventListener('fetch', event => {
    const url = new URL(event.request.url);

    if (isStaticAsset(url)) {
        event.respondWith(cacheFirst(event.request));
        return;
    }

    if (url.pathname.startsWith('/images/')) {
        event.respondWith(cacheFirst(event.request));
        return;
    }
});

function isStaticAsset(url) {
    return STATIC_ASSETS.some(a => url.pathname.endsWith(a))
        || url.pathname.startsWith('/fonts/');
}

async function cacheFirst(request) {
    const cached = await caches.match(request);
    if (cached) return cached;
    const response = await fetch(request);
    caches.open(CACHE).then(cache => cache.put(request, response.clone()));
    return response;
}