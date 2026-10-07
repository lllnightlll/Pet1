class BreadcrumbTrail {
  static ELEMENT_ID = 'app-breadcrumbs';
  static ROOT_LABEL = 'Darknet';
  static SEPARATOR = ' / ';
  static LABEL_PRODUCT_PREFIX = 'Product #';
  static LABEL_PROFILE = 'Profile';
  static LABEL_FAVORITES = 'Favorites';
  static LABEL_NOT_FOUND = '404';

  constructor() {
    this.element = document.getElementById(BreadcrumbTrail.ELEMENT_ID);
  }

  join(parts) {
    return parts.join(BreadcrumbTrail.SEPARATOR);
  }

  forProduct(params) {
    const productNumber = params && params.id ? params.id : '';
    return this.join([
      BreadcrumbTrail.ROOT_LABEL,
      `${BreadcrumbTrail.LABEL_PRODUCT_PREFIX}${productNumber}`
    ]);
  }

  forRoute(routeName, params) {
    if (routeName === RouteTable.SCREEN_HOME) {
      return BreadcrumbTrail.ROOT_LABEL;
    }
    if (routeName === RouteTable.SCREEN_PRODUCT) {
      return this.forProduct(params);
    }
    if (routeName === RouteTable.SCREEN_FAVORITES) {
      return this.join([BreadcrumbTrail.ROOT_LABEL, BreadcrumbTrail.LABEL_FAVORITES]);
    }
    if (routeName === RouteTable.SCREEN_PROFILE) {
      return this.join([BreadcrumbTrail.ROOT_LABEL, BreadcrumbTrail.LABEL_PROFILE]);
    }
    return this.join([BreadcrumbTrail.ROOT_LABEL, BreadcrumbTrail.LABEL_NOT_FOUND]);
  }

  render(routeName, params) {
    if (!this.element) return;
    this.element.textContent = this.forRoute(routeName, params);
  }
}
