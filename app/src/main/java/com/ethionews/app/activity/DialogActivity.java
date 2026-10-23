package com.ethionews.app.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;


import java.util.Objects;

import com.ethionews.app.R;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.dialog.DialogUtil;
import com.ethionews.app.utils.IfSupported;

public class DialogActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IfSupported.applyTheme(this);
        setContentView(R.layout.activity_launcher);
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(R.attr.ns_bg, tv, true);
        findViewById(R.id.rl_splash).setBackgroundColor(tv.data);

        String from = getIntent().getStringExtra("from");
        if (from == null){
            openMainActivity();
        } else if (from.equals(Callback.DIALOG_TYPE_UPDATE)){
            DialogUtil.upgradeDialog(this, this::openMainActivity);
        } else if (from.equals(Callback.DIALOG_TYPE_MAINTENANCE)){
            DialogUtil.maintenanceDialog(this);
        } else if (from.equals(Callback.DIALOG_TYPE_DEVELOPER)){
            DialogUtil.dModeDialog(this);
        } else if (from.equals(Callback.DIALOG_TYPE_VPN)){
            DialogUtil.vpnDialog(this);
        } else {
            openMainActivity();
        }
    }

    private void openMainActivity() {
        Intent intent = new Intent(DialogActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}