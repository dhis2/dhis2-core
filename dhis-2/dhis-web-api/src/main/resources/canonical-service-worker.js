/*
 * Canonical-aware service worker for DHIS2 when canonical app paths are enabled.
 * Handles the @dhis2/pwa message protocol but does not intercept fetches or cache.
 * All caching is handled by HTTP Cache-Control headers from StaticCacheControlService.
 */
self.addEventListener('install', () => self.skipWaiting());

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim());
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
    case 'GET_IMMEDIATE_DHIS2_CONNECTION_STATUS_UPDATE':
      // No-op: we don't track connection status in the canonical SW.
      // Respond with a default "online" status to prevent client-side errors.
      if (event.source) {
        event.source.postMessage({
          type: 'DHIS2_CONNECTION_STATUS_UPDATE',
          payload: { isConnected: true },
        });
      }
      break;
  }
});
