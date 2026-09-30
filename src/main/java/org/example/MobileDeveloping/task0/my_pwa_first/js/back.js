/**
 * Кнопка «Назад»: всегда открывает магазин по явному URL.
 * history.back() на localtunnel уводит на заглушку loca.lt, а не в shop-pwa.
 */
class ShopBackNavigator {
  static SHOP_SIBLING_URL = '../shop-pwa/index.html';
  static SHOP_ROOT_PATH = '/index.html';
  static BUTTON_SELECTOR = '#back-btn';

  static getShopUrl() {
    if (window.location.protocol === 'file:') {
      return new URL(ShopBackNavigator.SHOP_SIBLING_URL, window.location.href).href;
    }
    return new URL(ShopBackNavigator.SHOP_ROOT_PATH, window.location.origin).href;
  }

  static go() {
    window.location.assign(ShopBackNavigator.getShopUrl());
  }

  static bind() {
    const button = document.querySelector(ShopBackNavigator.BUTTON_SELECTOR);
    if (!button) return;
    button.dataset.bound = 'true';
    button.addEventListener('click', () => ShopBackNavigator.go());
  }
}

ShopBackNavigator.bind();
