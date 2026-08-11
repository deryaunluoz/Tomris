package com.tomris.ui;

import com.tomris.core.CalendarPlanService;
import com.tomris.core.ClaudeService;
import com.tomris.core.SpeechService;
import com.tomris.core.SpotifyNowPlayingService;
import com.tomris.core.VoiceInputService;
import com.tomris.core.WakeWordDetector;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.CompletionException;

/**
 * Tomris'in ana ekranını inşa eden sınıf. Sorumluluğu tamamen görünümdür:
 * kullanıcı etkileşimini {@link ClaudeService}, {@link SpeechService} ve
 * {@link VoiceInputService} çağrılarına bağlar, sonuçları gösterge paneli kartlarına ve
 * enerji küresine yansıtır. Arayüz tamamen sesle çalışır: yazılı bir sohbet alanı yoktur.
 * <p>
 * Mikrofon SÜREKLİ DİNLEME modundadır (arka planda her zaman açıktır); kullanıcı düğmeye
 * basıp konuşmak zorunda değildir, sadece "Tomris" diyerek çağırır. Vosk'un yazıya döktüğü
 * her cümle önce {@link WakeWordDetector} ile süzülür - "Tomris" (ya da yakın bir telaffuzu)
 * içermeyen cümleler (arka plan gürültüsü, TV sesi, alakasız konuşma) sessizce yok sayılır,
 * Claude'a asla gönderilmez. Küçük mikrofon butonu artık başlat/durdur değil, aç/kapat
 * (sessize alma) görevi görür; durum çubuğunda ise sadece kısa "dinliyorum / düşünüyorum /
 * konuşuyorum" gibi anlık durum metinleri görünür.
 * <p>
 * ÖNEMLİ: Tomris kesinlikle KENDİLİĞİNDEN konuşmaz - ne uygulama açılışında ne de periyodik
 * olarak. Tek konuşma tetikleyicisi, uyandırma kelimesi filtresinden geçen gerçek bir sesli
 * komuttur. Ayrıca yerel bir "hangi kelime hangi eyleme gider" eşleştirmesi (regex/keyword
 * router) YOKTUR - uyandırma filtresinden geçen HER metin, istisnasız, doğrudan
 * {@link ClaudeService}'e gönderilir; uygulama açma, takvim/mail/müzik işlemleri, genel bilgi
 * soruları, tarifler, sohbet - hepsi Claude'un kendi zekâsıyla (ve tomris-jarvis dizinindeki
 * system prompt / skill'lerle) karşılanır.
 */
public class MainView {

    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Duration TRANSIENT_STATUS_DURATION = Duration.seconds(3.5);

    private final ClaudeService claudeService;
    private final SpeechService speechService;
    private final VoiceInputService voiceInputService = new VoiceInputService();
    private final CalendarPlanService calendarPlanService;
    private final SpotifyNowPlayingService spotifyNowPlayingService;

    private final EnergyOrb orb = new EnergyOrb(164);
    private final WeatherCard weatherCard = new WeatherCard();
    private final Button micButton = new Button("🎤");
    private final Label statusLabel = new Label();

    private boolean processing = false;

    public MainView(ClaudeService claudeService, SpeechService speechService) {
        this.claudeService = claudeService;
        this.speechService = speechService;
        this.calendarPlanService = new CalendarPlanService(claudeService);
        this.spotifyNowPlayingService = new SpotifyNowPlayingService(claudeService);
    }

    /** Ana ekranın kök düğümünü oluşturur ve döndürür. */
    public Region build() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-background");

        root.setTop(buildTopBar());
        root.setCenter(buildCenterArea());
        root.setBottom(buildInputBar());

        return root;
    }

    /**
     * Uygulama açılır açılmaz çağrılır. DİKKAT: bu metod hiçbir şey SÖYLEMEZ/seslendirmez -
     * sadece sürekli dinlemeyi sessizce başlatır, böylece kullanıcı istediği an "Tomris" diyerek
     * çağırabilir. Önceden burada otomatik bir "/gunaydin" selamı tetiklenirdi; bu davranış
     * kasıtlı olarak kaldırıldı - Tomris artık SADECE kullanıcı onu sesle çağırdığında konuşur
     * (ör. "Tomris ben uyandım" dendiğinde günaydın selamı Claude tarafında tetiklenir).
     */
    public void startVoiceMode() {
        startContinuousVoiceMode();
    }

    /** Uygulama kapanırken sesli giriş servisinin native kaynaklarını serbest bırakır. */
    public void shutdown() {
        voiceInputService.shutdown();
    }

    // ------------------------------------------------------------------
    // Üst çubuk: başlık + canlı saat
    // ------------------------------------------------------------------

    private Region buildTopBar() {
        // JavaFX CSS harf aralığını desteklemediği için başlıkta boşluklarla genişlik verilir.
        // Bu etiket EnergyOrb'dan tamamen bağımsız bir düğümdür: küre konuşurken titreşip
        // parlasa da başlık hiçbir zaman ondan etkilenmez, sabit kalır.
        Label title = new Label("T O M R I S");
        title.getStyleClass().add("title-label");

        Label clock = new Label();
        clock.getStyleClass().add("clock-label");
        startClock(clock);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(title, spacer, clock);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(18, 28, 10, 28));
        return topBar;
    }

    private void startClock(Label clock) {
        clock.setText(LocalTime.now().format(CLOCK_FORMAT));
        Timeline ticker = new Timeline(new KeyFrame(Duration.seconds(1),
                event -> clock.setText(LocalTime.now().format(CLOCK_FORMAT))));
        ticker.setCycleCount(Animation.INDEFINITE);
        ticker.play();
    }

    // ------------------------------------------------------------------
    // Orta alan: gösterge paneli (sol/sağ kart sütunları + orb)
    // ------------------------------------------------------------------

    private Region buildCenterArea() {
        VBox leftColumn = new VBox(14, new SystemStatusCard(), new GreetingCard(),
                new MotivationCard(), new StickyNoteCard());
        leftColumn.setPrefWidth(310);
        leftColumn.setMinWidth(270);
        leftColumn.setMaxWidth(310);

        VBox rightColumn = new VBox(14, weatherCard,
                new TodayPlanCard(calendarPlanService), new CalendarGridCard());
        rightColumn.setPrefWidth(310);
        rightColumn.setMinWidth(270);
        rightColumn.setMaxWidth(310);

        StackPane orbHolder = new StackPane(orb);
        orbHolder.setAlignment(Pos.CENTER);
        orbHolder.setPadding(new Insets(6, 0, 6, 0));
        HBox.setHgrow(orbHolder, Priority.ALWAYS);

        HBox dashboardRow = new HBox(18, leftColumn, orbHolder, rightColumn);
        dashboardRow.setAlignment(Pos.TOP_CENTER);

        AppShortcutsBar appShortcutsBar = new AppShortcutsBar(this::showTransientStatus);
        HBox.setHgrow(appShortcutsBar, Priority.ALWAYS);

        HBox utilityRow = new HBox(16, new MusicPlayerCard(spotifyNowPlayingService), appShortcutsBar);
        utilityRow.setAlignment(Pos.CENTER_LEFT);

        VBox center = new VBox(14, dashboardRow, utilityRow);
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(0, 24, 24, 24));
        return center;
    }

    // ------------------------------------------------------------------
    // Alt çubuk: sadece mikrofon butonu + durum göstergesi
    // ------------------------------------------------------------------

    private Region buildInputBar() {
        micButton.getStyleClass().add("mic-button");
        micButton.setTooltip(new javafx.scene.control.Tooltip("Mikrofonu aç/kapat (sürekli dinleme)"));
        micButton.setOnAction(event -> handleMicToggle());

        statusLabel.getStyleClass().add("status-label");

        VBox micColumn = new VBox(8, micButton, statusLabel);
        micColumn.setAlignment(Pos.CENTER);

        VBox bottom = new VBox(10, micColumn, new TaglineBar());
        bottom.getStyleClass().add("bottom-bar");
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(10, 24, 16, 24));
        return bottom;
    }

    // ------------------------------------------------------------------
    // Sesli giriş: Vosk ile mikrofonu SÜREKLİ dinle -> "Tomris" uyandırma kelimesi
    // içeren cümleleri süz -> otomatik gönder
    // ------------------------------------------------------------------

    /** Mikrofon butonu artık başlat/durdur değil, sürekli dinlemeyi açıp kapatan bir anahtardır. */
    private void handleMicToggle() {
        System.out.println("[MainView] Mikrofon butonuna tiklandi. processing=" + processing
                + " | su an dinliyor mu=" + voiceInputService.isListening());
        if (processing) {
            System.out.println("[MainView] Bir islem zaten surdugu icin mikrofon tiklamasi yok sayildi.");
            return;
        }
        if (voiceInputService.isListening()) {
            System.out.println("[MainView] Kullanici mikrofonu manuel olarak sessize aldi.");
            voiceInputService.stopContinuousListening();
            micButton.getStyleClass().remove("mic-active");
            orb.setEnergyLevel(EnergyOrb.EnergyLevel.IDLE);
            setStatus("Mikrofon kapalı.");
        } else {
            System.out.println("[MainView] Kullanici mikrofonu manuel olarak tekrar actı.");
            startContinuousVoiceMode();
        }
    }

    /**
     * Sürekli dinlemeyi (yeniden) başlatır: mikrofon açık kalır, gelen her cümle
     * {@link #handleUtterance(java.util.List)} üzerinden uyandırma kelimesi filtresine girer.
     * Zaten dinleniyorsa hiçbir şey yapmaz (idempotent) - bu sayede hem uygulama açılışında
     * hem de her komut işlendikten sonra güvenle çağrılabilir.
     */
    private void startContinuousVoiceMode() {
        if (voiceInputService.isListening()) {
            return;
        }
        if (!voiceInputService.isModelAvailable()) {
            System.out.println("[MainView] Vosk modeli bulunamadigi icin surekli dinleme baslatilamadi.");
            showTransientStatus("Konuşma tanıma modeli bulunamadı.");
            return;
        }
        System.out.println("[MainView] Surekli dinleme baslatiliyor, 'Tomris' cagrisi bekleniyor...");
        micButton.getStyleClass().add("mic-active");
        orb.setEnergyLevel(EnergyOrb.EnergyLevel.IDLE);
        setStatus("Dinliyorum... (\"Tomris\" diyerek çağır)");

        voiceInputService.startContinuousListening(
                candidates -> Platform.runLater(() -> handleUtterance(candidates)),
                error -> Platform.runLater(() -> onContinuousListeningError(error)));
    }

    /**
     * Vosk'un tamamlanmış olarak işaretlediği her cümle burada değerlendirilir. Düşük doğruluklu
     * küçük Türkçe model yüzünden tek bir tahmine güvenmek yerine {@code candidates} listesindeki
     * (n-best) tahminlerin HERHANGİ birinde "Tomris" aranır. Hiçbirinde yoksa (ör. arka plan
     * gürültüsü, TV sesi, alakasız konuşma) cümle tamamen yok sayılır - Claude'a gönderilmez.
     */
    private void handleUtterance(java.util.List<String> candidates) {
        if (processing) {
            // Zaten bir komut islenirken mikrofon durdurulmus olmali; bu yalnizca bir guvenlik agi.
            return;
        }
        Optional<String> match = WakeWordDetector.findMatch(candidates);
        if (match.isEmpty()) {
            System.out.println("[MainView] Uyandirma kelimesi hicbir adayda yok, yoksayildi: " + candidates);
            return;
        }
        String text = match.get();
        System.out.println("[MainView] Uyandirma kelimesi algilandi (adaylar: " + candidates
                + "), kullanilan: \"" + text + "\"");

        // Komut islenip Tomris konusurken mikrofonu kapatiyoruz: hem kendi sesini tekrar
        // duyup yanlislikla yeni bir "Tomris" cagrisi algilamasin diye, hem de ust uste binen
        // konusmalari onlemek icin. endProcessing() isi bitince otomatik olarak yeniden acar.
        voiceInputService.stopContinuousListening();
        micButton.getStyleClass().remove("mic-active");

        submitMessage(text);
    }

    private void onContinuousListeningError(Throwable error) {
        String message = rootCause(error).getMessage();
        System.out.println("[MainView] Surekli dinleme hatayla durdu: " + message);
        micButton.getStyleClass().remove("mic-active");
        orb.setEnergyLevel(EnergyOrb.EnergyLevel.IDLE);
        showTransientStatus("Sesli giriş hatası: " + message);
    }

    // ------------------------------------------------------------------
    // Gönderme akışı: HER ŞEY doğrudan Claude Code'a -> sesli oku
    // (yerel kalıp eşleştirme/regex router bilerek yok - bkz. sınıf üstü Javadoc)
    // ------------------------------------------------------------------

    private void submitMessage(String text) {
        if (processing) {
            return;
        }

        System.out.println("[MainView] Kullanici soyledi: \"" + text + "\"");
        beginProcessing("Tomris düşünüyor...");
        orb.setEnergyLevel(EnergyOrb.EnergyLevel.THINKING);

        claudeService.sendMessageAsync(text)
                .thenAccept(response -> Platform.runLater(() -> onAssistantResponded(response)))
                .exceptionally(error -> {
                    Platform.runLater(() -> onFailure(error));
                    return null;
                });
    }

    private void onAssistantResponded(String response) {
        System.out.println("[MainView] Tomris yaniti: " + response);
        setStatus("Tomris konuşuyor...");
        orb.setEnergyLevel(EnergyOrb.EnergyLevel.SPEAKING);

        speechService.speakAsync(response)
                .whenComplete((ignored, error) -> Platform.runLater(() -> {
                    endProcessing();
                    if (error != null) {
                        String message = rootCause(error).getMessage();
                        System.out.println("[MainView] Seslendirme hatasi: " + message);
                        showTransientStatus("Seslendirme sırasında bir sorun oluştu.");
                    }
                }));
    }

    private void onFailure(Throwable error) {
        String message = rootCause(error).getMessage();
        System.out.println("[MainView] Hata: " + message);
        endProcessing();
        showTransientStatus("Hata: " + message);
    }

    private void beginProcessing(String status) {
        processing = true;
        micButton.setDisable(true);
        setStatus(status);
    }

    private void endProcessing() {
        processing = false;
        orb.setEnergyLevel(EnergyOrb.EnergyLevel.IDLE);
        micButton.setDisable(false);
        setStatus("");
        // Komut bitti (basarili ya da basarisiz): "Tomris" cagrisini yakalayabilmek icin
        // surekli dinlemeyi otomatik olarak yeniden baslat. Kullanici mikrofonu elle kapatmis
        // olsaydı zaten buraya hic gelinmezdi (handleUtterance yalnizca dinleniyorken tetiklenir).
        startContinuousVoiceMode();
    }

    private void setStatus(String status) {
        statusLabel.setText(status);
    }

    /** Durum etiketinde bir mesajı kısa süreliğine gösterip ardından ortam durumuna geri döner. */
    private void showTransientStatus(String message) {
        setStatus(message);
        PauseTransition pause = new PauseTransition(TRANSIENT_STATUS_DURATION);
        pause.setOnFinished(event -> setStatus(voiceInputService.isListening()
                ? "Dinliyorum... (\"Tomris\" diyerek çağır)"
                : ""));
        pause.play();
    }

    private static Throwable rootCause(Throwable error) {
        Throwable cause = error;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
