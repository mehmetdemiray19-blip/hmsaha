// HM Saha – çevrimdışı açılış için önbellek (uygulama dosyaları; kayıtlar hiçbir zaman önbelleğe alınmaz)
const SURUM = 'hm-saha-v4';
const DOSYALAR = ['./', './index.html', './ayar.js', './manifest.webmanifest',
  './icons/icon-192.png', './icons/icon-512.png', './icons/maskable-512.png', './icons/favicon.png', './filigran.png', './tutanak.html'];
self.addEventListener('install', e => { e.waitUntil(caches.open(SURUM).then(c => c.addAll(DOSYALAR))); self.skipWaiting(); });
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== SURUM).map(k => caches.delete(k)))));
  self.clients.claim();
});
self.addEventListener('fetch', e => {
  const u = new URL(e.request.url);
  if (e.request.method !== 'GET' || u.origin !== self.location.origin) return;   // sunucu çağrıları her zaman ağdan
  e.respondWith(
    fetch(e.request)
      .then(r => { const kopya = r.clone(); caches.open(SURUM).then(c => c.put(e.request, kopya)); return r; })
      .catch(() => caches.match(e.request).then(r => r || caches.match('./index.html')))
  );
});
