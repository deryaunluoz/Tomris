package com.tomris.ui;

import com.tomris.core.WeatherService;
import com.tomris.core.WeatherService.DayForecast;
import com.tomris.core.WeatherService.WeatherSummary;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Basit hava durumu kartı: üstte saat/tarih, altında Dubai'nin anlık sıcaklığı/durumu,
 * en altta yatay 5 günlük kısa tahmin. Veri {@link WeatherService} (Open-Meteo) üzerinden gelir.
 */
public class WeatherCard extends VBox {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale.forLanguageTag("tr"));

    private final WeatherService weatherService = new WeatherService();
    private final Label timeLabel = new Label();
    private final Label dateLabel = new Label();
    private final Label locationLabel = new Label();
    private final Label currentLabel = new Label("Hava durumu yükleniyor...");
    private final HBox forecastRow = new HBox(5);

    public WeatherCard() {
        getStyleClass().add("weather-card");
        setSpacing(6);
        setPadding(new Insets(14, 12, 14, 12));

        Label separator = new Label("·");
        separator.getStyleClass().add("weather-separator");

        HBox clockRow = new HBox(6, timeLabel, separator, dateLabel);
        clockRow.getStyleClass().add("weather-clock-row");
        clockRow.setAlignment(Pos.CENTER_LEFT);

        timeLabel.getStyleClass().add("weather-time");
        dateLabel.getStyleClass().add("weather-date");
        locationLabel.getStyleClass().add("weather-location");
        currentLabel.getStyleClass().add("weather-current");
        forecastRow.getStyleClass().add("weather-forecast-row");
        forecastRow.setAlignment(Pos.CENTER);

        getChildren().addAll(clockRow, locationLabel, currentLabel, forecastRow);

        startClock();
        refresh();
    }

    private void startClock() {
        updateClock();
        Timeline ticker = new Timeline(new KeyFrame(Duration.seconds(30), event -> updateClock()));
        ticker.setCycleCount(Animation.INDEFINITE);
        ticker.play();
    }

    private void updateClock() {
        LocalDateTime now = LocalDateTime.now();
        timeLabel.setText(now.format(TIME_FORMAT));
        dateLabel.setText(now.format(DATE_FORMAT));
    }

    /** Hava durumunu arka planda yeniden çeker ve kartı günceller. */
    public void refresh() {
        weatherService.fetchAsync()
                .thenAccept(summary -> Platform.runLater(() -> render(summary)))
                .exceptionally(error -> {
                    Platform.runLater(() -> currentLabel.setText("Hava durumu alınamadı"));
                    return null;
                });
    }

    private void render(WeatherSummary summary) {
        locationLabel.setText(summary.location());
        currentLabel.setText(summary.currentTempC() + "°C · " + summary.currentCondition());

        forecastRow.getChildren().clear();
        for (DayForecast day : summary.days()) {
            forecastRow.getChildren().add(buildDayTile(day));
        }
    }

    private VBox buildDayTile(DayForecast day) {
        Label dayLabel = new Label(day.dayName());
        dayLabel.getStyleClass().add("weather-day-name");

        Label conditionLabel = new Label(day.condition());
        conditionLabel.getStyleClass().add("weather-day-condition");

        Label tempLabel = new Label(day.highC() + "° / " + day.lowC() + "°");
        tempLabel.getStyleClass().add("weather-day-temp");

        VBox tile = new VBox(3, dayLabel, conditionLabel, tempLabel);
        tile.getStyleClass().add("weather-day-tile");
        tile.setAlignment(Pos.CENTER);
        return tile;
    }
}
