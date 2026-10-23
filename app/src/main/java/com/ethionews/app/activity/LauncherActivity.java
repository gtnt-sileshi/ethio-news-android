package com.ethionews.app.activity;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.util.UnstableApi;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.ethionews.app.BuildConfig;
import com.ethionews.app.R;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.callback.Method;
import com.ethionews.app.executor.LoadAbout;
import com.ethionews.app.executor.LoadLogin;
import com.ethionews.app.interfaces.AboutListener;
import com.ethionews.app.interfaces.LoginListener;
import com.ethionews.app.utils.helper.DBHelper;
import com.ethionews.app.utils.helper.Helper;
import com.ethionews.app.utils.helper.SPHelper;
import com.ethionews.app.utils.purchases.BillingUpdate;
import com.ethionews.app.utils.IfSupported;

public class LauncherActivity extends AppCompatActivity {

    private Helper helper;
    private SPHelper spHelper;
    private DBHelper dbHelper;
    private ProgressBar pb;
    Application application;

    private BillingUpdate billingUpdate;

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        IfSupported.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        helper = new Helper(this);
        spHelper = new SPHelper(this);
        dbHelper = new DBHelper(this);

        pb = findViewById(R.id.pb_splash);

        android.widget.TextView tvVersion = findViewById(R.id.tv_version);
        tvVersion.setText(getString(R.string.version_name, BuildConfig.VERSION_NAME));

        billingUpdate = new BillingUpdate(LauncherActivity.this, new BillingUpdate.Listener() {
            @Override
            public void onBillingServiceDisconnected() {
                if (isFinishing()){
                    return;
                }
                loadAboutData();
            }

            @Override
            public void onBillingSetupFinished(boolean isSubscribed) {
                if (isFinishing()){
                    return;
                }
                spHelper.setIsSubscribed(isSubscribed);
                loadAboutData();
            }
        });
    }

    private void loadAboutData() {
        if (!helper.isNetworkAvailable()) {
            if (Boolean.TRUE.equals(spHelper.getIsFirst())) {
                errorDialog(getString(R.string.err_internet_not_connected), getString(R.string.err_try_internet_connected));
                return;
            }
            try {
                dbHelper.getAbout();
                setSaveData();
            } catch (Exception e) {
                errorDialog(getString(R.string.err_internet_not_connected), getString(R.string.err_try_internet_connected));
            }
            return;
        }

        LoadAbout loadAbout = new LoadAbout(LauncherActivity.this, new AboutListener() {
            @Override
            public void onStart() {
                pb.setVisibility(View.VISIBLE);
            }

            @Override
            public void onEnd(String success, String verifyStatus, String message){
                if (isFinishing()){
                    return;
                }
                pb.setVisibility(View.GONE);
                if (success.equals("1")){
                    if (!verifyStatus.equals("-1") && !verifyStatus.equals("-2")){
                        dbHelper.addToAbout();
                        setSaveData();
                    } else {
                        errorDialog(getString(R.string.err_unauthorized_access), message);
                    }
                } else {
                    errorDialog(getString(R.string.err_server), getString(R.string.err_server_not_connected));
                }
            }
        });
        loadAbout.execute();
    }

    private void setSaveData() {
        // No Envato/purchase verification — proceed directly to app settings.
        loadSettings();
    }

    private void loadSettings() {
        if (Boolean.TRUE.equals(Callback.getIsAppUpdate()) && Callback.getAppNewVersion() != BuildConfig.VERSION_CODE){
            openDialogActivity(Callback.DIALOG_TYPE_UPDATE);
        } else if(Boolean.TRUE.equals(spHelper.getIsMaintenance())){
            openDialogActivity(Callback.DIALOG_TYPE_MAINTENANCE);
        } else {
            if (Boolean.TRUE.equals(spHelper.getIsFirst())) {
                if (Boolean.TRUE.equals(spHelper.getIsLogin())){
                    openSignInActivity();
                } else {
                    spHelper.setIsFirst(false);

                    application = getApplication();
                    ((MyApplication) application).loadAd(LauncherActivity.this);

                    openMainActivity();
                }
            } else {
                loadActivity();
            }
        }
    }

    private void loadActivity() {
        application = getApplication();
        ((MyApplication) application).loadAd(LauncherActivity.this);

        if (Boolean.FALSE.equals(spHelper.getIsAutoLogin())) {
            openMainActivity();
        } else {
            if (spHelper.getLoginType().equals(Method.LOGIN_TYPE_GOOGLE)) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    loadLogin(Method.LOGIN_TYPE_GOOGLE, spHelper.getAuthID());
                } else {
                    spHelper.setIsAutoLogin(false);
                    openMainActivity();
                }
            } else {
                loadLogin(Method.LOGIN_TYPE_NORMAL, "");
            }
        }
    }

    private void loadLogin(final String loginType, final String authID) {
        if (!helper.isNetworkAvailable()) {
            Toast.makeText(LauncherActivity.this, getString(R.string.err_internet_not_connected), Toast.LENGTH_SHORT).show();
            spHelper.setIsAutoLogin(false);
            openMainActivity();
            return;
        }
        LoadLogin loadLogin = new LoadLogin(new LoginListener() {
            @Override
            public void onStart() {
                pb.setVisibility(View.VISIBLE);
            }

            @Override
            public void onEnd(String success, String loginSuccess, String message, String userID,
                              String userName, String userGender, String userPhone, String profile) {
                if (isFinishing()){
                    return;
                }
                pb.setVisibility(View.GONE);
                if (success.equals("1") && (!loginSuccess.equals("-1"))) {
                    spHelper.setLoginDetails(userID, userName, userPhone, spHelper.getEmail(), userGender,
                            profile, authID, spHelper.getIsRemember(), spHelper.getPassword(), loginType
                    );
                    spHelper.setIsLogged(true);
                }
                openMainActivity();
            }
        }, helper.getAPIRequest(Method.METHOD_LOGIN, 0,"","","",
                "","","",spHelper.getEmail(),"","",
                spHelper.getPassword(),authID,loginType,null));
        loadLogin.execute();
    }

    private void openMainActivity() {
        pb.setVisibility(View.VISIBLE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;
            if (!((MyApplication) application).getAppOpenAdManager().isAdAvailable()
                    || (Callback.getIsAppOpenAdShown()
                    && !((MyApplication) application).getAppOpenAdManager().getIsShowingAd())) {
                intent = new Intent(LauncherActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else if(((MyApplication) application).getAppOpenAdManager().isAdAvailable()
                    && Boolean.TRUE.equals(!((MyApplication) application).getAppOpenAdManager().getIsShowingAd())) {
                ((MyApplication) application).getAppOpenAdManager().showAdIfAvailable(LauncherActivity.this);
            }
        }, 5500);
    }

    private void openSignInActivity() {
        Intent intent = new Intent(LauncherActivity.this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("from", "");
        startActivity(intent);
        finish();
    }

    private void openDialogActivity(String type) {
        Intent intent = new Intent(LauncherActivity.this, DialogActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("from", type);
        startActivity(intent);
        finish();
    }

    private void errorDialog(String title, String message) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(LauncherActivity.this, R.style.dialogTheme);
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setCancelable(false);
        if (title.equals(getString(R.string.err_internet_not_connected)) || title.equals(getString(R.string.err_server_not_connected))) {
            alertDialog.setNegativeButton(getString(R.string.retry), (dialog, which) -> loadAboutData());
        }
        alertDialog.setPositiveButton(getString(R.string.exit), (dialog, which) -> finish());
        alertDialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (billingUpdate != null){
            billingUpdate.resume();
        }
    }

    @Override
    public void onDestroy() {
        try {
            if (dbHelper != null){
                dbHelper.close();
            }
            if (billingUpdate != null) {
                billingUpdate.release();
            }
        } catch (Exception e) {
            Log.e("LauncherActivity", "Error in closing", e);
        }
        super.onDestroy();
    }

    public void hideNavigationBarStatusBars() {
        try {
            Window window = getWindow();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // API 30 and above
                WindowInsetsController controller = window.getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.navigationBars());
                    controller.hide(WindowInsets.Type.statusBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                // API 23–29: use the deprecated flag set (suppressed intentionally for old API compat)
                View decorView = window.getDecorView();
                //noinspection deprecation
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                );
            }
        } catch (Exception e) {
            Log.e("LauncherActivity", "Failed to hide Navigation Bar & Status Bar", e);
        }
    }
}