const CACHE = 'eosa-v2';

const STATIC_ASSETS = [
    '/htmx.min.js',
    '/tailwind.min.js',
    '/tailwind.config.js',
    '/fonts.css',
    '/favicon.jpg',
    '/offline.html',
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
    const { request } = event;
    const url = new URL(request.url);

    // Static assets + fonts → cache-first
    if (isStaticAsset(url)) {
        event.respondWith(cacheFirst(request));
        return;
    }

    // Product images → cache-first, lazy populate
    if (url.pathname.startsWith('/images/')) {
        event.respondWith(cacheFirst(request));
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
    try {
        const response = await fetch(request);
        const cache = await caches.open(CACHE);
        cache.put(request, response.clone());
        return response;
    } catch {
        return caches.match('/offline.html');
    }
}
