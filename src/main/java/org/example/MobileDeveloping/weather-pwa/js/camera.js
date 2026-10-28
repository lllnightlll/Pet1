/**
 * Камера: getUserMedia → <video> → кадр на <canvas> → сжатый JPEG через toDataURL.
 */
class CameraCapture {
    /**
     * @param {{
     *   videoElement: HTMLVideoElement,
     *   canvasElement: HTMLCanvasElement,
     *   previewElement: HTMLImageElement,
     *   startButton: HTMLElement,
     *   captureButton: HTMLElement,
     *   sizeElement: HTMLElement,
     *   sendButton: HTMLElement,
     *   resultElement: HTMLElement,
     *   classifier: PickMeClassifier,
     *   mimeType: string,
     *   quality: number,
     *   maxWidth: number,
     *   hiddenClass: string
     * }} options
     */
    constructor(options) {
        this.videoElement = options.videoElement;
        this.canvasElement = options.canvasElement;
        this.previewElement = options.previewElement;
        this.startButton = options.startButton;
        this.captureButton = options.captureButton;
        this.sizeElement = options.sizeElement;
        this.sendButton = options.sendButton;
        this.resultElement = options.resultElement;
        this.classifier = options.classifier;
        this.mimeType = options.mimeType;
        this.quality = options.quality;
        this.maxWidth = options.maxWidth;
        this.hiddenClass = options.hiddenClass;
        this.stream = null;
        this.lastDataUrl = null;

        this.startButton.addEventListener('click', () => {
            this.start();
        });
        this.captureButton.addEventListener('click', () => {
            this.capture();
        });
        this.sendButton.addEventListener('click', () => {
            this.send();
        });

        this.setCameraUi(false);
    }

    /**
     * Открывает камеру. Браузер покажет запрос разрешения.
     */
    async start() {
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            this.sizeElement.textContent = WeatherConfig.CAMERA_UNSUPPORTED;
            return;
        }

        this.stop();

        try {
            this.stream = await navigator.mediaDevices.getUserMedia({
                video: { facingMode: 'environment' },
                audio: false
            });
        } catch (firstError) {
            try {
                this.stream = await navigator.mediaDevices.getUserMedia({
                    video: true,
                    audio: false
                });
            } catch (error) {
                this.sizeElement.textContent = this.toUserMessage(error);
                return;
            }
        }

        this.videoElement.srcObject = this.stream;
        await this.videoElement.play();
        this.resultElement.textContent = '';
        this.setCameraUi(true);
    }

    /**
     * Берёт текущий кадр, уменьшает и сжимает в JPEG (~100–200 КБ).
     */
    async capture() {
        if (!this.stream) {
            return;
        }

        const sourceWidth = this.videoElement.videoWidth;
        const sourceHeight = this.videoElement.videoHeight;
        const scale = Math.min(1, this.maxWidth / sourceWidth);
        const targetWidth = Math.round(sourceWidth * scale);
        const targetHeight = Math.round(sourceHeight * scale);

        this.canvasElement.width = targetWidth;
        this.canvasElement.height = targetHeight;

        const context = this.canvasElement.getContext('2d');
        context.drawImage(this.videoElement, 0, 0, targetWidth, targetHeight);

        const dataUrl = this.canvasElement.toDataURL(this.mimeType, this.quality);
        this.lastDataUrl = dataUrl;
        this.previewElement.src = dataUrl;
        this.previewElement.classList.remove(this.hiddenClass);
        this.sendButton.classList.remove(this.hiddenClass);
        this.sizeElement.textContent = this.formatDataUrlSize(dataUrl);
        this.resultElement.textContent = WeatherConfig.PICKME_LOADING;

        this.stop();
        this.setCameraUi(false);
        await this.classifyPreview();
    }

    /**
     * Пишет под фото «пикми» / «не пикми» по сравнению лиц.
     */
    async classifyPreview() {
        try {
            await this.previewElement.decode();
            const label = await this.classifier.classify(this.previewElement);
            this.resultElement.textContent = label;
        } catch (error) {
            this.resultElement.textContent = WeatherConfig.PICKME_ERROR;
        }
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

    stop() {
        if (!this.stream) {
            return;
        }

        this.stream.getTracks().forEach((track) => {
            track.stop();
        });
        this.stream = null;
        this.videoElement.srcObject = null;
    }

    /**
     * @param {boolean} isLive
     */
    setCameraUi(isLive) {
        this.videoElement.classList.toggle(this.hiddenClass, !isLive);
        this.captureButton.classList.toggle(this.hiddenClass, !isLive);
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
        const name = error && error.name;
        if (name === 'NotAllowedError' || name === 'PermissionDeniedError') {
            return WeatherConfig.CAMERA_PERMISSION_DENIED;
        }
        if (name === 'NotFoundError' || name === 'DevicesNotFoundError') {
            return WeatherConfig.CAMERA_NOT_FOUND;
        }
        return error && error.message ? error.message : WeatherConfig.CAMERA_UNSUPPORTED;
    }
}
