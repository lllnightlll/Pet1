/**
 * Сравнивает лицо на снимке с эталонными фото (face-api.js).
 * Ближе порога — «пикми», иначе «не пикми».
 */
class PickMeClassifier {
    /**
     * @param {{
     *   modelUrl: string,
     *   referenceImageUrls: string[],
     *   matchDistance: number
     * }} options
     */
    constructor(options) {
        this.modelUrl = options.modelUrl;
        this.referenceImageUrls = options.referenceImageUrls;
        this.matchDistance = options.matchDistance;
        this.referenceDescriptors = [];
        this.isReady = false;
        this.loadPromise = null;
    }

    /**
     * Грузит веса сети и дескрипторы эталона один раз.
     */
    async ensureReady() {
        if (this.isReady) {
            return;
        }
        if (this.loadPromise) {
            await this.loadPromise;
            return;
        }

        this.loadPromise = this.loadModelsAndReferences();
        await this.loadPromise;
    }

    async loadModelsAndReferences() {
        if (typeof faceapi === 'undefined') {
            throw new Error('face-api is not loaded');
        }

        await faceapi.nets.ssdMobilenetv1.loadFromUri(this.modelUrl);
        await faceapi.nets.faceLandmark68Net.loadFromUri(this.modelUrl);
        await faceapi.nets.faceRecognitionNet.loadFromUri(this.modelUrl);

        const descriptors = [];
        for (const url of this.referenceImageUrls) {
            const image = await faceapi.fetchImage(url);
            const detections = await faceapi
                .detectAllFaces(image, this.createDetectorOptions())
                .withFaceLandmarks()
                .withFaceDescriptors();

            detections.forEach((detection) => {
                descriptors.push(detection.descriptor);
            });
        }

        this.referenceDescriptors = descriptors;
        this.isReady = true;
    }

    createDetectorOptions() {
        return new faceapi.SsdMobilenetv1Options({ minConfidence: 0.3 });
    }

    /**
     * @param {HTMLImageElement} imageElement
     * @returns {Promise<string>}
     */
    async classify(imageElement) {
        await this.ensureReady();

        if (this.referenceDescriptors.length === 0) {
            return WeatherConfig.PICKME_NO;
        }

        const detections = await faceapi
            .detectAllFaces(imageElement, this.createDetectorOptions())
            .withFaceLandmarks()
            .withFaceDescriptors();

        if (!detections.length) {
            return WeatherConfig.PICKME_NO;
        }

        let minDistance = Number.POSITIVE_INFINITY;
        detections.forEach((detection) => {
            this.referenceDescriptors.forEach((reference) => {
                const distance = faceapi.euclideanDistance(detection.descriptor, reference);
                if (distance < minDistance) {
                    minDistance = distance;
                }
            });
        });

        return minDistance <= this.matchDistance
            ? WeatherConfig.PICKME_YES
            : WeatherConfig.PICKME_NO;
    }
}
