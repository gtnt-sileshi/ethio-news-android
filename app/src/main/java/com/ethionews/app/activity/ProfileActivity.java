package com.ethionews.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import com.ethionews.app.BuildConfig;
import com.ethionews.app.R;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.callback.Method;
import com.ethionews.app.dialog.DialogUtil;
import com.ethionews.app.executor.LoadProfile;
import com.ethionews.app.executor.LoadStatus;
import com.ethionews.app.interfaces.ProfileListener;
import com.ethionews.app.interfaces.SuccessListener;
import com.ethionews.app.utils.IfSupported;
import com.ethionews.app.utils.helper.Helper;
import com.ethionews.app.utils.helper.SPHelper;

public class ProfileActivity extends AppCompatActivity {

    private Helper helper;
    private SPHelper sharedPref;
    private TextView name;
    private TextView email;
    private ImageView profile;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IfSupported.applyTheme(this);
        setContentView(R.layout.activity_profile);
        IfSupported.isRTL(this);
        IfSupported.isScreenshot(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(view -> finish());

        helper = new Helper(this);
        sharedPref = new SPHelper(this);

        progressDialog = new ProgressDialog(ProfileActivity.this);

        name = findViewById(R.id.tv_profile_name);
        email = findViewById(R.id.tv_profile_email);
        profile = findViewById(R.id.iv_profile);

        if (sharedPref.isLogged() && !sharedPref.getUserId().isEmpty()) {
            loadUserProfile();
        } else {
            helper.clickLogin();
        }

        setupButton();

        LinearLayout adView = findViewById(R.id.ll_adView);
        helper.showBannerAd(adView,"");
    }

    private void setupButton() {
        // Edit profile banner card AND avatar circle both open edit profile
        findViewById(R.id.rl_profile).setOnClickListener(view ->
                startActivity(new Intent(ProfileActivity.this, ProfileEditActivity.class)));
        View editBanner = findViewById(R.id.rl_edit_banner);
        if (editBanner != null) {
            editBanner.setOnClickListener(view ->
                    startActivity(new Intent(ProfileActivity.this, ProfileEditActivity.class)));
        }
        findViewById(R.id.iv_notifications).setOnClickListener(view ->
                startActivity(new Intent(ProfileActivity.this, NotificationActivity.class)));
        findViewById(R.id.ll_policy).setOnClickListener(view -> {
            Intent intent = new Intent(ProfileActivity.this, WebActivity.class);
            intent.putExtra("web_url", BuildConfig.BASE_URL+"data.php?privacy_policy");
            intent.putExtra("page_title", getResources().getString(R.string.privacy_policy));
            ActivityCompat.startActivity(ProfileActivity.this, intent, null);
        });
        findViewById(R.id.ll_terms).setOnClickListener(view -> {
            Intent intent = new Intent(ProfileActivity.this, WebActivity.class);
            intent.putExtra("web_url", BuildConfig.BASE_URL+"data.php?terms");
            intent.putExtra("page_title", getResources().getString(R.string.terms_and_conditions));
            ActivityCompat.startActivity(ProfileActivity.this, intent, null);
        });
        findViewById(R.id.ll_trash).setOnClickListener(view -> DialogUtil.trashDialog(this, new DialogUtil.DeleteListener() {
            @Override
            public void onDelete() {
                loadDelete();
            }

            @Override
            public void onCancel() {
                // no data
            }
        }));
        findViewById(R.id.ll_logout).setOnClickListener(view -> helper.clickLogin());
    }

    public void loadDelete() {
        if (helper.isNetworkAvailable()) {
            LoadStatus loadStatus = new LoadStatus(new SuccessListener() {
                @Override
                public void onStart() {
                    progressDialog.show();
                }

                @Override
                public void onEnd(String success, String registerSuccess, String message) {
                    dismissDialog();
                    if (isFinishing()) return;
                    if (success.equals("1")) {
                        helper.clickLogin();
                    } else {
                        Toast.makeText(ProfileActivity.this, getString(R.string.err_server_not_connected), Toast.LENGTH_SHORT).show();
                    }
                }
            }, helper.getAPIRequest(Method.METHOD_ACCOUNT_DELETE, 0, "", "", "", "",
                    sharedPref.getUserId(), "", "", "", "", "", "", "", null));
            loadStatus.execute();
        } else {
            Toast.makeText(ProfileActivity.this, getString(R.string.err_internet_not_connected), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserProfile() {
        if (!helper.isNetworkAvailable()) {
            Toast.makeText(ProfileActivity.this, getString(R.string.err_internet_not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        LoadProfile loadProfile = new LoadProfile(new ProfileListener() {
            @Override
            public void onStart() {
                progressDialog.show();
            }

            @Override
            public void onEnd(String success, String isApiSuccess, String message, String userID,
                              String userName, String email, String mobile, String gender, String profile) {
                dismissDialog();
                if (isFinishing()) return;
                if (success.equals("1")) {
                    if (isApiSuccess.equals("1")) {
                        sharedPref.setUserName(userName);
                        sharedPref.setEmail(email);
                        sharedPref.setUserMobile(mobile);
                        sharedPref.setProfileImages(profile);
                        setVariables();
                    } else {
                        helper.logout(ProfileActivity.this, sharedPref);
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, getString(R.string.err_server_not_connected), Toast.LENGTH_SHORT).show();
                }
            }
        }, helper.getAPIRequest(Method.METHOD_PROFILE, 0, "", "", "", "",
                sharedPref.getUserId(), "", "", "", "", "", "", "", null));
        loadProfile.execute();
    }

    /** Safely dismiss the progress dialog from any thread. */
    private void dismissDialog() {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        } catch (Exception ignored) {}
    }

    public void setVariables() {
        name.setText(sharedPref.getUserName());
        email.setText(sharedPref.getEmail());
        if (!sharedPref.getProfileImages().isEmpty()){
            findViewById(R.id.pb_iv_profile).setVisibility(View.VISIBLE);
            Picasso.get()
                    .load(sharedPref.getProfileImages())
                    .placeholder(R.drawable.user_photo)
                    .error(R.drawable.user_photo)
                    .into(profile, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            findViewById(R.id.pb_iv_profile).setVisibility(View.GONE);
                        }

                        @Override
                        public void onError(Exception e) {
                            findViewById(R.id.pb_iv_profile).setVisibility(View.GONE);
                        }
                    });
        }
    }

    @Override
    public void onResume() {
        if (Boolean.TRUE.equals(Callback.getIsProfileUpdate())) {
            Callback.setIsProfileUpdate(false);
            loadUserProfile();
        }
        super.onResume();
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