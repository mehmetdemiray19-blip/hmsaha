package com.hm.saha;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.print.PdfYazici;
import android.print.PrintDocumentAdapter;
import android.provider.MediaStore;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.File;

/**
 * HM Saha – telefon uygulaması.
 * Ekranlar GitHub Pages'teki sayfadan (BuildConfig.HM_URL) yüklenir; böylece sayfa güncellenince
 * uygulama da güncellenir. İnternet yoksa sayfanın önbelleği, o da yoksa APK içindeki yedek kopya açılır.
 * Kayıtlar sayfadan doğrudan HM Ana Dosya'ya (Apps Script) gider.
 */
public class MainActivity extends Activity {

    private static final String ADRES = BuildConfig.HM_URL;
    private static final String YEDEK = "file:///android_asset/www/index.html";
    private static final int IZIN_KONUM = 11;
    private static final int SECIM_FOTO = 21;

    private WebView web;
    private boolean yedekte = false;

    private GeolocationPermissions.Callback konumCevap;
    private String konumKaynak;

    private ValueCallback<Uri[]> fotoCevap;
    private Uri kameraUri;

    @Override
    protected void onCreate(Bundle durum) {
        super.onCreate(durum);
        web = new WebView(this);
        setContentView(web);

        WebSettings ayar = web.getSettings();
        ayar.setJavaScriptEnabled(true);
        ayar.setDomStorageEnabled(true);
        ayar.setGeolocationEnabled(true);
        ayar.setAllowFileAccess(true);
        ayar.setCacheMode(WebSettings.LOAD_DEFAULT);
        ayar.setUserAgentString(ayar.getUserAgentString() + " HMSahaApp/" + BuildConfig.VERSION_NAME);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.addJavascriptInterface(new Kopru(), "HMAndroid");   // tutanak sayfası: PDF üret, paylaş / aç

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest istek) {
                return disaridaAc(istek.getUrl());
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest istek, WebResourceError hata) {
                if (istek.isForMainFrame() && !yedekte) {
                    yedekte = true;
                    v.loadUrl(YEDEK);
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String kaynak, GeolocationPermissions.Callback cevap) {
                if (konumIzniVar()) {
                    cevap.invoke(kaynak, true, false);
                    return;
                }
                konumCevap = cevap;
                konumKaynak = kaynak;
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, IZIN_KONUM);
            }

            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cevap, FileChooserParams secenek) {
                return fotoSec(cevap);
            }
        });

        if (durum != null) {
            web.restoreState(durum);
        } else {
            web.loadUrl(ADRES);
        }
    }

    /* ---------- Tutanak PDF: sayfa A4 PDF'e yazılır, paylaş menüsü (WhatsApp, e-posta, SMS…) ya da PDF görüntüleyici açılır ---------- */

    private class Kopru {
        @JavascriptInterface
        public String surum() {
            return BuildConfig.VERSION_NAME;
        }

        @JavascriptInterface
        public void pdf(final String dosyaAdi, final String islem, final String konu, final String metin) {
            runOnUiThread(() -> pdfUret(dosyaAdi, islem, konu, metin));
        }
    }

    private void pdfUret(String dosyaAdi, final String islem, final String konu, final String metin) {
        try {
            File klasor = new File(getCacheDir(), "pdf");
            if (!klasor.exists()) {
                klasor.mkdirs();
            }
            String ad = dosyaAdi == null ? "" : dosyaAdi.replaceAll("[^A-Za-z0-9._-]", "_");
            if (!ad.toLowerCase().endsWith(".pdf")) {
                ad = ad + ".pdf";
            }
            if (ad.length() > 90) {
                ad = ad.substring(ad.length() - 90);
            }
            File dosya = new File(klasor, ad);
            PrintDocumentAdapter adapter = web.createPrintDocumentAdapter(ad);
            PdfYazici.yaz(adapter, dosya, new PdfYazici.Sonuc() {
                @Override
                public void tamam(File f) {
                    jsBildir(true, "");
                    pdfKullan(f, islem, konu, metin);
                }

                @Override
                public void hata(String mesaj) {
                    jsBildir(false, mesaj);
                    Toast.makeText(MainActivity.this, "PDF oluşturulamadı: " + mesaj, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            jsBildir(false, String.valueOf(e));
        }
    }

    private void pdfKullan(File f, String islem, String konu, String metin) {
        Uri uri = FileProvider.getUriForFile(this, "com.hm.saha.dosya", f);
        Intent niyet;
        if ("ac".equals(islem)) {
            niyet = new Intent(Intent.ACTION_VIEW);
            niyet.setDataAndType(uri, "application/pdf");
            niyet.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            Intent gonder = new Intent(Intent.ACTION_SEND);
            gonder.setType("application/pdf");
            gonder.putExtra(Intent.EXTRA_STREAM, uri);
            gonder.putExtra(Intent.EXTRA_SUBJECT, konu);
            gonder.putExtra(Intent.EXTRA_TEXT, metin);
            gonder.setClipData(ClipData.newRawUri(f.getName(), uri));
            gonder.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            niyet = Intent.createChooser(gonder, "Tutanağı gönder");
        }
        try {
            startActivity(niyet);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Bu telefonda PDF açacak uygulama yok.", Toast.LENGTH_LONG).show();
        }
    }

    private void jsBildir(boolean ok, String mesaj) {
        if (web != null) {
            web.evaluateJavascript("window.pdfSonuc && window.pdfSonuc(" + ok + "," + JSONObject.quote(mesaj) + ")", null);
        }
    }

    /** Uygulamanın kendi sayfası içeride, diğer linkler (harita vb.) telefonun kendi uygulamasında açılır. */
    private boolean disaridaAc(Uri adres) {
        String a = adres.toString();
        if (a.startsWith(ADRES) || a.startsWith("file:///android_asset/")) {
            return false;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, adres));
        } catch (ActivityNotFoundException e) {
            return false;
        }
        return true;
    }

    private boolean konumIzniVar() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int kod, String[] izinler, int[] sonuclar) {
        super.onRequestPermissionsResult(kod, izinler, sonuclar);
        if (kod == IZIN_KONUM && konumCevap != null) {
            konumCevap.invoke(konumKaynak, konumIzniVar(), false);
            konumCevap = null;
        }
    }

    /** Fiş fotoğrafı: kamera veya galeri seçeneği. Kamera izni gerekmez (sistem kamera uygulaması kullanılır). */
    private boolean fotoSec(ValueCallback<Uri[]> cevap) {
        if (fotoCevap != null) {
            fotoCevap.onReceiveValue(null);
        }
        fotoCevap = cevap;
        kameraUri = null;

        Intent kamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            File klasor = new File(getCacheDir(), "foto");
            if (!klasor.exists()) {
                klasor.mkdirs();
            }
            File dosya = File.createTempFile("fis_", ".jpg", klasor);
            kameraUri = FileProvider.getUriForFile(this, "com.hm.saha.dosya", dosya);
            kamera.putExtra(MediaStore.EXTRA_OUTPUT, kameraUri);
            kamera.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception e) {
            kamera = null;
        }

        Intent galeri = new Intent(Intent.ACTION_GET_CONTENT);
        galeri.addCategory(Intent.CATEGORY_OPENABLE);
        galeri.setType("image/*");

        Intent secici = Intent.createChooser(galeri, "Fiş fotoğrafı");
        if (kamera != null) {
            secici.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{kamera});
        }
        try {
            startActivityForResult(secici, SECIM_FOTO);
        } catch (ActivityNotFoundException e) {
            fotoCevap.onReceiveValue(null);
            fotoCevap = null;
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int kod, int sonuc, Intent veri) {
        if (kod != SECIM_FOTO || fotoCevap == null) {
            super.onActivityResult(kod, sonuc, veri);
            return;
        }
        Uri[] secilen = null;
        if (sonuc == RESULT_OK) {
            if (veri != null && veri.getData() != null) {
                secilen = new Uri[]{veri.getData()};
            } else if (kameraUri != null) {
                secilen = new Uri[]{kameraUri};
            }
        }
        fotoCevap.onReceiveValue(secilen);
        fotoCevap = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) {
            web.onResume();
            if (yedekte && internetVar()) {   // yedek kopyadan çık, güncel sayfaya dön
                yedekte = false;
                web.loadUrl(ADRES);
            }
        }
    }

    @Override
    protected void onPause() {
        if (web != null) {
            web.onPause();
        }
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @SuppressWarnings("deprecation")
    private boolean internetVar() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        NetworkInfo ag = cm.getActiveNetworkInfo();
        return ag != null && ag.isConnected();
    }

    @Override
    protected void onSaveInstanceState(Bundle durum) {
        super.onSaveInstanceState(durum);
        if (web != null) {
            web.saveState(durum);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
        }
        super.onDestroy();
    }
}
