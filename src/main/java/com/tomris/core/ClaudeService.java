package com.tomris.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tomris'in "beyni" ile olan köprü.
 * <p>
 * Gerçek zeka ve tüm entegrasyonlar (Gmail, iCloud, Google Takvim, Spotify) Claude Code CLI
 * üzerinde, {@code tomris-jarvis} çalışma dizininde zaten yapılandırılmış durumda. Bu servis,
 * kullanıcının mesajını o dizinde "claude -p" (print / non-interactive mod) komutuyla çalıştırır
 * ve tek satırlık nihai cevabı döndürür. Java tarafı sadece bir kabuk/arayüzdür; herhangi bir
 * zeka mantığı burada tekrarlanmaz.
 * <p>
 * ÖNEMLİ MİMARİ KARARI: Java tarafında (ör. eski {@code CommandRouter}) hiçbir yerel kalıp
 * eşleştirme/regex yönlendirmesi YOKTUR. Uyandırma kelimesi filtresinden geçen HER metin,
 * istisnasız, olduğu gibi buraya - dolayısıyla gerçek Claude'a - gönderilir. Uygulama açma,
 * takvim/mail/müzik işlemleri, tarif/genel bilgi soruları, günlük sohbet - hepsi Claude'un
 * kendi zekâsı ve MCP araçlarıyla karşılanır. Bunu mümkün kılmak için her çağrıya
 * {@link #TOMRIS_SYSTEM_PROMPT} ({@code --append-system-prompt}) eklenir; bu, Claude'un varsayılan
 * sistem promptunu SİLMEZ, üstüne ekler.
 */
public final class ClaudeService {

    /** Claude Code CLI'nin çalıştığı, tüm entegrasyonların (MCP sunucuları vb.) tanımlı olduğu dizin. */
    private static final Path WORKING_DIRECTORY =
            Path.of(System.getProperty("user.home"), "OneDrive", "Masaüstü", "tomris-jarvis");

    /** npm global kurulumunda Claude Code'un gerçek yürütülebilir dosyası. */
    private static final Path CLAUDE_EXECUTABLE = Path.of(
            System.getenv().getOrDefault("APPDATA", ""),
            "npm", "node_modules", "@anthropic-ai", "claude-code", "bin", "claude.exe");

    /**
     * Her çağrıya eklenen Tomris kişiliği + araç talimatları. Kullanıcının sesle söylediği HER
     * ŞEY buraya (bu prompt'un yönlendirdiği bir Claude çağrısına) gelir; bilgi sorusu, tarif,
     * uygulama açma, takvim/mail/müzik işlemi, günlük sohbet fark etmez - hepsi doğal bir
     * yapay zeka gibi karşılanmalı, önceden tanımlı kalıplara sıkıştırılmamalıdır.
     */
    private static final String TOMRIS_SYSTEM_PROMPT = """
            Kullanıcı sesli komut veriyor, sen Tomris'sin, onun kişisel asistanısın. Her türlü \
            isteğe (bilgi sorusu, tarif, uygulama açma, takvim/mail/müzik işlemleri, günlük sohbet) \
            gerçek bir yapay zeka gibi doğal şekilde cevap ver. Cevabın sesli okunacak, bu yüzden \
            markdown biçimlendirmesi (**, #, -, emoji vb.) KULLANMA - düz, konuşma diline uygun \
            Türkçe metin yaz.

            Kullanıcı uyandığını belirtirse (ör. "ben uyandım", "günaydın Tomris" gibi bir ifade \
            kullanırsa), /gunaydin skill'ini çalıştır.

            Uygulama açman gerekirse şu komutları kullanabilirsin (PowerShell/Bash aracıyla):
            - YouTube: Start-Process "https://www.youtube.com" (belirli bir arama için: \
            Start-Process "https://www.youtube.com/results?search_query=ARANACAK+KELIME")
            - WhatsApp: Start-Process "https://web.whatsapp.com"
            - GitHub: Start-Process "https://github.com"
            - VS Code: Start-Process "code" (veya: cmd /c code)
            - IntelliJ IDEA: önce kurulu sürümü bul, sonra aç:
              $dir = Get-ChildItem "C:\\Program Files\\JetBrains" | Where-Object Name -like \
            "IntelliJ IDEA*" | Sort-Object Name -Descending | Select-Object -First 1
              Start-Process "$($dir.FullName)\\bin\\idea64.exe\"""";

    private static final List<String> SYSTEM_PROMPT_FLAG = List.of("--append-system-prompt", TOMRIS_SYSTEM_PROMPT);

    private final ExecutorService executor =
            Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "tomris-claude-worker");
                thread.setDaemon(true);
                return thread;
            });

    /**
     * Kullanıcı mesajını Claude Code'a gönderir ve cevabı asenkron olarak döndürür.
     * UI thread'i bloklamamak için çağrı bir arka plan thread'inde yürütülür.
     */
    public CompletableFuture<String> sendMessageAsync(String userMessage) {
        return CompletableFuture.supplyAsync(() -> runClaude(userMessage), executor);
    }

    private String runClaude(String userMessage) {
        try {
            List<String> command = buildCommand(userMessage);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(WORKING_DIRECTORY.toFile());
            // stdout ve stderr tek akışta birleştirilir: iki akışı ayrı ayrı senkron okumak,
            // biri dolup diğeri boşalmadan process.waitFor() çağrılırsa kilitlenmeye yol açabilir.
            processBuilder.redirectErrorStream(true);
            // stdin'i Windows'un boş aygıtına bağla: aksi halde Claude CLI, olası bir pipe
            // girdisi olup olmadığını anlamak için birkaç saniye bekleyip bunu bir uyarı
            // olarak stderr'e (dolayısıyla cevabın içine) yazıyordu.
            processBuilder.redirectInput(new File("NUL"));

            Process process = processBuilder.start();
            String output = readStream(process.getInputStream());

            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new ClaudeException("Claude yanıt vermedi (zaman aşımı).");
            }

            if (process.exitValue() != 0) {
                throw new ClaudeException("Claude Code hata döndürdü: " + output.trim());
            }

            return output.trim();
        } catch (IOException e) {
            throw new ClaudeException("Claude Code çalıştırılamadı: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ClaudeException("Claude Code çağrısı iptal edildi.", e);
        }
    }

    /**
     * Yürütülebilir dosyayı doğrudan çağırır (kabuk/cmd araya girmez), böylece kullanıcı
     * mesajındaki özel karakterler (&, |, %, vb.) bir shell tarafından yorumlanmaz.
     * npm'in beklenen yolunda bulunamazsa PATH üzerinden "claude" komutuna geri döner.
     */
    private List<String> buildCommand(String userMessage) {
        String executable = Files.exists(CLAUDE_EXECUTABLE)
                ? CLAUDE_EXECUTABLE.toString()
                : "claude";
        List<String> command = new java.util.ArrayList<>();
        command.add(executable);
        command.addAll(SYSTEM_PROMPT_FLAG);
        command.add("-p");
        command.add(userMessage);
        return command;
    }

    private String readStream(java.io.InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append(System.lineSeparator());
            }
        }
        return builder.toString();
    }

    /** Uygulama kapanırken arka plan thread havuzunu düzgünce sonlandırır. */
    public void shutdown() {
        executor.shutdownNow();
    }

    /** Claude Code çağrısı sırasında oluşan hataları taşıyan işaretli (unchecked) istisna. */
    public static final class ClaudeException extends RuntimeException {
        public ClaudeException(String message) {
            super(message);
        }

        public ClaudeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
