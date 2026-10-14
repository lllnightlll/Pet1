class WeatherIndexedDbCache {
    constructor() {
        this.weatherStore = localforage.createInstance({
            name: 'weather-pwa',
            storeName: 'weather_cache'
        });
        this.maxAgeMs = 24 * 60 * 60 * 1000;
    }

    /**
     * Пишет успешный ответ. localForage сам кладёт объект в IndexedDB.
     * @param {string} city
     * @param {{name: string, temperature: number}} data
     */
    async save(city, data) {
        await this.weatherStore.setItem(city.toLowerCase(), {
            data: data,
            timestamp: Date.now()
        });
    }

    /**
     * Читает кэш города. isStale = запись старше суток.
     * @param {string} city
     * @returns {Promise<{data: {name: string, temperature: number}, isStale: boolean}|null>}
     */
    async get(city) {
        const cached = await this.weatherStore.getItem(city.toLowerCase());
        if (!cached) {
            return null;
        }

        return this.toCacheResult(cached);
    }

    /**
     * Берёт самую свежую запись: после reload город в форме неизвестен.
     * @returns {Promise<{city: string, data: {name: string, temperature: number}, isStale: boolean}|null>}
     */
    async getLatest() {
        let latestCity = null;
        let latestEntry = null;

        await this.weatherStore.iterate((value, key) => {
            if (!value || typeof value.timestamp !== 'number' || !value.data) {
                return;
            }

            if (!latestEntry || value.timestamp > latestEntry.timestamp) {
                latestCity = key;
                latestEntry = value;
            }
        });

        if (!latestEntry) {
            return null;
        }

        const result = this.toCacheResult(latestEntry);
        result.city = latestEntry.data.city || latestCity;
        return result;
    }

    /**
     * @param {{data: object, timestamp: number}} cached
     */
    toCacheResult(cached) {
        const age = Date.now() - cached.timestamp;
        return {
            data: cached.data,
            isStale: age > this.maxAgeMs
        };
    }
}

const weatherCache = new WeatherIndexedDbCache();

/**
 * Сохраняет прогноз по городу. Ошибку IndexedDB не пробрасываем на UI.
 * @param {string} city
 * @param {{name: string, temperature: number}} data
 */
async function saveWeatherToCache(city, data) {
    try {
        await weatherCache.save(city, data);
    } catch (err) {
        console.error('Ошибка сохранения:', err);
    }
}

/**
 * Возвращает кэш города или null, если записи нет / хранилище недоступно.
 * @param {string} city
 */
async function getWeatherFromCache(city) {
    try {
        return await weatherCache.get(city);
    } catch (err) {
        return null;
    }
}

/**
 * Последний сохранённый город — для восстановления UI после reload.
 */
async function getLastWeatherFromCache() {
    try {
        return await weatherCache.getLatest();
    } catch (err) {
        return null;
    }
}
