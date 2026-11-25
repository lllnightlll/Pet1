"""Локальная раздача weather-pwa. CSP задан <meta> в index.html."""
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer

APP_PORT = 5500

if __name__ == "__main__":
    server = ThreadingHTTPServer(("127.0.0.1", APP_PORT), SimpleHTTPRequestHandler)
    print(f"Serving weather-pwa on http://127.0.0.1:{APP_PORT}/")
    server.serve_forever()
