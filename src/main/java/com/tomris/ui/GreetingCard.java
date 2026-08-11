package com.tomris.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Kişisel selam kartı: "Merhaba Derya," + kısa bir motivasyon cümlesi ve etiketler. */
public class GreetingCard extends VBox {

    private static final String USER_NAME = "Derya";

    public GreetingCard() {
        getStyleClass().add("greeting-card");
        setSpacing(8);
        setPadding(new Insets(14, 16, 14, 16));

        Label greeting = new Label("Merhaba " + USER_NAME + ",");
        greeting.getStyleClass().add("greeting-title");

        Label subtitle = new Label("Bugün harika şeyler gerçekleştirebilirsin.");
        subtitle.getStyleClass().add("greeting-subtitle");
        subtitle.setWrapText(true);

        HBox tags = new HBox(6, tag("Odaklan"), tag("Planla"), tag("Gerçekleştir"));
        tags.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(greeting, subtitle, tags);
    }

    private Label tag(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("greeting-tag");
        return label;
    }
}
