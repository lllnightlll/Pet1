/**
 * Фото через системную камеру: <input capture> → canvas JPEG → IndexedDB.
 * Live getUserMedia на iOS Safari «тупит» (play + display:none + video.load),
 * а label+file input открывает камеру сразу по тапу, даже до загрузки app.js.
 */
class CameraCapture {
    /**
     * @param {{
     *   fileInput: HTMLInputElement,
     *   canvasElement: HTMLCanvasElement,
     *   previewElement: HTMLImageElement,
     *   sizeElement: HTMLElement,
     *   sendButton: HTMLElement,
     *   mimeType: string,
     *   quality: number,
     *   maxWidth: number,
     *   hiddenClass: string
     * }} options
     */
    constructor(options) {
        this.fileInput = options.fileInput;
        this.canvasElement = options.canvasElement;
        this.previewElement = options.previewElement;
        this.sizeElement = options.sizeElement;
        this.sendButton = options.sendButton;
        this.mimeType = options.mimeType;
        this.quality = options.quality;
        this.maxWidth = options.maxWidth;
        this.hiddenClass = options.hiddenClass;
        this.lastDataUrl = null;

        this.prepareFileInput();
        this.fileInput.addEventListener('change', () => this.onFileChosen());
        this.sendButton.addEventListener('click', () => {
            this.send().catch((error) => {
                this.sizeElement.textContent = this.toUserMessage(error);
            });
        });
    }

    /**
     * capture=environment — задняя камера iOS, не галерея.
     * input нельзя display:none: Safari тогда игнорирует tap по label.
     */
    prepareFileInput() {
        this.fileInput.setAttribute('accept', WeatherConfig.PHOTO_ACCEPT);
        this.fileInput.setAttribute('capture', WeatherConfig.PHOTO_CAPTURE);
        this.fileInput.setAttribute('type', 'file');
    }

    /**
     * Пользователь снял кадр в системной камере и вернулся на страницу.
     */
    async onFileChosen() {
        const file = this.fileInput.files && this.fileInput.files[0];
        this.fileInput.value = '';

        if (!file) {
            return;
        }

        this.sizeElement.textContent = WeatherConfig.PHOTO_COMPRESSING;

        try {
            const dataUrl = await this.compressFile(file);
            this.lastDataUrl = dataUrl;
            this.previewElement.src = dataUrl;
            this.previewElement.classList.remove(this.hiddenClass);
            this.sendButton.classList.remove(this.hiddenClass);
            this.sizeElement.textContent = this.formatDataUrlSize(dataUrl);
        } catch (error) {
            this.sizeElement.textContent = this.toUserMessage(error);
        }
    }

    /**
     * Уменьшает снимок и кодирует JPEG, как раньше кадр с <video>.
     * @param {Blob} file
     * @returns {Promise<string>}
     */
    async compressFile(file) {
        const image = await this.loadImage(file);
        const sourceWidth = image.width;
        const sourceHeight = image.height;
        const scale = Math.min(1, this.maxWidth / sourceWidth);
        const targetWidth = Math.max(1, Math.round(sourceWidth * scale));
        const targetHeight = Math.max(1, Math.round(sourceHeight * scale));

        this.canvasElement.width = targetWidth;
        this.canvasElement.height = targetHeight;

        const context = this.canvasElement.getContext('2d');
        context.drawImage(image, 0, 0, targetWidth, targetHeight);

        if (typeof image.close === 'function') {
            image.close();
        }

        return this.canvasElement.toDataURL(this.mimeType, this.quality);
    }

    /**
     * imageOrientation: from-image — иначе iOS-снимки часто лежат на боку.
     * @param {Blob} file
     * @returns {Promise<ImageBitmap|HTMLImageElement>}
     */
    async loadImage(file) {
        if (typeof createImageBitmap === 'function') {
            try {
                return await createImageBitmap(file, { imageOrientation: 'from-image' });
            } catch (error) {
                return await createImageBitmap(file);
            }
        }

        return this.loadHtmlImage(file);
    }

    /**
     * @param {Blob} file
     * @returns {Promise<HTMLImageElement>}
     */
    loadHtmlImage(file) {
        return new Promise((resolve, reject) => {
            const image = new Image();
            const objectUrl = URL.createObjectURL(file);

            image.onload = () => {
                URL.revokeObjectURL(objectUrl);
                resolve(image);
            };
            image.onerror = () => {
                URL.revokeObjectURL(objectUrl);
                reject(new Error(WeatherConfig.CAMERA_UNSUPPORTED));
            };
            image.src = objectUrl;
        });
    }

    /**
     * «Отправить» — кладём сжатый dataURL в IndexedDB (store photos).
     */
    async send() {
        if (!this.lastDataUrl) {
            return;
        }

        const photoId = await savePhotoToIndexedDb(this.lastDataUrl);
        if (!photoId) {
            this.sizeElement.textContent = WeatherConfig.PHOTO_SAVE_ERROR;
            return;
        }

        this.sizeElement.textContent = `${WeatherConfig.PHOTO_SAVED_MESSAGE} ${photoId}`;
    }

    /**
     * dataURL ≈ 4/3 от сырых байт из‑за Base64.
     * @param {string} dataUrl
     * @returns {string}
     */
    formatDataUrlSize(dataUrl) {
        const base64 = dataUrl.split(',')[1] || '';
        const padding = (base64.match(/=+$/) || [''])[0].length;
        const bytes = Math.floor((base64.length * 3) / 4) - padding;
        const kilobytes = Math.round(bytes / 1024);
        return `${WeatherConfig.PHOTO_SIZE_LABEL} ${kilobytes} KB`;
    }

    /**
     * @param {unknown} error
     * @returns {string}
     */
    toUserMessage(error) {
        return error && error.message ? error.message : WeatherConfig.CAMERA_UNSUPPORTED;
    }
}
