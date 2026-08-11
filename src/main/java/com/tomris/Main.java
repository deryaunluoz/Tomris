package com.tomris;

import com.tomris.ui.TomrisApplication;
import javafx.application.Application;

/**
 * Uygulamanın gerçek giriş noktası.
 * <p>
 * JavaFX {@code Application} sınıfını doğrudan çalıştırmak yerine ayrı bir {@code main}
 * sınıfı kullanmak, uygulama modül yolu olmadan (ör. düz bir fat-jar ile) çalıştırıldığında
 * "JavaFX runtime components are missing" hatasının önüne geçer.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Application.launch(TomrisApplication.class, args);
    }
}
