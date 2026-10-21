class WeatherConfig {
    static NOMINATIM_URL = 'https://nominatim.openstreetmap.org/search';
    static FORECAST_URL = 'https://api.met.no/weatherapi/locationforecast/2.0/compact';
    static LANGUAGE = 'ru';

    static FORM_ID = 'city-form';
    static CITY_INPUT_ID = 'city-input';
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

class WeatherView {
    constructor(placeElementId, temperatureElementId, warningElementId) {
        this.placeElement = document.getElementById(placeElementId);
        this.temperatureElement = document.getElementById(temperatureElementId);
        this.warningElement = document.getElementById(warningElementId);
    }

    showLoading() {
        this.placeElement.textContent = '';
        this.temperatureElement.textContent = WeatherConfig.LOADING_MESSAGE;
        this.hideCacheWarning();
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

    showError() {
        this.placeElement.textContent = '';
        this.temperatureElement.textContent = WeatherConfig.ERROR_MESSAGE;
        this.hideCacheWarning();
    }

    hideCacheWarning() {
        this.warningElement.textContent = '';
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
        this.view = new WeatherView(
            WeatherConfig.PLACE_ELEMENT_ID,
            WeatherConfig.TEMPERATURE_ELEMENT_ID,
            WeatherConfig.CACHE_WARNING_ELEMENT_ID
        );
        this.form = document.getElementById(WeatherConfig.FORM_ID);
        this.cityInput = document.getElementById(WeatherConfig.CITY_INPUT_ID);
        this.currentRequest = null;
        this.hasUserRequestedWeather = false;

        this.form.addEventListener('submit', (event) => {
            this.onSubmit(event);
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
