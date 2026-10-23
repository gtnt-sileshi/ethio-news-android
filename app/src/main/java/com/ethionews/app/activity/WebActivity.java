package com.ethionews.app.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

import com.ethionews.app.R;
import com.ethionews.app.utils.IfSupported;
import com.ethionews.app.utils.helper.Helper;

public class WebActivity extends AppCompatActivity {

    private Helper helper;
    private WebView webView;
    private FrameLayout frameLayout;
    private String webWRL = "";

    OnBackPressedCallback callback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            goBack();
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IfSupported.applyTheme(this);
        setContentView(R.layout.activity_web);
        IfSupported.isRTL(this);
        IfSupported.isScreenshot(this);

        Intent intent = getIntent();
        webWRL = intent.getStringExtra("web_url");
        String title = intent.getStringExtra("page_title");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(view -> goBack());
        setTitle(title);

        helper = new Helper(this);

        final ProgressBar pb = findViewById(R.id.pb_web);
        frameLayout = findViewById(R.id.fl_empty);
        webView = findViewById(R.id.web);
        webView.setWebViewClient(new MyWebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                pb.setProgress(progress);
                if (progress == 100) {
                    pb.setVisibility(View.GONE);
                } else {
                    pb.setVisibility(View.VISIBLE);
                }
            }
        });
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        loadData();

        LinearLayout adView = findViewById(R.id.ll_adView);
        new Helper(this).showBannerAd(adView,"");

        getOnBackPressedDispatcher().addCallback(this, callback);
        callback.setEnabled(true);
    }

    private void loadData() {
        if (helper.isNetworkAvailable()) {
            webView.setVisibility(View.VISIBLE);
            webView.loadUrl(Objects.requireNonNull(webWRL));
            frameLayout.setVisibility(View.GONE);
        } else {
            webView.setVisibility(View.GONE);
            frameLayout.setVisibility(View.VISIBLE);

            frameLayout.removeAllViews();
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            @SuppressLint("InflateParams") View myView = inflater.inflate(R.layout.row_empty, null);

            TextView textView = myView.findViewById(R.id.tv_empty_msg);
            textView.setText(getString(R.string.err_internet_not_connected));

            myView.findViewById(R.id.ll_empty_try).setOnClickListener(v -> loadData());

            frameLayout.addView(myView);
        }
    }

    private static class MyWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(@NonNull WebView view, @NonNull WebResourceRequest request) {
            view.loadUrl(request.getUrl().toString());
            return true;
        }
    }

    private void goBack() {
        if (webView != null){
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            goBack();
        } else {
            return super.onOptionsItemSelected(menuItem);
        }
        return true;
    }
}