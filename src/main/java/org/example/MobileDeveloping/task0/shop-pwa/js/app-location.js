class AppLocation {
  static STATE_KEY = 'path';
  static DEPTH_KEY = 'depth';

  constructor(pathMatcher) {
    this.pathMatcher = pathMatcher;
  }

  getDepth() {
    const state = history.state;
    if (state && Number.isInteger(state[AppLocation.DEPTH_KEY])) {
      return state[AppLocation.DEPTH_KEY];
    }
    return 0;
  }

  canGoBack() {
    return this.getDepth() > 0;
  }

  buildState(path, depth) {
    return {
      [AppLocation.STATE_KEY]: path,
      [AppLocation.DEPTH_KEY]: depth
    };
  }

  static normalize(path) {
    if (!path) return RouteTable.HREF_HOME;
    const withoutHash = String(path).replace(/^#/, '');
    if (!withoutHash || withoutHash === '/') return RouteTable.HREF_HOME;
    return withoutHash.startsWith('/') ? withoutHash : `/${withoutHash}`;
  }

  static toHash(path) {
    return `#${AppLocation.normalize(path)}`;
  }

  fromUrl(url) {
    const hashPath = AppLocation.normalize(url.hash);
    if (url.hash && hashPath) return hashPath;
    if (this.pathMatcher.match(url.pathname)) return url.pathname;
    return AppLocation.normalize(url.pathname);
  }

  fromBrowser() {
    if (location.hash) {
      return AppLocation.normalize(location.hash);
    }
    if (this.pathMatcher.match(location.pathname)) {
      return location.pathname;
    }
    if (history.state && history.state[AppLocation.STATE_KEY]) {
      return AppLocation.normalize(history.state[AppLocation.STATE_KEY]);
    }
    return RouteTable.HREF_HOME;
  }

  commit(path, options = {}) {
    const normalized = AppLocation.normalize(path);
    const nextHash = AppLocation.toHash(normalized);
    const currentDepth = this.getDepth();
    const forceReplace = Boolean(options.replace);
    const sameScreen = location.hash === nextHash;
    const replace = forceReplace || sameScreen;
    const depth = forceReplace ? 0 : (replace ? currentDepth : currentDepth + 1);
    const state = this.buildState(normalized, depth);

    if (replace) {
      history.replaceState(state, '', nextHash);
    } else {
      history.pushState(state, '', nextHash);
    }
    return normalized;
  }
}
