package com.ethionews.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import com.ethionews.app.R;
import com.ethionews.app.callback.Method;
import com.ethionews.app.executor.LoadLogin;
import com.ethionews.app.executor.LoadRegister;
import com.ethionews.app.interfaces.LoginListener;
import com.ethionews.app.interfaces.SocialLoginListener;
import com.ethionews.app.utils.IfSupported;
import com.ethionews.app.utils.helper.Helper;
import com.ethionews.app.utils.helper.SPHelper;

public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "SignInActivity";

    private String from = "";
    private Helper helper;
    private SPHelper sharedPref;
    private EditText etLoginEmail;
    private EditText etLoginPassword;
    private CheckBox rememberMe;
    private FirebaseAuth mAuth;
    private Boolean isVisibility = false;
    private ProgressDialog progressDialog;
    private CredentialManager credentialManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IfSupported.applyTheme(this);
        setContentView(R.layout.activity_sign_in);
        IfSupported.isRTL(this);
        IfSupported.isScreenshot(this);
        IfSupported.keepScreenOn(this);

        mAuth = FirebaseAuth.getInstance();
        try {
            FirebaseAuth.getInstance().signOut();
        } catch (Exception e) {
            Log.e(TAG, "Error FirebaseAuth signOut", e);
        }

        // Credential Manager replaces the deprecated GoogleSignIn API
        credentialManager = CredentialManager.create(this);

        from = getIntent().getStringExtra("from");

        helper = new Helper(this);
        sharedPref = new SPHelper(this);

        progressDialog = new ProgressDialog(SignInActivity.this);

        rememberMe = findViewById(R.id.cb_remember_me);
        etLoginEmail = findViewById(R.id.et_login_email);
        etLoginPassword = findViewById(R.id.et_login_password);

        if(Boolean.TRUE.equals(sharedPref.getIsRemember())) {
            etLoginEmail.setText(sharedPref.getEmail());
            etLoginPassword.setText(sharedPref.getPassword());
        }

        setupButton();
    }

    private void setupButton() {
        findViewById(R.id.ll_checkbox).setOnClickListener(v -> rememberMe.setChecked(!rememberMe.isChecked()));
        findViewById(R.id.tv_login_btn).setOnClickListener(v -> attemptLogin());
        findViewById(R.id.tv_login_signup).setOnClickListener(v -> {
            Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
        findViewById(R.id.tv_skip_btn).setOnClickListener(view -> {
            sharedPref.setIsFirst(false);
            openMainActivity();
        });
        findViewById(R.id.tv_forgot_pass).setOnClickListener(view ->
                startActivity(new Intent(SignInActivity.this, ForgotPasswordActivity.class)));

        LinearLayout linearLayout = findViewById(R.id.ll_login_google);
        linearLayout.setVisibility(Boolean.TRUE.equals(new SPHelper(this).getIsGoogleLogin())
                ? View.VISIBLE : View.GONE);
        linearLayout.setOnClickListener(view -> {
            if (helper.isNetworkAvailable()) {
                launchGoogleSignIn();
            } else {
                Toast.makeText(SignInActivity.this, getString(R.string.err_internet_not_connected), Toast.LENGTH_SHORT).show();
            }
        });

        ImageView passVisibility = findViewById(R.id.iv_visibility);
        passVisibility.setImageResource(Boolean.TRUE.equals(isVisibility)
                ? R.drawable.ic_login_visibility : R.drawable.ic_login_visibility_off);
        passVisibility.setOnClickListener(v -> {
            isVisibility = !isVisibility;
            passVisibility.setImageResource(Boolean.TRUE.equals(isVisibility)
                    ? R.drawable.ic_login_visibility : R.drawable.ic_login_visibility_off);
            etLoginPassword.setTransformationMethod(Boolean.TRUE.equals(isVisibility)
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
        });
    }

    /**
     * Launches the Google Sign-In flow using the modern Credential Manager API.
     * This replaces the deprecated {@code GoogleSignInClient.getSignInIntent()} flow.
     */
    private void launchGoogleSignIn() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                Runnable::run,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleGoogleCredential(result.getCredential());
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.e(TAG, "Google Sign-In failed", e);
                        runOnUiThread(() ->
                                Toast.makeText(SignInActivity.this,
                                        getString(R.string.err_login_google), Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void handleGoogleCredential(@NonNull Credential credential) {
        if (credential instanceof CustomCredential customCredential
                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                        .equals(customCredential.getType())) {
            try {
                GoogleIdTokenCredential googleIdToken =
                        GoogleIdTokenCredential.createFrom(customCredential.getData());
                firebaseAuthWithGoogle(googleIdToken.getIdToken());
            } catch (Exception e) {
                Log.e(TAG, "Invalid Google ID token credential", e);
                runOnUiThread(() ->
                        Toast.makeText(SignInActivity.this,
                                getString(R.string.err_login_google), Toast.LENGTH_SHORT).show());
            }
        } else {
            Log.w(TAG, "Unexpected credential type: " + credential.getClass());
            runOnUiThread(() ->
                    Toast.makeText(SignInActivity.this,
                            getString(R.string.err_login_google), Toast.LENGTH_SHORT).show());
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    loadLoginSocial(user.getDisplayName(), user.getEmail(), user.getUid());
                } else {
                    Toast.makeText(SignInActivity.this, "Failed to Sign IN", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(SignInActivity.this, "Failed to Sign IN", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void attemptLogin() {
        etLoginEmail.setError(null);
        etLoginPassword.setError(null);

        String email = etLoginEmail.getText().toString();
        String password = etLoginPassword.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(password)) {
            etLoginPassword.setError(getString(R.string.err_cannot_empty));
            focusView = etLoginPassword;
            cancel = true;
        }
        if (etLoginPassword.getText().toString().endsWith(" ")) {
            etLoginPassword.setError(getString(R.string.err_pass_end_space));
            focusView = etLoginPassword;
            cancel = true;
        }

        if (TextUtils.isEmpty(email)) {
            etLoginEmail.setError(getString(R.string.err_cannot_empty));
            focusView = etLoginEmail;
            cancel = true;
        } else if (!isEmailValid(email)) {
            etLoginEmail.setError(getString(R.string.err_invalid_email));
            focusView = etLoginEmail;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            loadLogin();
        }
    }

    private boolean isEmailValid(@NonNull String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void loadLogin() {
        if (!helper.isNetworkAvailable()) {
            Toast.makeText(SignInActivity.this, getString(R.string.err_internet_not_connected), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(SignInActivity.this, getString(R.string.err_server_not_connected), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (loginSuccess.equals("1")) {
                    sharedPref.setLoginDetails(userID, userName, userPhone,
                            etLoginEmail.getText().toString(), userGender, profile, "",
                            rememberMe.isChecked(), etLoginPassword.getText().toString(),
                            Method.LOGIN_TYPE_NORMAL);
                    sharedPref.setIsFirst(false);
                    sharedPref.setIsLogged(true);
                    sharedPref.setIsAutoLogin(true);

                    registerFcmTokenForUser(userID);
                    Toast.makeText(SignInActivity.this, message, Toast.LENGTH_SHORT).show();

                    if (from.equals("app")) {
                        finish();
                    } else {
                        openMainActivity();
                    }
                } else if (loginSuccess.equals("2")) {
                    // Account exists but email not verified — route to OTP screen
                    Intent verifyIntent = new Intent(SignInActivity.this, VerifyEmailActivity.class);
                    verifyIntent.putExtra("email", etLoginEmail.getText().toString());
                    startActivity(verifyIntent);
                } else {
                    Toast.makeText(SignInActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        }, helper.getAPIRequest(Method.METHOD_LOGIN, 0, "", "", "", "",
                "", "", etLoginEmail.getText().toString(), "", "",
                etLoginPassword.getText().toString(), "", Method.LOGIN_TYPE_NORMAL, null));
        loadLogin.execute();
    }

    private void loadLoginSocial(final String name, String email, final String authId) {
        if (!helper.isNetworkAvailable()) {
            Toast.makeText(SignInActivity.this, getString(R.string.err_internet_not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        LoadRegister loadRegister = new LoadRegister(new SocialLoginListener() {
            @Override
            public void onStart() {
                progressDialog.show();
            }

            @Override
            public void onEnd(String success, String registerSuccess, String message, String userID,
                              String userName, String email, String authId) {
                progressDialog.dismiss();
                if (isFinishing()) return;
                if (!success.equals("1")) {
                    Toast.makeText(SignInActivity.this, getString(R.string.err_server_not_connected), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (registerSuccess.equals("1")) {
                    sharedPref.setLoginDetails(userID, userName, "", email, "", "",
                            authId, rememberMe.isChecked(), "", Method.LOGIN_TYPE_GOOGLE);
                    sharedPref.setIsFirst(false);
                    sharedPref.setIsLogged(true);
                    sharedPref.setIsAutoLogin(true);

                    Toast.makeText(SignInActivity.this, message, Toast.LENGTH_SHORT).show();

                    if (from.equals("app")) {
                        finish();
                    } else {
                        openMainActivity();
                    }
                } else {
                    registerFailed(message);
                }
            }
        }, helper.getAPIRequest(Method.METHOD_REGISTER, 0, "", "", "",
                "", "", name, email, "", "", "", authId,
                Method.LOGIN_TYPE_GOOGLE, null));
        loadRegister.execute();
    }

    private void registerFailed(@NonNull String message) {
        if (message.contains("already") || message.contains("Invalid email format")) {
            etLoginEmail.setError(message);
            etLoginEmail.requestFocus();
        } else {
            Toast.makeText(SignInActivity.this, message, Toast.LENGTH_SHORT).show();
        }

        try {
            FirebaseAuth.getInstance().signOut();
        } catch (Exception e) {
            Log.e(TAG, "Error FirebaseAuth signOut", e);
        }
    }

    private void openMainActivity() {
        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
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

    /**
     * Fetches the current FCM token and sends it to the backend for this user.
     * Called right after login so users who logged in before the token-sync
     * feature existed get their token registered immediately.
     */
    private void registerFcmTokenForUser(String userId) {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token == null || token.isEmpty()) return;
                    new Thread(() -> {
                        try {
                            okhttp3.RequestBody body = helper.getAPIRequest(
                                    Method.METHOD_UPDATE_FCM_TOKEN,
                                    0, "", "", "", "",
                                    userId, token,
                                    "", "", "", "", "", "", null);
                            com.ethionews.app.utils.ApplicationUtil.responsePost(
                                    com.ethionews.app.callback.Callback.API_URL, body);
                        } catch (Exception e) {
                            android.util.Log.w("SignInActivity",
                                    "Failed to register FCM token: " + e.getMessage());
                        }
                    }).start();
                });
    }
}