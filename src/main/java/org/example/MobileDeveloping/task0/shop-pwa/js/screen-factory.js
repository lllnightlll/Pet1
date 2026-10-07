class ScreenFactory {
  static EMPTY_FAVORITES_TEXT = 'No favorites yet';
  static PRODUCT_NOT_FOUND_TEXT = 'Product not found';
  static BUY_LABEL = 'Buy';
  static FAVORITES_TITLE = 'Favorites';

  constructor(catalog, favoritesStore) {
    this.catalog = catalog;
    this.favoritesStore = favoritesStore;
  }

  productCardHtml(product) {
    return `
        <a href="#/product/${product.id}" class="product-card" data-link>
          <img src="${product.image}" alt="${product.title}">
          <div class="info">
            <div class="title">${product.title}</div>
            <div class="price">${product.price} ₽</div>
          </div>
        </a>`;
  }

  home() {
    return `
    <div class="product-list">
      ${this.catalog.items.map((product) => this.productCardHtml(product)).join('')}
    </div>
  `;
  }

  product(params) {
    const item = this.catalog.findById(params.id);
    if (!item) {
      return `<div class="screen"><h2>${ScreenFactory.PRODUCT_NOT_FOUND_TEXT}</h2></div>`;
    }
    const favorited = this.favoritesStore.has(item.id);
    return `
      <div class="screen">
        <div class="product-detail">
          <img src="${item.image}" alt="${item.title}">
          <h3>${item.title}</h3>
          <div class="price">${item.price} ₽</div>
          <div class="description">${item.description}</div>
          <button type="button" class="buy-btn favorite-btn${favorited ? ' favorite-btn--on' : ''}" data-favorite-toggle="${item.id}">
            ${this.favoritesStore.labelFor(item.id)}
          </button>
          <button type="button" class="buy-btn">${ScreenFactory.BUY_LABEL}</button>
        </div>
      </div>
    `;
  }

  favorites() {
    const items = this.favoritesStore.listProducts();
    if (items.length === 0) {
      return `
      <div class="screen">
        <div class="settings-card">
          <h2>${ScreenFactory.FAVORITES_TITLE}</h2>
          <p class="empty-favorites">${ScreenFactory.EMPTY_FAVORITES_TEXT}</p>
        </div>
      </div>
    `;
    }
    return `
    <div class="product-list">
      ${items.map((product) => this.productCardHtml(product)).join('')}
    </div>
  `;
  }

  profile() {
    ProfileNavigator.go();
    return '';
  }

  asMap() {
    return {
      home: () => this.home(),
      product: (params) => this.product(params),
      favorites: () => this.favorites(),
      profile: () => this.profile()
    };
  }
}
