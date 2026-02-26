/*
 * Canonical-aware service worker for DHIS2 global shell.
 *
 * Served by GlobalShellFilter when canonicalAppPaths is ON, replacing the
 * standard @dhis2/pwa worker which conflicts with the canonical URL scheme.
 *
 * Key differences from the standard worker:
 * 1. No Workbox precaching -- HTTP Cache-Control headers handle all caching
 * 2. Only caches shell HTML for exact /apps/{name} navigations (not subresources)
 * 3. Lets /apps/{name}/{resource} pass through for actual app resource serving
 * 4. Cleans up stale Workbox caches from the previous worker
 * 5. Preserves the message bus for shell-app communication (update prompts)
 */

const SHELL_NAV_PATTERN = /^\/apps\/[^/.]+\/?$/;
const SHELL_CACHE = 'dhis2-shell-v1';

self.addEventListener('install', () => self.skipWaiting());

self.addEventListener('activate', (event) => {
  event.waitUntil(
      caches.keys()
          .then((keys) => Promise.all(
              keys.filter((k) => k.startsWith('workbox-')).map((k) => caches.delete(k))
          ))
          .then(() => self.clients.claim())
  );
});

self.addEventListener('message', (event) => {
  if (!event.data) return;
  if (event.data.type === 'SKIP_WAITING') self.skipWaiting();
  if (event.data.type === 'CLIENTS_INFO') {
    self.clients.matchAll().then((clients) => {
      event.source.postMessage({
        type: 'CLIENTS_INFO',
        payload: { clientsCount: clients.length },
      });
    });
  }
});

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);
  if (url.origin !== self.location.origin) return;

  // Navigation to exact /apps/{name} or /apps/{name}/ -- serve shell HTML.
  // Do NOT intercept /apps/{name}/index.html, /apps/{name}/assets/*, etc.
  // Those are actual app resources that the backend routes correctly.
  if (
      event.request.mode === 'navigation' &&
      SHELL_NAV_PATTERN.test(url.pathname)
  ) {
    event.respondWith(
        fetch(event.request)
            .then((response) => {
              const clone = response.clone();
              caches.open(SHELL_CACHE).then((cache) => cache.put('shell-index', clone));
              return response;
            })
            .catch(() => caches.match('shell-index'))
    );
    return;
  }

  // Everything else: no interception. The browser's HTTP cache (populated by
  // Cache-Control headers from StaticCacheControlService) handles caching.
  // Hashed assets get max-age=31536000 (1 year), HTML gets no-store with
  // ?v= cache-busting. This provides the same instant-load performance that
  // Workbox precaching provided, without URL-scheme conflicts.
});
