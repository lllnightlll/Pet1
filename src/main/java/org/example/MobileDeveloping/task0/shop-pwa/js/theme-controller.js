class ThemeController {
  static STORAGE_KEY = 'theme';
  static THEME_LIGHT = 'light';
  static THEME_DARK = 'dark';
  static SELECTOR_TOGGLE = '.theme-toggle';
  static ICON_LIGHT = '☀️';
  static ICON_DARK = '🌙';

  constructor() {
    this.root = document.documentElement;
    this.toggleButton = document.querySelector(ThemeController.SELECTOR_TOGGLE);
  }

  isDark() {
    return this.root.classList.contains(ThemeController.THEME_DARK);
  }

  applyTheme(themeName) {
    const isDark = themeName === ThemeController.THEME_DARK;
    this.root.classList.toggle(ThemeController.THEME_DARK, isDark);
    this.root.classList.toggle(ThemeController.THEME_LIGHT, !isDark);
    localStorage.setItem(ThemeController.STORAGE_KEY, themeName);
    this.updateIcon();
  }

  updateIcon() {
    if (!this.toggleButton) return;
    this.toggleButton.textContent = this.isDark()
      ? ThemeController.ICON_DARK
      : ThemeController.ICON_LIGHT;
  }

  toggle() {
    const nextTheme = this.isDark()
      ? ThemeController.THEME_LIGHT
      : ThemeController.THEME_DARK;
    this.applyTheme(nextTheme);
  }

  init() {
    const saved = localStorage.getItem(ThemeController.STORAGE_KEY);
    const initialTheme = saved === ThemeController.THEME_DARK
      ? ThemeController.THEME_DARK
      : ThemeController.THEME_LIGHT;
    this.applyTheme(initialTheme);

    if (this.toggleButton) {
      this.toggleButton.addEventListener('click', () => this.toggle());
    }
  }
}
