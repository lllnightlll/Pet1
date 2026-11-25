/**
 * Учебный AES-GCM: неизвлекаемый CryptoKey в IndexedDB.
 *
 * Это НЕ делает токены безопасными. XSS на странице вызывает decrypt
 * в активной сессии и читает plaintext. Ключ с extractable: false
 * нельзя выгрузить, но расшифрованные данные — можно.
 * Токены аутентификации — только HttpOnly cookie: JS (и XSS) её не читает.
 * XSS всё ещё может дернуть запрос от имени пользователя (CSRF) — SameSite.
 * Set-Cookie из Service Worker не взлетит: это forbidden response header.
 * Практика: HttpOnly + Secure + SameSite=Lax|Strict; префикс __Host-
 * (требует Secure, Path=/, без Domain — поддомен cookie не перезапишет).
 * Шифрование в IndexedDB — только для данных, которые не открывают аккаунт.
 * weather_cache шифруем как учебный пример; токены так хранить нельзя.
 */
class NonExtractableKeyStore {
    /**
     * @param {{
     *   dbName: string,
     *   dbVersion: number,
     *   storeName: string,
     *   keyId: string
     * }} options
     */
    constructor(options) {
        this.dbName = options.dbName;
        this.dbVersion = options.dbVersion;
        this.storeName = options.storeName;
        this.keyId = options.keyId;
    }

    /**
     * IndexedDB умеет structured clone CryptoKey и сохраняет extractable: false.
     * @returns {Promise<IDBDatabase>}
     */
    openDatabase() {
        return new Promise((resolve, reject) => {
            const request = indexedDB.open(this.dbName, this.dbVersion);
            request.onupgradeneeded = () => {
                const db = request.result;
                if (!db.objectStoreNames.contains(this.storeName)) {
                    db.createObjectStore(this.storeName);
                }
            };
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    /**
     * @param {IDBDatabase} db
     * @returns {Promise<CryptoKey|undefined>}
     */
    loadKey(db) {
        return new Promise((resolve, reject) => {
            const tx = db.transaction(this.storeName, 'readonly');
            const request = tx.objectStore(this.storeName).get(this.keyId);
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    /**
     * @param {IDBDatabase} db
     * @param {CryptoKey} key
     */
    saveKey(db, key) {
        return new Promise((resolve, reject) => {
            const tx = db.transaction(this.storeName, 'readwrite');
            tx.objectStore(this.storeName).put(key, this.keyId);
            tx.oncomplete = () => resolve();
            tx.onerror = () => reject(tx.error);
        });
    }
}

class AesGcmCipher {
    /**
     * @param {{
     *   algorithm: string,
     *   keyLength: number,
     *   extractable: boolean,
     *   usages: KeyUsage[],
     *   ivBytes: number
     * }} options
     */
    constructor(options) {
        this.algorithm = options.algorithm;
        this.keyLength = options.keyLength;
        this.extractable = options.extractable;
        this.usages = options.usages;
        this.ivBytes = options.ivBytes;
    }

    /**
     * extractable: false — crypto.subtle.exportKey отклонит ключ.
     * @returns {Promise<CryptoKey>}
     */
    generateKey() {
        return crypto.subtle.generateKey(
            { name: this.algorithm, length: this.keyLength },
            this.extractable,
            this.usages
        );
    }

    /**
     * IV 12 байт (96 бит) — рекомендация NIST для AES-GCM.
     * @param {CryptoKey} key
     * @param {string} plaintext
     * @returns {Promise<{iv: Uint8Array, ciphertext: ArrayBuffer}>}
     */
    async encryptText(key, plaintext) {
        const iv = crypto.getRandomValues(new Uint8Array(this.ivBytes));
        const encoded = new TextEncoder().encode(plaintext);
        const ciphertext = await crypto.subtle.encrypt(
            { name: this.algorithm, iv: iv },
            key,
            encoded
        );
        return { iv, ciphertext };
    }

    /**
     * @param {CryptoKey} key
     * @param {Uint8Array} iv
     * @param {BufferSource} ciphertext
     * @returns {Promise<string>}
     */
    async decryptText(key, iv, ciphertext) {
        const decrypted = await crypto.subtle.decrypt(
            { name: this.algorithm, iv: iv },
            key,
            ciphertext
        );
        return new TextDecoder().decode(decrypted);
    }
}

class WeatherCryptoDemo {
    constructor() {
        this.keyStore = new NonExtractableKeyStore({
            dbName: WeatherConfig.CRYPTO_DB_NAME,
            dbVersion: WeatherConfig.CRYPTO_DB_VERSION,
            storeName: WeatherConfig.CRYPTO_KEYS_STORE,
            keyId: WeatherConfig.CRYPTO_KEY_ID
        });
        this.cipher = new AesGcmCipher({
            algorithm: WeatherConfig.CRYPTO_ALGORITHM,
            keyLength: WeatherConfig.CRYPTO_KEY_LENGTH,
            extractable: WeatherConfig.CRYPTO_EXTRACTABLE,
            usages: WeatherConfig.CRYPTO_KEY_USAGES,
            ivBytes: WeatherConfig.CRYPTO_IV_BYTES
        });
    }

    /**
     * Кладёт session_key в security-test, гоняет encrypt/decrypt
     * на '{"temp": 5}' и возвращает ключ для шифрования weather_cache.
     * @returns {Promise<CryptoKey>}
     */
    async run() {
        const db = await this.keyStore.openDatabase();
        let key = await this.keyStore.loadKey(db);
        if (!key) {
            key = await this.cipher.generateKey();
            await this.keyStore.saveKey(db, key);
        }
        db.close();

        const { iv, ciphertext } = await this.cipher.encryptText(
            key,
            WeatherConfig.CRYPTO_DEMO_PAYLOAD
        );
        const plaintext = await this.cipher.decryptText(key, iv, ciphertext);
        console.log(plaintext);
        return key;
    }
}
