package com.tomris.core;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Vosk'un yazıya döktüğü metinde "Tomris" uyandırma kelimesinin geçip geçmediğini denetler.
 * <p>
 * Sürekli dinleme modunda mikrofon her zaman açık olduğundan, arka plan gürültüsü, TV sesi
 * ya da alakasız konuşmalar da sürekli olarak metne çevrilir. Bu sınıf, yalnızca "Tomris"
 * çağrısı içeren cümlelerin gerçek bir komut olarak işlenmesi için bir filtre görevi görür;
 * geri kalan her şey sessizce yok sayılır.
 * <p>
 * Kontrol kasıtlı olarak ESNEK tutulmuştur - iki ayrı sebepten: (1) kullanıcının telaffuzunda
 * "r" harfi net çıkmayabilir ("Tomis", "Tomiş" gibi) ya da Vosk modeli sesi biraz farklı
 * çözebilir ("Tombis", "Tomğis", "Tomrış" gibi); (2) Türkçe için Vosk'un tek resmi modeli
 * "small" (küçük/düşük doğruluklu) olduğundan "Tomris" bazen TEK bir kelime olarak değil,
 * "tom" + başka bir kelime şeklinde İKİYE BÖLÜNMÜŞ olarak da tanınabilir (örn. "merhaba tom
 * umarız" - burada aslında "Tomris" denmiştir ama Vosk "umarız" diye devam etmiştir). Bu
 * yüzden iki ayrı örüntü kontrol edilir: "tom" ile başlayıp "i"/"ı" + "s"/"ş" ile biten TEK
 * bir kelime, YA DA bağımsız/tek başına duran "tom" kelimesi. Amaç, kullanıcının hiçbir
 * çağırma denemesinin kaçırılmamasıdır - bu yüzden yanlış pozitif ihtimaline (nadir bir
 * kelimenin yanlışlıkla eşleşmesi) karşı yanlış negatife (gerçek bir çağrının kaçırılması)
 * göre daha toleranslıdır.
 */
public final class WakeWordDetector {

    // \b...\b : "tom" bir kelimenin başında olmalı (örn. "otomatik" içindeki "tom" eşleşmez).
    // \w{0,4} : aradaki 0-4 harf (UNICODE_CHARACTER_CLASS sayesinde "ğ" gibi Türkçe harfleri de kapsar).
    // [iı][sş] : "i"/"ı" ile devam edip "s"/"ş" ile bitmeli.
    private static final Pattern WAKE_WORD_PATTERN = Pattern.compile(
            "\\btom\\w{0,4}[iı][sş]\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);

    // Vosk "Tomris"i "tom" + ayrı bir kelimeye ("umarız" vb.) bölebiliyor; bu durumda yukarıdaki
    // örüntü eşleşmez çünkü "tom"dan hemen sonra i/ı+s/ş gelmiyor. Bağımsız "tom" kelimesini de
    // (küçük bir yanlış pozitif riskini göze alarak) bir uyandırma sinyali sayıyoruz.
    private static final Pattern STANDALONE_TOM_PATTERN = Pattern.compile(
            "\\btom\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);

    private WakeWordDetector() {
    }

    /** Metinde "Tomris" uyandırma kelimesine (ya da yakın bir telaffuzuna/bölünmesine) rastlandı mı? */
    public static boolean containsWakeWord(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return WAKE_WORD_PATTERN.matcher(text).find() || STANDALONE_TOM_PATTERN.matcher(text).find();
    }

    /**
     * Aday transkript listesinde (Vosk'un n-best sonuçları, en olası en başta) uyandırma
     * kelimesini içeren İLK adayı döndürür. Bu, hem "en olası tahmin doğruysa onu kullan" hem
     * de "en olası tahmin yanlışsa ama düşük olasılıklı bir alternatif doğruysa onu yakala"
     * davranışını tek bir taramada sağlar.
     */
    public static Optional<String> findMatch(List<String> candidates) {
        if (candidates == null) {
            return Optional.empty();
        }
        return candidates.stream().filter(WakeWordDetector::containsWakeWord).findFirst();
    }
}
