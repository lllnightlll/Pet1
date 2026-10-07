class RouterShell {
  static ROOT_ID = 'app-root';
  static TITLE_ID = 'app-title';
  static BACK_BTN_ID = 'back-btn';
  static TABBAR_ID = 'tabbar';
  static BREADCRUMBS_ID = 'app-breadcrumbs';

  static query() {
    return {
      root: document.getElementById(RouterShell.ROOT_ID),
      titleEl: document.getElementById(RouterShell.TITLE_ID),
      backBtn: document.getElementById(RouterShell.BACK_BTN_ID),
      tabbar: document.getElementById(RouterShell.TABBAR_ID),
      breadcrumbsEl: document.getElementById(RouterShell.BREADCRUMBS_ID)
    };
  }
}
