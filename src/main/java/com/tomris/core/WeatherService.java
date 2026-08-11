package com.tomris.core;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Dubai için güncel hava durumu ve 5 günlük tahmini Open-Meteo'nun ücretsiz, anahtar
 * gerektirmeyen API'sinden çeker. (wttr.in yalnızca 3 günlük tahmin sağladığı için tercih edildi.)
 */
public final class WeatherService {

    private static final double DUBAI_LATITUDE = 25.2048;
    private static final double DUBAI_LONGITUDE = 55.2708;
    private static final String LOCATION_LABEL = "Dubai, BAE";

    private static final String FORECAST_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=" + DUBAI_LATITUDE
                    + "&longitude=" + DUBAI_LONGITUDE
                    + "&current_weather=true&daily=weathercode,temperature_2m_max,temperature_2m_min"
                    + "&timezone=Asia%2FDubai&forecast_days=5";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Tek bir günün kısa tahmini: gün adı, yüksek/düşük sıcaklık ve kısa durum metni. */
    public record DayForecast(String dayName, int highC, int lowC, String condition) {
    }

    /** Anlık sıcaklık/durum ve önümüzdeki günlerin tahminini bir arada taşır. */
    public record WeatherSummary(String location, int currentTempC, String currentCondition,
                                  List<DayForecast> days) {
    }

    public CompletableFuture<WeatherSummary> fetchAsync() {
        return CompletableFuture.supplyAsync(this::fetch);
    }

    private WeatherSummary fetch() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(FORECAST_URL))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new WeatherException("Hava durumu servisi HTTP " + response.statusCode() + " döndürdü.");
            }

            JSONObject root = new JSONObject(response.body());

            JSONObject currentWeather = root.getJSONObject("current_weather");
            int currentTemp = (int) Math.round(currentWeather.getDouble("temperature"));
            String currentCondition = describeWeatherCode(currentWeather.getInt("weathercode"));

            JSONObject daily = root.getJSONObject("daily");
            JSONArray dates = daily.getJSONArray("time");
            JSONArray codes = daily.getJSONArray("weathercode");
            JSONArray highs = daily.getJSONArray("temperature_2m_max");
            JSONArray lows = daily.getJSONArray("temperature_2m_min");

            List<DayForecast> days = new ArrayList<>();
            for (int i = 0; i < dates.length(); i++) {
                LocalDate date = LocalDate.parse(dates.getString(i));
                String dayName = capitalize(date.getDayOfWeek()
                        .getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("tr")));
                days.add(new DayForecast(dayName,
                        (int) Math.round(highs.getDouble(i)),
                        (int) Math.round(lows.getDouble(i)),
                        describeWeatherCode(codes.getInt(i))));
            }

            return new WeatherSummary(LOCATION_LABEL, currentTemp, currentCondition, days);
        } catch (WeatherException e) {
            throw e;
        } catch (Exception e) {
            throw new WeatherException("Hava durumu alınamadı: " + e.getMessage(), e);
        }
    }

    private static String capitalize(String text) {
        if (text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** Open-Meteo'nun WMO hava kodunu kısa bir Türkçe ifadeye çevirir. */
    private static String describeWeatherCode(int code) {
        return switch (code) {
            case 0 -> "Açık";
            case 1 -> "Az bulutlu";
            case 2 -> "Parçalı bulutlu";
            case 3 -> "Bulutlu";
            case 45, 48 -> "Sisli";
            case 51, 53, 55, 56, 57 -> "Çiseli";
            case 61, 63, 65, 66, 67 -> "Yağmurlu";
            case 71, 73, 75, 77 -> "Karlı";
            case 80, 81, 82 -> "Sağanak";
            case 85, 86 -> "Kar sağanağı";
            case 95, 96, 99 -> "Fırtınalı";
            default -> "Bilinmiyor";
        };
    }

    /** Hava durumu alınırken oluşan hataları taşıyan işaretli (unchecked) istisna. */
    public static final class WeatherException extends RuntimeException {
        public WeatherException(String message) {
            super(message);
        }

        public WeatherException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
