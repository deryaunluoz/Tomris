package com.tomris.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/** "Günün motivasyonu" kartı: küratörlü Türkçe sözlerden güne göre biri seçilir. */
public class MotivationCard extends VBox {

    private static final List<String> QUOTES = List.of(
            "Disiplin, hayallerin ile başarıların arasındaki köprüdür.",
            "Küçük adımlar, büyük yolculukların başlangıcıdır.",
            "Bugün yapabileceğini yarına bırakma.",
            "En karanlık an, şafaktan hemen öncesidir.",
            "Başarı, her gün tekrarlanan küçük çabaların toplamıdır.",
            "Zorluklar, seni büyütmek için oradadır.",
            "Odaklan, gerisi kendiliğinden gelir."
    );

    public MotivationCard() {
        getStyleClass().add("motivation-card");
        setSpacing(8);
        setPadding(new Insets(14, 16, 14, 16));

        Label heading = new Label("GÜNÜN MOTİVASYONU");
        heading.getStyleClass().add("card-heading");

        String quote = QUOTES.get((int) (LocalDate.now().toEpochDay() % QUOTES.size()));
        Label quoteLabel = new Label("“" + quote + "”");
        quoteLabel.getStyleClass().add("motivation-quote");
        quoteLabel.setWrapText(true);

        getChildren().addAll(heading, quoteLabel);
    }
}
