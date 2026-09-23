const CACHE_NAME = 'v1';
const ASSETS = [
  './',
  './index.html',
  './styles.css',
  './parchment.ttf',
  './manifest.json',
  './image.png'
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