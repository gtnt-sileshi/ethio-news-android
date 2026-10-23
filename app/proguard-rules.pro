# ============================================================
#  Ethio News — R8 / ProGuard rules
#  Goal: maximise obfuscation score on Google Play Console
# ============================================================

# ── Debugging: keep line numbers in crash traces ─────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Preserve annotations needed at runtime ──────────────────
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ── App entry points — Activities / Services / Receivers ─────
-keep class com.ethionews.app.activity.MyApplication { *; }
-keep class com.ethionews.app.service.MediaPlaybackService { *; }
-keep class com.ethionews.app.service.EthioTvMessagingService { *; }
-keep class com.ethionews.app.cast.CastOptionsProvider { *; }

# ── Data / API models — Gson deserialises these by field name ─
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ── EventBus ─────────────────────────────────────────────────
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# ── Parcelable ───────────────────────────────────────────────
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ── Media3 / MediaSession ────────────────────────────────────
-keep class androidx.media3.common.MediaItem { *; }
-keep class androidx.media3.common.Player { *; }
-keep class androidx.media3.session.** { *; }
-dontwarn androidx.media3.**

# ── Google Mobile Ads (AdMob) ────────────────────────────────
-keep class com.google.android.gms.ads.** { public *; }
-keep class com.google.android.gms.appset.** { public *; }
-keep class com.google.android.gms.tasks.** { public *; }
-dontwarn com.google.android.gms.**

# ── IronSource ───────────────────────────────────────────────
-keep class com.ironsource.adapters.** { public protected *; }
-keep public class com.ironsource.mediationsdk.IronSource { public *; }
-keep public class com.ironsource.mediationsdk.** { public protected *; }
-keepclassmembers class com.ironsource.** { public *; }
-dontwarn com.ironsource.**
-dontwarn com.ironsource.adapters.**

# ── Facebook Audience Network ────────────────────────────────
-keep public class com.facebook.ads.AudienceNetworkAds { *; }
-keep public class com.facebook.ads.AdSettings { *; }
-keepclassmembers class com.facebook.ads.** { public protected *; }
-dontwarn com.facebook.ads.**
-dontwarn com.facebook.ads.internal.**

# ── StartApp ─────────────────────────────────────────────────
-keep public class com.startapp.sdk.adsbase.StartAppAd { public *; }
-keep public class com.startapp.sdk.adsbase.StartAppSDK { public *; }
-keepclassmembers class com.startapp.** { public protected *; }
-keep class com.truenet.** { public *; }
-dontwarn com.startapp.**
-dontwarn android.webkit.JavascriptInterface

# ── Firebase ─────────────────────────────────────────────────
-dontwarn com.google.firebase.**

# ── Misc ─────────────────────────────────────────────────────
-dontwarn com.squareup.okhttp.**
-dontwarn org.apache.http.**
-dontwarn sun.misc.**
-dontwarn org.jetbrains.annotations.**
-dontwarn com.bytedance.**
-dontwarn com.yandex.metrica.**
