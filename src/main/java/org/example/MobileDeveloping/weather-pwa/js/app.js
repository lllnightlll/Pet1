class WeatherConfig {
    static NOMINATIM_URL = 'https://nominatim.openstreetmap.org/search';
    static NOMINATIM_REVERSE_URL = 'https://nominatim.openstreetmap.org/reverse';
    static FORECAST_URL = 'https://api.met.no/weatherapi/locationforecast/2.0/compact';
    static LANGUAGE = 'ru';

    static FORM_ID = 'city-form';
    static CITY_INPUT_ID = 'city-input';
    static LOCATE_BUTTON_ID = 'locate-button';
    static PLACE_ELEMENT_ID = 'place';
    static TEMPERATURE_ELEMENT_ID = 'temperature';

    static CACHE_WARNING_ELEMENT_ID = 'cache-warning';
    static NETWORK_STATUS_ELEMENT_ID = 'network-status';

    static ERROR_MESSAGE = 'Error loading';
    static LOADING_MESSAGE = 'Loading...';
    static CACHE_WARNING_MESSAGE = 'Showing cached data';
    static ONLINE_TEXT = 'Online';
    static OFFLINE_TEXT = 'Offline';
    static ONLINE_CLASS = 'network-online';
    static OFFLINE_CLASS = 'network-offline';
    static ABORT_ERROR_NAME = 'AbortError';
    static RETRY_MAX_ATTEMPTS = 4;
    static RETRY_BASE_DELAY_MS = 1000;

    static GEO_TIMEOUT_MS = 10000;
    static GEO_MAXIMUM_AGE_MS = 60000;
    static GEO_PERMISSION_DENIED = 'Geolocation permission denied';
    static GEO_POSITION_UNAVAILABLE = 'Geolocation unavailable';
    static GEO_TIMEOUT = 'Geolocation timeout';
    static GEO_UNSUPPORTED = 'Geolocation is not supported';
    static GEO_PERMISSION_DENIED_CODE = 1;
    static GEO_POSITION_UNAVAILABLE_CODE = 2;
    static GEO_TIMEOUT_CODE = 3;
    static CURRENT_LOCATION_LABEL = 'Current location';
    static GEO_FOREGROUND_WARNING_ID = 'geo-foreground-warning';
    static GEO_FOREGROUND_WARNING =
        'Geolocation works only while the screen is active. iOS does not update coordinates in the background.';

    static PHOTO_BUTTON_ID = 'photo-button';
    static CAPTURE_BUTTON_ID = 'capture-button';
    static CAMERA_VIDEO_ID = 'camera-video';
    static CAMERA_CANVAS_ID = 'camera-canvas';
    static PHOTO_PREVIEW_ID = 'photo-preview';
    static PHOTO_SIZE_ID = 'photo-size';
    static PHOTO_MIME = 'image/jpeg';
    static PHOTO_QUALITY = 0.72;
    static PHOTO_MAX_WIDTH = 800;
    static CAMERA_HIDDEN_CLASS = 'is-hidden';
    static CAMERA_PERMISSION_DENIED = 'Camera permission denied';
    static CAMERA_NOT_FOUND = 'Camera not found';
    static CAMERA_UNSUPPORTED = 'Camera is not supported';
    static PHOTO_SIZE_LABEL = 'Compressed:';
    static PHOTO_SEND_BUTTON_ID = 'photo-send-button';
    static PHOTO_SAVED_MESSAGE = 'Photo saved to IndexedDB:';
    static PHOTO_SAVE_ERROR = 'Photo was not saved';
    static PICKME_RESULT_ID = 'pickme-result';
    static PICKME_YES = 'пикми';
    static PICKME_NO = 'не пикми';
    static PICKME_LOADING = 'определяю...';
    static PICKME_ERROR = 'не пикми';
    static PICKME_MODEL_URL = 'https://cdn.jsdelivr.net/npm/@vladmandic/face-api@1.7.15/model';
    static PICKME_MATCH_DISTANCE = 0.55;
    static PICKME_REFERENCE_URLS = [
        '/img/pickme-1.jpg',
        '/img/pickme-2.jpg',
        '/img/pickme-3.jpg'
    ];

    static SHAKE_BUTTON_ID = 'shake-button';
    static SHAKE_STOP_BUTTON_ID = 'shake-stop-button';
    static SHAKE_STATUS_ID = 'shake-status';
    static SHAKE_COUNT_ID = 'shake-count';
    static SHAKE_COUNT_LABEL = 'Shakes:';
    static SHAKE_STOPPED = 'Game stopped';
    static WEATHER_CARD_ID = 'weather-card';
    static SHAKE_SHUFFLE_CLASS = 'is-shuffling';
    static SHAKE_THRESHOLD = 18;
    static SHAKE_COOLDOWN_MS = 1500;
    static SHAKE_HINT = 'Shake the phone to shuffle the city';
    static SHAKE_PERMISSION_DENIED = 'Motion permission denied';
    static SHAKE_UNSUPPORTED = 'Device motion is not supported';
    static SHUFFLE_CITIES = ['London', 'Paris', 'Tokyo', 'Berlin', 'Rome', 'Madrid', 'Oslo', 'Helsinki'];
}

/**
 * Один цикл загрузки: geocode + forecast можно оборвать вместе.
 */
class WeatherRequestSession {
    constructor() {
        this.controller = new AbortController();
    }

    /**
     * @returns {AbortSignal}
     */
    get signal() {
        return this.controller.signal;
    }

    abort() {
        this.controller.abort();
    }
}

class NominatimGeocoder {
    /**
     * @param {RetryingHttpClient} httpClient
     */
    constructor(httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * @param {string} city название города из формы
     * @param {AbortSignal} [signal]
     * @returns {Promise<{latitude: number, longitude: number, name: string}>}
     */
    async geocode(city, signal) {
        const encodedCity = encodeURIComponent(city);
        const geoUrl = `${WeatherConfig.NOMINATIM_URL}?q=${encodedCity}&format=json&limit=1&accept-language=${WeatherConfig.LANGUAGE}`;

        const geoData = await this.httpClient.fetchJson(geoUrl, signal);
        if (!Array.isArray(geoData) || geoData.length === 0) {
            throw new Error('City not found');
        }

        const { lat, lon, display_name } = geoData[0];
        const latitude = parseFloat(lat);
        const longitude = parseFloat(lon);
        const name = display_name.split(',')[0];

        return { latitude, longitude, name };
    }

    /**
     * Координаты → короткое имя места. Если reverse не ответил — запасной текст.
     * @param {number} latitude
     * @param {number} longitude
     * @param {AbortSignal} [signal]
     * @returns {Promise<string>}
     */
    async reverseGeocode(latitude, longitude, signal) {
        const geoUrl = `${WeatherConfig.NOMINATIM_REVERSE_URL}?lat=${latitude}&lon=${longitude}&format=json&accept-language=${WeatherConfig.LANGUAGE}`;
        const geoData = await this.httpClient.fetchJson(geoUrl, signal);
        if (!geoData || !geoData.display_name) {
            return WeatherConfig.CURRENT_LOCATION_LABEL;
        }
        return geoData.display_name.split(',')[0];
    }
}

class WeatherClient {
    /**
     * @param {RetryingHttpClient} httpClient
     */
    constructor(httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * @param {number} latitude
     * @param {number} longitude
     * @param {AbortSignal} [signal]
     */
    async loadTemperature(latitude, longitude, signal) {
        const url = `${WeatherConfig.FORECAST_URL}?lat=${latitude}&lon=${longitude}`;
        const data = await this.httpClient.fetchJson(url, signal);
        return data.properties.timeseries[0].data.instant.details.air_temperature;
    }
}

/**
 * Обёртка над navigator.geolocation.getCurrentPosition.
 * Только разовый снимок: watchPosition не используем — iOS в фоне
 * координаты не отдаёт, обновлять их «втихую» было бы обманом.
 */
class DeviceGeolocation {
    /**
     * @returns {Promise<{latitude: number, longitude: number}>}
     */
    getCurrentPosition() {
        return new Promise((resolve, reject) => {
            if (!navigator.geolocation) {
                reject(new Error(WeatherConfig.GEO_UNSUPPORTED));
                return;
            }

            navigator.geolocation.getCurrentPosition(
                (position) => {
                    resolve({
                        latitude: position.coords.latitude,
                        longitude: position.coords.longitude
                    });
                },
                (error) => {
                    reject(error);
                },
                {
                    enableHighAccuracy: true,
                    timeout: WeatherConfig.GEO_TIMEOUT_MS,
                    maximumAge: WeatherConfig.GEO_MAXIMUM_AGE_MS
                }
            );
        });
    }

    /**
     * @param {unknown} error
     * @returns {string}
     */
    toUserMessage(error) {
        const code = error && error.code;
        if (code === WeatherConfig.GEO_PERMISSION_DENIED_CODE) {
            return WeatherConfig.GEO_PERMISSION_DENIED;
        }
        if (code === WeatherConfig.GEO_POSITION_UNAVAILABLE_CODE) {
            return WeatherConfig.GEO_POSITION_UNAVAILABLE;
        }
        if (code === WeatherConfig.GEO_TIMEOUT_CODE) {
            return WeatherConfig.GEO_TIMEOUT;
        }
        return error && error.message ? error.message : WeatherConfig.GEO_UNSUPPORTED;
    }
}

class WeatherView {
    constructor(placeElementId, temperatureElementId, warningElementId, geoWarningElementId) {
        this.placeElement = document.getElementById(placeElementId);
        this.temperatureElement = document.getElementById(temperatureElementId);
        this.warningElement = document.getElementById(warningElementId);
        this.geoWarningElement = document.getElementById(geoWarningElementId);
    }

    showLoading() {
        this.placeElement.textContent = '';
        this.temperatureElement.textContent = WeatherConfig.LOADING_MESSAGE;
        this.hideCacheWarning();
        this.hideGeoForegroundWarning();
    }

    showWeather(name, temperature) {
        this.placeElement.textContent = name;
        this.temperatureElement.textContent = `${temperature} °C`;
        this.hideCacheWarning();
    }

    /**
     * Тот же прогноз, но с пометкой, что данные из IndexedDB.
     * @param {string} name
     * @param {number} temperature
     */
    showCachedWeather(name, temperature) {
        this.placeElement.textContent = name;
        this.temperatureElement.textContent = `${temperature} °C`;
        this.warningElement.textContent = WeatherConfig.CACHE_WARNING_MESSAGE;
    }

    /**
     * @param {string} [message]
     */
    showError(message) {
        this.placeElement.textContent = '';
        this.temperatureElement.textContent = message || WeatherConfig.ERROR_MESSAGE;
        this.hideCacheWarning();
    }

    hideCacheWarning() {
        this.warningElement.textContent = '';
    }

    showGeoForegroundWarning() {
        this.geoWarningElement.textContent = WeatherConfig.GEO_FOREGROUND_WARNING;
    }

    hideGeoForegroundWarning() {
        this.geoWarningElement.textContent = '';
    }
}

class WeatherApp {
    constructor() {
        const retryPolicy = new RetryPolicy({
            maxAttempts: WeatherConfig.RETRY_MAX_ATTEMPTS,
            baseDelayMs: WeatherConfig.RETRY_BASE_DELAY_MS,
            onRetry: (attempt, delayMs, error) => {
                console.log(
                    `Попытка ${attempt} не удалась (${error.message}), повтор через ${delayMs} мс`
                );
            }
        });
        const httpClient = new RetryingHttpClient(retryPolicy);
        this.geocoder = new NominatimGeocoder(httpClient);
        this.weatherClient = new WeatherClient(httpClient);
        this.deviceGeolocation = new DeviceGeolocation();
        this.view = new WeatherView(
            WeatherConfig.PLACE_ELEMENT_ID,
            WeatherConfig.TEMPERATURE_ELEMENT_ID,
            WeatherConfig.CACHE_WARNING_ELEMENT_ID,
            WeatherConfig.GEO_FOREGROUND_WARNING_ID
        );
        this.form = document.getElementById(WeatherConfig.FORM_ID);
        this.cityInput = document.getElementById(WeatherConfig.CITY_INPUT_ID);
        this.locateButton = document.getElementById(WeatherConfig.LOCATE_BUTTON_ID);
        this.currentRequest = null;
        this.hasUserRequestedWeather = false;

        this.form.addEventListener('submit', (event) => {
            this.onSubmit(event);
        });
        this.locateButton.addEventListener('click', () => {
            this.onLocate();
        });

        this.cameraCapture = new CameraCapture({
            videoElement: document.getElementById(WeatherConfig.CAMERA_VIDEO_ID),
            canvasElement: document.getElementById(WeatherConfig.CAMERA_CANVAS_ID),
            previewElement: document.getElementById(WeatherConfig.PHOTO_PREVIEW_ID),
            startButton: document.getElementById(WeatherConfig.PHOTO_BUTTON_ID),
            captureButton: document.getElementById(WeatherConfig.CAPTURE_BUTTON_ID),
            sizeElement: document.getElementById(WeatherConfig.PHOTO_SIZE_ID),
            sendButton: document.getElementById(WeatherConfig.PHOTO_SEND_BUTTON_ID),
            resultElement: document.getElementById(WeatherConfig.PICKME_RESULT_ID),
            classifier: new PickMeClassifier({
                modelUrl: WeatherConfig.PICKME_MODEL_URL,
                referenceImageUrls: WeatherConfig.PICKME_REFERENCE_URLS,
                matchDistance: WeatherConfig.PICKME_MATCH_DISTANCE
            }),
            mimeType: WeatherConfig.PHOTO_MIME,
            quality: WeatherConfig.PHOTO_QUALITY,
            maxWidth: WeatherConfig.PHOTO_MAX_WIDTH,
            hiddenClass: WeatherConfig.CAMERA_HIDDEN_CLASS
        });

        this.shakeGame = new ShakeGame({
            startButton: document.getElementById(WeatherConfig.SHAKE_BUTTON_ID),
            stopButton: document.getElementById(WeatherConfig.SHAKE_STOP_BUTTON_ID),
            statusElement: document.getElementById(WeatherConfig.SHAKE_STATUS_ID),
            countElement: document.getElementById(WeatherConfig.SHAKE_COUNT_ID),
            cardElement: document.getElementById(WeatherConfig.WEATHER_CARD_ID),
            getCurrentCity: () => this.cityInput.value.trim(),
            cities: WeatherConfig.SHUFFLE_CITIES,
            shakeThreshold: WeatherConfig.SHAKE_THRESHOLD,
            cooldownMs: WeatherConfig.SHAKE_COOLDOWN_MS,
            shuffleClass: WeatherConfig.SHAKE_SHUFFLE_CLASS,
            onShake: (city) => this.onShakeShuffle(city)
        });

        this.networkStatus = new NetworkStatus(WeatherConfig.NETWORK_STATUS_ELEMENT_ID, {
            onlineText: WeatherConfig.ONLINE_TEXT,
            offlineText: WeatherConfig.OFFLINE_TEXT,
            onlineClass: WeatherConfig.ONLINE_CLASS,
            offlineClass: WeatherConfig.OFFLINE_CLASS,
            onOnline: () => this.refreshCurrentCity()
        });

        this.restoreCache();
    }

    /**
     * После online браузер сам не знает город — берём его из поля ввода.
     */
    refreshCurrentCity() {
        const city = this.cityInput.value.trim();
        if (!city) {
            return;
        }
        this.loadWeather(city);
    }

    /**
     * Встряска выбрала новый город — подставляем в форму и грузим погоду.
     * @param {string} city
     */
    onShakeShuffle(city) {
        this.hasUserRequestedWeather = true;
        this.cityInput.value = city;
        this.loadWeather(city);
    }

    /**
     * IndexedDB читается асинхронно, поэтому после reload город
     * сам не появится, пока явно не достанем последнюю запись.
     * Если сеть есть — сразу перезапрашиваем погоду и перезаписываем weather_cache.
     */
    async restoreCache() {
        const cached = await getLastWeatherFromCache();

        // Пока ждали IndexedDB, пользователь уже мог нажать Show.
        if (this.hasUserRequestedWeather) {
            return;
        }

        if (cached) {
            this.cityInput.value = cached.city;
            this.view.showCachedWeather(cached.data.name, cached.data.temperature);
        }

        if (this.hasUserRequestedWeather) {
            return;
        }

        if (this.networkStatus.isOnline()) {
            this.refreshCurrentCity();
        }
    }

    onSubmit(event) {
        event.preventDefault();
        const city = this.cityInput.value.trim();
        if (!city) {
            return;
        }
        this.hasUserRequestedWeather = true;
        this.loadWeather(city);
    }

    /**
     * 📍 → разрешение → координаты → погода.
     * Нет сети — сразу кэш последних координат из localStorage.
     */
    async onLocate() {
        this.hasUserRequestedWeather = true;

        if (!this.networkStatus.isOnline()) {
            this.showLastKnownGeoOrError();
            return;
        }

        this.view.showLoading();

        try {
            const coords = await this.deviceGeolocation.getCurrentPosition();
            await this.loadWeatherByCoords(coords.latitude, coords.longitude);
        } catch (error) {
            if (this.showLastKnownGeo()) {
                return;
            }
            this.view.showError(this.deviceGeolocation.toUserMessage(error));
        }
    }

    /**
     * Fallback геолокации: последние lat/lon и температура из localStorage.
     * @returns {boolean}
     */
    showLastKnownGeo() {
        const saved = getLastKnownGeo();
        if (!saved) {
            return false;
        }

        this.cityInput.value = saved.name;
        this.view.showCachedWeather(saved.name, saved.temperature);
        return true;
    }

    showLastKnownGeoOrError() {
        if (this.showLastKnownGeo()) {
            return;
        }
        this.view.showError();
    }

    /**
     * Погода по уже известным координатам (геолокация).
     * @param {number} latitude
     * @param {number} longitude
     */
    async loadWeatherByCoords(latitude, longitude) {
        if (this.currentRequest) {
            this.currentRequest.abort();
        }

        const request = new WeatherRequestSession();
        this.currentRequest = request;
        this.view.showLoading();

        try {
            let name = WeatherConfig.CURRENT_LOCATION_LABEL;
            try {
                name = await this.geocoder.reverseGeocode(latitude, longitude, request.signal);
            } catch (reverseError) {
                if (request.signal.aborted) {
                    throw reverseError;
                }
            }

            const temperature = await this.weatherClient.loadTemperature(
                latitude,
                longitude,
                request.signal
            );
            const data = { city: name, name, temperature };
            await saveWeatherToCache(name, data);
            saveLastKnownGeo({ latitude, longitude, name, temperature });
            this.cityInput.value = name;

            if (this.currentRequest !== request) {
                return;
            }

            this.view.showWeather(name, temperature);
            this.view.showGeoForegroundWarning();
        } catch (error) {
            if (request.signal.aborted) {
                return;
            }
            if (this.showLastKnownGeo()) {
                return;
            }
            await this.showCacheOrError(this.cityInput.value.trim() || WeatherConfig.CURRENT_LOCATION_LABEL);
        }
    }

    /**
     * Новый город отменяет предыдущий fetch, иначе ответ Лондона
     * может прийти позже Парижа и перезаписать UI.
     * @param {string} city
     */
    async loadWeather(city) {
        if (this.currentRequest) {
            this.currentRequest.abort();
        }

        const request = new WeatherRequestSession();
        this.currentRequest = request;
        this.view.showLoading();

        try {
            const { latitude, longitude, name } = await this.geocoder.geocode(city, request.signal);
            const temperature = await this.weatherClient.loadTemperature(
                latitude,
                longitude,
                request.signal
            );
            const data = { city, name, temperature };
            await saveWeatherToCache(city, data);

            if (this.currentRequest !== request) {
                return;
            }

            this.view.showWeather(name, temperature);
        } catch (error) {
            // Таймаут попытки — не abort пользователя, его ретраим / показываем кэш.
            if (request.signal.aborted) {
                return;
            }
            await this.showCacheOrError(city);
        }
    }

    /**
     * Сеть недоступна: кэш этого города из IndexedDB, иначе ошибка.
     * @param {string} city
     */
    async showCacheOrError(city) {
        const cached = await getWeatherFromCache(city);
        if (!cached) {
            this.view.showError();
            return;
        }

        this.view.showCachedWeather(cached.data.name, cached.data.temperature);
    }
}

new WeatherApp();
