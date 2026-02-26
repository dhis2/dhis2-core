/*
 * Canonical-aware service worker for DHIS2 when canonical app paths are enabled.
 * Handles the @dhis2/pwa message protocol but does not intercept fetches or cache.
 * All caching is handled by HTTP Cache-Control headers from StaticCacheControlService.
 */
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', () => {});

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
