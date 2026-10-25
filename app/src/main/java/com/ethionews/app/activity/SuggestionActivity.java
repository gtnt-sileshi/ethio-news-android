package com.ethionews.app.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;

import com.ethionews.app.R;
import com.ethionews.app.callback.Method;
import com.ethionews.app.executor.LoadStatus;
import com.ethionews.app.interfaces.SuccessListener;
import com.ethionews.app.utils.IfSupported;
import com.ethionews.app.utils.helper.Helper;

import android.provider.Settings;

public class SuggestionActivity extends AppCompatActivity {

    private Helper helper;
    private String deviceId;
    private TextInputEditText etTitle;
    private TextInputEditText etDesc;
    private ImageView ivSugg;
    private LinearLayout llPlaceholder;
    private String imagePath = "";
    private Bitmap bitmap;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IfSupported.applyTheme(this);
        setContentView(R.layout.activity_suggestion);
        IfSupported.isRTL(this);
        IfSupported.isScreenshot(this);

        helper   = new Helper(this);
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        progressDialog = new ProgressDialog(this);

        etTitle      = findViewById(R.id.et_title);
        etDesc       = findViewById(R.id.et_description);
        ivSugg       = findViewById(R.id.iv_sugg);
        llPlaceholder = findViewById(R.id.ll_sugg_placeholder);

        // Tap anywhere on the image picker area
        findViewById(R.id.ll_sugg).setOnClickListener(v -> pickImage());

        MaterialButton btnSubmit = findViewById(R.id.btn_sugg_submit);
        btnSubmit.setOnClickListener(v -> {
            String titleText = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String descText  = etDesc.getText()  != null ? etDesc.getText().toString().trim()  : "";
            if (titleText.isEmpty()) {
                etTitle.setError(getString(R.string.enter_your_title_here_suggestion));
                etTitle.requestFocus();
            } else if (descText.isEmpty()) {
                etDesc.setError(getString(R.string.enter_your_description_here_suggestion));
                etDesc.requestFocus();
            } else {
                loadSuggestion(titleText, descText);
            }
        });

        LinearLayout adView = findViewById(R.id.ll_adView);
        helper.showBannerAd(adView, "");
    }

    private void loadSuggestion(String titleText, String descText) {
        if (!helper.isNetworkAvailable()) {
            Toast.makeText(this, getString(R.string.err_internet_not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        LoadStatus loadSuggestion = new LoadStatus(new SuccessListener() {
            @Override
            public void onStart() {
                progressDialog.show();
            }

            @Override
            public void onEnd(String success, String registerSuccess, String message) {
                progressDialog.dismiss();
                if (isFinishing()) return;
                if (success.equals("1")) {
                    // Reset form
                    etTitle.setText("");
                    etDesc.setText("");
                    imagePath = "";
                    bitmap    = null;
                    ivSugg.setVisibility(View.GONE);
                    llPlaceholder.setVisibility(View.VISIBLE);
                    showSuccessDialog(message);
                } else {
                    Toast.makeText(SuggestionActivity.this,
                            getString(R.string.err_server_not_connected), Toast.LENGTH_SHORT).show();
                }
            }
        }, helper.getAPIRequest(Method.METHOD_SUGGESTION, 0, "", "",
                titleText, descText,
                deviceId, "", "", "", "", "",
                "", "", imagePath.isEmpty() ? null : new File(imagePath)));
        loadSuggestion.execute();
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

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        ivSugg.setImageBitmap(bitmap);
                        ivSugg.setVisibility(View.VISIBLE);
                        llPlaceholder.setVisibility(View.GONE);

                        File tempFile = File.createTempFile("sugg_", ".jpg", getCacheDir());
                        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                        }
                        imagePath = tempFile.getAbsolutePath();
                    } catch (IOException e) {
                        Toast.makeText(this, getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void showSuccessDialog(String message) {
        new AlertDialog.Builder(this, R.style.dialogTheme)
                .setTitle(getString(R.string.upload_success))
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), (d, w) -> d.dismiss())
                .show();
    }
}
