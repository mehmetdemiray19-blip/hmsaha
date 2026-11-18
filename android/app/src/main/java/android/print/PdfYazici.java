package android.print;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

import java.io.File;

/**
 * WebView'deki sayfayı yazdırma motoruyla (Chrome'un "PDF olarak kaydet"i ile aynı) A4 PDF dosyasına yazar.
 * Yazdırma diyaloğu açılmaz. LayoutResultCallback / WriteResultCallback kurucuları yalnız bu paketten
 * çağrılabildiği için sınıf android.print paketindedir.
 */
public class PdfYazici {

    public interface Sonuc {
        void tamam(File dosya);

        void hata(String mesaj);
    }

    public static void yaz(final PrintDocumentAdapter adapter, final File dosya, final Sonuc sonuc) {
        final PrintAttributes ozellik = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(new PrintAttributes.Resolution("hm", "hm", 600, 600))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();
        adapter.onStart();
        adapter.onLayout(null, ozellik, new CancellationSignal(), new PrintDocumentAdapter.LayoutResultCallback() {
            @Override
            public void onLayoutFinished(PrintDocumentInfo bilgi, boolean degisti) {
                final ParcelFileDescriptor pfd;
                try {
                    pfd = ParcelFileDescriptor.open(dosya, ParcelFileDescriptor.MODE_CREATE
                            | ParcelFileDescriptor.MODE_TRUNCATE | ParcelFileDescriptor.MODE_READ_WRITE);
                } catch (Exception e) {
                    adapter.onFinish();
                    sonuc.hata(String.valueOf(e));
                    return;
                }
                adapter.onWrite(new PageRange[]{PageRange.ALL_PAGES}, pfd, new CancellationSignal(),
                        new PrintDocumentAdapter.WriteResultCallback() {
                            @Override
                            public void onWriteFinished(PageRange[] sayfalar) {
                                kapat(pfd);
                                adapter.onFinish();
                                sonuc.tamam(dosya);
                            }

                            @Override
                            public void onWriteFailed(CharSequence hata) {
                                kapat(pfd);
                                adapter.onFinish();
                                sonuc.hata(String.valueOf(hata));
                            }

                            @Override
                            public void onWriteCancelled() {
                                kapat(pfd);
                                adapter.onFinish();
                                sonuc.hata("iptal edildi");
                            }
                        });
            }

            @Override
            public void onLayoutFailed(CharSequence hata) {
                adapter.onFinish();
                sonuc.hata(String.valueOf(hata));
            }

            @Override
            public void onLayoutCancelled() {
                adapter.onFinish();
                sonuc.hata("iptal edildi");
            }
        }, new Bundle());
    }

    private static void kapat(ParcelFileDescriptor p) {
        try {
            p.close();
        } catch (Exception e) {
            // yok say
        }
    }
}
