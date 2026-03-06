/*
 * Canonical-aware service worker for DHIS2 when canonical app paths are enabled.
 * Handles the @dhis2/pwa message protocol and caches global-shell assets.
 * All other caching is handled by HTTP Cache-Control headers from StaticCacheControlService.
 */
var SHELL_CACHE = 'global-shell-v1';
var SHELL_ASSET_PREFIX = '/apps/global-shell/';

self.addEventListener('install', () => self.skipWaiting());

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys.filter((k) => k.startsWith('workbox-') || k.startsWith('other-assets') || k.startsWith('app-shell'))
            .map((k) => caches.delete(k))
      )
    ).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  var url = new URL(event.request.url);
  if (url.origin !== self.location.origin) return;

  // Cache-first for global-shell assets (hashed filenames = immutable content)
  if (url.pathname.startsWith(SHELL_ASSET_PREFIX)) {
    event.respondWith(
      caches.match(event.request, { ignoreSearch: true }).then((cached) => {
        if (cached) return cached;
        return fetch(event.request).then((response) => {
          if (response.ok) {
            var clone = response.clone();
            caches.open(SHELL_CACHE).then((cache) => cache.put(event.request, clone));
          }
          return response;
        });
      })
    );
    return;
  }

  // Everything else: pass through to network (HTTP cache handles it)
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
      if (event.source) {
        event.source.postMessage({
          type: 'DHIS2_CONNECTION_STATUS_UPDATE',
          payload: { isConnected: true },
        });
      }
      break;
  }
});
