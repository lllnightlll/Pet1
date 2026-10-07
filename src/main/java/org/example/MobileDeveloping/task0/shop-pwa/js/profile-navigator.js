/**
 * Открывает профиль из my_pwa_first отдельной страницей, а не экраном магазина.
 */
class ProfileNavigator {
  static SIBLING_URL = '../my_pwa_first/index.html';
  static ACTION_OPEN_PROFILE = 'open-profile';
  static TAB_SELECTOR = '[data-action="open-profile"]';

  /**
   * Рядом с магазином (file://, /shop-pwa/) — соседняя папка.
   * В Docker/туннеле магазин в корне, профиль лежит в /my_pwa_first/.
   */
  static getTargetUrl() {
    if (window.location.protocol === 'file:') {
      return new URL(ProfileNavigator.SIBLING_URL, window.location.href).href;
    }
    return new URL('/my_pwa_first/', window.location.origin).href;
  }

  static go() {
    window.location.assign(ProfileNavigator.getTargetUrl());
  }

  static bindTab() {
    document.querySelectorAll(ProfileNavigator.TAB_SELECTOR).forEach((tab) => {
      tab.addEventListener('click', (event) => {
        event.preventDefault();
        ProfileNavigator.go();
      });
    });
  }
}
