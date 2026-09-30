if ('serviceWorker' in navigator) {
  const workerUrl = location.protocol === 'file:'
    ? './sw.js'
    : '/my_pwa_first/sw.js';
  const scope = location.protocol === 'file:'
    ? './'
    : '/my_pwa_first/';

  navigator.serviceWorker.getRegistrations().then((registrations) => {
    return Promise.all(registrations.map((registration) => {
      const scriptUrl = (registration.active && registration.active.scriptURL) || '';
      if (scriptUrl && !scriptUrl.includes('/my_pwa_first/') && scriptUrl.endsWith('/sw.js')) {
        return registration.unregister();
      }
      return Promise.resolve();
    }));
  }).finally(() => {
    navigator.serviceWorker.register(workerUrl, { scope: scope });
  });
}
