package com.tomris.core;

import org.json.JSONObject;

import java.util.concurrent.CompletableFuture;

/**
 * "Müzik Çalar" kartı için, Claude Code üzerinden (tomris-jarvis'te bağlı Spotify
 * entegrasyonu ile) o an çalan şarkıyı yapılandırılmış (JSON) olarak sorar.
 */
public final class SpotifyNowPlayingService {

    private static final String PROMPT = """
            Spotify'da şu an çalan şarkıyı kontrol et. SADECE aşağıdaki JSON formatında \
            yanıt ver, başında/sonunda başka hiçbir açıklama veya metin ekleme:
            {"playing": true veya false, "track": "şarkı adı", "artist": "sanatçı adı"}
            Hiçbir şey çalmıyorsa playing:false ve track/artist için boş string kullan.""";

    private final ClaudeService claudeService;

    public SpotifyNowPlayingService(ClaudeService claudeService) {
        this.claudeService = claudeService;
    }

    /** O an çalan (ya da çalmayan) parçanın durumu. */
    public record NowPlaying(boolean playing, String track, String artist) {
        public static NowPlaying idle() {
            return new NowPlaying(false, "", "");
        }
    }

    public CompletableFuture<NowPlaying> fetchNowPlayingAsync() {
        return claudeService.sendMessageAsync(PROMPT)
                .thenApply(this::parse)
                .exceptionally(error -> NowPlaying.idle());
    }

    private NowPlaying parse(String response) {
        try {
            JSONObject json = new JSONObject(JsonExtractor.extractObject(response));
            return new NowPlaying(json.optBoolean("playing", false),
                    json.optString("track", ""), json.optString("artist", ""));
        } catch (Exception e) {
            return NowPlaying.idle();
        }
    }
}
