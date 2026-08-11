package com.tomris.core;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Vosk kullanarak mikrofon girdisini çevrimdışı olarak Türkçe metne çevirir.
 * <p>
 * Model dosyaları internete gitmeden, projenin {@code models/vosk-model-small-tr-0.3}
 * klasöründen yüklenir. Servis SÜREKLİ DİNLEME modunda çalışır: {@link #startContinuousListening}
 * çağrıldığında mikrofon açık kalır ve Vosk her sessizlik/duraklama anında bir cümleyi
 * tamamlanmış sayıp {@code onUtterance} geri çağrısını tetikler; bu, kullanıcı her seferinde
 * bir düğmeye basmadan sürekli arka planda dinlenmesini sağlar. "Tomris" uyandırma kelimesi
 * filtresi bilerek burada değil, çağıran taraf ({@code MainView}) içinde uygulanır - bu servis
 * yalnızca ham metni üretmekten sorumludur.
 * <p>
 * Türkçe için Vosk'un tek resmi modeli "small" (küçük/düşük doğruluklu) boyutta olduğundan tek
 * bir en-olası tahmine güvenmek yetersiz kalıyor. Bu yüzden {@link Recognizer#setMaxAlternatives}
 * ile her cümle için birden fazla aday transkript (n-best) isteniyor ve {@code onUtterance}
 * çağıranına TEK bir metin yerine adayların listesi ({@code en olası tahmin en başta}) veriliyor;
 * çağıran taraf uyandırma kelimesini adaylardan HERHANGİ birinde arayarak yakalama şansını artırır.
 */
public final class VoiceInputService {

    static {
        // KOK NEDEN DUZELTMESI: Proje yolu Turkce karakter iceriyor (ornegin "Masaüstü").
        // JNA, varsayilan olarak JVM'in "file.encoding" degerini (genelde UTF-8) kullanarak
        // Java String -> native char* donusumu yapar. Ancak Windows'ta Vosk'un native
        // kutuphanesi (libvosk.dll) dosya yollarini isletim sisteminin ANSI kod sayfasiyla
        // (sun.jnu.encoding, ornegin Turkce Windows'ta Cp1254) acar. Bu uyusmazlik "ü", "ş"
        // gibi karakterleri bozarak stat()/fopen() cagrilarinin dosyayi bulamamasina ve
        // Vosk'un "Failed to create a model" hatasi vermesine yol aciyordu (native log:
        // "ERROR (VoskAPI:Model():model.cc:122) Folder '...' does not contain model files").
        // Cozum: JNA'ya native cagrilarda JVM'in kendi native/dosya kodlamasini (sun.jnu.encoding)
        // kullanmasini soylemek. Bu, org.vosk.* siniflari ilk kez yuklenmeden (Model/LibVosk
        // JNA baglantisini kurmadan) once yapilmali; bu yuzden statik blokta, sinif ilk
        // referans alindigi anda calisir.
        if (System.getProperty("jna.encoding") == null) {
            String nativeEncoding = System.getProperty("sun.jnu.encoding");
            if (nativeEncoding != null) {
                System.setProperty("jna.encoding", nativeEncoding);
                log("jna.encoding ayarlanmadigi icin sun.jnu.encoding'den alinan '" + nativeEncoding
                        + "' degeri kullanildi (Turkce/ozel karakter iceren dosya yollarinin native "
                        + "kutuphaneye dogru iletilmesi icin).");
            }
        }
    }

    private static final Path MODEL_PATH =
            Path.of(System.getProperty("user.dir"), "models", "vosk-model-small-tr-0.3");
    private static final float SAMPLE_RATE = 16000f;
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    // Kucuk Turkce modelin dusuk dogrulugunu telafi etmek icin tek tahmin yerine birden fazla
    // aday transkript istiyoruz - uyandirma kelimesi bunlardan herhangi birinde aranacak.
    private static final int MAX_RECOGNITION_ALTERNATIVES = 5;

    private final AtomicBoolean listening = new AtomicBoolean(false);
    private volatile TargetDataLine currentLine;
    private Model model;

    public boolean isModelAvailable() {
        boolean available = Files.isDirectory(MODEL_PATH);
        log("Model yolu kontrol ediliyor: " + MODEL_PATH.toAbsolutePath() + " | bulundu mu: " + available
                + " | calisma dizini (user.dir): " + System.getProperty("user.dir"));
        return available;
    }

    /**
     * Sürekli dinlemeyi başlatır: mikrofon açık kalır, Vosk her cümle tamamlandığında (bir
     * sessizlik/duraklama algıladığında) {@code onUtterance} geri çağrısını o cümlenin
     * metniyle tetikler ve dinlemeye devam eder - {@link #stopContinuousListening()}
     * çağrılana kadar bu döngü sürer. Zaten dinleniyorsa çağrı yok sayılır (idempotent).
     * <p>
     * Geri çağrılar yakalama arka plan iş parçacığından (capture thread) tetiklenir; çağıran
     * taraf UI güncellemesi yapacaksa kendi thread'ine (ör. {@code Platform.runLater}) geçmekle
     * yükümlüdür.
     */
    public void startContinuousListening(Consumer<List<String>> onUtterance, Consumer<Throwable> onError) {
        log("startContinuousListening() cagrildi.");
        if (!listening.compareAndSet(false, true)) {
            log("Zaten surekli dinleniyor, yeni istek yok sayildi.");
            return;
        }

        Thread captureThread = new Thread(() -> continuousCaptureLoop(onUtterance, onError), "tomris-voice-capture");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    /** Sürekli dinlemeyi durdurur; yakalama döngüsü bunu görüp mikrofonu kapatır. */
    public void stopContinuousListening() {
        log("stopContinuousListening() cagrildi.");
        listening.set(false);
        TargetDataLine line = currentLine;
        if (line != null) {
            line.stop();
            line.close();
        }
    }

    /** Sürekli dinleme şu an aktif mi (mikrofon açık, arka planda dinliyor mu)? */
    public boolean isListening() {
        return listening.get();
    }

    /** Uygulama kapanırken yüklü Vosk modelinin native kaynaklarını serbest bırakır. */
    public synchronized void shutdown() {
        stopContinuousListening();
        if (model != null) {
            model.close();
            model = null;
        }
    }

    private void continuousCaptureLoop(Consumer<List<String>> onUtterance, Consumer<Throwable> onError) {
        log("Surekli yakalama dongusu basladi (thread: " + Thread.currentThread().getName() + ").");
        try {
            ensureModelLoaded();

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
            log("Istenen ses formati: " + AUDIO_FORMAT + " | sistem genelinde destekleniyor mu: "
                    + AudioSystem.isLineSupported(info));
            if (!AudioSystem.isLineSupported(info)) {
                logAvailableMixers(info);
                throw new VoiceInputException("Mikrofon 16kHz mono ses formatını desteklemiyor.");
            }

            try (TargetDataLine line = openMicrophoneLine(info);
                 Recognizer recognizer = new Recognizer(model, SAMPLE_RATE)) {

                recognizer.setMaxAlternatives(MAX_RECOGNITION_ALTERNATIVES);
                currentLine = line;
                log("Ses hatti aciliyor: " + line.getLineInfo());
                line.open(AUDIO_FORMAT);
                line.start();
                log("Surekli dinleme basladi, 'Tomris' uyandirma kelimesi bekleniyor...");

                byte[] buffer = new byte[4096];
                long totalBytes = 0;
                int chunkCount = 0;
                long peakAmplitude = 0;
                while (listening.get()) {
                    int bytesRead = line.read(buffer, 0, buffer.length);
                    if (bytesRead <= 0) {
                        continue;
                    }
                    totalBytes += bytesRead;
                    chunkCount++;
                    peakAmplitude = Math.max(peakAmplitude, peakAmplitude(buffer, bytesRead));
                    if (chunkCount % 200 == 0) {
                        log("... " + totalBytes + " byte ses verisi yakalandi (o ana kadarki tepe genlik: "
                                + peakAmplitude + ")");
                    }

                    boolean utteranceFinished = recognizer.acceptWaveForm(buffer, bytesRead);
                    if (utteranceFinished) {
                        List<String> candidates = extractCandidates(recognizer.getResult());
                        if (!candidates.isEmpty()) {
                            log("Bir cumle tamamlandi, Vosk'un aday tahminleri: " + candidates);
                            onUtterance.accept(candidates);
                        }
                    }
                }
                log("Surekli dinleme durduruldu. Toplam " + totalBytes + " byte yakalandi, tepe genlik: "
                        + peakAmplitude + (peakAmplitude < 200
                        ? " -> UYARI: ses seviyesi cok dusuk, mikrofon susturulmus ya da yanlis cihaz secilmis olabilir."
                        : ""));
            }
        } catch (LineUnavailableException | VoiceInputException e) {
            log("HATA - Mikrofona erisilemedi: " + e.getMessage());
            e.printStackTrace();
            listening.set(false);
            onError.accept(new VoiceInputException("Mikrofona erişilemedi: " + e.getMessage()
                    + " (Windows Ayarlar > Gizlilik > Mikrofon altinda masaustu uygulamalarina mikrofon izni verildiginden emin olun.)", e));
        } catch (Exception e) {
            log("HATA - Surekli sesli giris basarisiz: " + e.getMessage());
            e.printStackTrace();
            listening.set(false);
            onError.accept(new VoiceInputException("Sesli giriş başarısız: " + e.getMessage(), e));
        } finally {
            currentLine = null;
            listening.set(false);
            log("Surekli yakalama dongusu sona erdi.");
        }
    }

    /**
     * Uygun mikrofon hattini destekleyen ses cihazini (mixer) acikca arar ve loglar;
     * boylece hangi cihazin secildigi konsoldan izlenebilir. Hicbiri bulunamazsa
     * sistemin varsayilan cozumlemesine geri doner.
     */
    private TargetDataLine openMicrophoneLine(DataLine.Info info) throws LineUnavailableException {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        log(mixers.length + " ses cihazi taraniyor...");
        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.isLineSupported(info)) {
                log("Secilen mikrofon cihazi: '" + mixerInfo.getName() + "' (" + mixerInfo.getDescription() + ")");
                return (TargetDataLine) mixer.getLine(info);
            }
        }
        log("Formati destekleyen ozel bir cihaz bulunamadi, sistem varsayilanina dusuluyor.");
        return (TargetDataLine) AudioSystem.getLine(info);
    }

    private void logAvailableMixers(DataLine.Info info) {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        log("Sistemde bulunan ses cihazlari (" + mixers.length + " adet):");
        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            boolean supportsRequested = mixer.isLineSupported(info);
            log("  - '" + mixerInfo.getName() + "' (" + mixerInfo.getDescription() + ") | istenen formati destekliyor: "
                    + supportsRequested + " | girdi hatti sayisi: " + mixer.getTargetLineInfo().length);
        }
    }

    private static long peakAmplitude(byte[] buffer, int length) {
        long peak = 0;
        for (int i = 0; i + 1 < length; i += 2) {
            int sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
            peak = Math.max(peak, Math.abs(sample));
        }
        return peak;
    }

    private static void log(String message) {
        System.out.println("[VoiceInput] " + message);
    }

    private synchronized void ensureModelLoaded() {
        if (model != null) {
            log("Vosk modeli zaten yukluydu, tekrar yuklenmiyor.");
            return;
        }
        if (!isModelAvailable()) {
            throw new VoiceInputException("Vosk modeli bulunamadı: " + MODEL_PATH);
        }
        try {
            log("Vosk modeli yukleniyor: " + MODEL_PATH);
            model = new Model(MODEL_PATH.toString());
            log("Vosk modeli basariyla yuklendi.");
        } catch (IOException e) {
            throw new VoiceInputException("Vosk modeli yüklenemedi: " + e.getMessage(), e);
        }
    }

    /**
     * {@code setMaxAlternatives} açıkken Vosk'un sonuç JSON'u {@code {"text": "..."}} yerine
     * {@code {"alternatives": [{"text": "...", "confidence": ...}, ...]}} şeklinde geliyor
     * (en olası tahmin dizinin başında). Bu metod tüm adayları, en olası olan en başta olacak
     * şekilde bir listeye çıkarır; boş metinleri eler. Alternatifler yoksa (ör. beklenmedik bir
     * eski format) düz "text" alanına geri döner.
     */
    private List<String> extractCandidates(String resultJson) {
        List<String> candidates = new ArrayList<>();
        try {
            JSONObject result = new JSONObject(resultJson);
            JSONArray alternatives = result.optJSONArray("alternatives");
            if (alternatives != null) {
                for (int i = 0; i < alternatives.length(); i++) {
                    String text = alternatives.getJSONObject(i).optString("text", "").trim();
                    if (!text.isBlank()) {
                        candidates.add(text);
                    }
                }
            } else {
                String text = result.optString("text", "").trim();
                if (!text.isBlank()) {
                    candidates.add(text);
                }
            }
        } catch (Exception e) {
            log("Vosk sonucu ayristirilamadi: " + e.getMessage());
        }
        return candidates;
    }

    /** Sesli giriş sırasında oluşan hataları taşıyan işaretli (unchecked) istisna. */
    public static final class VoiceInputException extends RuntimeException {
        public VoiceInputException(String message) {
            super(message);
        }

        public VoiceInputException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
