ARG NODE_IMAGE=node:24-slim
FROM ${NODE_IMAGE}

ARG NPM_VERSION=12.1.0
ARG LOCALTUNNEL_VERSION=latest
ARG SERVE_VERSION=latest

ENV APP_PORT=5500
WORKDIR /app

RUN npm install -g "npm@${NPM_VERSION}" \
    && npm install -g "localtunnel@${LOCALTUNNEL_VERSION}" \
    && npm install -g "serve@${SERVE_VERSION}" \
    && npm --version \
    && lt --help >/dev/null \
    && serve --version

COPY . .

EXPOSE ${APP_PORT}

CMD ["sh", "-c", "serve -l tcp://0.0.0.0:${APP_PORT} . & lt --port ${APP_PORT}"]
