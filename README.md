# Kobaa TV — v2

تطبيق Android TV / موبايل لتشغيل قنوات IPTV عبر Xtream Codes API، مع اختيار جودة (مثل يوتيوب) ودعم اللغتين العربية والإنجليزية.

## الميزات الجديدة
- **Xtream Codes**: شاشة دخول (Host / Username / Password) تحقق عبر `player_api.php`.
- **قائمة القنوات** عبر `get_live_streams`.
- **مشغّل ExoPlayer (HLS)** يبث من `/live/USER/PASS/ID.m3u8`.
- **اختيار جودة ديناميكي** (Auto / 1080p / 720p / 480p…) من خلال `DefaultTrackSelector` على track groups من نوع الفيديو.
- **اللغتان العربية والإنجليزية** (`values/` + `values-ar/`) مع تبديل من شاشة الدخول أو من الإعدادات، ودعم RTL تلقائي.

## البناء
1. افتح المجلد في Android Studio (Hedgehog أو أحدث).
2. Sync Gradle (JDK 17).
3. Run على جهاز أو محاكي (Android 7.0+ / Android TV).

## ملاحظات
- إذا كان السيرفر HTTP فقط فقد فعّلنا `usesCleartextTraffic`.
- صيغة الرابط: `http://your.host:8080` بدون `/` في النهاية.
- لتشغيل صيغة TS بدل HLS غيّر `hlsUrl()` إلى `tsUrl()` في `ChannelsActivity`.
