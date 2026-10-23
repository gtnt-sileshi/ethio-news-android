package com.ethionews.app.fragment.player;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ethionews.app.R;

/**
 * Embeds a WebView directly in the player section of VideoDetailsActivity.
 * Handles "webview" stream types inline — no separate activity needed.
 */
public class WebViewPlayerFragment extends Fragment {

    private WebView webView;
    private ProgressBar progressBar;

    private static final String TAG_URL = "streamUrl";

    @NonNull
    public static WebViewPlayerFragment newInstance(String streamUrl) {
        WebViewPlayerFragment f = new WebViewPlayerFragment();
        Bundle args = new Bundle();
        args.putString(TAG_URL, streamUrl);
        f.setArguments(args);
        return f;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_webview_player, container, false);

        String streamUrl = "";
        if (getArguments() != null) {
            streamUrl = getArguments().getString(TAG_URL, "");
        }

        webView = rootView.findViewById(R.id.webView);
        progressBar = rootView.findViewById(R.id.pb_webview);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.getSettings().setDefaultTextEncodingName("UTF-8");
        webView.setBackgroundColor(0);
        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {

            private View mCustomView;
            private CustomViewCallback mCustomViewCallback;
            private int mOriginalOrientation;
            private int mOriginalSystemUiVisibility;

            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progress == 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public Bitmap getDefaultVideoPoster() {
                if (mCustomView == null) {
                    return null;
                }
                return BitmapFactory.decodeResource(
                        getResources(), android.R.drawable.presence_video_online);
            }

            @Override
            public void onHideCustomView() {
                Activity activity = getActivity();
                if (activity == null) return;

                ((FrameLayout) activity.getWindow().getDecorView()).removeView(this.mCustomView);
                this.mCustomView = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowInsetsController ctrl = activity.getWindow().getInsetsController();
                    if (ctrl != null) ctrl.show(WindowInsets.Type.systemBars());
                } else {
                    //noinspection deprecation
                    activity.getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
                }
                activity.setRequestedOrientation(this.mOriginalOrientation);
                this.mCustomViewCallback.onCustomViewHidden();
                this.mCustomViewCallback = null;
            }

            @Override
            public void onShowCustomView(View paramView, CustomViewCallback paramCustomViewCallback) {
                Activity activity = getActivity();
                if (activity == null) return;

                if (this.mCustomView != null) {
                    onHideCustomView();
                    return;
                }
                this.mCustomView = paramView;
                //noinspection deprecation
                this.mOriginalSystemUiVisibility = activity.getWindow().getDecorView().getSystemUiVisibility();
                this.mOriginalOrientation = activity.getRequestedOrientation();
                this.mCustomViewCallback = paramCustomViewCallback;
                ((FrameLayout) activity.getWindow().getDecorView())
                        .addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowInsetsController ctrl = activity.getWindow().getInsetsController();
                    if (ctrl != null) {
                        ctrl.hide(WindowInsets.Type.systemBars());
                        ctrl.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                    }
                } else {
                    //noinspection deprecation
                    activity.getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                }
            }
        });

        if (streamUrl.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(getContext(), getString(R.string.stream_not_found), Toast.LENGTH_SHORT).show();
        } else {
            webView.loadUrl(streamUrl);
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
    }
}
