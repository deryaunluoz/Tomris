package com.tomris.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/** Sarı/altın tonlu yapışkan not kartı. Şimdilik yalnızca serbest metin alanı içerir. */
public class StickyNoteCard extends VBox {

    public StickyNoteCard() {
        getStyleClass().add("sticky-note-card");
        setSpacing(8);
        setPadding(new Insets(14, 16, 14, 16));

        Label heading = new Label("NOT");
        heading.getStyleClass().add("sticky-note-heading");

        TextArea noteArea = new TextArea();
        noteArea.getStyleClass().add("sticky-note-area");
        noteArea.setPromptText("Fikirlerini buraya yaz...");
        noteArea.setWrapText(true);
        noteArea.setPrefRowCount(3);

        getChildren().addAll(heading, noteArea);
    }
}
