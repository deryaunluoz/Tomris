package com.tomris.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Random;

/** Tüm genişliği kaplayan, ortalanmış italik bir söz + kalp ikonu gösteren ince alt çubuk. */
public class TaglineBar extends HBox {

    private static final List<String> TAGLINES = List.of(
            "Planların seni bugün nereye götürecek?",
            "Küçük adımlar, büyük hedeflere götürür.",
            "Bugün kendine bir söz ver.",
            "Hayallerin bugün bir adım daha yakın.",
            "Zaman, sahip olduğun en değerli yatırım.",
            "Bir fikir, doğru anda büyük bir şeye dönüşebilir."
    );

    public TaglineBar() {
        getStyleClass().add("tagline-bar");
        setAlignment(Pos.CENTER);

        String tagline = TAGLINES.get(new Random().nextInt(TAGLINES.size()));

        Label quote = new Label(tagline);
        quote.getStyleClass().add("tagline-text");

        Label heart = new Label("♥");
        heart.getStyleClass().add("tagline-heart");

        getChildren().addAll(quote, heart);
        setSpacing(8);
    }
}
