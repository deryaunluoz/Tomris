package com.tomris.core;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * "UYGULAMALARIM" kısayol satırı ve yerel komutlar (ör. YouTube araması) için
 * masaüstü uygulamalarını ve tarayıcıyı açan yardımcı sınıf.
 */
public final class AppLauncher {

    private static final Path JETBRAINS_DIR = Path.of("C:", "Program Files", "JetBrains");

    private AppLauncher() {
    }

    /** VS Code'u "code" komutuyla açar (npm/Node CLI'lerde olduğu gibi bir .cmd shim'i). */
    public static void openVsCode() {
        try {
            new ProcessBuilder("cmd.exe", "/c", "code").start();
        } catch (IOException e) {
            throw new LaunchException("VS Code açılamadı: " + e.getMessage(), e);
        }
    }

    /** JetBrains klasöründeki en güncel IntelliJ IDEA kurulumunu bulup açar. */
    public static void openIntelliJ() {
        Path executable = findIntelliJExecutable()
                .orElseThrow(() -> new LaunchException("IntelliJ IDEA kurulumu bulunamadı."));
        try {
            new ProcessBuilder(executable.toString()).start();
        } catch (IOException e) {
            throw new LaunchException("IntelliJ IDEA açılamadı: " + e.getMessage(), e);
        }
    }

    public static void openGitHub() {
        openUrl("https://github.com");
    }

    public static void openGmail() {
        openUrl("https://mail.google.com");
    }

    public static void openICloud() {
        openUrl("https://www.icloud.com");
    }

    public static void openSpotifyWeb() {
        openUrl("https://open.spotify.com");
    }

    public static void openYouTube() {
        openUrl("https://www.youtube.com");
    }

    public static void openYouTubeSearch(String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        openUrl("https://www.youtube.com/results?search_query=" + encoded);
    }

    public static void openUrl(String url) {
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (IOException e) {
            throw new LaunchException("Tarayıcı açılamadı: " + e.getMessage(), e);
        }
    }

    /** "C:\Program Files\JetBrains" altındaki "IntelliJ IDEA*" klasörlerinden en yenisini seçer. */
    private static Optional<Path> findIntelliJExecutable() {
        if (!Files.isDirectory(JETBRAINS_DIR)) {
            return Optional.empty();
        }
        try (Stream<Path> entries = Files.list(JETBRAINS_DIR)) {
            return entries
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("IntelliJ IDEA"))
                    .max(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(path -> path.resolve("bin").resolve("idea64.exe"))
                    .filter(Files::exists);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /** Uygulama/tarayıcı başlatma sırasında oluşan hataları taşıyan işaretli (unchecked) istisna. */
    public static final class LaunchException extends RuntimeException {
        public LaunchException(String message) {
            super(message);
        }

        public LaunchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
