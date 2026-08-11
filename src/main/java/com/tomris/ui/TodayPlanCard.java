package com.tomris.ui;

import com.tomris.core.CalendarPlanService;
import com.tomris.core.CalendarPlanService.PlanItem;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/** "Bugünkü Planın" kartı: günün gerçek Google Takvim programını (Claude Code üzerinden) gösterir. */
public class TodayPlanCard extends VBox {

    private final CalendarPlanService calendarPlanService;
    private final VBox itemsBox = new VBox(6);

    public TodayPlanCard(CalendarPlanService calendarPlanService) {
        this.calendarPlanService = calendarPlanService;
        getStyleClass().add("plan-card");
        setSpacing(8);
        setPadding(new Insets(14, 16, 14, 16));

        Label heading = new Label("BUGÜNKÜ PLANIN");
        heading.getStyleClass().add("card-heading");

        Label loading = new Label("Takvim kontrol ediliyor...");
        loading.getStyleClass().add("plan-loading");
        itemsBox.getChildren().add(loading);

        getChildren().addAll(heading, itemsBox);
        refresh();
    }

    /** Takvimi arka planda yeniden sorgular ve listeyi günceller. */
    public void refresh() {
        calendarPlanService.fetchTodayPlanAsync()
                .thenAccept(items -> Platform.runLater(() -> render(items)));
    }

    private void render(List<PlanItem> items) {
        itemsBox.getChildren().clear();
        if (items.isEmpty()) {
            Label empty = new Label("Bugün için planlanmış bir etkinlik yok.");
            empty.getStyleClass().add("plan-empty");
            itemsBox.getChildren().add(empty);
            return;
        }
        for (PlanItem item : items) {
            CheckBox checkBox = new CheckBox();
            checkBox.getStyleClass().add("plan-checkbox");

            Label time = new Label(item.time());
            time.getStyleClass().add("plan-time");

            Label title = new Label(item.title());
            title.getStyleClass().add("plan-title");
            title.setWrapText(true);

            HBox row = new HBox(8, checkBox, time, title);
            row.setAlignment(Pos.CENTER_LEFT);
            itemsBox.getChildren().add(row);
        }
    }
}
