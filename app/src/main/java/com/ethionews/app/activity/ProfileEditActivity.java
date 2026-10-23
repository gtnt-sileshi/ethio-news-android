package com.ethionews.app.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageView;
import android.app.ProgressDialog;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;

import com.ethionews.app.R;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.callback.Method;
import com.ethionews.app.executor.LoadProfile;
import com.ethionews.app.interfaces.ProfileListener;
import com.ethionews.app.utils.IfSupported;
import com.ethionews.app.utils.MediaPath;
import com.ethionews.app.utils.helper.Helper;
import com.ethionews.app.utils.helper.SPHelper;

public class ProfileEditActivity extends AppCompatActivity {

    private Helper helper;
    private SPHelper sharedPref;
    private EditText etName;
    private EditText etEmail;
    private EditText etPhone;
    private EditText etPass;
    private EditText etcPass;
    private ImageView profile;
    private String imagePath = "";
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IfSupported.applyTheme(this);
        setContentView(R.layout.activity_profile_edit);
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

        progressDialog = new ProgressDialog(ProfileEditActivity.this);

        helper = new Helper(this);
        sharedPref = new SPHelper(this);

        etName = findViewById(R.id.editText_profedit_name);
        etEmail = findViewById(R.id.editText_profedit_email);
        etPhone = findViewById(R.id.editText_profedit_phone);
        etPass = findViewById(R.id.editText_profedit_password);
        etcPass = findViewById(R.id.editText_profedit_cpassword);

        android.widget.LinearLayout adView = findViewById(R.id.ll_adView);
        helper.showBannerAd(adView, "");

        if(sharedPref.getLoginType().equals(Method.LOGIN_TYPE_NORMAL)) {
            etcPass.setEnabled(true);
            etPass.setEnabled(true);
        } else {
            etcPass.setEnabled(false);
            etPass.setEnabled(false);
        }

        profile = findViewById(R.id.iv_profile_edit);
        String profileUrl = sharedPref.getProfileImages();
        Picasso.get()
                .load(profileUrl.isEmpty() ? null : profileUrl)
                .placeholder(R.drawable.user_photo)
                .error(R.drawable.user_photo)
                .into(profile);

        findViewById(R.id.rl_profile_edit).setOnClickListener(v -> pickImage());
        findViewById(R.id.ll_update_btn).setOnClickListener(view -> {
            if (validate()) {
                loadUpdateProfile();
            }
        });
        setProfileVar();
    }

    public void setProfileVar() {
        etName.setText(sharedPref.getUserName());
        etPhone.setText(sharedPref.getUserMobile());
        etEmail.setText(sharedPref.getEmail());
    }

    private void pickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
            pickImageLauncher.launch(intent);
        } else {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            pickImageLauncher.launch(Intent.createChooser(intent, getString(R.string.select_image)));
        }
    }

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        profile.setImageBitmap(bitmap);

                        // Write a compressed JPEG to the cache dir — avoids scoped storage
                        // issues and keeps upload size small regardless of source image size.
                        File tempFile = File.createTempFile("profile_", ".jpg", getCacheDir());
                        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                        }
                        imagePath = tempFile.getAbsolutePath();
                        uploadImage();
                    } catch (IOException e) {
                        Toast.makeText(ProfileEditActivity.this, "Error picking image.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @NonNull
    private Boolean validate() {
        etName.setError(null);
        etEmail.setError(null);
        etcPass.setError(null);
        if (etName.getText().toString().trim().isEmpty()) {
            etName.setError(getString(R.string.err_cannot_empty));
            etName.requestFocus();
            return false;
        } else if (etEmail.getText().toString().trim().isEmpty()) {
            etEmail.setError(getString(R.string.err_password));
            etEmail.requestFocus();
            return false;
        } else if (etPass.getText().toString().endsWith(" ")) {
            etPass.setError(getString(R.string.err_pass_end_space));
            etPass.requestFocus();
            return false;
        } else if (!etPass.getText().toString().trim().equals(etcPass.getText().toString().trim())) {
            etcPass.setError(getString(R.string.err_password));
            etcPass.requestFocus();
            return false;
        } else {
            return true;
        }
    }

    private void loadUpdateProfile() {
        if (!helper.isNetworkAvailable()) {
            Toast.makeText(ProfileEditActivity.this, getString(R.string.err_internet_not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        LoadProfile loadProfileEdit = new LoadProfile(new ProfileListener() {
            @Override
            public void onStart() {
                progressDialog.show();
            }

            @Override
            public void onEnd(String success, String isApiSuccess, String message, String userID,
                              String userName, String email, String mobile, String gender, String profileImg) {
                dismissDialog();
                if (isFinishing()) return;
                if (!success.equals("1")) {
                    Toast.makeText(ProfileEditActivity.this, getString(R.string.err_server_not_connected), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (isApiSuccess.equals("1")) {
                    updateArray(userName, email, mobile, profileImg);
                    Callback.setIsProfileUpdate(true);
                    finish();
                    Toast.makeText(ProfileEditActivity.this, message, Toast.LENGTH_SHORT).show();
                } else {
                    if (message.contains("Email address already used")) {
                        etEmail.setError(message);
                        etEmail.requestFocus();
                    } else {
                        Toast.makeText(ProfileEditActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }, helper.getAPIRequest(Method.METHOD_EDIT_PROFILE, 0, "", "", "", "",
                sharedPref.getUserId(), etName.getText().toString(), etEmail.getText().toString(),
                etPhone.getText().toString(), "", etPass.getText().toString(), "", "", null));
        loadProfileEdit.execute();
    }

    private void updateArray(String userName, String email, String mobile, String profileImg) {
        sharedPref.setUserName(userName);
        sharedPref.setEmail(email);
        sharedPref.setUserMobile(mobile);
        if (!profileImg.isEmpty()) {
            sharedPref.setProfileImages(profileImg);
        }
        if (!etPass.getText().toString().isEmpty()) {
            sharedPref.setRemember(false);
        }
    }

    public void uploadImage() {
        if (!helper.isNetworkAvailable()) {
            Toast.makeText(ProfileEditActivity.this, getString(R.string.err_internet_not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        LoadProfile loadStatus = new LoadProfile(new ProfileListener() {
            @Override
            public void onStart() {
                progressDialog.show();
            }

            @Override
            public void onEnd(String success, String isApiSuccess, String message, String userID,
                              String userName, String email, String mobile, String gender, String profileImg) {
                dismissDialog();
                if (isFinishing()) return;
                if (!success.equals("1")) {
                    Toast.makeText(ProfileEditActivity.this, getString(R.string.err_server_not_connected), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!profileImg.isEmpty()) {
                    sharedPref.setProfileImages(profileImg);
                }
                Callback.setIsProfileUpdate(true);
                imagePath = "";
                Toast.makeText(ProfileEditActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        }, helper.getAPIRequest(Method.METHOD_USER_IMAGES_UPDATE, 0, "", "",
                "", "", sharedPref.getUserId(), "", "", "",
                "", "", "", "", new File(imagePath)));
        loadStatus.execute();
    }

    /** Safely dismiss the progress dialog from any thread. */
    private void dismissDialog() {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        } catch (Exception ignored) {}
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