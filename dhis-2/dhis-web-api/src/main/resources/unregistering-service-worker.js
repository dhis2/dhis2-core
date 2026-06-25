/*
 * Self-unregistering service worker served for per-app scope requests
 * (e.g. /apps/dashboard/service-worker.js) when canonical app paths are enabled.
 *
 * Returning 404 for these requests would be ideal (no SW lifecycle at all),
 * but @dhis2/pwa's localhost validation logic fetches the SW URL manually and
 * calls registration.unregister() when it receives a 404 — which destroys the
 * PARENT scope's canonical service worker at /apps/.
 *
 * Serving a valid JS response avoids that unregister path. This SW activates
 * immediately, unregisters its own (per-app) registration, and disappears —
 * leaving the canonical /apps/ service worker untouched.
 */
self.addEventListener('install', function () {
  self.skipWaiting();
});

self.addEventListener('activate', function () {
  self.registration.unregister();
});
