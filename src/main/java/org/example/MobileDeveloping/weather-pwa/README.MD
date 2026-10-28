Цель: Построить погодное приложение и последовательно усложнять его, добавляя кэширование, офлайн-режим, отмену запросов и retry.
Каждая версия – должен быть рабочий код, чтобы увидеть результат (см. где указано слово Проверка).

## Docker

Контекст сборки — папка `MobileDeveloping` (Dockerfile копирует `weather-pwa/` в образ). В контейнере: `serve` на порту 5500 и localtunnel.

```
cd src\main\java\org\example\MobileDeveloping
docker build -f weather-pwa/Dockerfile -t my-pwa-lt .
docker run --rm -p 5500:5500 my-pwa-lt
```

- Локально: http://127.0.0.1:5500/
- С телефона: URL вида `https://….loca.lt` печатается в логе контейнера (`your url is: …`)

Остановка: Ctrl+C в терминале с `docker run` (флаг `--rm` удалит контейнер).
