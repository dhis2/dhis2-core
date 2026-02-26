/*
 * Minimal service worker for DHIS2 when canonical app paths are enabled.
 * Replaces the standard @dhis2/pwa worker which conflicts with the canonical URL scheme.
 * Does not cache or intercept any requests -- all caching is handled by HTTP Cache-Control
 * headers set by StaticCacheControlService.
 */
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});
