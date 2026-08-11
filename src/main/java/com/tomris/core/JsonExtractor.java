package com.tomris.core;

/**
 * Claude Code çoğu zaman istenen JSON'un etrafına küçük bir açıklama ekleyebildiği için,
 * yanıt metninin içinden ilk geçerli JSON dizisini/nesnesini ayıklayan küçük bir yardımcı.
 */
final class JsonExtractor {

    private JsonExtractor() {
    }

    static String extractArray(String text) {
        return extract(text, '[', ']');
    }

    static String extractObject(String text) {
        return extract(text, '{', '}');
    }

    private static String extract(String text, char open, char close) {
        int start = text.indexOf(open);
        int end = text.lastIndexOf(close);
        if (start < 0 || end < 0 || end < start) {
            throw new IllegalArgumentException("Yanıt içinde JSON bulunamadı.");
        }
        return text.substring(start, end + 1);
    }
}
