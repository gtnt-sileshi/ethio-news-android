package com.ethionews.app.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.widget.Toast;

import com.ethionews.app.R;
import com.ethionews.app.callback.Method;
import com.ethionews.app.dialog.DialogUtil;
import com.ethionews.app.executor.LoadStatus;
import com.ethionews.app.interfaces.SuccessListener;
import com.ethionews.app.utils.IfSupported;
import com.ethionews.app.utils.helper.Helper;

public class ForgotPasswordActivity extends AppCompatActivity {

    private Helper helper;
    private EditText etEmail;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IfSupported.applyTheme(this);
        setContentView(R.layout.activity_forgot_password);
        IfSupported.isRTL(this);
        IfSupported.isScreenshot(this);
        IfSupported.keepScreenOn(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(view -> finish());

        helper = new Helper(this);

        progressDialog = new ProgressDialog(ForgotPasswordActivity.this);

        etEmail = findViewById(R.id.et_forgot_email);

        findViewById(R.id.ll_btn_forgot_send).setOnClickListener(v -> {
            if (!etEmail.getText().toString().trim().isEmpty()) {
                loadForgotPass();
            } else {
                Toast.makeText(ForgotPasswordActivity.this, getString(R.string.err_email), Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout adView = findViewById(R.id.ll_adView);
        helper.showBannerAd(adView,"");
    }

    private void loadForgotPass() {
        if (!helper.isNetworkAvailable()) {
            Toast.makeText(ForgotPasswordActivity.this, getString(R.string.err_internet_not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        LoadStatus loadForgotPass = new LoadStatus(new SuccessListener() {
            @Override
            public void onStart() {
                progressDialog.show();
            }

            @Override
            public void onEnd(String success, String registerSuccess, String message) {
                progressDialog.dismiss();
                if (isFinishing()) return;
                if (success.equals("1")) {
                    DialogUtil.verifyDialog(ForgotPasswordActivity.this, getString(R.string.app_name), message, () -> etEmail.setText(""));
                    Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, getString(R.string.err_server_not_connected), Toast.LENGTH_SHORT).show();
                }
            }
        }, helper.getAPIRequest(Method.METHOD_FORGOT_PASSWORD, 0, "", "",
                "", "", "", "", etEmail.getText().toString(),
                "", "", "", "", "", null));
        loadForgotPass.execute();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        } else {
            return super.onOptionsItemSelected(menuItem);
        }
        return true;
    }
}