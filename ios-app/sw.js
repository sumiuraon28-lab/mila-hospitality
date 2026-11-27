const CACHE_NAME = "mila-ios-pwa-v3";
const SHELL_ASSETS = [
  "./",
  "./index.html",
  "./styles.css?v=20261127c",
  "./app.js?v=20261127c",
  "./manifest.json",
  "./apple-touch-icon.png",
  "./icon-192.png",
  "./icon-512.png",
  "./hero-hotel.jpg"
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(SHELL_ASSETS)).then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.method !== "GET") return;

  const url = new URL(req.url);

  // Never cache cross-origin API calls, Supabase endpoints, authenticated requests, or runtime config
  if (url.origin !== self.location.origin) return;
  if (req.headers.has("Authorization") || req.headers.has("apikey")) return;
  if (url.pathname.endsWith("/config.js")) return;

  // Network-first for HTML navigation so app updates arrive promptly
  if (req.mode === "navigate" || url.pathname.endsWith("/index.html") || url.pathname.endsWith("/")) {
    event.respondWith(
      fetch(req)
        .then((res) => {
          if (res && res.ok) {
            const copy = res.clone();
            caches.open(CACHE_NAME).then((c) => c.put(req, copy)).catch(() => {});
          }
          return res;
        })
        .catch(() => caches.match(req).then((hit) => hit || caches.match("./index.html")))
    );
    return;
  }

  // Stale-while-revalidate for static assets (CSS, JS, PNG/JPG icons)
  event.respondWith(
    caches.match(req).then((cached) => {
      const networkPromise = fetch(req)
        .then((res) => {
          if (res && res.ok) {
            const copy = res.clone();
            caches.open(CACHE_NAME).then((c) => c.put(req, copy)).catch(() => {});
          }
          return res;
        })
        .catch(() => cached);
      return cached || networkPromise;
    })
  );
});
