/*
 * Canonical-aware service worker for DHIS2 when canonical app paths are enabled.
 * Replaces the standard @dhis2/pwa worker which conflicts with the canonical URL scheme.
 *
 * Handles the @dhis2/pwa message protocol (GET_CLIENTS_INFO, SKIP_WAITING, CLAIM_CLIENTS)
 * but does NOT intercept fetches or precache -- all caching is handled by HTTP
 * Cache-Control headers set by StaticCacheControlService.
 */
self.addEventListener('install', () => self.skipWaiting());

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('message', (event) => {
  if (!event.data || !event.data.type) return;

  switch (event.data.type) {
    case 'GET_CLIENTS_INFO':
      self.clients.matchAll().then((clients) => {
        if (event.source) {
          event.source.postMessage({
            type: 'CLIENTS_INFO',
            payload: { clientsCount: clients.length },
          });
        }
      });
      break;

    case 'SKIP_WAITING':
      self.skipWaiting();
      break;

    case 'CLAIM_CLIENTS':
      self.clients.claim();
      break;
  }
});
