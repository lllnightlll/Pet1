/**
 * Повторяет fetch только если пакеты не ушли: Failed to fetch / нет HTTP-ответа.
 * Медленная сеть (Slow 3G) — не повод отменять и ретраить.
 * Задержка после ошибки: baseDelayMs * 2^attempt — 2s, 4s, 8s.
 */
class RetryPolicy {
    /**
     * @param {{
     *   maxAttempts: number,
     *   baseDelayMs: number,
     *   onRetry: Function
     * }} options
     */
    constructor(options) {
        this.maxAttempts = options.maxAttempts;
        this.baseDelayMs = options.baseDelayMs;
        this.onRetry = options.onRetry;
    }

    /**
     * @param {(signal: AbortSignal) => Promise<any>} task
     * @param {AbortSignal} [userSignal]
     */
    async execute(task, userSignal) {
        let lastError = null;

        for (let attempt = 1; attempt <= this.maxAttempts; attempt += 1) {
            if (userSignal && userSignal.aborted) {
                throw userSignal.reason || lastError;
            }

            try {
                return await task(userSignal);
            } catch (error) {
                lastError = error;

                if (userSignal && userSignal.aborted) {
                    throw error;
                }

                if (!this.shouldRetry(error) || attempt === this.maxAttempts) {
                    throw error;
                }

                const delayMs = this.baseDelayMs * Math.pow(2, attempt);
                if (typeof this.onRetry === 'function') {
                    this.onRetry(attempt, delayMs, error);
                }
                await this.wait(delayMs, userSignal);
            }
        }

        throw lastError;
    }

    /**
     * TypeError «Failed to fetch» — запрос не дошёл.
     * Если есть HTTP status, пакеты уже ушли, повтор не нужен.
     * @param {unknown} error
     * @returns {boolean}
     */
    shouldRetry(error) {
        if (!error || error.name === 'AbortError') {
            return false;
        }
        if (typeof error.status === 'number') {
            return false;
        }
        return error.name === 'TypeError';
    }

    /**
     * @param {number} delayMs
     * @param {AbortSignal} [userSignal]
     */
    wait(delayMs, userSignal) {
        return new Promise((resolve, reject) => {
            const timerId = setTimeout(resolve, delayMs);

            if (!userSignal) {
                return;
            }

            const onAbort = () => {
                clearTimeout(timerId);
                reject(userSignal.reason || new DOMException('Aborted', 'AbortError'));
            };

            if (userSignal.aborted) {
                onAbort();
                return;
            }

            userSignal.addEventListener('abort', onAbort, { once: true });
        });
    }
}

/**
 * fetch + JSON с Retry Policy.
 */
class RetryingHttpClient {
    /**
     * @param {RetryPolicy} retryPolicy
     */
    constructor(retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    /**
     * @param {string} url
     * @param {AbortSignal} [userSignal]
     */
    async fetchJson(url, userSignal) {
        return this.retryPolicy.execute(async (attemptSignal) => {
            const response = await fetch(url, { signal: attemptSignal });
            if (!response.ok) {
                const error = new Error(`HTTP ${response.status}`);
                error.status = response.status;
                throw error;
            }
            return response.json();
        }, userSignal);
    }
}

/**
 * Следит за сетью: navigator.onLine + события window online/offline.
 * Сам fetch не делает — только статус и колбэк при восстановлении.
 */
class NetworkStatus {
    /**
     * @param {string} elementId
     * @param {{
     *   onlineText: string,
     *   offlineText: string,
     *   onlineClass: string,
     *   offlineClass: string,
     *   onOnline: Function
     * }} options
     */
    constructor(elementId, options) {
        this.element = document.getElementById(elementId);
        this.onlineText = options.onlineText;
        this.offlineText = options.offlineText;
        this.onlineClass = options.onlineClass;
        this.offlineClass = options.offlineClass;
        this.onOnline = options.onOnline;

        window.addEventListener('online', () => this.handleOnline());
        window.addEventListener('offline', () => this.handleOffline());

        this.render(this.isOnline());
    }

    /**
     * Текущее состояние из браузера (без отдельного ping).
     * @returns {boolean}
     */
    isOnline() {
        return navigator.onLine;
    }

    handleOnline() {
        this.render(true);
        if (typeof this.onOnline === 'function') {
            this.onOnline();
        }
    }

    handleOffline() {
        this.render(false);
    }

    /**
     * @param {boolean} isOnline
     */
    render(isOnline) {
        this.element.textContent = isOnline ? this.onlineText : this.offlineText;
        this.element.classList.toggle(this.onlineClass, isOnline);
        this.element.classList.toggle(this.offlineClass, !isOnline);
    }
}
