package com.ethionews.app.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory;
import androidx.media3.extractor.ts.TsExtractor;

import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import com.ethionews.app.BuildConfig;
import com.ethionews.app.R;
import com.ethionews.app.utils.player.CustomPlayerView;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class ApplicationUtil {

    private ApplicationUtil() {
        throw new IllegalStateException("Utility class");
    }

    private static final Random RANDOM = new Random();

    /**
     * Singleton OkHttpClient — reuses the internal thread pool and connection pool
     * across all calls instead of creating a new instance per request.
     */
    private static final OkHttpClient HTTP_CLIENT;

    static {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        // Only log request/response bodies in debug builds to avoid leaking
        // credentials or tokens in production logs.
        logging.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);

        HTTP_CLIENT = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                // For non-file requests: force identity encoding so LiteSpeed returns
                // plain JSON (multipart-only requests triggered gzip the server couldn't
                // decompress on the OkHttp side).
                // For file-upload requests (multipart with binary parts): let the server
                // decide — we decompress gzip manually in responsePost if needed.
                .addInterceptor(chain -> {
                    Request req = chain.request();
                    RequestBody body = req.body();
                    boolean isFileUpload = body instanceof okhttp3.MultipartBody &&
                            ((okhttp3.MultipartBody) body).parts().stream()
                                    .anyMatch(p -> p.headers() != null &&
                                            p.headers().toString().contains("filename="));
                    if (isFileUpload) {
                        return chain.proceed(req);
                    }
                    return chain.proceed(req.newBuilder()
                            .header("Accept-Encoding", "identity")
                            .build());
                })
                .addInterceptor(logging)
                .build();
    }

    public static int getRandomValue(int bound) {
        return RANDOM.nextInt(bound);
    }

    @NonNull
    public static String responsePost(String url, RequestBody requestBody) {
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        try {
            Response response = HTTP_CLIENT.newCall(request).execute();
            if (response.body() == null) return "";
            // Decompress gzip manually if the server returned a compressed body
            // (happens on file-upload responses where we didn't send Accept-Encoding: identity)
            String encoding = response.header("Content-Encoding", "");
            if ("gzip".equalsIgnoreCase(encoding)) {
                try (java.util.zip.GZIPInputStream gis =
                             new java.util.zip.GZIPInputStream(response.body().byteStream())) {
                    return new String(gis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                }
            }
            return response.body().string();
        } catch (Exception e) {
            Log.e("ApplicationUtil", "responsePost failed", e);
            return "";
        }
    }

    public static String toBase64(String input) {
        byte[] encodeValue = Base64.encode(input.getBytes(), Base64.DEFAULT);
        return new String(encodeValue);
    }

    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            InputStream input;
            if (src.startsWith("https://")) {
                // Do NOT override the default hostname verifier — the system's
                // default verifier correctly validates the server certificate.
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                connection.connect();
                input = connection.getInputStream();
            } else {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                input = connection.getInputStream();
            }
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            Log.e("ApplicationUtil", "getBitmapFromURL failed for: " + src, e);
            return null;
        }
    }

    public static GradientDrawable getGradientDrawable(int first, int second) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(15);
        gd.setColors(new int[]{first, second});
        gd.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        gd.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP);
        gd.mutate();
        return gd;
    }

    public static String format(Number number) {
        if (number != null){
            char[] suffix = {' ', 'k', 'M', 'B', 'T', 'P', 'E'};
            long numValue = number.longValue();
            int value = (int) Math.floor(Math.log10(numValue));
            int base = value / 3;
            if (value >= 3 && base < suffix.length) {
                return new DecimalFormat("#0.0").format(numValue / Math.pow(10, (double) base * 3)) + suffix[base];
            } else {
                return new DecimalFormat("#,##0").format(numValue);
            }
        } else {
            return String.valueOf(0);
        }
    }

    public static String readableFileSize(long size) {
        if (size <= 0) return "0 Bytes";
        final String[] units = new String[]{"Bytes", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static int getColumnWidth(int column, int gridPadding, @NonNull Context ctx) {
        Resources r = ctx.getResources();
        float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, gridPadding, r.getDisplayMetrics());
        return (int) ((getScreenWidth(ctx) - ((column + 1) * padding)) / column);
    }

    public static int getScreenWidth(@NonNull Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowManager wm = ctx.getSystemService(WindowManager.class);
            WindowMetrics windowMetrics = wm.getCurrentWindowMetrics();
            return windowMetrics.getBounds().width();
        } else {
            DisplayMetrics displayMetrics = ctx.getResources().getDisplayMetrics();
            return displayMetrics.widthPixels;
        }
    }

    @Nullable
    public static String getVideoId(String videoUrl) {
        // Simplified regular expression to capture the video ID
        final String reg = "(youtu\\.be/|youtube\\.com/(watch\\?v=|embed/|v/|.+?&v=))([a-zA-Z0-9_-]{11})";
        if (videoUrl == null || videoUrl.trim().isEmpty())
            return null;

        Pattern pattern = Pattern.compile(reg, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(videoUrl);
        if (matcher.find())
            return matcher.group(3); // Group 3 captures the video ID directly

        return null;
    }

    public static boolean isLandscape(@NonNull Context context) {
        int orientation = context.getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    @NonNull
    @OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
    public static DefaultExtractorsFactory getDefaultExtractorsFactory() {
        return new DefaultExtractorsFactory()
                .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS)
                .setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE);
    }

    @NonNull
    @OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
    public static DefaultRenderersFactory getDefaultRenderersFactory(Context context) {
        int extensionRendererMode = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON;
        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context);
        renderersFactory.setExtensionRendererMode(extensionRendererMode);
        return renderersFactory;
    }

    // Player --------------------------------------------------------------------------------------
    @OptIn(markerClass = UnstableApi.class)
    public static void showText(final CustomPlayerView playerView, final String text) {
        showText(playerView, text, 1200);
    }

    @OptIn(markerClass = UnstableApi.class)
    public static void showText(@NonNull final CustomPlayerView playerView, final String text, final long timeout) {
        playerView.removeCallbacks(playerView.textClearRunnable);
        playerView.clearIcon();
        playerView.setCustomErrorMessage(text);
        playerView.postDelayed(playerView.textClearRunnable, timeout);
    }

    public static int getBatteryDrawable(int status, int level, int scale) {
        float batteryLevel = (level / (float) scale) * 100;
        boolean isCharging;
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            isCharging = true;
        } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
            isCharging = false;
        } else {
            isCharging = false;
        }
        if (isCharging){
            return R.drawable.ic_battery_charging;
        } else if (batteryLevel < 10){
            return R.drawable.ic_battery_disable;
        } else if (batteryLevel < 20){
            return R.drawable.ic_battery_empty;
        } else if (batteryLevel < 30){
            return R.drawable.ic_battery_one;
        } else if (batteryLevel < 50){
            return R.drawable.ic_battery_two;
        } else {
            return R.drawable.ic_battery_full;
        }
    }

    @NonNull
    @Contract(pure = true)
    public static String getVideoResolution(int height) {
        try {
            if (height >= 4320) {
                return "8k";
            } else if (height >= 2160) {
                return "4k";
            } else if (height >= 1440) {
                return "2k";
            } else if (height >= 1080) {
                return "1080p";
            } else if (height >= 720) {
                return "720p";
            } else if (height >= 480) {
                return "480p";
            } else if (height >= 360) {
                return "360p";
            } else if (height >= 240) {
                return "240p";
            } else if (height >= 140) {
                return "140p";
            } else {
                return "Unknown resolution";
            }
        } catch (Exception e) {
            return "Unknown resolution";
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    @NonNull
    public static String getInfoAudio( ExoPlayer exoPlayer) {
        String infoAudio = "Audio Sample Rate: N/A" + "\n\n"
                + "Audio Channels: N/A" + "\n\n"
                + "Audio Type: N/A"+"\n\n"
                + "Audio MIME Type: N/A"+"\n";

        if (exoPlayer == null){
            return infoAudio;
        }
        if (exoPlayer.getAudioFormat() != null){
            int audioSampleRate = exoPlayer.getAudioFormat().sampleRate;
            int audioChannels = exoPlayer.getAudioFormat().channelCount;
            String audioMimeType = exoPlayer.getAudioFormat().sampleMimeType;

            infoAudio = "Audio Sample Rate: " + audioSampleRate + "\n\n"
                    + "Audio Channels: " + audioChannels + "\n\n"
                    + "Audio Type: "+ formatAudioFromMime(audioMimeType) +"\n\n"
                    + "Audio MIME Type: " + audioMimeType +"\n";

        }
        return infoAudio;
    }

    @OptIn(markerClass = UnstableApi.class)
    @NonNull
    public static String getInfoVideo(ExoPlayer exoPlayer) {
        String infoVideo = "Video Quality : Unknown resolution" + "\n\n"
                + "Video Width: N/A" + "\n\n"
                + "Video Height: N/A" + "\n";

        if (exoPlayer == null){
            return infoVideo;
        }
        if (exoPlayer.getVideoFormat() != null){
            int videoWidth = exoPlayer.getVideoFormat().width;
            int videoHeight = exoPlayer.getVideoFormat().height;
            infoVideo = "Video Quality: " + ApplicationUtil.getVideoResolution(videoHeight)+ "\n\n"
                    + "Video Width: " + videoWidth + "\n\n"
                    + "Video Height: " + videoHeight + "\n";
        }
        return infoVideo;
    }

    public static String formatAudioFromMime(final String mimeType) {
        if (mimeType == null) {
            return "N/A";
        }
        return switch (mimeType) {
            case MimeTypes.AUDIO_DTS -> "DTS";
            case MimeTypes.AUDIO_DTS_HD -> "DTS-HD";
            case MimeTypes.AUDIO_DTS_EXPRESS -> "DTS Express";
            case MimeTypes.AUDIO_TRUEHD -> "TrueHD";
            case MimeTypes.AUDIO_AC3 -> "AC-3";
            case MimeTypes.AUDIO_E_AC3 -> "E-AC-3";
            case MimeTypes.AUDIO_E_AC3_JOC -> "E-AC-3-JOC";
            case MimeTypes.AUDIO_AC4 -> "AC-4";
            case MimeTypes.AUDIO_AAC -> "AAC";
            case MimeTypes.AUDIO_MPEG -> "MP3";
            case MimeTypes.AUDIO_MPEG_L2 -> "MP2";
            case MimeTypes.AUDIO_VORBIS -> "Vorbis";
            case MimeTypes.AUDIO_OPUS -> "Opus";
            case MimeTypes.AUDIO_FLAC -> "FLAC";
            case MimeTypes.AUDIO_ALAC -> "ALAC";
            case MimeTypes.AUDIO_WAV -> "WAV";
            case MimeTypes.AUDIO_AMR -> "AMR";
            case MimeTypes.AUDIO_AMR_NB -> "AMR-NB";
            case MimeTypes.AUDIO_AMR_WB -> "AMR-WB";
            case MimeTypes.APPLICATION_PGS -> "PGS";
            case MimeTypes.APPLICATION_SUBRIP -> "SRT";
            case MimeTypes.TEXT_SSA -> "SSA";
            case MimeTypes.TEXT_VTT -> "VTT";
            case MimeTypes.APPLICATION_TTML -> "TTML";
            case MimeTypes.APPLICATION_TX3G -> "TX3G";
            case MimeTypes.APPLICATION_DVBSUBS -> "DVB";
            default -> mimeType;
        };
    }
}
