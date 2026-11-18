# HM Saha – saha kayıt uygulaması (Android APK)

Saha ekibinin malzeme, yapılan iş, puantaj, masraf (fiş fotoğrafı) ve ölçüm kayıtlarını
HM Ana Dosya'ya (Google Sheets) gönderdiği uygulama.

| Klasör | Ne işe yarar |
|---|---|
| `docs/` | Uygulamanın ekranları. GitHub Pages'te yayınlanır; telefondaki uygulama her açılışta buradan güncel hâli alır. |
| `docs/ayar.js` | Sunucu adresi (Apps Script web uygulaması, …/exec). |
| `android/` | APK kabuğu: ekranı açar, GPS izni, kamera ile fiş fotoğrafı, harita linklerini Google Haritalar'da açar, internet yokken yedek kopya. |
| `.github/workflows/apk.yml` | APK'yı GitHub'ın kendi sunucusunda derler ve **Releases** bölümüne koyar. |

## Kurulum (bir kez)
1. **Settings → Secrets and variables → Actions → New repository secret** ile iki gizli değer ekleyin:
   - `HM_KEYSTORE_B64` → `HM_KEYSTORE_B64.txt` dosyasının içeriği
   - `HM_KEYSTORE_PASS` → `HM_KEYSTORE_PASS.txt` dosyasının içeriği
2. **Settings → Pages → Build and deployment → Source: Deploy from a branch → Branch: `main` / `/docs` → Save.**
3. **Actions** sekmesinde "HM Saha APK" çalışır (5–10 dk). Yeşil tik olunca **Releases** bölümünde `HM-Saha.apk` çıkar.
   Gizli değerler 1. adımdan önce eklenmediyse: Actions → HM Saha APK → **Run workflow**.

## Personelin telefonuna yükleme
- Link (her zaman son sürüm): `https://github.com/<kullanıcı>/hm-saha/releases/latest/download/HM-Saha.apk`
- Telefonda linki açın → indir → dosyaya dokunun → "Bu kaynaktan yüklemeye izin ver" → **Yükle**.
- İlk açılışta **konum iznine "İzin ver"** deyin. Kayıt ol → ofis onayı (Personel sayfası N = ONAY) → giriş.

## Güncelleme
- **Ekran değişiklikleri** (`docs/`): dosyayı GitHub'da değiştirin; 1–2 dakika içinde herkesin uygulamasında görünür. APK yeniden yüklenmez.
- **Malzeme / iş / personel listeleri**: HM Ana Dosya'dan gelir; GitHub'a dokunmak gerekmez.
- **APK kabuğu** (`android/`): yalnız simge, izin vb. değişirse yeni APK çıkar; aynı imza anahtarıyla eskisinin üzerine kurulur.

İmza anahtarını (`hm-saha.jks` ve şifresi) GitHub'a dosya olarak YÜKLEMEYİN; yalnız gizli değer olarak ekleyin ve ofiste saklayın.
