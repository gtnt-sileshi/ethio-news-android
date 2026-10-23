package com.ethionews.app.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ethionews.app.R;
import com.ethionews.app.dialog.SleepTimerDialog;
import com.ethionews.app.utils.IfSupported;

public class EmbeddedPlayerActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private SleepTimerDialog sleepTimerDialog;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IfSupported.applyTheme(this);
        setContentView(R.layout.activity_embedded_player);
        // Use WindowInsetsController to hide status bar (FLAG_FULLSCREEN deprecated in API 30)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            //noinspection deprecation
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent intent = getIntent();
        String streamUrl = intent.getStringExtra("streamUrl");

        webView = findViewById(R.id.video);
        progressBar = findViewById(R.id.load);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setFocusableInTouchMode(false);
        webView.setFocusable(false);
        webView.getSettings().setDefaultTextEncodingName("UTF-8");
        webView.getSettings().setJavaScriptEnabled(true);

        String mimeType = "text/html";
        String encoding = "utf-8";

        String text = "<html><head>"
                + "<style type=\"text/css\">body{color: #525252;}"
                + "</style></head>"
                + "<body>"
                + streamUrl
                + "</body></html>";

        webView.setWebChromeClient(new WebChromeClient() {

            private View mCustomView;
            private CustomViewCallback mCustomViewCallback;
            private int mOriginalOrientation;
            private int mOriginalSystemUiVisibility;

            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progress == 100) {
                    progressBar.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                }

            }

            @Override
            public Bitmap getDefaultVideoPoster() {
                if (mCustomView == null) {
                    return null;
                }
                return BitmapFactory.decodeResource(getApplicationContext().getResources(), 2130837573);
            }

            @Override
            public void onHideCustomView() {
                ((FrameLayout)getWindow().getDecorView()).removeView(this.mCustomView);
                this.mCustomView = null;
                // Restore system UI using the modern API
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowInsetsController ctrl = getWindow().getInsetsController();
                    if (ctrl != null) {
                        ctrl.show(WindowInsets.Type.systemBars());
                    }
                } else {
                    //noinspection deprecation
                    getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
                }
                setRequestedOrientation(this.mOriginalOrientation);
                this.mCustomViewCallback.onCustomViewHidden();
                this.mCustomViewCallback = null;
            }

            @Override
            public void onShowCustomView(View paramView, CustomViewCallback paramCustomViewCallback) {
                if (this.mCustomView != null) {
                    onHideCustomView();
                    return;
                }
                this.mCustomView = paramView;
                //noinspection deprecation
                this.mOriginalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
                this.mOriginalOrientation = getRequestedOrientation();
                this.mCustomViewCallback = paramCustomViewCallback;
                ((FrameLayout)getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
                // Enter fullscreen using the modern WindowInsetsController (API 30+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowInsetsController ctrl = getWindow().getInsetsController();
                    if (ctrl != null) {
                        ctrl.hide(WindowInsets.Type.systemBars());
                        ctrl.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                    }
                } else {
                    // Magic number 3846 = IMMERSIVE_STICKY | HIDE_NAV | FULLSCREEN | LAYOUT_FULLSCREEN | LAYOUT_STABLE
                    //noinspection deprecation
                    getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                }
            }
        });
        webView.loadDataWithBaseURL(null, text, mimeType, encoding, null);

        // Sleep timer
        sleepTimerDialog = new SleepTimerDialog(this, () -> {
            Toast.makeText(this, R.string.sleep_timer_expired, Toast.LENGTH_SHORT).show();
            if (webView != null) webView.loadUrl("about:blank");
            finish();
        });
        ImageButton btnSleepTimer = findViewById(R.id.btn_sleep_timer);
        btnSleepTimer.setOnClickListener(v -> sleepTimerDialog.showDialog());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sleepTimerDialog != null) {
            sleepTimerDialog.cancelTimer();
        }
        if (webView != null) {
            webView.loadUrl("");
        }
    }
}
