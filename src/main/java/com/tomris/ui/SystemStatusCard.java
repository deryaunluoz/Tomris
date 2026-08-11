package com.tomris.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

/**
 * Dekoratif "sistem durumu" kartı. Arkasında gerçek bir metrik kaynağı olmadığından
 * değerler güne göre deterministik olarak üretilir: gün içinde sabit kalır, ertesi gün değişir.
 */
public class SystemStatusCard extends VBox {

    public SystemStatusCard() {
        getStyleClass().add("status-card");
        setSpacing(10);
        setPadding(new Insets(12, 16, 12, 16));

        Label heading = new Label("SİSTEM DURUMU");
        heading.getStyleClass().add("card-heading");

        int seed = (int) (LocalDate.now().toEpochDay() % 97);

        HBox metricsRow = new HBox(14,
                buildMetric("ENERJİ", "⚡", 70 + (seed * 3) % 26),
                buildMetric("ODAK", "◎", 65 + (seed * 7) % 30),
                buildMetric("MOTİVASYON", "📈", 60 + (seed * 11) % 35),
                buildMetric("VERİMLİLİK", "⏱", 68 + (seed * 5) % 28));

        getChildren().addAll(heading, metricsRow);
    }

    private VBox buildMetric(String label, String glyph, int value) {
        Label glyphLabel = new Label(glyph);
        glyphLabel.getStyleClass().add("status-glyph");

        Label valueLabel = new Label(Math.min(value, 99) + "%");
        valueLabel.getStyleClass().add("status-value");

        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("status-name");

        VBox tile = new VBox(2, glyphLabel, valueLabel, nameLabel);
        tile.getStyleClass().add("status-tile");
        tile.setAlignment(Pos.CENTER);
        return tile;
    }
}
