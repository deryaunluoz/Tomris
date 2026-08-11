package com.tomris.ui;

import com.tomris.core.SpotifyNowPlayingService;
import com.tomris.core.SpotifyNowPlayingService.NowPlaying;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Basit "şu an çalıyor" kartı: gerçek Spotify durumunu Claude Code üzerinden gösterir. */
public class MusicPlayerCard extends VBox {

    private final SpotifyNowPlayingService spotifyService;
    private final Label stateGlyph = new Label("♪");
    private final Label trackLabel = new Label("Yükleniyor...");
    private final Label artistLabel = new Label("");

    public MusicPlayerCard(SpotifyNowPlayingService spotifyService) {
        this.spotifyService = spotifyService;
        getStyleClass().add("music-card");
        setSpacing(6);
        setPadding(new Insets(14, 16, 14, 16));

        Label heading = new Label("SPOTIFY");
        heading.getStyleClass().add("card-heading");

        stateGlyph.getStyleClass().add("music-state-glyph");
        trackLabel.getStyleClass().add("music-track");
        trackLabel.setWrapText(true);
        artistLabel.getStyleClass().add("music-artist");

        Button refreshButton = new Button("⟳ Yenile");
        refreshButton.getStyleClass().add("music-refresh-button");
        refreshButton.setOnAction(event -> refresh());

        VBox textBox = new VBox(2, trackLabel, artistLabel);
        HBox row = new HBox(10, stateGlyph, textBox);
        row.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(heading, row, refreshButton);
        refresh();
    }

    /** Spotify durumunu arka planda yeniden sorgular ve kartı günceller. */
    public void refresh() {
        trackLabel.setText("Kontrol ediliyor...");
        artistLabel.setText("");
        spotifyService.fetchNowPlayingAsync()
                .thenAccept(nowPlaying -> Platform.runLater(() -> render(nowPlaying)));
    }

    private void render(NowPlaying nowPlaying) {
        if (!nowPlaying.playing() || nowPlaying.track().isBlank()) {
            stateGlyph.setText("♪");
            trackLabel.setText("Şu an bir şey çalmıyor");
            artistLabel.setText("");
            return;
        }
        stateGlyph.setText("▶");
        trackLabel.setText(nowPlaying.track());
        artistLabel.setText(nowPlaying.artist());
    }
}
