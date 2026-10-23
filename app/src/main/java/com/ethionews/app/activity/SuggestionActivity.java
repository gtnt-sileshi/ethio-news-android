package com.ethionews.app.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;

import java.io.File;
import java.io.IOException;

import com.ethionews.app.R;
import com.ethionews.app.callback.Method;
import com.ethionews.app.executor.LoadStatus;
import com.ethionews.app.interfaces.SuccessListener;
import com.ethionews.app.utils.IfSupported;
import com.ethionews.app.utils.MediaPath;
import com.ethionews.app.utils.helper.Helper;
import com.ethionews.app.utils.helper.SPHelper;

public class SuggestionActivity extends AppCompatActivity {

    private Helper helper;
    private SPHelper spHelper;
    private TextView title;
    private TextView desc;
    private ImageView suggest;
    private String imagePath = "";
    private Bitmap bitmap;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        IfSupported.applyTheme(this);
        setContentView(R.layout.activity_suggestion);
        IfSupported.isRTL(this);
        IfSupported.isScreenshot(this);

        helper = new Helper(this);
        spHelper = new SPHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            toolbar.setNavigationOnClickListener(view -> finish());
        }

        progressDialog = new ProgressDialog(SuggestionActivity.this);

        suggest = findViewById(R.id.iv_sugg);
        desc = findViewById(R.id.et_description);
        title = findViewById(R.id.et_title);

        findViewById(R.id.ll_sugg).setOnClickListener(v -> pickImage());

        findViewById(R.id.btn_sugg_submit).setOnClickListener(v -> {
            if(title.getText().toString().isEmpty()) {
                Toast.makeText(SuggestionActivity.this, getString(R.string.enter_your_title_here_suggestion), Toast.LENGTH_SHORT).show();
            } else if(desc.getText().toString().isEmpty()) {
                Toast.makeText(SuggestionActivity.this, getString(R.string.enter_your_description_here_suggestion), Toast.LENGTH_SHORT).show();
            } else {
                if(spHelper.isLogged()) {
                    loadSuggestion();
                } else {
                    helper.clickLogin();
                }
            }
        });

        LinearLayout adView = findViewById(R.id.ll_adView);
        helper.showBannerAd(adView,"");
    }

    public void loadSuggestion() {
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
                    imagePath = "";
                    bitmap = null;
                    title.setText("");
                    desc.setText("");
                    suggest.setImageDrawable(ContextCompat.getDrawable(SuggestionActivity.this,R.drawable.logo));
                    uploadDialog(message);
                } else {
                    Toast.makeText(SuggestionActivity.this, getString(R.string.err_server_not_connected), Toast.LENGTH_SHORT).show();
                }
            }
        }, helper.getAPIRequest(Method.METHOD_SUGGESTION, 0, "", "",
                title.getText().toString(),desc.getText().toString(),
                spHelper.getUserId(), "", "", "", "", "",
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

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        suggest.setImageBitmap(bitmap);
                        File tempFile = File.createTempFile("sugg_", ".jpg", getCacheDir());
                        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                        }
                        imagePath = tempFile.getAbsolutePath();
                    } catch (IOException e) {
                        Toast.makeText(SuggestionActivity.this, "Error pick Image.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void uploadDialog(String message) {
        AlertDialog.Builder alertDialog;
        alertDialog = new AlertDialog.Builder(SuggestionActivity.this, R.style.dialogTheme);
        alertDialog.setTitle(getString(R.string.upload_success));
        alertDialog.setMessage(message);
        alertDialog.setNegativeButton(getString(R.string.ok), (dialog, which) -> {
        });
        alertDialog.show();
    }
}