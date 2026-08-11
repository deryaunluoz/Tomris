package com.tomris.ui;

import com.tomris.core.AppLauncher;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.simpleicons.SimpleIcons;

import java.util.function.Consumer;

/**
 * "UYGULAMALARIM" satırı: sık kullanılan uygulamalara ve web servislerine tek tıkla erişim.
 * Her kısayol, gerçek marka logosu (Simple Icons) üstte / isim altta olacak şekilde eşit
 * boyutlu bir kutucuktur; kutucuklar aralarındaki esnek boşluklarla satırın tüm genişliğine
 * eşit aralıklı olarak yayılır. Bir kısayol başarısız olursa (ör. uygulama kurulu değilse)
 * hata, durum göstergesinde kısa süreliğine gösterilsin diye {@code onError} geri
 * çağrısına iletilir.
 */
public class AppShortcutsBar extends VBox {

    public AppShortcutsBar(Consumer<String> onError) {
        getStyleClass().add("app-shortcuts");
        setSpacing(10);

        Label heading = new Label("UYGULAMALARIM");
        heading.getStyleClass().add("card-heading");

        HBox row = new HBox();
        row.setAlignment(Pos.CENTER);
        addTileRow(row, onError);

        getChildren().addAll(heading, row);
    }

    private void addTileRow(HBox row, Consumer<String> onError) {
        Button[] tiles = {
                buildTile("VS Code", SimpleIcons.VISUALSTUDIOCODE, "#007ACC", AppLauncher::openVsCode, onError),
                buildTile("IntelliJ", SimpleIcons.INTELLIJIDEA, "#FC801D", AppLauncher::openIntelliJ, onError),
                buildTile("GitHub", SimpleIcons.GITHUB, "#F0F6FC", AppLauncher::openGitHub, onError),
                buildTile("Gmail", SimpleIcons.GMAIL, "#EA4335", AppLauncher::openGmail, onError),
                buildTile("iCloud", SimpleIcons.ICLOUD, "#3693F3", AppLauncher::openICloud, onError),
                buildTile("Spotify", SimpleIcons.SPOTIFY, "#1DB954", AppLauncher::openSpotifyWeb, onError),
                buildTile("YouTube", SimpleIcons.YOUTUBE, "#FF0000", AppLauncher::openYouTube, onError),
        };

        for (int i = 0; i < tiles.length; i++) {
            row.getChildren().add(tiles[i]);
            if (i < tiles.length - 1) {
                row.getChildren().add(growSpacer());
            }
        }
    }

    private Region growSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private Button buildTile(String name, Ikon icon, String colorHex, Runnable action, Consumer<String> onError) {
        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(22);
        fontIcon.setIconColor(Color.web(colorHex));

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("app-tile-name");

        VBox content = new VBox(6, fontIcon, nameLabel);
        content.setAlignment(Pos.CENTER);

        Button tile = new Button();
        tile.setGraphic(content);
        tile.getStyleClass().add("app-tile");
        tile.setPadding(new Insets(10, 12, 10, 12));
        tile.setTooltip(new Tooltip(name + " aç"));
        tile.setOnAction(event -> {
            try {
                action.run();
            } catch (Exception e) {
                onError.accept(name + " açılamadı: " + e.getMessage());
            }
        });
        return tile;
    }
}
