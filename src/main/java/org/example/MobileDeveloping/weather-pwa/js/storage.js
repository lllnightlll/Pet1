class WeatherIndexedDbCache {
    constructor() {
        this.weatherStore = localforage.createInstance({
            name: 'weather-pwa',
            storeName: 'weather_cache'
        });
        this.maxAgeMs = 24 * 60 * 60 * 1000;
        this.cipher = null;
        this.key = null;
    }

    /**
     * AES-GCM из Шага 4. В DevTools ciphertext — ArrayBuffer, не JSON.
     * @param {CryptoKey} key
     * @param {AesGcmCipher} cipher
     */
    enableEncryption(key, cipher) {
        this.key = key;
        this.cipher = cipher;
    }

    /**
     * @returns {boolean}
     */
    isEncryptionEnabled() {
        return Boolean(this.key && this.cipher);
    }

    /**
     * Пишет прогноз. Поле data больше не кладём открытым текстом.
     * @param {string} city
     * @param {{name: string, temperature: number}} data
     * @param {number} [timestamp]
     */
    async save(city, data, timestamp) {
        const savedAt = typeof timestamp === 'number' ? timestamp : Date.now();
        let record;

        if (this.isEncryptionEnabled()) {
            const { iv, ciphertext } = await this.cipher.encryptText(
                this.key,
                JSON.stringify(data)
            );
            record = {
                iv: iv.buffer.slice(iv.byteOffset, iv.byteOffset + iv.byteLength),
                ciphertext: ciphertext,
                timestamp: savedAt
            };
        } else {
            record = { data: data, timestamp: savedAt };
        }

        await this.weatherStore.setItem(city.toLowerCase(), record);
    }

    /**
     * Старые открытые записи переписываем ciphertext+iv тем же timestamp.
     */
    async migratePlaintext() {
        if (!this.isEncryptionEnabled()) {
            return;
        }

        const pending = [];
        await this.weatherStore.iterate((value, key) => {
            if (value && value.data && !value.ciphertext) {
                pending.push({ key: key, value: value });
            }
        });

        for (const item of pending) {
            await this.save(item.key, item.value.data, item.value.timestamp);
        }
    }

    /**
     * @param {object} cached
     * @returns {Promise<{data: object, timestamp: number}|null>}
     */
    async decodeEntry(cached) {
        if (!cached) {
            return null;
        }

        if (cached.data) {
            return cached;
        }

        if (!cached.ciphertext || !this.isEncryptionEnabled()) {
            return null;
        }

        const iv = cached.iv instanceof Uint8Array
            ? cached.iv
            : new Uint8Array(cached.iv);
        const plaintext = await this.cipher.decryptText(this.key, iv, cached.ciphertext);
        return {
            data: JSON.parse(plaintext),
            timestamp: cached.timestamp
        };
    }

    /**
     * Читает кэш города. isStale = запись старше суток.
     * @param {string} city
     * @returns {Promise<{data: {name: string, temperature: number}, isStale: boolean}|null>}
     */
    async get(city) {
        const cached = await this.weatherStore.getItem(city.toLowerCase());
        const decoded = await this.decodeEntry(cached);
        if (!decoded) {
            return null;
        }

        return this.toCacheResult(decoded);
    }

    /**
     * Берёт самую свежую запись: после reload город в форме неизвестен.
     * @returns {Promise<{city: string, data: {name: string, temperature: number}, isStale: boolean}|null>}
     */
    async getLatest() {
        const entries = [];
        await this.weatherStore.iterate((value, key) => {
            entries.push({ key: key, value: value });
        });

        let latestCity = null;
        let latestEntry = null;

        for (const entry of entries) {
            const decoded = await this.decodeEntry(entry.value);
            if (!decoded || typeof decoded.timestamp !== 'number' || !decoded.data) {
                continue;
            }

            if (!latestEntry || decoded.timestamp > latestEntry.timestamp) {
                latestCity = entry.key;
                latestEntry = decoded;
            }
        }

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

/**
 * Последние координаты геолокации — fallback, когда сети нет.
 * localStorage синхронный: 📍 может показать кэш сразу, без IndexedDB.
 */
class LastKnownGeoStorage {
    /**
     * @param {string} storageKey
     */
    constructor(storageKey) {
        this.storageKey = storageKey;
    }

    /**
     * @param {{latitude: number, longitude: number, name: string, temperature: number}} snapshot
     */
    save(snapshot) {
        localStorage.setItem(this.storageKey, JSON.stringify(snapshot));
    }

    /**
     * @returns {{latitude: number, longitude: number, name: string, temperature: number}|null}
     */
    load() {
        const raw = localStorage.getItem(this.storageKey);
        if (!raw) {
            return null;
        }

        try {
            return JSON.parse(raw);
        } catch (error) {
            return null;
        }
    }
}

const lastKnownGeoStorage = new LastKnownGeoStorage('weather-pwa:last-geo');

/**
 * @param {{latitude: number, longitude: number, name: string, temperature: number}} snapshot
 */
function saveLastKnownGeo(snapshot) {
    try {
        lastKnownGeoStorage.save(snapshot);
    } catch (err) {
        console.error('Ошибка сохранения координат:', err);
    }
}

/**
 * @returns {{latitude: number, longitude: number, name: string, temperature: number}|null}
 */
function getLastKnownGeo() {
    try {
        return lastKnownGeoStorage.load();
    } catch (err) {
        return null;
    }
}

/**
 * Сжатые фото в отдельном store той же IndexedDB weather-pwa.
 */
class PhotoIndexedDbStore {
    constructor() {
        this.photoStore = localforage.createInstance({
            name: 'weather-pwa',
            storeName: 'photos'
        });
    }

    /**
     * @param {string} dataUrl
     * @returns {Promise<string>} ключ записи
     */
    async save(dataUrl) {
        const photoId = `photo-${Date.now()}`;
        await this.photoStore.setItem(photoId, {
            dataUrl: dataUrl,
            timestamp: Date.now()
        });
        return photoId;
    }
}

const photoStore = new PhotoIndexedDbStore();

/**
 * «Отправка» фото = запись dataURL в IndexedDB.
 * @param {string} dataUrl
 */
async function savePhotoToIndexedDb(dataUrl) {
    try {
        return await photoStore.save(dataUrl);
    } catch (err) {
        console.error('Ошибка сохранения фото:', err);
        return null;
    }
}
