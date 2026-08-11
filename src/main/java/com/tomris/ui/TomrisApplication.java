package com.tomris.ui;

import com.tomris.core.ClaudeService;
import com.tomris.core.SpeechService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * Tomris masaüstü asistanının JavaFX giriş noktası.
 * <p>
 * Bu sınıf sadece bir kabuktur: gerçek zeka ve entegrasyonlar (Gmail, iCloud, Google Takvim,
 * Spotify) {@link ClaudeService} aracılığıyla çağrılan Claude Code CLI üzerinde çalışır.
 */
public class TomrisApplication extends Application {

    private final ClaudeService claudeService = new ClaudeService();
    private final SpeechService speechService = new SpeechService();
    private MainView mainView;

    @Override
    public void start(Stage primaryStage) {
        mainView = new MainView(claudeService, speechService);
        Region root = mainView.build();

        Scene scene = new Scene(root, 1300, 1000);
        scene.getStylesheets().add(getClass().getResource("/tomris.css").toExternalForm());

        primaryStage.setTitle("Tomris");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(820);
        primaryStage.show();

        // Uygulama açılır açılmaz mikrofon sessizce dinlemeye başlar - Tomris HİÇBİR ŞEY
        // söylemez/seslendirmez, sadece "Tomris" cağrısını beklemeye başlar. Otomatik/kendiliğinden
        // konuşma kasıtlı olarak yok: tek konuşma tetikleyicisi gerçek bir sesli komuttur.
        mainView.startVoiceMode();
    }

    @Override
    public void stop() {
        // Arka planda çalışan claude/speak.ps1/vosk kaynaklarını temiz şekilde kapat.
        claudeService.shutdown();
        speechService.shutdown();
        mainView.shutdown();
    }
}
