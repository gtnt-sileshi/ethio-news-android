package com.ethionews.app.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.ethionews.app.R;
import com.ethionews.app.callback.Method;
import com.ethionews.app.executor.LoadLogin;
import com.ethionews.app.executor.LoadStatus;
import com.ethionews.app.interfaces.LoginListener;
import com.ethionews.app.interfaces.SuccessListener;
import com.ethionews.app.utils.IfSupported;
import com.ethionews.app.utils.helper.Helper;
import com.ethionews.app.utils.helper.SPHelper;

public class VerifyEmailActivity extends AppCompatActivity {

    private String email = "";
    private Helper helper;
    private SPHelper spHelper;
    private EditText etOtp;
    private TextView tvResend;
    private ProgressDialog progressDialog;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IfSupported.applyTheme(this);
        setContentView(R.layout.activity_verify_email);
        IfSupported.isRTL(this);
        IfSupported.isScreenshot(this);

        email = getIntent().getStringExtra("email");
        if (email == null) email = "";

        helper   = new Helper(this);
        spHelper = new SPHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etOtp      = findViewById(R.id.et_otp);
        tvResend   = findViewById(R.id.tv_resend);
        progressDialog = new ProgressDialog(this);

        // Show which email the code was sent to
        TextView tvSubtitle = findViewById(R.id.tv_verify_subtitle);
        if (!email.isEmpty()) {
            tvSubtitle.setText(getString(R.string.verify_email_sent_to, email));
        }

        findViewById(R.id.ll_btn_verify).setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            if (otp.isEmpty()) {
                etOtp.setError(getString(R.string.err_cannot_empty));
                etOtp.requestFocus();
            } else if (otp.length() < 6) {
                etOtp.setError(getString(R.string.err_otp_length));
                etOtp.requestFocus();
            } else {
                verifyOtp(otp);
            }
        });

        tvResend.setOnClickListener(v -> resendOtp());

        // Start cooldown immediately — a code was just sent on registration
        startResendCooldown();
    }

    private void verifyOtp(String otp) {
        if (!helper.isNetworkAvailable()) {
            Toast.makeText(this, getString(R.string.err_internet_not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        LoadLogin loadLogin = new LoadLogin(new LoginListener() {
            @Override
            public void onStart() {
                progressDialog.show();
            }

            @Override
            public void onEnd(String success, String loginSuccess, String message, String userID,
                              String userName, String userGender, String userPhone, String profile) {
                progressDialog.dismiss();
                if (isFinishing()) return;

                if (!success.equals("1")) {
                    Toast.makeText(VerifyEmailActivity.this,
                            getString(R.string.err_server_not_connected), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (loginSuccess.equals("1")) {
                    // Verified — log the user in immediately
                    spHelper.setLoginDetails(userID, userName, userPhone, email,
                            userGender, profile, "", false, "", Method.LOGIN_TYPE_NORMAL);
                    spHelper.setIsFirst(false);
                    spHelper.setIsLogged(true);
                    spHelper.setIsAutoLogin(true);

                    Toast.makeText(VerifyEmailActivity.this, message, Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(VerifyEmailActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    etOtp.setError(message);
                    etOtp.requestFocus();
                    etOtp.setText("");
                }
            }
        }, helper.getAPIRequest(Method.METHOD_VERIFY_EMAIL, 0, "", "", "", "",
                "", "", email, "", "", "", otp, "", null));

        loadLogin.execute();
    }

    private void resendOtp() {
        if (!helper.isNetworkAvailable()) {
            Toast.makeText(this, getString(R.string.err_internet_not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable during request
        tvResend.setEnabled(false);

        LoadStatus loadStatus = new LoadStatus(new SuccessListener() {
            @Override
            public void onStart() {
                progressDialog.show();
            }

            @Override
            public void onEnd(String success, String serverSuccess, String message) {
                progressDialog.dismiss();
                if (isFinishing()) return;
                Toast.makeText(VerifyEmailActivity.this, message, Toast.LENGTH_SHORT).show();
                if (success.equals("1") && serverSuccess.equals("1")) {
                    etOtp.setText("");
                    etOtp.requestFocus();
                    startResendCooldown();
                } else {
                    // Server error or cooldown message — re-enable immediately
                    tvResend.setEnabled(true);
                    tvResend.setText(getString(R.string.resend));
                }
            }
        }, helper.getAPIRequest(Method.METHOD_RESEND_OTP, 0, "", "", "", "",
                "", "", email, "", "", "", "", "", null));

        loadStatus.execute();
    }

    /** Disables the resend button and counts down 60 s before re-enabling it. */
    @SuppressLint("SetTextI18n")
    private void startResendCooldown() {
        if (countDownTimer != null) countDownTimer.cancel();
        tvResend.setEnabled(false);

        countDownTimer = new CountDownTimer(60_000, 1_000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isFinishing()) return;
                tvResend.setText(getString(R.string.resend) + " (" + (millisUntilFinished / 1000) + "s)");
            }

            @Override
            public void onFinish() {
                if (isFinishing()) return;
                tvResend.setEnabled(true);
                tvResend.setText(getString(R.string.resend));
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) countDownTimer.cancel();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
