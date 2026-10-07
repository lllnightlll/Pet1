class NotFoundScreen {
  static TITLE = '404';
  static CODE_LABEL = '404 Not Found';
  static MEME_TEXT = 'окак';
  static HINT = 'такой страницы нет';

  static toHtml() {
    return `
      <div class="screen not-found">
        <div class="settings-card not-found-card">
          <p class="not-found-code">${NotFoundScreen.CODE_LABEL}</p>
          <span class="not-found-cat" aria-hidden="true">🐱</span>
          <h2 class="not-found-okak">${NotFoundScreen.MEME_TEXT}</h2>
          <p class="not-found-hint">${NotFoundScreen.HINT}</p>
        </div>
      </div>
    `;
  }
}
