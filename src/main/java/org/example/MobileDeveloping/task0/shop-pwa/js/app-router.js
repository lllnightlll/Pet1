class AppRouter {
  static SCROLL_KEY_PREFIX = 'scroll:';
  static TAB_BUTTON_SELECTOR = '.tab-btn';
  static CLASS_ACTIVE = 'active';
  static CLASS_HIDDEN = 'hidden';

  constructor(deps) {
    this.root = deps.root;
    this.titleEl = deps.titleEl;
    this.backBtn = deps.backBtn;
    this.tabbar = deps.tabbar;
    this.screens = deps.screens;
    this.pathMatcher = deps.pathMatcher;
    this.appLocation = deps.appLocation;
    this.breadcrumbs = deps.breadcrumbs;
  }

  getScrollKey(path) {
    return AppRouter.SCROLL_KEY_PREFIX + path;
  }

  restoreScroll(path) {
    const saved = sessionStorage.getItem(this.getScrollKey(path));
    if (saved) {
      window.scrollTo(0, Number(saved));
    } else {
      window.scrollTo(0, 0);
    }
  }

  saveScroll() {
    const path = this.appLocation.fromBrowser();
    sessionStorage.setItem(this.getScrollKey(path), String(window.scrollY));
  }

  updateActiveTab(path, routeName) {
    const isHome = routeName === RouteTable.SCREEN_HOME;
    const isFavorites = routeName === RouteTable.SCREEN_FAVORITES;
    const isProfile = routeName === RouteTable.SCREEN_PROFILE;
    const isSettings = path === RouteTable.HREF_SETTINGS;

    this.tabbar.querySelectorAll(AppRouter.TAB_BUTTON_SELECTOR).forEach((btn) => {
      btn.classList.toggle(
        AppRouter.CLASS_ACTIVE,
        (btn.dataset.href === RouteTable.HREF_HOME && isHome) ||
        (btn.dataset.href === RouteTable.HREF_FAVORITES && isFavorites) ||
        (btn.dataset.href === RouteTable.HREF_PROFILE && isProfile) ||
        (btn.dataset.href === RouteTable.HREF_SETTINGS && isSettings)
      );
    });
  }

  paint(found, path) {
    const { route, params } = found;
    this.root.innerHTML = this.screens[route.name](params);
    this.titleEl.textContent = route.title;
    this.breadcrumbs.render(route.name, params);
    this.backBtn.classList.toggle(
      AppRouter.CLASS_HIDDEN,
      route.name === RouteTable.SCREEN_HOME
    );
    this.updateActiveTab(path, route.name);
  }

  paintNotFound(path) {
    this.root.innerHTML = NotFoundScreen.toHtml();
    this.titleEl.textContent = NotFoundScreen.TITLE;
    this.breadcrumbs.render(null, {});
    this.backBtn.classList.remove(AppRouter.CLASS_HIDDEN);
    this.updateActiveTab(path, null);
  }

  render(path) {
    const found = this.pathMatcher.match(path);
    const doRender = () => {
      if (!found) {
        this.paintNotFound(path);
        return;
      }
      this.paint(found, path);
    };

    if (document.startViewTransition) {
      const transition = document.startViewTransition(doRender);
      transition.finished.then(() => {
        this.restoreScroll(path);
      });
    } else {
      doRender();
      this.restoreScroll(path);
    }
  }
}
