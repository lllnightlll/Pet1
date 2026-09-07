const CACHE_NAME = 'v1';
// ВАЖНО: используем относительные пути без ведущего слеша
// или указываем полные пути с учётом базового URL
const ASSETS = [
  './',           // текущая директория (главная страница)
  './index.html', // файл index.html
  './manifest.json' // файл манифеста
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
  event.waitUntil(clients.claim());
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