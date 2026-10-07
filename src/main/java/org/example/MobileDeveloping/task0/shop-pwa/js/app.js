class ShopApp {
  constructor() {
    this.catalog = new ProductCatalog();
    this.favoritesStore = new FavoritesStore(this.catalog);
    this.screenFactory = new ScreenFactory(this.catalog, this.favoritesStore);
    this.shell = RouterShell.query();
    this.pathMatcher = new PathMatcher(RouteTable.create(this.shell.titleEl.textContent.trim()));
    this.appLocation = new AppLocation(this.pathMatcher);
    this.router = new AppRouter({
      root: this.shell.root,
      titleEl: this.shell.titleEl,
      backBtn: this.shell.backBtn,
      tabbar: this.shell.tabbar,
      screens: this.screenFactory.asMap(),
      pathMatcher: this.pathMatcher,
      appLocation: this.appLocation,
      breadcrumbs: new BreadcrumbTrail()
    });
    this.themeController = new ThemeController();
    this.navigationBinder = new NavigationApiBinder(
      this.appLocation,
      (path) => this.router.render(path)
    );
    this.interactionBinder = new AppInteractionBinder({
      router: this.router,
      appLocation: this.appLocation,
      shell: this.shell,
      favoritesStore: this.favoritesStore
    });
  }

  start() {
    this.themeController.init();
    this.navigationBinder.bind();
    this.interactionBinder.bind();
    ProfileNavigator.bindTab();
  }
}

const shopApp = new ShopApp();
shopApp.start();
