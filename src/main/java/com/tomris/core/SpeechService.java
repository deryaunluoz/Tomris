package com.tomris.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tomris'in metni sesli okumasını sağlar.
 * <p>
 * Gerçek metin-konuşma dönüşümü {@code tomris-jarvis\speak.ps1} PowerShell betiğinde
 * (edge-tts + Türkçe ses) yapılıyor; bu servis sadece o betiği çağırır ve seslendirme
 * bitene kadar bekler (betik zaten oynatmayı senkron olarak yönetiyor).
 */
public final class SpeechService {

    private static final Path SPEAK_SCRIPT = Path.of(
            System.getProperty("user.home"), "OneDrive", "Masaüstü", "tomris-jarvis", "speak.ps1");

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "tomris-speech-worker");
                thread.setDaemon(true);
                return thread;
            });

    /**
     * Verilen metni sesli okur. speak.ps1 oynatma bitene kadar bloklandığı için
     * bu çağrı da arka planda tamamlanana kadar sürer.
     */
    public CompletableFuture<Void> speakAsync(String text) {
        return CompletableFuture.runAsync(() -> speak(text), executor);
    }

    private void speak(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-File", SPEAK_SCRIPT.toString(),
                    text);
            processBuilder.directory(SPEAK_SCRIPT.getParent().toFile());
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException e) {
            throw new SpeechException("Seslendirme başlatılamadı: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SpeechException("Seslendirme iptal edildi.", e);
        }
    }

    /** Uygulama kapanırken arka plan thread'ini düzgünce sonlandırır. */
    public void shutdown() {
        executor.shutdownNow();
    }

    /** Seslendirme sırasında oluşan hataları taşıyan işaretli (unchecked) istisna. */
    public static final class SpeechException extends RuntimeException {
        public SpeechException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
