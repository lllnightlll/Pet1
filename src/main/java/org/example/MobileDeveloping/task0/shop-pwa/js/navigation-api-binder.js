class NavigationApiBinder {
  static EVENT_NAVIGATE = 'navigate';

  constructor(appLocation, onNavigate) {
    this.appLocation = appLocation;
    this.onNavigate = onNavigate;
  }

  static isSupported() {
    return Boolean(window.navigation);
  }

  static isExternalProfile(url) {
    const profileUrl = new URL(ProfileNavigator.getTargetUrl());
    return url.pathname === profileUrl.pathname;
  }

  static shouldIntercept(event) {
    const url = new URL(event.destination.url);
    if (url.origin !== location.origin) return false;
    if (!event.canIntercept) return false;
    if (event.downloadRequest !== null) return false;
    if (NavigationApiBinder.isExternalProfile(url)) return false;
    return true;
  }

  bind() {
    if (!NavigationApiBinder.isSupported()) return;

    window.navigation.addEventListener(NavigationApiBinder.EVENT_NAVIGATE, (event) => {
      if (!NavigationApiBinder.shouldIntercept(event)) return;

      const url = new URL(event.destination.url);
      const path = this.appLocation.fromUrl(url);

      event.intercept({
        handler: () => {
          this.onNavigate(path);
        }
      });
    });
  }
}
