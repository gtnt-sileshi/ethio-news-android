package com.ethionews.app.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import com.ethionews.app.BuildConfig;
import com.ethionews.app.R;
import com.ethionews.app.adapter.PlanAdapter;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.callback.Method;
import com.ethionews.app.dialog.DialogUtil;
import com.ethionews.app.item.ItemPlan;
import com.ethionews.app.utils.ApplicationUtil;
import com.ethionews.app.utils.AsyncTaskExecutor;
import com.ethionews.app.utils.IfSupported;
import com.ethionews.app.utils.helper.Helper;
import com.ethionews.app.utils.helper.SPHelper;
import okhttp3.RequestBody;

public class BillingSubscribeActivity extends AppCompatActivity {

    private Helper helper;
    private SPHelper spHelper;
    private ProgressBar pb;
    private RecyclerView rv;
    private TextView proceed;
    private String errorMsg;
    private FrameLayout frameLayout;
    private ArrayList<ItemPlan> mListItem;
    private int selectedPlan = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IfSupported.applyTheme(this);
        setContentView(R.layout.activity_billing_subscribe);
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
        spHelper = new SPHelper(this);

        mListItem = new ArrayList<>();
        pb = findViewById(R.id.pb);
        frameLayout = findViewById(R.id.fl_empty);
        proceed = findViewById(R.id.tv_btn_proceed);

        rv = findViewById(R.id.rv_plan);
        rv.setHasFixedSize(true);
        rv.setLayoutManager(new LinearLayoutManager(BillingSubscribeActivity.this, LinearLayoutManager.VERTICAL, false));
        rv.setFocusable(false);
        rv.setNestedScrollingEnabled(false);

        findViewById(R.id.tv_terms).setOnClickListener(view -> openWebActivity());

        getPlan();
    }

    private void openWebActivity() {
        Intent intent1 = new Intent(BillingSubscribeActivity.this, WebActivity.class);
        intent1.putExtra("web_url", BuildConfig.BASE_URL+"data.php?terms");
        intent1.putExtra("page_title", getResources().getString(R.string.terms_and_conditions));
        startActivity(intent1);
    }

    private void getPlan() {
        if (!helper.isNetworkAvailable()) {
            errorMsg = getString(R.string.err_internet_not_connected);
            setEmpty();
            return;
        }

        LoadPlan loadPlan = new LoadPlan(new PlanListener() {
            @Override
            public void onStart() {
                pb.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
                frameLayout.setVisibility(View.GONE);
            }

            @Override
            public void onEnd(String success, boolean verifyStatus, String message,
                              ArrayList<ItemPlan> itemPlans) {
                if (isFinishing()){
                    return;
                }
                if (success.equals("1")) {
                    if (verifyStatus) {
                        rv.setVisibility(View.VISIBLE);
                        pb.setVisibility(View.GONE);
                        mListItem.addAll(itemPlans);
                        displayData();
                    } else {
                        DialogUtil.verifyDialog(BillingSubscribeActivity.this, getString(R.string.err_unauthorized_access), message, () -> {
                        });
                    }
                } else {
                    errorMsg = getString(R.string.err_server_not_connected);
                    setEmpty();
                }
            }
        },helper.getAPIRequest(Method.METHOD_PLAN, 0, "", "", "",
                "", "", "", "", "", "", "",
                "", "", null));
        loadPlan.execute();
    }

    @SuppressLint("SetTextI18n")
    private void displayData() {
        PlanAdapter adapter = new PlanAdapter(BillingSubscribeActivity.this, mListItem);
        rv.setAdapter(adapter);
        adapter.select(-1);

        adapter.setOnItemClickListener(position -> {
            selectedPlan = position;
            adapter.select(position);
            proceed.setText("Try for "+ mListItem.get(position).getPlanPrice()+" "
                    +mListItem.get(position).getPlanCurrencyCode());
        });

        proceed.setOnClickListener(view -> {
            if(spHelper.isLogged()) {
                if (Boolean.FALSE.equals(spHelper.getIsSubscribed())){
                    if (selectedPlan != -1){
                        Intent intent = getIntentBillingConnector();
                        startActivity(intent);
                    } else {
                        proceed.setText("no selected");
                    }
                } else {
                    Toast.makeText(BillingSubscribeActivity.this, "Item already subscribed", Toast.LENGTH_SHORT).show();
                }
            } else {
                helper.clickLogin();
            }
        });
    }

    @NonNull
    private Intent getIntentBillingConnector() {
        ItemPlan itemPlan = mListItem.get(selectedPlan);
        Intent intent = new Intent(BillingSubscribeActivity.this, BillingConnectorActivity.class);
        intent.putExtra("planId", itemPlan.getPlanId());
        intent.putExtra("planName", itemPlan.getPlanName());
        intent.putExtra("planPrice", itemPlan.getPlanPrice());
        intent.putExtra("planDuration", itemPlan.getPlanDuration());
        intent.putExtra("planCurrencyCode", itemPlan.getPlanCurrencyCode());
        intent.putExtra("subscription_id", itemPlan.getSubscriptionID());
        intent.putExtra("base_key", itemPlan.getBaseKey());
        return intent;
    }

    private static class LoadPlan extends AsyncTaskExecutor<String, String, String> {

        private final RequestBody requestBody;
        private final PlanListener planListener;
        private final ArrayList<ItemPlan> arrayList = new ArrayList<>();
        private String message = "";
        private boolean verifyStatus = true;

        public LoadPlan(PlanListener planListener, RequestBody requestBody) {
            this.planListener = planListener;
            this.requestBody = requestBody;
        }

        @Override
        protected void onPreExecute() {
            planListener.onStart();
            super.onPreExecute();
        }

        @NonNull
        @Override
        protected String doInBackground(String strings) {
            String json = ApplicationUtil.responsePost(Callback.API_URL, requestBody);
            try {
                JSONObject jOb = new JSONObject(json);
                JSONArray jsonArray = jOb.getJSONArray(Callback.TAG_ROOT);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject objJson = jsonArray.getJSONObject(i);

                    if (!objJson.has(Callback.TAG_SUCCESS)) {

                        String id = objJson.getString("id");
                        String planName = objJson.getString("plan_name");
                        String planDuration = objJson.getString("plan_duration");
                        String planPrice = objJson.getString("plan_price");
                        String currencyCode = objJson.getString("currency_code");
                        String subscriptionID = objJson.getString("subscription_id");
                        String baseKey = objJson.getString("base_key");

                        ItemPlan item = new ItemPlan(id, planName, planDuration, planPrice,
                                currencyCode, subscriptionID, baseKey);
                        arrayList.add(item);
                    } else {
                        verifyStatus = objJson.getBoolean(Callback.TAG_SUCCESS);
                        message = objJson.getString(Callback.TAG_MSG);
                    }
                }
                return "1";
            } catch (Exception ee) {
                return "0";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            planListener.onEnd(s, verifyStatus, message, arrayList);
        }
    }

    private void setEmpty() {
        pb.setVisibility(View.GONE);
        rv.setVisibility(View.GONE);
        frameLayout.setVisibility(View.VISIBLE);

        frameLayout.removeAllViews();
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        @SuppressLint("InflateParams") View myView = inflater.inflate(R.layout.row_empty, null);

        TextView textView = myView.findViewById(R.id.tv_empty_msg);
        textView.setText(errorMsg);

        myView.findViewById(R.id.ll_empty_try).setOnClickListener(v -> getPlan());

        frameLayout.addView(myView);
    }

    private interface PlanListener {
        void onStart();
        void onEnd(String success, boolean verifyStatus, String message, ArrayList<ItemPlan> itemPlans);
    }
}