const C = "bozor-v1";
self.addEventListener("install", () => self.skipWaiting());
self.addEventListener("activate", e => {
  e.waitUntil(caches.keys().then(k => Promise.all(k.filter(x => x !== C).map(x => caches.delete(x)))).then(() => self.clients.claim()));
});
// Har doim avval internetdan olinadi (yangilanishlar darhol keladi); internet yo'q bo'lsa, oxirgi nusxa.
self.addEventListener("fetch", e => {
  const r = e.request, u = new URL(r.url);
  if (r.method !== "GET" || u.origin !== location.origin || u.pathname.startsWith("/api/")) return;
  e.respondWith(fetch(r).then(res => {
    if (res.ok && r.mode === "navigate") { const c = res.clone(); caches.open(C).then(x => x.put("/", c)); }
    return res;
  }).catch(() => r.mode === "navigate" ? caches.match("/").then(m => m || Response.error()) : Response.error()));
});
