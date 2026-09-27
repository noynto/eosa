// Service worker of the administration PWA (admin.eosa.me).
//
// Bump VERSION whenever this file changes: the new worker then replaces the old one and
// deletes its caches.
const VERSION = 'v2';
const STATIC_CACHE = `eosa-admin-static-${VERSION}`;
const IMAGE_CACHE = `eosa-admin-images-${VERSION}`;
const OFFLINE_URL = '/offline.html';

const PRECACHE = [
    OFFLINE_URL,
    '/htmx.min.js',
    '/tailwind.min.js',
    '/tailwind.config.js',
    '/fonts.css',
    '/favicon.jpg',
    '/icon-192.png',
];

self.addEventListener('install', event => {
    event.waitUntil(caches.open(STATIC_CACHE).then(cache => cache.addAll(PRECACHE)));
    self.skipWaiting();
});

self.addEventListener('activate', event => {
    const current = [STATIC_CACHE, IMAGE_CACHE];
    event.waitUntil(
        caches.keys()
            .then(keys => Promise.all(keys.filter(k => !current.includes(k)).map(k => caches.delete(k))))
            .then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', event => {
    const request = event.request;
    // Only same-origin GETs: form posts, htmx PATCH/DELETE and the sign-in always hit the network.
    if (request.method !== 'GET') return;
    const url = new URL(request.url);
    if (url.origin !== self.location.origin) return;

    // Pages are never cached (they hold private, always-changing data): network, or the
    // offline page when there is no connection.
    if (request.mode === 'navigate') {
        event.respondWith(fetch(request).catch(() => caches.match(OFFLINE_URL)));
        return;
    }

    // Uploaded images never change for a given id.
    if (url.pathname.startsWith('/images/')) {
        event.respondWith(cacheFirst(request, IMAGE_CACHE));
        return;
    }

    // Scripts, styles, fonts, icons: served from cache for speed, refreshed in the
    // background so a deployment is picked up on the next load.
    if (isStaticAsset(url)) {
        event.respondWith(staleWhileRevalidate(event, STATIC_CACHE));
    }
});

function isStaticAsset(url) {
    return /\.(js|css|woff2|png|jpg|webp|json)$/.test(url.pathname);
}

async function cacheFirst(request, cacheName) {
    const cached = await caches.match(request);
    if (cached) return cached;
    const response = await fetch(request);
    if (response.ok) {
        const cache = await caches.open(cacheName);
        await cache.put(request, response.clone());
    }
    return response;
}

async function staleWhileRevalidate(event, cacheName) {
    const cache = await caches.open(cacheName);
    const cached = await cache.match(event.request);
    const network = fetch(event.request).then(response => {
        if (response.ok) cache.put(event.request, response.clone());
        return response;
    });
    if (cached) {
        event.waitUntil(network.catch(() => {}));
        return cached;
    }
    return network;
}
