# Tomris — Kişisel Yapay Zeka Masaüstü Asistanı 🎙️

> **🚧 Aktif Geliştirme Aşamasında** — Bu proje şu anda üzerinde çalışmaya devam ettiğim, tamamlanmamış bir projedir (yaklaşık %70-75 tamamlanma durumunda). Sesli komutlarla çalışan, gerçek bir yapay zeka masaüstü asistanı geliştiriyorum.

## Proje Özeti

Tomris, Java ile geliştirilmekte olan, sesli komutlarla çalışan, gerçek yapay zeka destekli bir masaüstü kişisel asistan uygulamasıdır. E-posta, takvim, müzik ve genel bilgi/sohbet gibi günlük ihtiyaçları sesli komutlarla yönetebilen, konuşarak etkileşim kurulan bir sistem olarak tasarlandı.

## Kullanılan Teknolojiler

### Ana Dil ve Framework
- **Java 21** — uygulamanın çekirdek dili
- **JavaFX** — masaüstü arayüzü (UI/UX) geliştirme
- **Maven** — bağımlılık yönetimi ve proje derleme sistemi

### Yapay Zeka Entegrasyonu
- **Anthropic Claude API** (Claude Code CLI üzerinden) — doğal dil anlama, genel soru cevaplama, komut yorumlama. Önceden tanımlı kalıp eşleştirme (regex/keyword) yerine gerçek bir büyük dil modelinin doğal dil anlama kapasitesini kullanacak şekilde tasarlandı.
- **MCP (Model Context Protocol)** — Gmail, Google Calendar, Spotify ve özel bir IMAP (iCloud Mail) bağlayıcısı ile entegrasyon

### Ses İşleme
- **Vosk** (offline, açık kaynak konuşma tanıma motoru) — Türkçe sesli komutları yazıya çevirme, sürekli/arka plan dinleme modu
- **JNA (Java Native Access)** — Vosk'un native (C++) kütüphanesiyle Java arasında köprü kurma; Windows'ta Türkçe karakter (UTF-8/ANSI kodlama) uyumsuzluğu gibi düşük seviyeli native entegrasyon sorunlarının çözümü
- **edge-tts** (Python tabanlı, Microsoft Edge'in ücretsiz metin-okuma motoru) — doğal, akıcı Türkçe sesli yanıt üretimi
- **PowerShell betikleri** — Python/edge-tts süreçlerinin Java'dan tetiklenmesi ve ses dosyalarının oynatılması

### Sistem Entegrasyonu
- **ProcessBuilder (Java)** — harici süreçleri (Claude CLI, PowerShell betikleri) güvenli şekilde tetikleme ve çıktılarını yönetme
- **Windows API entegrasyonları** — uygulama başlatma (VS Code, IntelliJ, tarayıcı), pencere yönetimi

### Harici API'ler
- **Open-Meteo** — gerçek zamanlı hava durumu verisi (API anahtarı gerektirmeyen ücretsiz servis)
- **Spotify Web API** (MCP üzerinden) — müzik arama, çalma, mevcut çalan parça bilgisi

## Öne Çıkan Özellikler

- **Sesli uyandırma kelimesi tespiti** — Kullanıcı "Tomris" ismini söylediğinde (telaffuz farklılıklarına karşı esnek algılama ile) sistem aktif hale geliyor
- **Sürekli dinleme modu** — Kullanıcı bir butona basmadan, doğal konuşmayla komut verebiliyor
- **Gerçek zamanlı veri gösterimi** — Hava durumu, takvim etkinlikleri, sistem durumu gibi bilgiler canlı olarak arayüzde güncelleniyor
- **Animasyonlu, özgün arayüz tasarımı** — JavaFX Canvas ile elden çizilmiş, konuşma durumuna göre tepki veren (nefes alma, hızlanma, titreşim) merkezi bir görsel eleman (EnergyOrb)
- **Doğal dil ile görev yönetimi** — Takvime etkinlik ekleme, hatırlatma alma, mail özetleme gibi işlemler serbest metin/konuşma ile yapılabiliyor

## Karşılaşılan Teknik Zorluklar ve Çözümler

- **Native kütüphane / kodlama sorunu:** Proje yolunda Türkçe karakter (ör. "Masaüstü") bulunması, Vosk'un native tarafının dosya yolunu okuyamamasına sebep oldu. Kök neden analizi (Vosk'un C++ kaynak kodunu inceleyerek) yapılıp JNA kodlama ayarı ile çözüldü.
- **Asenkron süreç yönetimi:** Sesli yanıtların üst üste binmesini (kekeleme etkisi) önlemek için ses çağrılarının sıralı ve senkronize çalışması sağlandı.
- **Mimari refactoring:** İlk sürümde kalıp-eşleştirmeli (rule-based) bir komut yönlendirme sistemi kullanılırken, esneklik ve doğallık için tüm komut işleme gerçek bir LLM'e (Claude) devredildi.

## Proje Yapısı

```
Tomris/
├── src/main/java/com/tomris/
│   ├── core/                    # İş mantığı ve servisler
│   │   ├── ClaudeService.java       # Claude API entegrasyonu
│   │   ├── SpeechService.java       # Ses işleme
│   │   ├── VoiceInputService.java   # Vosk ile konuşma tanıma
│   │   ├── WakeWordDetector.java    # "Tomris" uyandırma kelimesi tespiti
│   │   ├── CalendarPlanService.java # Takvim entegrasyonu
│   │   ├── SpotifyNowPlayingService.java
│   │   └── WeatherService.java
│   ├── ui/                      # JavaFX arayüz bileşenleri
│   │   ├── EnergyOrb.java           # Animasyonlu merkezi görsel
│   │   ├── MainView.java
│   │   ├── MusicPlayerCard.java
│   │   ├── CalendarGridCard.java
│   │   └── ...
│   └── Main.java
├── src/main/resources/
│   └── tomris.css
├── models/                      # Vosk konuşma tanıma modeli (Türkçe)
└── pom.xml
```

## Durum

Bu proje, kişisel bir yapay zeka asistanı geliştirme sürecinde uçtan uca (ses girişi → doğal dil işleme → harici API entegrasyonları → sesli/gerçek zamanlı arayüz) bir sistem tasarlama ve uygulama deneyimini kapsamaktadır.

**Şu anda tamamlanmamıştır, aktif olarak geliştirilmeye devam edilmektedir.** Bilinen açık noktalar arasında ses tanımanın (Vosk) her zaman istenen komutu tam doğru algılayamaması ve buna bağlı iyileştirme çalışmaları yer almaktadır.
