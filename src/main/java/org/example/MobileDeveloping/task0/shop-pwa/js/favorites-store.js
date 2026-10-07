class FavoritesStore {
  static STORAGE_KEY = 'favoriteProductIds';
  static TOGGLE_SELECTOR = '[data-favorite-toggle]';
  static LABEL_ADD = 'Add to favorites';
  static LABEL_REMOVE = 'Remove from favorites';
  static CLASS_ON = 'favorite-btn--on';

  constructor(catalog) {
    this.catalog = catalog;
  }

  readIds() {
    try {
      const raw = localStorage.getItem(FavoritesStore.STORAGE_KEY);
      if (!raw) return [];
      const parsed = JSON.parse(raw);
      if (!Array.isArray(parsed)) return [];
      return parsed.map(Number).filter((id) => Number.isInteger(id) && id > 0);
    } catch (error) {
      return [];
    }
  }

  writeIds(ids) {
    localStorage.setItem(FavoritesStore.STORAGE_KEY, JSON.stringify(ids));
  }

  has(id) {
    return this.readIds().includes(Number(id));
  }

  add(id) {
    const numericId = Number(id);
    const ids = this.readIds();
    if (!ids.includes(numericId)) {
      ids.push(numericId);
      this.writeIds(ids);
    }
  }

  remove(id) {
    const numericId = Number(id);
    this.writeIds(this.readIds().filter((item) => item !== numericId));
  }

  toggle(id) {
    if (this.has(id)) {
      this.remove(id);
    } else {
      this.add(id);
    }
  }

  listProducts() {
    return this.readIds()
      .map((id) => this.catalog.findById(id))
      .filter(Boolean);
  }

  labelFor(id) {
    return this.has(id)
      ? FavoritesStore.LABEL_REMOVE
      : FavoritesStore.LABEL_ADD;
  }

  syncButton(button, id) {
    const favorited = this.has(id);
    button.textContent = this.labelFor(id);
    button.classList.toggle(FavoritesStore.CLASS_ON, favorited);
  }

  bindClicks(rootEl) {
    rootEl.addEventListener('click', (event) => {
      const button = event.target.closest(FavoritesStore.TOGGLE_SELECTOR);
      if (!button) return;
      event.preventDefault();
      const id = button.getAttribute('data-favorite-toggle');
      this.toggle(id);
      this.syncButton(button, id);
    });
  }
}
