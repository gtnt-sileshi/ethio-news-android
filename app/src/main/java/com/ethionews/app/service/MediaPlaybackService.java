package com.ethionews.app.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.app.ServiceCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.session.MediaNotification;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.util.Collections;

import com.ethionews.app.R;
import com.ethionews.app.activity.RadioPlayerActivity;
import com.ethionews.app.activity.MainActivity;
import com.ethionews.app.utils.ApplicationUtil;

/**
 * Foreground {@link MediaSessionService} that keeps audio alive when the user
 * leaves the Radio / Live TV / Event screens.
 *
 * Start / switch streams by sending an Intent with these extras:
 *   {@link #EXTRA_STREAM_URL}    — HLS / DASH / RTSP / MP4 URL (required)
 *   {@link #EXTRA_TITLE}         — channel / station name shown in the notification
 *   {@link #EXTRA_LOGO}          — artwork URL (optional)
 *   {@link #EXTRA_CONTENT_TYPE}  — {@link #TYPE_RADIO}, {@link #TYPE_LIVE_TV}, or {@link #TYPE_EVENT}
 *
 * Media3 builds and posts a {@code MediaStyle} notification automatically
 * (with play/pause/stop controls and artwork).  We hook into
 * {@link #onUpdateNotification} so that every time Media3 updates its
 * notification we also call {@link #startForeground} with the new
 * notification — this is the correct way to keep the foreground slot
 * in sync on Android 12+ (API 31+).
 */
@OptIn(markerClass = UnstableApi.class)
public class MediaPlaybackService extends MediaSessionService {

    private static final String TAG = "MediaPlaybackService";

    // ── Intent extras ─────────────────────────────────────────────────────────

    public static final String EXTRA_STREAM_URL   = "stream_url";
    public static final String EXTRA_TITLE        = "stream_title";
    public static final String EXTRA_LOGO         = "stream_logo";
    public static final String EXTRA_CONTENT_TYPE = "content_type";

    public static final String TYPE_RADIO   = "radio";
    public static final String TYPE_LIVE_TV = "live_tv";
    public static final String TYPE_EVENT   = "event";

    /**
     * Channel ID used by Media3's {@code DefaultMediaNotificationProvider}.
     * Declared in {@link com.ethionews.app.activity.MyApplication}.
     */
    public static final String CHANNEL_ID = "media_playback_channel";

    /**
     * Notification ID used by Media3 1.x (hardcoded in
     * {@code DefaultMediaNotificationProvider#NOTIFICATION_ID}).
     */
    static final int MEDIA3_NOTIFICATION_ID = 1001;

    // ── State ─────────────────────────────────────────────────────────────────

    private ExoPlayer    player;
    private MediaSession mediaSession;
    private boolean      foregroundStarted = false;

    // Current stream info — used to build the notification tap PendingIntent
    private String currentStreamUrl   = "";
    private String currentTitle       = "";
    private String currentLogo        = "";
    private String currentContentType = TYPE_RADIO;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ── Service lifecycle ─────────────────────────────────────────────────────

    @Override
    public void onCreate() {
        super.onCreate();
        buildPlayer();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent != null) {
            String url         = intent.getStringExtra(EXTRA_STREAM_URL);
            String title       = intent.getStringExtra(EXTRA_TITLE);
            String logo        = intent.getStringExtra(EXTRA_LOGO);
            String contentType = intent.getStringExtra(EXTRA_CONTENT_TYPE);

            if (url != null && !url.isEmpty()) {
                // Must enter foreground before the 5-second deadline on Android 14+.
                if (!foregroundStarted) {
                    buildInitialNotification(title);
                }
                // Store for session activity PendingIntent
                currentStreamUrl   = url;
                currentTitle       = title   != null ? title   : "";
                currentLogo        = logo    != null ? logo    : "";
                currentContentType = contentType != null ? contentType : TYPE_RADIO;

                // Rebuild the session so the new sessionActivity PendingIntent is set
                rebuildSessionActivity();

                playStream(url, title, logo, contentType);
            }
        }

        return START_STICKY;
    }

    /**
     * Called by Media3 every time the notification should change (play→pause,
     * metadata update, artwork loaded, etc.).  We re-call startForeground so
     * the foreground slot always shows the latest MediaStyle notification with
     * correct controls.
     */
    @Override
    public void onUpdateNotification(@NonNull MediaSession session,
                                     boolean startInForegroundRequired) {
        // Let Media3 do its default work (builds + posts the MediaStyle notification)
        super.onUpdateNotification(session, startInForegroundRequired);

        // Now retrieve what Media3 just posted and re-bind it to the foreground slot.
        // Media3's DefaultMediaNotificationProvider always uses MEDIA3_NOTIFICATION_ID.
        // By calling startForeground with that same ID, the OS updates the foreground
        // notification in place — giving us play/pause/stop controls and the title.
        mainHandler.post(this::refreshForegroundNotification);
    }

    /** Reads Media3's latest notification back from the system and re-applies it to startForeground. */
    private void refreshForegroundNotification() {
        if (player == null || !player.isCommandAvailable(androidx.media3.common.Player.COMMAND_PLAY_PAUSE)) return;
        try {
            android.app.NotificationManager nm =
                    (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm == null) return;
            for (android.service.notification.StatusBarNotification sbn : nm.getActiveNotifications()) {
                if (sbn.getId() == MEDIA3_NOTIFICATION_ID) {
                    promoteToForeground(sbn.getNotification());
                    return;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "refreshForegroundNotification: " + e.getMessage());
        }
    }

    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        if (player != null) player.stop();
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) { mediaSession.release(); mediaSession = null; }
        if (player != null)       { player.release();       player = null; }
        super.onDestroy();
    }

    // ── Player / session setup ────────────────────────────────────────────────

    private void buildPlayer() {
        DefaultRenderersFactory renderersFactory =
                ApplicationUtil.getDefaultRenderersFactory(this);
        DefaultExtractorsFactory extractorsFactory =
                ApplicationUtil.getDefaultExtractorsFactory();

        player = new ExoPlayer.Builder(this, renderersFactory)
                .setMediaSourceFactory(
                        new DefaultMediaSourceFactory(this, extractorsFactory)
                                .setDataSourceFactory(buildDataSourceFactory()))
                .setLoadControl(buildLoadControl())
                .build();

        player.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                /* handleAudioFocus= */ true);

        player.setWakeMode(C.WAKE_MODE_NETWORK);

        mediaSession = new MediaSession.Builder(this, player)
                .setSessionActivity(buildSessionPendingIntent())
                .setCallback(new MediaSession.Callback() {
                    @NonNull
                    @Override
                    public ListenableFuture<MediaSession.MediaItemsWithStartPosition>
                    onPlaybackResumption(@NonNull MediaSession session,
                                        @NonNull MediaSession.ControllerInfo controller) {
                        return Futures.immediateFuture(
                                new MediaSession.MediaItemsWithStartPosition(
                                        Collections.emptyList(), 0, C.TIME_UNSET));
                    }
                })
                .build();
    }

    /**
     * Builds a PendingIntent that opens the appropriate player screen when the
     * user taps the media notification.
     *
     * - Radio  → RadioPlayerActivity (with all required extras so it can
     *            reconnect to the service and show the correct station info)
     * - Other  → MainActivity (live TV / events don't have a standalone player
     *            activity that can be cold-launched without a post_id)
     *
     * FLAG_ACTIVITY_SINGLE_TOP + FLAG_ACTIVITY_REORDER_TO_FRONT ensures the
     * existing activity instance is brought forward rather than a new one created.
     */
    private PendingIntent buildSessionPendingIntent() {
        final Intent activityIntent;

        if (TYPE_RADIO.equals(currentContentType) && !currentStreamUrl.isEmpty()) {
            activityIntent = new Intent(this, RadioPlayerActivity.class)
                    .putExtra("stream_url",  currentStreamUrl)
                    .putExtra("radio_name",  currentTitle)
                    .putExtra("logo",        currentLogo)
                    .putExtra("radio_id",    "")
                    .putExtra("genre",       "")
                    .putExtra("description", "")
                    .putExtra("isPremium",   false)
                    .putExtra("is_favourite", false)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                            | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        } else {
            activityIntent = new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                            | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE : 0);

        return PendingIntent.getActivity(this, 0, activityIntent, flags);
    }

    /**
     * Called after currentContentType / currentStreamUrl are updated so the
     * notification tap target reflects the latest stream.
     */
    private void rebuildSessionActivity() {
        if (mediaSession == null) return;
        mediaSession.setSessionActivity(buildSessionPendingIntent());
    }

    // ── Buffer tuning ─────────────────────────────────────────────────────────

    private static DefaultLoadControl buildLoadControl() {
        return new DefaultLoadControl.Builder()
                .setBufferDurationsMs(15_000, 50_000, 1_000, 2_000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();
    }

    // ── DataSource ────────────────────────────────────────────────────────────

    private DataSource.Factory buildDataSourceFactory() {
        HttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(Util.getUserAgent(this, "ExoPlayerDemo"))
                .setAllowCrossProtocolRedirects(true)
                .setKeepPostFor302Redirects(true);
        return new DefaultDataSource.Factory(this, httpFactory);
    }

    // ── Stream loading ────────────────────────────────────────────────────────

    private void playStream(String url, @Nullable String title,
                            @Nullable String logo, @Nullable String contentType) {
        String subtitle = subtitleForType(contentType);

        MediaMetadata metadata = new MediaMetadata.Builder()
                .setTitle(title != null ? title : "")
                .setArtist(subtitle)
                .build();

        MediaItem item = new MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(metadata)
                .setLiveConfiguration(new MediaItem.LiveConfiguration.Builder()
                        .setTargetOffsetMs(C.TIME_UNSET)
                        .setMinPlaybackSpeed(0.97f)
                        .setMaxPlaybackSpeed(1.03f)
                        .build())
                .build();

        player.setMediaItem(item);
        player.prepare();
        player.setPlayWhenReady(true);

        if (logo != null && !logo.isEmpty() && !logo.equals("null")) {
            fetchArtworkAndUpdate(url, title, subtitle, logo);
        }
    }

    private void fetchArtworkAndUpdate(String url, @Nullable String title,
                                       String subtitle, String logoUrl) {
        new Thread(() -> {
            try {
                Bitmap art = Picasso.get().load(logoUrl).get();
                if (art == null || player == null) return;

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                art.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                byte[] artBytes = baos.toByteArray();

                MediaMetadata richMeta = new MediaMetadata.Builder()
                        .setTitle(title != null ? title : "")
                        .setArtist(subtitle)
                        .setArtworkData(artBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                        .build();

                MediaItem richItem = new MediaItem.Builder()
                        .setUri(url)
                        .setMediaMetadata(richMeta)
                        .setLiveConfiguration(new MediaItem.LiveConfiguration.Builder()
                                .setTargetOffsetMs(C.TIME_UNSET)
                                .setMinPlaybackSpeed(0.97f)
                                .setMaxPlaybackSpeed(1.03f)
                                .build())
                        .build();

                mainHandler.post(() -> {
                    if (player == null) return;
                    player.replaceMediaItem(player.getCurrentMediaItemIndex(), richItem);
                });
            } catch (Exception e) {
                Log.w(TAG, "Artwork fetch failed: " + e.getMessage());
            }
        }).start();
    }

    // ── Foreground helpers ────────────────────────────────────────────────────

    /**
     * Posts a bare notification and calls startForeground() immediately.
     * Used only for the initial foreground promotion (5-second deadline).
     * Media3's real MediaStyle notification will replace this via
     * onUpdateNotification → refreshForegroundNotification.
     */
    private void buildInitialNotification(@Nullable String title) {
        // Build a minimal notification using Media3's channel so it matches
        androidx.core.app.NotificationCompat.Builder builder =
                new androidx.core.app.NotificationCompat.Builder(this, "default_channel_id")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title != null && !title.isEmpty()
                                ? title : getString(R.string.now_playing))
                        .setContentText(getString(R.string.loading))
                        .setOngoing(true)
                        .setSilent(true);
        promoteToForeground(builder.build());
    }

    private void promoteToForeground(Notification notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(MEDIA3_NOTIFICATION_ID, notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(MEDIA3_NOTIFICATION_ID, notification);
            }
            foregroundStarted = true;
        } catch (Exception e) {
            Log.e(TAG, "startForeground failed: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @NonNull
    private String subtitleForType(@Nullable String contentType) {
        if (contentType == null) return "";
        return switch (contentType) {
            case TYPE_RADIO   -> getString(R.string.live_radio);
            case TYPE_LIVE_TV -> getString(R.string.live_tv);
            case TYPE_EVENT   -> getString(R.string.event);
            default           -> "";
        };
    }
}
