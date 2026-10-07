class AppInteractionBinder {
  static EVENT_CLICK = 'click';
  static EVENT_POPSTATE = 'popstate';
  static TAB_BUTTON_SELECTOR = '.tab-btn';
  static INTERNAL_LINK_SELECTOR = 'a[data-link]';
  static CLASS_BACK_TRANSITION = 'back-transition';

  constructor(deps) {
    this.router = deps.router;
    this.appLocation = deps.appLocation;
    this.shell = deps.shell;
    this.favoritesStore = deps.favoritesStore;
  }

  markForward() {
    document.documentElement.classList.remove(AppInteractionBinder.CLASS_BACK_TRANSITION);
  }

  markBack() {
    document.documentElement.classList.add(AppInteractionBinder.CLASS_BACK_TRANSITION);
  }

  goTo(href) {
    this.markForward();
    const path = this.appLocation.commit(href);
    this.router.render(path);
  }

  goBack() {
    this.markBack();
    if (this.appLocation.canGoBack()) {
      history.back();
      return;
    }
    const current = this.appLocation.fromBrowser();
    if (current === RouteTable.HREF_HOME) {
      return;
    }
    const path = this.appLocation.commit(RouteTable.HREF_HOME, { replace: true });
    this.router.render(path);
  }

  resolveInitialPath() {
    return this.appLocation.fromBrowser();
  }

  bind() {
    const { backBtn, tabbar, root } = this.shell;

    backBtn.addEventListener(AppInteractionBinder.EVENT_CLICK, () => {
      this.router.saveScroll();
      this.goBack();
    });

    tabbar.addEventListener(AppInteractionBinder.EVENT_CLICK, (event) => {
      const btn = event.target.closest(AppInteractionBinder.TAB_BUTTON_SELECTOR);
      if (!btn) return;

      this.router.saveScroll();

      if (btn.dataset.action === ProfileNavigator.ACTION_OPEN_PROFILE) {
        return;
      }

      this.goTo(btn.dataset.href);
    });

    document.addEventListener(AppInteractionBinder.EVENT_CLICK, (event) => {
      const link = event.target.closest(AppInteractionBinder.INTERNAL_LINK_SELECTOR);
      if (!link) return;
      event.preventDefault();
      this.router.saveScroll();
      this.goTo(link.getAttribute('href'));
    });

    window.addEventListener(AppInteractionBinder.EVENT_POPSTATE, () => {
      this.router.render(this.appLocation.fromBrowser());
    });

    const initialPath = this.resolveInitialPath();
    history.replaceState(
      this.appLocation.buildState(initialPath, 0),
      '',
      AppLocation.toHash(initialPath)
    );
    this.router.render(initialPath);
    this.favoritesStore.bindClicks(root);
  }
}
