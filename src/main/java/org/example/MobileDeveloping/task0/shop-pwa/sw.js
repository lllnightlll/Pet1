/**
 * Service Worker магазина: кэширует оболочку PWA при установке
 * и отдаёт её из кэша при повторных запросах.
 */
const CACHE_NAME = 'shop-pwa-v6';
const ASSETS = [
  './',
  './index.html',
  './styles.css',
  './manifest.json',
  './js/navigator-sw.js',
  './js/vpn-gate.js',
  './avatar.png',
  './parchment.ttf'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('Кэшируем файлы:', ASSETS);
        return cache.addAll(ASSETS);
      })
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(key => key !== CACHE_NAME).map(key => caches.delete(key)))
    ).then(() => clients.claim())
  );
});

self.addEventListener('fetch', event => {
  const requestUrl = new URL(event.request.url);
  if (requestUrl.origin !== self.location.origin) {
    return;
  }
  if (requestUrl.pathname === '/my_pwa_first' || requestUrl.pathname.startsWith('/my_pwa_first/')) {
    return;
  }

  event.respondWith(
    caches.match(event.request)
      .then(cached => {
        if (cached) {
          console.log('Отдаём из кэша:', event.request.url);
          return cached;
        }
        console.log('Загружаем из сети:', event.request.url);
        return fetch(event.request);
      })
  );
});
