package com.ethionews.app.fragment.player;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.dash.DefaultDashChunkSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.exoplayer.smoothstreaming.DefaultSsChunkSource;
import androidx.media3.exoplayer.smoothstreaming.SsMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.ui.AspectRatioFrameLayout;

import org.greenrobot.eventbus.Subscribe;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

import com.ethionews.app.R;
import com.ethionews.app.dialog.DialogUtil;
import com.ethionews.app.utils.ApplicationUtil;
import com.ethionews.app.utils.Events;
import com.ethionews.app.utils.GlobalBus;
import com.ethionews.app.utils.player.BrightnessVolumeControl;
import com.ethionews.app.utils.player.CustomPlayerView;

@UnstableApi
public class ExoPlayerFragment extends Fragment {

    private ExoPlayer exoPlayer;
    private CustomPlayerView playerView;
    private DefaultBandwidthMeter bandwidthMeter;
    private DataSource.Factory mediaDataSourceFactory;
    private ProgressBar progressBar;
    ImageView imgFull;
    private boolean isFullScr = false;
    Button btnTryAgain;
    String channelUrl;
    String name;
    String userAgentName;
    boolean isUserAgent;
    private static final String TAG_STREAM_NAME = "streamName";
    private static final String TAG_STREAM_URL = "streamUrl";
    private static final String TAG_USER_AGENT = "userAgent";
    private static final String TAG_USER_AGENT_ON = "userAgentOnOff";
    RelativeLayout layoutTop;
    ImageView play;

    private BroadcastReceiver batteryReceiver;
    private ImageView exoResize;

    private static final CookieManager DEFAULT_COOKIE_MANAGER;
    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    @NonNull
    public static ExoPlayerFragment newInstance(String videoUrl, String name,
                                                String userAgentName, boolean isUserAgent) {
        ExoPlayerFragment f = new ExoPlayerFragment();
        Bundle args = new Bundle();
        args.putString(TAG_STREAM_URL, videoUrl);
        args.putString(TAG_STREAM_NAME, name);
        args.putString(TAG_USER_AGENT, userAgentName);
        args.putBoolean(TAG_USER_AGENT_ON, isUserAgent);
        f.setArguments(args);
        return f;
    }

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_exo_player, container, false);
        GlobalBus.getBus().register(this);

        if (getArguments() != null) {
            channelUrl = getArguments().getString(TAG_STREAM_URL);
            name = getArguments().getString(TAG_STREAM_NAME);
            userAgentName = getArguments().getString(TAG_USER_AGENT);
            isUserAgent = getArguments().getBoolean(TAG_USER_AGENT_ON, false);
        }

        layoutTop = rootView.findViewById(R.id.rl_video_top);
        layoutTop.setVisibility(isFullScr ? View.VISIBLE : View.GONE);

        progressBar = rootView.findViewById(R.id.pb_player);
        imgFull = rootView.findViewById(R.id.img_full_scr);
        btnTryAgain = rootView.findViewById(R.id.btn_try_again);
        play = rootView.findViewById(R.id.iv_play);

        exoResize = rootView.findViewById(R.id.exo_resize);
        exoResize.setOnClickListener(firstListener);

        rootView.findViewById(R.id.iv_back_player).setOnClickListener(v -> {
            if (isFullScr) {
                isFullScr = false;
                Events.FullScreen fullScreen = new Events.FullScreen();
                fullScreen.setFullScreen(false);
                GlobalBus.getBus().post(fullScreen);
            } else {
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });

        TextView playerTitle = rootView.findViewById(R.id.tv_player_title);
        playerTitle.setText(name);

        bandwidthMeter = new DefaultBandwidthMeter.Builder(requireActivity()).build();
        mediaDataSourceFactory = buildDataSourceFactory(true);
        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }
        // https://github.com/google/ExoPlayer/issues/8571
        DefaultExtractorsFactory extractorsFactory = ApplicationUtil.getDefaultExtractorsFactory();
        DefaultRenderersFactory renderersFactory = ApplicationUtil.getDefaultRenderersFactory(requireActivity());

        exoPlayer = new ExoPlayer.Builder(requireActivity(), renderersFactory)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(requireActivity(), extractorsFactory))
                .setLoadControl(buildLoadControl())
                .build();

        // Set audio attributes for the player
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build();
        exoPlayer.setAudioAttributes(audioAttributes, true);

        playerView = rootView.findViewById(R.id.exoPlayerView);
        playerView.setPlayer(exoPlayer);
        playerView.setUseController(true);
        playerView.requestFocus();
        playerView.setBrightnessControl(new BrightnessVolumeControl(requireActivity()));

        Uri uri = Uri.parse(channelUrl);

        MediaSource mediaSource = buildMediaSource(uri);
        exoPlayer.setMediaSource(mediaSource);
        exoPlayer.prepare();
        exoPlayer.setPlayWhenReady(true);

        play.setImageResource(R.drawable.ic_pause);
        playerListener();

        imgFull.setOnClickListener(v -> {
            if (isFullScr) {
                isFullScr = false;
                Events.FullScreen fullScreen = new Events.FullScreen();
                fullScreen.setFullScreen(false);
                GlobalBus.getBus().post(fullScreen);
            } else {
                isFullScr = true;
                Events.FullScreen fullScreen = new Events.FullScreen();
                fullScreen.setFullScreen(true);
                GlobalBus.getBus().post(fullScreen);
            }
        });

        btnTryAgain.setOnClickListener(v -> {
            btnTryAgain.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            retryLoad();
        });

        exoResize.setVisibility(isFullScr ? View.VISIBLE : View.GONE);

        play.setOnClickListener(v -> {
            exoPlayer.setPlayWhenReady(!exoPlayer.getPlayWhenReady());
            play.setImageResource(Boolean.TRUE.equals(exoPlayer.getPlayWhenReady()) ? R.drawable.ic_pause : R.drawable.ic_play);
        });

        ImageView batteryInfo = rootView.findViewById(R.id.iv_battery_info);
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                batteryInfo.setImageResource(ApplicationUtil.getBatteryDrawable(status,level,scale));
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        requireActivity().registerReceiver(batteryReceiver, filter);

        rootView.findViewById(R.id.iv_media_info).setOnClickListener(v -> {
            if (exoPlayer != null && exoPlayer.getPlayWhenReady() && exoPlayer.getVideoFormat() != null){
                playerView.hideController();
                DialogUtil.dialogPlayerInfo(requireActivity(), exoPlayer);
            } else {
                Toast.makeText(requireActivity(),getString(R.string.please_wait_a_minute), Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    private static DefaultLoadControl buildLoadControl() {
        return new DefaultLoadControl.Builder()
                .setBufferDurationsMs(15_000, 50_000, 1_000, 2_000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();
    }

    private void playerListener() {
        exoPlayer.addListener(new Player.Listener() {

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Player.Listener.super.onIsPlayingChanged(isPlaying);
                playerView.setKeepScreenOn(isPlaying);
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                if (playbackState == Player.STATE_READY) {
                    play.setImageResource(R.drawable.ic_pause);
                    progressBar.setVisibility(View.GONE);
                } else if (playbackState == Player.STATE_BUFFERING) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Player.Listener.super.onPlayerError(error);
                exoPlayer.stop();
                btnTryAgain.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                play.setImageResource(R.drawable.ic_play);
                Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Pauses playback — called by the host activity before handing off to background service. */
    public void pausePlayer() {
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(false);
            play.setImageResource(R.drawable.ic_play);
        }
    }

    public void retryLoad() {
        Uri uri = Uri.parse(channelUrl);
        MediaSource mediaSource = buildMediaSource(uri);
        exoPlayer.setMediaSource(mediaSource);
        exoPlayer.prepare();
        exoPlayer.setPlayWhenReady(true);
    }

    @SuppressLint("SwitchIntDef")
    @NonNull
    private MediaSource buildMediaSource(Uri uri) {
        int contentType  = Util.inferContentType(uri);
        MediaItem.LiveConfiguration liveConfig = new MediaItem.LiveConfiguration.Builder()
                .setTargetOffsetMs(C.TIME_UNSET)
                .setMinPlaybackSpeed(0.97f)
                .setMaxPlaybackSpeed(1.03f)
                .build();
        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(uri)
                .setLiveConfiguration(liveConfig)
                .build();
        return switch (contentType) {
            case C.CONTENT_TYPE_DASH ->
                    new DashMediaSource.Factory(new DefaultDashChunkSource
                            .Factory(mediaDataSourceFactory), buildDataSourceFactory(false))
                            .createMediaSource(mediaItem);
            case C.CONTENT_TYPE_SS ->
                    new SsMediaSource.Factory(new DefaultSsChunkSource
                            .Factory(mediaDataSourceFactory), buildDataSourceFactory(false))
                            .createMediaSource(mediaItem);
            case C.CONTENT_TYPE_HLS ->
                    new HlsMediaSource.Factory(mediaDataSourceFactory)
                            .createMediaSource(mediaItem);
            case C.CONTENT_TYPE_RTSP ->
                    new RtspMediaSource.Factory()
                            .createMediaSource(mediaItem);
            case C.CONTENT_TYPE_OTHER ->
                    new ProgressiveMediaSource.Factory(mediaDataSourceFactory)
                            .createMediaSource(mediaItem);
            default ->
                    // This is the MediaSource representing the media to be played.
                    new ProgressiveMediaSource.Factory(mediaDataSourceFactory)
                            .createMediaSource(mediaItem);
        };
    }

    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return buildDataSourceFactory(useBandwidthMeter ? bandwidthMeter : null);
    }

    public DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        HttpDataSource.Factory httpDataSourceFactory = buildHttpDataSourceFactory(bandwidthMeter);
        return new DefaultDataSource.Factory(requireActivity(), httpDataSourceFactory);
    }

    public HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        CookieHandler.setDefault(cookieManager);
        return new DefaultHttpDataSource.Factory()
                .setUserAgent(isUserAgent ? userAgentName : Util.getUserAgent(requireActivity(), "ExoPlayerDemo"))
                .setTransferListener(bandwidthMeter)
                .setAllowCrossProtocolRedirects(true)
                .setKeepPostFor302Redirects(true);
    }

    View.OnClickListener firstListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
            exoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_DEFAULT);
            playerView.showController();
            ApplicationUtil.showText(playerView, "Full Scree");
            exoResize.setOnClickListener(secondListener);
        }
    };
    View.OnClickListener secondListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
            exoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_DEFAULT);
            playerView.showController();
            ApplicationUtil.showText(playerView, "Zoom");
            exoResize.setOnClickListener(thirdListener);
        }
    };
    View.OnClickListener thirdListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            exoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_DEFAULT);
            playerView.showController();
            ApplicationUtil.showText(playerView, "Fit");
            exoResize.setOnClickListener(firstListener);
        }
    };

    @Subscribe
    public void getFullScreen(@NonNull Events.FullScreen fullScreen) {
        isFullScr = fullScreen.isFullScreen();
        if (fullScreen.isFullScreen()) {
            layoutTop.setVisibility(View.VISIBLE);
            imgFull.setImageResource(R.drawable.ic_fullscreen_exit);
        } else {
            layoutTop.setVisibility(View.GONE);
            imgFull.setImageResource(R.drawable.ic_fullscreen);
        }
        exoResize.setVisibility(fullScreen.isFullScreen() ? View.VISIBLE : View.GONE);
        layoutTop.setVisibility(fullScreen.isFullScreen() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (exoPlayer != null && exoPlayer.getPlayWhenReady()) {
            exoPlayer.setPlayWhenReady(false);
            exoPlayer.getPlaybackState();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (exoPlayer != null && exoPlayer.getPlayWhenReady()) {
            exoPlayer.setPlayWhenReady(false);
            exoPlayer.getPlaybackState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Only resume if we weren't paused by startBackgroundPlayback (onStop).
        // We check getPlayWhenReady; if a background service was started it will
        // have taken audio focus, so we stay paused here and let the user tap play.
        if (exoPlayer != null && exoPlayer.getPlaybackState() != Player.STATE_IDLE) {
            exoPlayer.setPlayWhenReady(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        GlobalBus.getBus().unregister(this);
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(false);
            exoPlayer.stop();
            exoPlayer.release();
        }
        try {
            if (batteryReceiver != null){
                requireActivity().unregisterReceiver(batteryReceiver);
            }
        } catch (Exception e) {
            Log.e("ExoPlayerFragment", "Failed to unregister battery receiver", e);
        }
    }
}
