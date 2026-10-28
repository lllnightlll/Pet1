/**
 * Игра «встряхни телефон»: DeviceMotionEvent + requestPermission (iOS).
 * При резком ускорении вызывается onShake.
 */
class ShakeGame {
    /**
     * @param {{
     *   startButton: HTMLElement,
     *   statusElement: HTMLElement,
     *   cardElement: HTMLElement,
     *   getCurrentCity: Function,
     *   cities: string[],
     *   shakeThreshold: number,
     *   cooldownMs: number,
     *   shuffleClass: string,
     *   stopButton: HTMLElement,
     *   countElement: HTMLElement,
     *   onShake: Function
     * }} options
     */
    constructor(options) {
        this.startButton = options.startButton;
        this.stopButton = options.stopButton;
        this.statusElement = options.statusElement;
        this.countElement = options.countElement;
        this.cardElement = options.cardElement;
        this.getCurrentCity = options.getCurrentCity;
        this.cities = options.cities;
        this.shakeThreshold = options.shakeThreshold;
        this.cooldownMs = options.cooldownMs;
        this.shuffleClass = options.shuffleClass;
        this.onShake = options.onShake;

        this.isListening = false;
        this.shakeCount = 0;
        this.lastShakeAt = 0;
        this.lastAcceleration = { x: 0, y: 0, z: 0 };
        this.handleMotion = (event) => this.onDeviceMotion(event);

        this.startButton.addEventListener('click', () => {
            this.requestPermissionAndStart();
        });
        this.stopButton.addEventListener('click', () => {
            this.stop();
        });
        this.renderCount();
    }

    /**
     * На iOS permission можно запросить только из клика.
     * На Android/desktop requestPermission нет — сразу слушаем движение.
     */
    async requestPermissionAndStart() {
        if (this.isListening) {
            this.statusElement.textContent = WeatherConfig.SHAKE_HINT;
            return;
        }

        if (typeof DeviceMotionEvent === 'undefined') {
            this.statusElement.textContent = WeatherConfig.SHAKE_UNSUPPORTED;
            return;
        }

        if (typeof DeviceMotionEvent.requestPermission === 'function') {
            try {
                const permission = await DeviceMotionEvent.requestPermission();
                if (permission !== 'granted') {
                    this.statusElement.textContent = WeatherConfig.SHAKE_PERMISSION_DENIED;
                    return;
                }
            } catch (error) {
                this.statusElement.textContent = WeatherConfig.SHAKE_PERMISSION_DENIED;
                return;
            }
        }

        window.addEventListener('devicemotion', this.handleMotion);
        this.isListening = true;
        this.statusElement.textContent = WeatherConfig.SHAKE_HINT;
    }

    /**
     * Снимает devicemotion — датчик больше не крутит игру.
     */
    stop() {
        window.removeEventListener('devicemotion', this.handleMotion);
        this.isListening = false;
        this.statusElement.textContent = WeatherConfig.SHAKE_STOPPED;
    }

    /**
     * Сравниваем соседние отсчёты accelerationIncludingGravity.
     * @param {DeviceMotionEvent} event
     */
    onDeviceMotion(event) {
        const acceleration = event.accelerationIncludingGravity;
        if (!acceleration) {
            return;
        }

        const deltaX = acceleration.x - this.lastAcceleration.x;
        const deltaY = acceleration.y - this.lastAcceleration.y;
        const deltaZ = acceleration.z - this.lastAcceleration.z;
        const intensity = Math.abs(deltaX) + Math.abs(deltaY) + Math.abs(deltaZ);

        this.lastAcceleration = {
            x: acceleration.x || 0,
            y: acceleration.y || 0,
            z: acceleration.z || 0
        };

        if (intensity < this.shakeThreshold) {
            return;
        }

        const now = Date.now();
        if (now - this.lastShakeAt < this.cooldownMs) {
            return;
        }

        this.lastShakeAt = now;
        this.shakeCount += 1;
        this.renderCount();
        this.shuffleCity();
    }

    renderCount() {
        this.countElement.textContent = `${WeatherConfig.SHAKE_COUNT_LABEL} ${this.shakeCount}`;
    }

    shuffleCity() {
        const currentCity = this.getCurrentCity().toLowerCase();
        const otherCities = this.cities.filter((city) => city.toLowerCase() !== currentCity);
        const pool = otherCities.length > 0 ? otherCities : this.cities;
        const nextCity = pool[Math.floor(Math.random() * pool.length)];

        this.playShuffleAnimation();
        if (typeof this.onShake === 'function') {
            this.onShake(nextCity);
        }
    }

    playShuffleAnimation() {
        this.cardElement.classList.remove(this.shuffleClass);
        void this.cardElement.offsetWidth;
        this.cardElement.classList.add(this.shuffleClass);
    }
}
