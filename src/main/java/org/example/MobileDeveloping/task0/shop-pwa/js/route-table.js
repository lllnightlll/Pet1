class RouteTable {
  static SCREEN_HOME = 'home';
  static SCREEN_PRODUCT = 'product';
  static SCREEN_FAVORITES = 'favorites';
  static SCREEN_PROFILE = 'profile';
  static SCREEN_SETTINGS = 'settings';

  static HREF_HOME = '/';
  static HREF_FAVORITES = '/favorites';
  static HREF_PROFILE = '/profile';
  static HREF_SETTINGS = '/settings';

  static TITLE_PRODUCT = 'Product';
  static TITLE_FAVORITES = 'Favorites';
  static TITLE_PROFILE = 'Profile';

  static PATTERN_HOME = /^\/$/;
  static PATTERN_PRODUCT = /^\/product\/(\d+)$/;
  static PATTERN_FAVORITES = /^\/favorites$/;
  static PATTERN_PROFILE = /^\/profile$/;

  static create(homeTitle) {
    return [
      {
        pattern: RouteTable.PATTERN_HOME,
        name: RouteTable.SCREEN_HOME,
        title: homeTitle
      },
      {
        pattern: RouteTable.PATTERN_PRODUCT,
        name: RouteTable.SCREEN_PRODUCT,
        title: RouteTable.TITLE_PRODUCT
      },
      {
        pattern: RouteTable.PATTERN_FAVORITES,
        name: RouteTable.SCREEN_FAVORITES,
        title: RouteTable.TITLE_FAVORITES
      },
      {
        pattern: RouteTable.PATTERN_PROFILE,
        name: RouteTable.SCREEN_PROFILE,
        title: RouteTable.TITLE_PROFILE
      }
    ];
  }
}
