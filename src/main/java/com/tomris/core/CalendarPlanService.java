package com.tomris.core;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * "Bugünkü Planın" kartı için, Claude Code üzerinden (tomris-jarvis'te bağlı Google Takvim
 * entegrasyonu ile) günün gerçek programını yapılandırılmış (JSON) olarak sorar.
 */
public final class CalendarPlanService {

    private static final String PROMPT = """
            Bugünkü Google Takvim programımı kontrol et. SADECE aşağıdaki JSON dizisi \
            formatında yanıt ver, başında/sonunda başka hiçbir açıklama veya metin ekleme:
            [{"time":"HH:MM","title":"kısa başlık"}]
            Saatleri artan sırada ver. Bugün için hiç etkinlik yoksa sadece [] döndür.""";

    private final ClaudeService claudeService;

    public CalendarPlanService(ClaudeService claudeService) {
        this.claudeService = claudeService;
    }

    /** Tek bir takvim maddesi: saat ve kısa başlık. */
    public record PlanItem(String time, String title) {
    }

    public CompletableFuture<List<PlanItem>> fetchTodayPlanAsync() {
        return claudeService.sendMessageAsync(PROMPT)
                .thenApply(this::parse)
                .exceptionally(error -> List.of());
    }

    private List<PlanItem> parse(String response) {
        try {
            JSONArray array = new JSONArray(JsonExtractor.extractArray(response));
            List<PlanItem> items = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                items.add(new PlanItem(item.optString("time", "--:--"), item.optString("title", "")));
            }
            return items;
        } catch (Exception e) {
            return List.of();
        }
    }
}
