# ============================================================
#  Ethio News — R8 / ProGuard rules
#  Goal: maximise obfuscation score on Google Play Console
#
#  Key principle: prefer -keep,allowobfuscation or
#  -keepclassmembers,allowobfuscation over plain -keep so that
#  R8 can still rename identifiers (raises obfuscation %).
#  Only use plain -keep for true entry points the Android
#  runtime or third-party SDK loads by class/method name via
#  reflection / JNI.
# ============================================================

# ── Debugging: keep line numbers in crash traces ─────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Preserve annotations needed at runtime ──────────────────
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ── App entry points — loaded by name from AndroidManifest ───
# R8 keeps Activities / Services / Receivers from the manifest
# automatically; we only protect classes loaded by reflection.
-keep class com.ethionews.app.activity.MyApplication { <init>(); }
-keep class com.ethionews.app.service.MediaPlaybackService { <init>(); }
-keep class com.ethionews.app.service.EthioTvMessagingService { <init>(); }
-keep class com.ethionews.app.cast.CastOptionsProvider { <init>(); *; }

# ── Data / API models — Gson deserialises by field name ──────
# allowobfuscation lets R8 rename the class itself; the
# @SerializedName annotation preserves the serialised key.
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation class * implements com.google.gson.TypeAdapterFactory
-keep,allowobfuscation class * implements com.google.gson.JsonSerializer
-keep,allowobfuscation class * implements com.google.gson.JsonDeserializer

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
# Keep the public API surface that ExoPlayer loads reflectively;
# allow obfuscation of implementation classes.
-keep,allowobfuscation class androidx.media3.common.MediaItem { *; }
-keep,allowobfuscation class androidx.media3.common.Player { *; }
-keepclassmembers,allowobfuscation class androidx.media3.session.** { *; }
-dontwarn androidx.media3.**

# ── Google Mobile Ads (AdMob) ────────────────────────────────
# The GMS ads SDK ships its own embedded consumer rules; these
# lines only guard the handful of classes accessed by JNI/name.
-keepclassmembers,allowobfuscation class com.google.android.gms.ads.** { public *; }
-keepclassmembers,allowobfuscation class com.google.android.gms.appset.** { public *; }
-keepclassmembers,allowobfuscation class com.google.android.gms.tasks.** { public *; }
-dontwarn com.google.android.gms.**

# ── IronSource ───────────────────────────────────────────────
# IronSource SDK keeps its own consumer rules inside the AAR.
# We only need to protect the public surface used by the app;
# allowobfuscation lets R8 rename internal implementation.
-keep,allowobfuscation public class com.ironsource.mediationsdk.IronSource { public *; }
-keepclassmembers,allowobfuscation class com.ironsource.mediationsdk.** { public protected *; }
-keepclassmembers,allowobfuscation class com.ironsource.adapters.** { public protected *; }
-keepclassmembers,allowobfuscation class com.ironsource.** { public *; }
-dontwarn com.ironsource.**
-dontwarn com.ironsource.adapters.**

# ── Facebook Audience Network ────────────────────────────────
-keep,allowobfuscation public class com.facebook.ads.AudienceNetworkAds { *; }
-keep,allowobfuscation public class com.facebook.ads.AdSettings { *; }
-keepclassmembers,allowobfuscation class com.facebook.ads.** { public protected *; }
-dontwarn com.facebook.ads.**
-dontwarn com.facebook.ads.internal.**

# ── StartApp ─────────────────────────────────────────────────
-keep,allowobfuscation public class com.startapp.sdk.adsbase.StartAppAd { public *; }
-keep,allowobfuscation public class com.startapp.sdk.adsbase.StartAppSDK { public *; }
-keepclassmembers,allowobfuscation class com.startapp.** { public protected *; }
-keep,allowobfuscation class com.truenet.** { public *; }
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
