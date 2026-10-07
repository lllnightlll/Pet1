/**
 * Service Worker магазина: кэширует оболочку PWA при установке
 * и отдаёт её из кэша при повторных запросах.
 */
const CACHE_NAME = 'shop-pwa-v11';
const ASSETS = [
  './',
  './index.html',
  './styles.css',
  './manifest.json',
  './avatar.png',
  './parchment.ttf',
  './js/navigator-sw.js',
  './js/vpn-gate.js',
  './js/product-catalog.js',
  './js/favorites-store.js',
  './js/profile-navigator.js',
  './js/not-found-screen.js',
  './js/screen-factory.js',
  './js/router-shell.js',
  './js/route-table.js',
  './js/breadcrumb-trail.js',
  './js/path-matcher.js',
  './js/app-location.js',
  './js/app-router.js',
  './js/navigation-api-binder.js',
  './js/app-interaction-binder.js',
  './js/theme-controller.js',
  './js/app.js'
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

function isSpaDocumentPath(pathname) {
  return pathname === '/favorites'
    || pathname === '/settings'
    || /^\/product\/\d+$/.test(pathname);
}

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
        return fetch(event.request).then((response) => {
          if (
            event.request.mode === 'navigate'
            && !response.ok
            && isSpaDocumentPath(requestUrl.pathname)
          ) {
            return caches.match('./index.html').then((indexPage) => indexPage || response);
          }
          return response;
        });
      })
  );
});
