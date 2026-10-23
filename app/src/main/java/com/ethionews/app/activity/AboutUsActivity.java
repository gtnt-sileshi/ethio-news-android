package com.ethionews.app.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

import com.ethionews.app.BuildConfig;
import com.ethionews.app.R;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.interfaces.InterAdListener;
import com.ethionews.app.utils.IfSupported;
import com.ethionews.app.utils.helper.Helper;

public class AboutUsActivity extends AppCompatActivity implements InterAdListener {

    private TextView author;
    private TextView email;
    private TextView website;
    private TextView contact;
    private TextView description;
    private TextView version;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IfSupported.applyTheme(this);
        setContentView(R.layout.activity_about_us);
        IfSupported.isRTL(this);
        IfSupported.isScreenshot(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(view -> finish());

        author = findViewById(R.id.tv_company);
        email = findViewById(R.id.tv_email);
        website = findViewById(R.id.tv_website);
        contact = findViewById(R.id.tv_contact);
        description = findViewById(R.id.tv_app_des);
        version = findViewById(R.id.tv_version);

        setAboutUs();
        setupButton();
    }

    private void setupButton() {
        Helper helper = new Helper(this, this);
        findViewById(R.id.ll_share).setOnClickListener(v -> helper.showInterAd(0, getResources().getString(R.string.share)));
        findViewById(R.id.ll_rate).setOnClickListener(v -> helper.showInterAd(0, getResources().getString(R.string.rate_the_app)));
        findViewById(R.id.ll_domain).setOnClickListener(v -> helper.showInterAd(0, getResources().getString(R.string.website)));
        findViewById(R.id.ll_contact).setOnClickListener(v -> helper.showInterAd(0, getResources().getString(R.string.contact)));
        findViewById(R.id.ll_email).setOnClickListener(v -> helper.showInterAd(0, getResources().getString(R.string.email)));
        findViewById(R.id.ll_more).setOnClickListener(v -> helper.showInterAd(0, getResources().getString(R.string.more_apps)));
    }

    private void setAboutUs() {
        if (Callback.getItemAbout() == null) {
            return;
        }
        author.setText(!Callback.getItemAbout().getAuthor().trim().isEmpty() ? Callback.getItemAbout().getAuthor() : "");
        email.setText(!Callback.getItemAbout().getEmail().trim().isEmpty() ? Callback.getItemAbout().getEmail() : "");
        website.setText(!Callback.getItemAbout().getWebsite().trim().isEmpty() ? Callback.getItemAbout().getWebsite() : "");
        contact.setText(!Callback.getItemAbout().getContact().trim().isEmpty() ? Callback.getItemAbout().getContact() : "");
        description.setText(!Callback.getItemAbout().getAppDesc().trim().isEmpty() ? Callback.getItemAbout().getAppDesc() : "");
        version.setText(BuildConfig.VERSION_NAME);
    }

    @Override
    public void onClick(int position, String type) {
        if (type != null && type.isEmpty()){
            return;
        }
        if (getResources().getString(R.string.share).equals(type)){
            final String appName = getPackageName();
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.app_name));
            sendIntent.putExtra(Intent.EXTRA_TEXT, "http://play.google.com/store/apps/details?id=" + appName);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, "Share"));
        }
        else if (getResources().getString(R.string.rate_the_app).equals(type)){
            final String appName = getPackageName();
            shareUrl("http://play.google.com/store/apps/details?id=" + appName);
        }
        else if (getResources().getString(R.string.website).equals(type)){
            shareUrl(Callback.getItemAbout().getWebsite());
        }
        else if (getResources().getString(R.string.contact).equals(type)){
            String contactData = Callback.getItemAbout().getContact(); // use country code with your phone number
            if (!contactData.isEmpty()){
                String url = "https://api.whatsapp.com/send?phone=" + contactData;
                try {
                    PackageManager pm = getPackageManager();
                    pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e("AboutUsActivity", "Failed to whatsapp", e);
                }
            }
        }
        else if (getResources().getString(R.string.email).equals(type)){
            String emailData = Callback.getItemAbout().getEmail();
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:")); // only email apps should handle this
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailData,});
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            intent.putExtra(Intent.EXTRA_TEXT, "note");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        }
        else if (getResources().getString(R.string.more_apps).equals(type)){
            shareUrl(Callback.getItemAbout().getMoreApps());
        }
    }

    private void shareUrl(String webURL) {
        if (webURL == null){
            Toast.makeText(AboutUsActivity.this, "Invalid URL", Toast.LENGTH_SHORT).show();
            return;
        }
        if (webURL.contains("http://") || webURL.contains("https://")){
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webURL)));
        } else if (!webURL.isEmpty()){
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://"+webURL)));
        } else {
            Toast.makeText(AboutUsActivity.this, "Invalid URL", Toast.LENGTH_SHORT).show();
        }
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