package com.ethionews.app.utils.purchases;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.QueryPurchasesParams;

import org.jetbrains.annotations.Contract;

public class BillingUpdate {

    private static final String TAG = "BillingUpdate";

    private final BillingClient billingClient;
    private final Listener listener;

    private boolean shouldEnableLogging = false;

    public BillingUpdate(Context context, Listener billingListener) {
        this.listener = billingListener;
        this.billingClient = BillingClient.newBuilder(context)
                .setListener((billingResult, list) -> {
                    if (list == null){
                        Log("No purchases found");
                        return;
                    }
                    Log("Purchase updated: " + list.size());
                })
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();
        startConnection();
    }

    private void startConnection() {
        if (billingClient == null){
            findUiHandler().post(listener::onBillingServiceDisconnected);
            return;
        }

        if (billingClient.isReady()) {
            checkIfSubscribed();
            return;
        }

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {
                Log("Disconnected from billing service");
                findUiHandler().post(listener::onBillingServiceDisconnected);
            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                    Log("Billing service connected and ready");
                    checkIfSubscribed();
                } else {
                    Log("Billing service: error");
                    findUiHandler().post(listener::onBillingServiceDisconnected);
                }
            }
        });
    }

    private void checkIfSubscribed() {
        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build(), (billingResult, purchases) -> {
                    int isAcknowledged = 0;
                    Log("purchases: " + purchases.size());
                    if(!purchases.isEmpty()) {
                        for (int i = 0; i < purchases.size(); i++) {
                            Log( "purchase: " + purchases.get(i).toString());
                            isAcknowledged++;
                        }
                    }
                    boolean isSubscribed = isAcknowledged > 0;
                    findUiHandler().post(() -> listener.onBillingSetupFinished(isSubscribed));
                }
        );
    }

    @NonNull
    @Contract(" -> new")
    private Handler findUiHandler() {
        return new Handler(Looper.getMainLooper());
    }

    private void Log(String debugMessage) {
        if (shouldEnableLogging) {
            Log.d(TAG, debugMessage);
        }
    }

    public void resume() {
        Log("BillingUpdate resume");
        startConnection();
    }

    public void release() {
        if (billingClient != null && billingClient.isReady()) {
            Log("BillingUpdate instance release: ending connection...");
            billingClient.endConnection();
        }
    }

    public interface Listener {
        void onBillingServiceDisconnected();
        void onBillingSetupFinished(boolean isSubscribed);
    }
}