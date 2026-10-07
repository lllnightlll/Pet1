class ProductCatalog {
  static PRODUCT_COUNT = 40;
  static PRICE_STEP = 100;
  static IMAGE_SIZE = 200;
  static IMAGE_BG = 'ff8fab';
  static IMAGE_FG = 'ffffff';

  static buildImageUrl(productId) {
    return `https://dummyjson.com/image/${ProductCatalog.IMAGE_SIZE}/${ProductCatalog.IMAGE_BG}/${ProductCatalog.IMAGE_FG}?text=${productId}`;
  }

  static createProduct(index) {
    const id = index + 1;
    return {
      id,
      title: `Product ${id}`,
      price: id * ProductCatalog.PRICE_STEP,
      image: ProductCatalog.buildImageUrl(id),
      description: `Detailed description of product ${id}. The perfect choice for those who value quality and style.`
    };
  }

  static createAll() {
    return Array.from(
      { length: ProductCatalog.PRODUCT_COUNT },
      (_, index) => ProductCatalog.createProduct(index)
    );
  }

  constructor() {
    this.items = ProductCatalog.createAll();
  }

  findById(id) {
    return this.items.find((product) => product.id === Number(id));
  }
}
