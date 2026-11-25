/**
 * Регистрация своего sw.js. CSP worker-src 'self' разрешает только этот origin.
 * worker-src 'none' ломает register() — в Console будет Refused to create a worker.
 */
class ServiceWorkerRegistrar {
    /**
     * @param {string} scriptUrl
     */
    constructor(scriptUrl) {
        this.scriptUrl = scriptUrl;
    }

    register() {
        if (!('serviceWorker' in navigator)) {
            return;
        }

        navigator.serviceWorker.register(this.scriptUrl).catch((error) => {
            console.error('Service Worker registration failed', error);
        });
    }
}

new ServiceWorkerRegistrar('/sw.js').register();
