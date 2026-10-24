package com.ethionews.app.activity;

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
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ethionews.app.BuildConfig;
import com.ethionews.app.R;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.executor.LoadAbout;
import com.ethionews.app.interfaces.AboutListener;
import com.ethionews.app.utils.helper.DBHelper;
import com.ethionews.app.utils.helper.Helper;
import com.ethionews.app.utils.helper.SPHelper;
import com.ethionews.app.utils.IfSupported;

/**
 * Splash / launcher screen.
 *
 * Blog app has no user login or billing — this just fetches app_details,
 * checks for maintenance / force-update, then opens MainActivity.
 */
public class LauncherActivity extends AppCompatActivity {

    private Helper helper;
    private SPHelper spHelper;
    private DBHelper dbHelper;
    private ProgressBar pb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        IfSupported.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        helper   = new Helper(this);
        spHelper = new SPHelper(this);
        dbHelper = new DBHelper(this);

        pb = findViewById(R.id.pb_splash);

        TextView tvVersion = findViewById(R.id.tv_version);
        tvVersion.setText(getString(R.string.version_name, BuildConfig.VERSION_NAME));

        loadAboutData();
    }

    private void loadAboutData() {
        if (!helper.isNetworkAvailable()) {
            if (Boolean.TRUE.equals(spHelper.getIsFirst())) {
                errorDialog(getString(R.string.err_internet_not_connected), getString(R.string.err_try_internet_connected));
                return;
            }
            try {
                dbHelper.getAbout();
                proceedToApp();
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
            public void onEnd(String success, String verifyStatus, String message) {
                if (isFinishing()) return;
                pb.setVisibility(View.GONE);
                if ("1".equals(success)) {
                    if (!"-1".equals(verifyStatus) && !"-2".equals(verifyStatus)) {
                        dbHelper.addToAbout();
                        proceedToApp();
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

    private void proceedToApp() {
        if (Boolean.TRUE.equals(Callback.getIsAppUpdate())
                && Callback.getAppNewVersion() != BuildConfig.VERSION_CODE) {
            openDialogActivity(Callback.DIALOG_TYPE_UPDATE);
        } else if (Boolean.TRUE.equals(spHelper.getIsMaintenance())) {
            openDialogActivity(Callback.DIALOG_TYPE_MAINTENANCE);
        } else {
            spHelper.setIsFirst(false);
            MyApplication app = (MyApplication) getApplication();
            app.loadAd(LauncherActivity.this);
            openMainActivity();
        }
    }

    private void openMainActivity() {
        pb.setVisibility(View.VISIBLE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            MyApplication app = (MyApplication) getApplication();
            if (app.getAppOpenAdManager().isAdAvailable()
                    && Boolean.TRUE.equals(!app.getAppOpenAdManager().getIsShowingAd())) {
                // Show the open ad — it will navigate to MainActivity on dismiss
                app.getAppOpenAdManager().showAdIfAvailable(LauncherActivity.this);
            } else {
                // No ad available — navigate directly
                Intent intent = new Intent(LauncherActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        }, 3000);
    }

    private void openDialogActivity(String type) {
        Intent intent = new Intent(LauncherActivity.this, DialogActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("from", type);
        startActivity(intent);
        finish();
    }

    private void errorDialog(String title, String message) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(LauncherActivity.this, R.style.dialogTheme);
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setCancelable(false);
        if (title.equals(getString(R.string.err_internet_not_connected))
                || title.equals(getString(R.string.err_server_not_connected))) {
            alertDialog.setNegativeButton(getString(R.string.retry), (dialog, which) -> loadAboutData());
        }
        alertDialog.setPositiveButton(getString(R.string.exit), (dialog, which) -> finish());
        alertDialog.show();
    }

    @Override
    protected void onDestroy() {
        try {
            if (dbHelper != null) dbHelper.close();
        } catch (Exception e) {
            Log.e("LauncherActivity", "Error in closing", e);
        }
        super.onDestroy();
    }
}
