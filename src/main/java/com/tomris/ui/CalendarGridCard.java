package com.tomris.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/** Ay görünümünde takvim kartı: bugünü vurgular, önceki/sonraki ay arasında gezinilebilir. */
public class CalendarGridCard extends VBox {

    private static final String[] DAY_HEADERS = {"PZT", "SAL", "ÇAR", "PER", "CUM", "CTS", "PAZ"};
    private static final Locale TURKISH = Locale.forLanguageTag("tr");

    private final Label monthLabel = new Label();
    private final GridPane grid = new GridPane();
    private YearMonth shownMonth = YearMonth.now();

    public CalendarGridCard() {
        getStyleClass().add("calendar-card");
        setSpacing(8);
        setPadding(new Insets(14, 16, 14, 16));

        Button prev = new Button("‹");
        prev.getStyleClass().add("calendar-nav-button");
        prev.setOnAction(event -> {
            shownMonth = shownMonth.minusMonths(1);
            render();
        });

        Button next = new Button("›");
        next.getStyleClass().add("calendar-nav-button");
        next.setOnAction(event -> {
            shownMonth = shownMonth.plusMonths(1);
            render();
        });

        monthLabel.getStyleClass().add("calendar-month-label");

        HBox header = new HBox(10, prev, monthLabel, next);
        header.setAlignment(Pos.CENTER);

        grid.setHgap(6);
        grid.setVgap(6);
        grid.setAlignment(Pos.CENTER);

        getChildren().addAll(header, grid);
        render();
    }

    private void render() {
        String monthName = shownMonth.getMonth().getDisplayName(TextStyle.FULL, TURKISH);
        monthLabel.setText(capitalize(monthName) + " " + shownMonth.getYear());

        grid.getChildren().clear();
        for (int i = 0; i < DAY_HEADERS.length; i++) {
            Label dayHeader = new Label(DAY_HEADERS[i]);
            dayHeader.getStyleClass().add("calendar-day-header");
            grid.add(dayHeader, i, 0);
        }

        LocalDate firstOfMonth = shownMonth.atDay(1);
        int offset = firstOfMonth.getDayOfWeek().getValue() - 1; // Pazartesi = 0
        LocalDate today = LocalDate.now();

        int row = 1;
        int col = offset;
        for (int day = 1; day <= shownMonth.lengthOfMonth(); day++) {
            Label dayLabel = new Label(String.valueOf(day));
            dayLabel.getStyleClass().add("calendar-day-cell");
            if (shownMonth.atDay(day).equals(today)) {
                dayLabel.getStyleClass().add("calendar-day-today");
            }
            grid.add(dayLabel, col, row);

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }
    }

    private String capitalize(String text) {
        if (text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
