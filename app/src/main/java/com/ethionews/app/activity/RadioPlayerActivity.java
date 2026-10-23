package com.ethionews.app.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.widget.Toolbar;
import androidx.media3.common.Player;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.appcompat.app.AppCompatActivity;

import com.google.common.util.concurrent.ListenableFuture;
import com.squareup.picasso.Picasso;

import com.ethionews.app.R;
import com.ethionews.app.callback.Method;
import com.ethionews.app.dialog.FeedBackDialog;
import com.ethionews.app.dialog.SleepTimerDialog;
import com.ethionews.app.executor.LoadStatus;
import com.ethionews.app.interfaces.SuccessListener;
import com.ethionews.app.service.MediaPlaybackService;
import com.ethionews.app.utils.IfSupported;
import com.ethionews.app.utils.helper.Helper;
import com.ethionews.app.utils.helper.SPHelper;

/**
 * Displays a radio station and controls playback via {@link MediaPlaybackService}.
 *
 * The UI controls (play/pause) talk to a {@link MediaController} that is connected
 * to the service, so audio continues even when the screen is off or the user
 * navigates away from this Activity.
 *
 * Tapping the toolbar's back button or pressing system-back finishes the Activity
 * but leaves the service — and the playback notification — alive.  The user can
 * stop playback from the notification or by returning to this screen and pausing.
 */
@OptIn(markerClass = UnstableApi.class)
public class RadioPlayerActivity extends AppCompatActivity {

    private static final String TAG = "RadioPlayerActivity";

    // MediaController is the remote handle to MediaPlaybackService
    private MediaController                   mediaController;
    private ListenableFuture<MediaController> controllerFuture;

    // Main-thread handler — used to ensure all UI updates stay on the main thread
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean isPlaying = false;

    private ImageButton btnPlayPause;
    private ProgressBar pbBuffering;
    private TextView    tvStatus;
    private Helper      helper;
    private SPHelper    spHelper;
    private ImageView   fav;

    private String streamUrl;
    private String radioId;
    private String radioName;
    private String logo;
    private boolean isFav;

    private SleepTimerDialog sleepTimerDialog;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IfSupported.applyTheme(this);
        setContentView(R.layout.activity_radio_player);
        IfSupported.isRTL(this);
        IfSupported.isScreenshot(this);
        IfSupported.keepScreenOn(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Ensure system back button/gesture also finishes immediately without
        // going through AppCompatActivity's default fragment-popping back handling.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        Intent intent = getIntent();
        radioId   = intent.getStringExtra("radio_id");
        streamUrl = intent.getStringExtra("stream_url");
        radioName = intent.getStringExtra("radio_name");
        logo      = intent.getStringExtra("logo");
        String genre       = intent.getStringExtra("genre");
        String description = intent.getStringExtra("description");

        setTitle(radioName != null ? radioName : getString(R.string.live_radio));

        helper   = new Helper(this);
        spHelper = new SPHelper(this);

        // Bind views
        ImageView ivLogo        = findViewById(R.id.iv_radio_logo);
        TextView  tvName        = findViewById(R.id.tv_radio_name);
        TextView  tvGenre       = findViewById(R.id.tv_radio_genre);
        TextView  tvDescription = findViewById(R.id.tv_radio_description);
        btnPlayPause            = findViewById(R.id.btn_play_pause);
        pbBuffering             = findViewById(R.id.pb_radio_buffer);
        tvStatus                = findViewById(R.id.tv_radio_status);
        fav                     = findViewById(R.id.iv_fav);
        LinearLayout adView     = findViewById(R.id.ll_adView);

        // Populate station info
        if (logo != null && !logo.isEmpty() && !logo.equals("null")) {
            Picasso.get()
                    .load(logo)
                    .placeholder(R.drawable.material_design_default)
                    .error(R.drawable.material_design_default)
                    .into(ivLogo);
        }
        tvName.setText(radioName != null ? radioName : "");
        tvGenre.setText(genre != null && !genre.isEmpty() ? genre : "");
        tvDescription.setText(description != null && !description.isEmpty() ? description : "");

        helper.showBannerAd(adView, "");

        // Favorite toggle
        isFav = intent.getBooleanExtra("is_favourite", false);
        changeFav(isFav);
        fav.setOnClickListener(v -> loadFav());

        // Share button on-screen
        ImageView ivShare = findViewById(R.id.iv_share);
        ivShare.setOnClickListener(v -> shareStation());

        // Start the service, THEN bind the controller from onResume
        startMediaService();

        btnPlayPause.setOnClickListener(v -> {
            if (mediaController != null) {
                if (mediaController.isPlaying()) {
                    mediaController.pause();
                } else {
                    mediaController.play();
                }
            }
        });

        sleepTimerDialog = new SleepTimerDialog(this, () -> {
            Toast.makeText(this, R.string.sleep_timer_expired, Toast.LENGTH_SHORT).show();
            if (mediaController != null) mediaController.pause();
            stopMediaService();
            finish();
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Called when the activity is already running and the notification is tapped.
        // Nothing to do — the service is already playing and the controller
        // reconnects in onStart(). Just bring the activity to the front (handled
        // by FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_REORDER_TO_FRONT in the
        // PendingIntent) so the user sees the current player UI.
        setIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectController();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Release the controller; service and playback keep running in background
        releaseController();
    }

    @Override
    protected void onDestroy() {
        if (sleepTimerDialog != null) {
            sleepTimerDialog.cancelTimer();
        }
        releaseController();
        super.onDestroy();
    }

    // ── Options menu ──────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_radio_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_sleep_timer) {
            sleepTimerDialog.showDialog();
            return true;
        } else if (id == R.id.menu_stop_playback) {
            stopMediaService();
            finish();
            return true;
        } else if (id == R.id.menu_feedback) {
            new FeedBackDialog(this).showDialog(radioId, radioName);
            return true;
        } else if (id == R.id.menu_share) {
            shareStation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ── Favorite ──────────────────────────────────────────────────────────────

    private void loadFav() {
        if (!spHelper.isLogged()) {
            helper.clickLogin();
            return;
        }
        if (!helper.isNetworkAvailable()) {
            Toast.makeText(this, getString(R.string.err_internet_not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        LoadStatus loadFav = new LoadStatus(new SuccessListener() {
            @Override
            public void onStart() {
                changeFav(!isFav);
            }

            @Override
            public void onEnd(String success, String favSuccess, String message) {
                // success   = HTTP-level result ("1" = call succeeded, "0" = network/parse failure)
                // favSuccess = JSON body "success" field:
                //   "1" → added to favourites
                //   "2" → removed from favourites
                //   anything else → server-side error
                boolean serverAccepted = success.equals("1") &&
                        (favSuccess.equals("1") || favSuccess.equals("2"));
                if (serverAccepted) {
                    // The optimistic toggle in onStart() already flipped the icon correctly —
                    // just confirm it with a toast.
                    Toast.makeText(RadioPlayerActivity.this, message, Toast.LENGTH_SHORT).show();
                } else {
                    // Server rejected the request — revert the optimistic toggle
                    changeFav(!isFav);
                    Toast.makeText(RadioPlayerActivity.this,
                            getString(R.string.err_server_not_connected), Toast.LENGTH_SHORT).show();
                }
            }
        }, helper.getAPIRequest(Method.METHOD_DO_FAV, 0, radioId, "", "",
                "", spHelper.getUserId(), "", "", "", "",
                "", "", "radio", null));
        loadFav.execute();
    }

    private void changeFav(boolean favourite) {
        isFav = favourite;
        fav.setImageResource(favourite ? R.drawable.ic_turned_in : R.drawable.ic_turned_in_not);
    }

    // ── Share ─────────────────────────────────────────────────────────────────

    private void shareStation() {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT,
                getResources().getString(R.string.share_the_app));
        String shareText = (radioName != null ? radioName : getString(R.string.live_radio))
                + "\n\nvia "
                + getResources().getString(R.string.app_name)
                + " - http://play.google.com/store/apps/details?id=" + getPackageName();
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(sharingIntent,
                getResources().getString(R.string.share)));
    }

    // ── Service helpers ───────────────────────────────────────────────────────

    private void startMediaService() {
        if (streamUrl == null || streamUrl.isEmpty()) {
            Toast.makeText(this, getString(R.string.stream_not_found), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent serviceIntent = new Intent(this, MediaPlaybackService.class);
        serviceIntent.putExtra(MediaPlaybackService.EXTRA_STREAM_URL,   streamUrl);
        serviceIntent.putExtra(MediaPlaybackService.EXTRA_TITLE,        radioName);
        serviceIntent.putExtra(MediaPlaybackService.EXTRA_LOGO,         logo);
        serviceIntent.putExtra(MediaPlaybackService.EXTRA_CONTENT_TYPE, MediaPlaybackService.TYPE_RADIO);
        startService(serviceIntent);
    }

    private void stopMediaService() {
        stopService(new Intent(this, MediaPlaybackService.class));
    }

    // ── MediaController connection ────────────────────────────────────────────

    private void connectController() {
        if (controllerFuture != null) return; // already connecting / connected

        SessionToken token = new SessionToken(
                this,
                new ComponentName(this, MediaPlaybackService.class));

        controllerFuture = new MediaController.Builder(this, token).buildAsync();

        // IMPORTANT: use mainHandler (main-looper executor) so that the callback
        // and all subsequent listener callbacks always run on the main thread.
        // Using directExecutor() here causes callbacks to arrive on binder threads,
        // which triggers UI-from-wrong-thread exceptions and ANRs.
        controllerFuture.addListener(this::onControllerReady,
                mainHandler::post);
    }

    private void onControllerReady() {
        if (isFinishing() || isDestroyed()) return;
        try {
            mediaController = controllerFuture.get();
        } catch (Exception e) {
            android.util.Log.e(TAG, "MediaController connection failed", e);
            return;
        }
        if (mediaController == null) return;

        syncUiFromController();

        mediaController.addListener(new Player.Listener() {

            @Override
            public void onPlaybackStateChanged(int state) {
                switch (state) {
                    case Player.STATE_BUFFERING:
                        pbBuffering.setVisibility(View.VISIBLE);
                        tvStatus.setText(getString(R.string.loading));
                        break;
                    case Player.STATE_READY:
                        pbBuffering.setVisibility(View.GONE);
                        tvStatus.setText(isPlaying
                                ? getString(R.string.radio_playing)
                                : getString(R.string.radio_paused));
                        break;
                    case Player.STATE_ENDED:
                    case Player.STATE_IDLE:
                        pbBuffering.setVisibility(View.GONE);
                        tvStatus.setText(getString(R.string.radio_stopped));
                        isPlaying = false;
                        btnPlayPause.setImageResource(R.drawable.ic_play);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onIsPlayingChanged(boolean playing) {
                isPlaying = playing;
                btnPlayPause.setImageResource(
                        playing ? R.drawable.ic_pause : R.drawable.ic_play);
                tvStatus.setText(playing
                        ? getString(R.string.radio_playing)
                        : getString(R.string.radio_paused));
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                pbBuffering.setVisibility(View.GONE);
                tvStatus.setText(getString(R.string.stream_not_found));
                isPlaying = false;
                btnPlayPause.setImageResource(R.drawable.ic_play);
                Toast.makeText(RadioPlayerActivity.this,
                        getString(R.string.stream_not_found), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void syncUiFromController() {
        if (mediaController == null) return;
        isPlaying = mediaController.isPlaying();
        btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        int state = mediaController.getPlaybackState();
        if (state == Player.STATE_BUFFERING) {
            pbBuffering.setVisibility(View.VISIBLE);
            tvStatus.setText(getString(R.string.loading));
        } else if (state == Player.STATE_READY) {
            pbBuffering.setVisibility(View.GONE);
            tvStatus.setText(isPlaying
                    ? getString(R.string.radio_playing)
                    : getString(R.string.radio_paused));
        }
    }

    private void releaseController() {
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
            controllerFuture = null;
            mediaController  = null;
        }
    }
}
