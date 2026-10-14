class WeatherConfig {
    static NOMINATIM_URL = 'https://nominatim.openstreetmap.org/search';
    static FORECAST_URL = 'https://api.met.no/weatherapi/locationforecast/2.0/compact';
    static LANGUAGE = 'ru';

    static FORM_ID = 'city-form';
    static CITY_INPUT_ID = 'city-input';
    static PLACE_ELEMENT_ID = 'place';
    static TEMPERATURE_ELEMENT_ID = 'temperature';

    static CACHE_WARNING_ELEMENT_ID = 'cache-warning';

    static ERROR_MESSAGE = 'Error loading';
    static LOADING_MESSAGE = 'Loading...';
    static CACHE_WARNING_MESSAGE = 'Showing cached data';
}

class NominatimGeocoder {
    /**
     * @param {string} city название города из формы
     * @returns {Promise<{latitude: number, longitude: number, name: string}>}
     */
    async geocode(city) {
        const encodedCity = encodeURIComponent(city);
        const geoUrl = `${WeatherConfig.NOMINATIM_URL}?q=${encodedCity}&format=json&limit=1&accept-language=${WeatherConfig.LANGUAGE}`;

        const response = await fetch(geoUrl);
        if (!response.ok) {
            throw new Error(`Nominatim HTTP ${response.status}`);
        }

        const geoData = await response.json();
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
    async loadTemperature(latitude, longitude) {
        const url = `${WeatherConfig.FORECAST_URL}?lat=${latitude}&lon=${longitude}`;
        const response = await fetch(url);

        if (!response.ok) {
            throw new Error(`Forecast HTTP ${response.status}`);
        }

        const data = await response.json();
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
        this.geocoder = new NominatimGeocoder();
        this.weatherClient = new WeatherClient();
        this.view = new WeatherView(
            WeatherConfig.PLACE_ELEMENT_ID,
            WeatherConfig.TEMPERATURE_ELEMENT_ID,
            WeatherConfig.CACHE_WARNING_ELEMENT_ID
        );
        this.form = document.getElementById(WeatherConfig.FORM_ID);
        this.cityInput = document.getElementById(WeatherConfig.CITY_INPUT_ID);

        this.form.addEventListener('submit', (event) => {
            this.onSubmit(event);
        });

        this.restoreCache();
    }

    /**
     * IndexedDB читается асинхронно, поэтому после reload город
     * сам не появится, пока явно не достанем последнюю запись.
     */
    async restoreCache() {
        const cached = await getLastWeatherFromCache();
        if (!cached) {
            return;
        }

        this.cityInput.value = cached.city;
        this.view.showCachedWeather(cached.data.name, cached.data.temperature);
    }

    onSubmit(event) {
        event.preventDefault();
        const city = this.cityInput.value.trim();
        if (!city) {
            return;
        }
        this.loadWeather(city);
    }

    async loadWeather(city) {
        this.view.showLoading();

        try {
            const { latitude, longitude, name } = await this.geocoder.geocode(city);
            const temperature = await this.weatherClient.loadTemperature(latitude, longitude);
            const data = { city, name, temperature };
            await saveWeatherToCache(city, data);
            this.view.showWeather(name, temperature);
        } catch (error) {
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
