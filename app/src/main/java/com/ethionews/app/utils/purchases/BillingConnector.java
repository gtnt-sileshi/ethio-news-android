package com.ethionews.app.utils.purchases;

import static com.ethionews.app.utils.purchases.enums.SkuProductType.NON_CONSUMABLE;
import static com.ethionews.app.utils.purchases.enums.SkuProductType.SUBSCRIPTION;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ethionews.app.utils.purchases.enums.ErrorType;
import com.ethionews.app.utils.purchases.enums.ProductType;
import com.ethionews.app.utils.purchases.enums.PurchasedResult;
import com.ethionews.app.utils.purchases.enums.SkuProductType;
import com.ethionews.app.utils.purchases.enums.SupportState;
import com.ethionews.app.utils.purchases.models.BillingResponse;
import com.ethionews.app.utils.purchases.models.ProductInfo;
import com.ethionews.app.utils.purchases.models.PurchaseInfo;

@SuppressLint("NewApi")
public class BillingConnector {

    private static final String TAG = "BillingConnector";
    private static final int DEFAULT_RESPONSE_CODE = 99;

    private static final long RECONNECT_TIMER_START_MILLISECONDS = 1000L;
    private static final long RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L;
    private long reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS;

    private final String base64Key;

    private BillingClient billingClient;
    private BillingEventListener billingEventListener;

    private String subscriptionId;

    private final List<QueryProductDetailsParams.Product> allProductList = new ArrayList<>();

    private final List<ProductInfo> fetchedProductInfoList = new ArrayList<>();
    private final List<PurchaseInfo> purchasedProductsList = new ArrayList<>();

    private boolean shouldAutoAcknowledge = false;
    private boolean shouldAutoConsume = false;
    private boolean shouldEnableLogging = false;

    private boolean isConnected = false;
    private boolean fetchedPurchasedProducts = false;

    /**
     * BillingConnector public constructor
     *
     * @param context   - is the application context
     * @param base64Key - is the public developer key from Play Console
     */
    public BillingConnector(Context context, String base64Key) {
        this.init(context);
        this.base64Key = base64Key;
    }

    /**
     * To initialize BillingConnector
     */
    private void init(Context context) {
        billingClient = BillingClient.newBuilder(context)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .setListener((billingResult, purchases) -> {
                    switch (billingResult.getResponseCode()) {
                        case BillingClient.BillingResponseCode.OK -> {
                            if (purchases != null) {
                                processPurchases(ProductType.COMBINED, purchases, false);
                            } else {
                                Log("No purchases found despite OK response.");
                            }
                        }
                        case BillingClient.BillingResponseCode.USER_CANCELED -> {
                            Log("User pressed back or canceled a dialog." + " Response code: " + billingResult.getResponseCode());
                            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                                    new BillingResponse(ErrorType.USER_CANCELED, billingResult)));
                        }
                        case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                            Log("Network connection is down." + " Response code: " + billingResult.getResponseCode());
                            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                                    new BillingResponse(ErrorType.SERVICE_UNAVAILABLE, billingResult)));
                        }
                        case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                            Log("Billing API version is not supported for the type requested." + " Response code: " + billingResult.getResponseCode());
                            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                                    new BillingResponse(ErrorType.BILLING_UNAVAILABLE, billingResult)));
                        }
                        case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> {
                            Log("Requested product is not available for purchase." + " Response code: " + billingResult.getResponseCode());
                            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                                    new BillingResponse(ErrorType.ITEM_UNAVAILABLE, billingResult)));
                        }
                        case BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                            Log("Invalid arguments provided to the API." + " Response code: " + billingResult.getResponseCode());
                            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                                    new BillingResponse(ErrorType.DEVELOPER_ERROR, billingResult)));
                        }
                        case BillingClient.BillingResponseCode.ERROR -> {
                            Log("Fatal error during the API action." + " Response code: " + billingResult.getResponseCode());
                            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                                    new BillingResponse(ErrorType.ERROR, billingResult)));
                        }
                        case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                            Log("Failure to purchase since item is already owned." + " Response code: " + billingResult.getResponseCode());
                            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                                    new BillingResponse(ErrorType.ITEM_ALREADY_OWNED, billingResult)));
                        }
                        case BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                            Log("Failure to consume since item is not owned." + " Response code: " + billingResult.getResponseCode());
                            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                                    new BillingResponse(ErrorType.ITEM_NOT_OWNED, billingResult)));
                        }
                        case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, BillingClient.BillingResponseCode.SERVICE_TIMEOUT ->
                                Log("Initialization error: service disconnected/timeout. Trying to reconnect...");
                        default ->
                                Log("Initialization error: " + new BillingResponse(ErrorType.BILLING_ERROR, billingResult));
                    }
                })
                .build();
    }

    /**
     * To attach an event listener to establish a bridge with the caller
     */
    public final void setBillingEventListener(BillingEventListener billingEventListener) {
        this.billingEventListener = billingEventListener;
    }

    /**
     * To set subscription products ids
     */
    public final BillingConnector setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
        return this;
    }

    /**
     * To auto acknowledge the purchase
     */
    public final BillingConnector autoAcknowledge() {
        shouldAutoAcknowledge = true;
        return this;
    }

    /**
     * To auto consume the purchase
     */
    public final BillingConnector autoConsume() {
        shouldAutoConsume = true;
        return this;
    }

    /**
     * To enable logging for debugging
     */
    public final BillingConnector enableLogging() {
        shouldEnableLogging = true;
        return this;
    }

    /**
     * Returns the state of the billing client
     */
    public final boolean isReady() {
        if (!isConnected) {
            Log("Billing client is not ready because no connection is established yet");
        }

        if (!billingClient.isReady()) {
            Log("Billing client is not ready yet");
        }

        return isConnected && billingClient.isReady() && !fetchedProductInfoList.isEmpty();
    }

    /**
     * Returns a boolean state of the product
     *
     * @param productId - is the product id that has to be checked
     */
    private boolean checkProductBeforeInteraction(String productId) {
        if (!isReady()) {
            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.CLIENT_NOT_READY,
                    "Client is not ready yet", DEFAULT_RESPONSE_CODE)));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (productId != null && fetchedProductInfoList.stream().noneMatch(it -> it.getProduct().equals(productId))) {
                findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.PRODUCT_NOT_EXIST,
                        "The product id: " + productId + " doesn't seem to exist on Play Console", DEFAULT_RESPONSE_CODE)));
            } else {
                return isReady();
            }
        }
        return false;
    }

    /**
     * To connect the billing client with Play Console
     */
    public final BillingConnector connect() {
        List<QueryProductDetailsParams.Product> productSubsList = new ArrayList<>();

        if (subscriptionId == null || subscriptionId.isEmpty()) {
            subscriptionId = null;
        } else {
            productSubsList.add(QueryProductDetailsParams.Product.newBuilder().setProductId(subscriptionId).setProductType(BillingClient.ProductType.SUBS).build());
        }

        allProductList.addAll(productSubsList);

        //check if any list is provided
        if (allProductList.isEmpty()) {
            throw new IllegalArgumentException("At least one list of consumables, non-consumables or subscriptions is needed");
        }

        //check for duplicates product ids
        int allIdsSize = allProductList.size();
        int allIdsSizeDistinct = (int) allProductList.stream().distinct().count();
        if (allIdsSize != allIdsSizeDistinct) {
            throw new IllegalArgumentException("The product id must appear only once in a list. Also, it must not be in different lists");
        }

        Log("Billing service: connecting...");
        if (!billingClient.isReady()) {
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingServiceDisconnected() {
                    isConnected = false;

                    findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.CLIENT_DISCONNECTED,
                            "Billing service: disconnected", DEFAULT_RESPONSE_CODE)));

                    Log("Billing service: Trying to reconnect...");
                    retryBillingClientConnection();
                }

                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    isConnected = false;
                    switch (billingResult.getResponseCode()) {
                        case BillingClient.BillingResponseCode.OK:
                            isConnected = true;
                            Log("Billing service: connected");

                            //query subscription product details
                            if (subscriptionId != null) {
                                queryProductDetails(productSubsList);
                            }
                            break;
                        case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                            Log("Billing service: unavailable");
                            retryBillingClientConnection();
                            break;
                        default:
                            Log("Billing service: error");
                            retryBillingClientConnection();
                            break;
                    }
                }
            });
        }

        return this;
    }

    /**
     * Retries the billing client connection with exponential backoff
     * Max out at the time specified by RECONNECT_TIMER_MAX_TIME_MILLISECONDS (15 minutes)
     */
    private void retryBillingClientConnection() {
        findUiHandler().postDelayed(this::connect, reconnectMilliseconds);
        reconnectMilliseconds = Math.min(reconnectMilliseconds * 2, RECONNECT_TIMER_MAX_TIME_MILLISECONDS);
    }

    /**
     * Fires a query in Play Console to show products available to purchase
     */
    private void queryProductDetails(List<QueryProductDetailsParams.Product> productList) {
        QueryProductDetailsParams productDetailsParams = QueryProductDetailsParams.newBuilder().setProductList(productList).build();
        billingClient.queryProductDetailsAsync(productDetailsParams, (billingResult, productDetailsResult) -> {
            List<ProductDetails> productDetailsList = productDetailsResult.getProductDetailsList();
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                if (productDetailsList.isEmpty()) {
                    Log("Query Product Details: data not found. Make sure product ids are configured on Play Console");

                    findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.BILLING_ERROR,
                            "No product found", DEFAULT_RESPONSE_CODE)));
                } else {
                    Log("Query Product Details: data found");

                    List<ProductInfo> fetchedProductInfo = productDetailsList.stream().map(this::generateProductInfo).collect(Collectors.toList());
                    fetchedProductInfoList.addAll(fetchedProductInfo);

                    findUiHandler().post(() -> billingEventListener.onProductsFetched(fetchedProductInfo));

                    fetchPurchasedProducts();
                }
            } else {
                Log("Query Product Details: failed");
                findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.BILLING_ERROR, billingResult)));
            }
        });
    }

    /**
     * Returns a new ProductInfo object containing the product type and product details
     *
     * @param productDetails - is the object provided by the billing client API
     */
    @NonNull
    @Contract("_ -> new")
    private ProductInfo generateProductInfo(@NonNull ProductDetails productDetails) {
        SkuProductType skuProductType;
        if (productDetails.getProductType().equals(BillingClient.ProductType.SUBS)) {
            skuProductType = SUBSCRIPTION;
        } else {
            throw new IllegalStateException("Product type is not implemented correctly");
        }
        return new ProductInfo(skuProductType, productDetails);
    }

    /**
     * Returns purchases details for currently owned items without a network request
     */
    private void fetchPurchasedProducts() {
        if (!billingClient.isReady()) {
            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.FETCH_PURCHASED_PRODUCTS_ERROR,
                    "Billing client is not ready yet", DEFAULT_RESPONSE_CODE)));
            return;
        }

        //query subscription purchases for supported devices
        if (isSubscriptionSupported() == SupportState.SUPPORTED) {
            billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                    (billingResult, purchases) -> {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            if (purchases.isEmpty()) {
                                Log("Query SUBS Purchases: the list is empty");
                            } else {
                                Log("Query SUBS Purchases: data found and progress");
                            }

                            processPurchases(ProductType.SUBS, purchases, true);
                        } else {
                            Log("Query SUBS Purchases: failed");
                        }
                    }
            );
        }
    }

    /**
     * Before using subscriptions, device-support must be checked
     * Not all devices support subscriptions
     */
    public SupportState isSubscriptionSupported() {
        BillingResult response = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS);
        switch (response.getResponseCode()) {
            case BillingClient.BillingResponseCode.OK -> {
                Log("Subscriptions support check: success");
                return SupportState.SUPPORTED;
            }
            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                Log("Subscriptions support check: disconnected. Trying to reconnect...");
                return SupportState.DISCONNECTED;
            }
            default -> {
                Log("Subscriptions support check: error -> " + response.getResponseCode() + " " + response.getDebugMessage());
                return SupportState.NOT_SUPPORTED;
            }
        }
    }

    /**
     * Checks purchases signature for more security
     */
    private void processPurchases(ProductType productType, @NonNull List<Purchase> allPurchases, boolean purchasedProductsFetched) {
        List<PurchaseInfo> signatureValidPurchases = new ArrayList<>();

        //create a list with signature valid purchases
        List<Purchase> validPurchases = allPurchases.stream().filter(this::isPurchaseSignatureValid).toList();
        for (Purchase purchase : validPurchases) {

            //query all products as a list
            List<String> purchasesProducts = purchase.getProducts();

            //loop through all products and progress for each product individually
            for (int i = 0; i < purchasesProducts.size(); i++) {
                String purchaseProduct = purchasesProducts.get(i);
                Optional<ProductInfo> productInfo = fetchedProductInfoList.stream().filter(it -> it.getProduct().equals(purchaseProduct)).findFirst();
                if (productInfo.isPresent()) {
                    ProductDetails productDetails = productInfo.get().getProductDetails();
                    PurchaseInfo purchaseInfo = new PurchaseInfo(generateProductInfo(productDetails), purchase);
                    signatureValidPurchases.add(purchaseInfo);
                }
            }
        }

        if (purchasedProductsFetched) {
            fetchedPurchasedProducts = true;
            findUiHandler().post(() -> billingEventListener.onPurchasedProductsFetched(productType, signatureValidPurchases));
        } else {
            findUiHandler().post(() -> billingEventListener.onProductsPurchased(signatureValidPurchases));
        }

        purchasedProductsList.addAll(signatureValidPurchases);

        for (PurchaseInfo purchaseInfo : signatureValidPurchases) {
            if (shouldAutoConsume) {
                consumePurchase(purchaseInfo);
            }

            if (shouldAutoAcknowledge) {
                boolean isProductConsumable = purchaseInfo.getSkuProductType() == SkuProductType.CONSUMABLE;
                if (!isProductConsumable) {
                    acknowledgePurchase(purchaseInfo);
                }
            }
        }
    }

    /**
     * Consume consumable products so that the user can buy the item again
     * <p>
     * Consumable products might be bought/consumed by users multiple times (for eg. diamonds, coins etc)
     * They have to be consumed within 3 days otherwise Google will refund the products
     */
    public void consumePurchase(@NonNull PurchaseInfo purchaseInfo) {
        if (checkProductBeforeInteraction(purchaseInfo.getProduct()) && (purchaseInfo.getSkuProductType() == SkuProductType.CONSUMABLE)) {
            if (purchaseInfo.getPurchase().getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                ConsumeParams consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchaseInfo.getPurchase().getPurchaseToken()).build();
                billingClient.consumeAsync(consumeParams, (billingResult, purchaseToken) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        purchasedProductsList.remove(purchaseInfo);
                        findUiHandler().post(() -> billingEventListener.onPurchaseConsumed(purchaseInfo));
                    } else {
                        Log("Handling consumables: error during consumption attempt: " + billingResult.getDebugMessage());

                        findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                                new BillingResponse(ErrorType.CONSUME_ERROR, billingResult)));
                    }
                });
            } else if (purchaseInfo.getPurchase().getPurchaseState() == Purchase.PurchaseState.PENDING) {
                Log("Handling consumables: purchase can not be consumed because the state is PENDING. " +
                        "A purchase can be consumed only when the state is PURCHASED");

                findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.CONSUME_WARNING,
                        "Warning: purchase can not be consumed because the state is PENDING. Please consume the purchase later", DEFAULT_RESPONSE_CODE)));
            }
        }
    }

    /**
     * Acknowledge non-consumable products & subscriptions
     * <p>
     * This will avoid refunding for these products to users by Google
     */
    public void acknowledgePurchase(PurchaseInfo purchaseInfo) {
        if (!checkProductBeforeInteraction(purchaseInfo.getProduct())) {
            return;
        }

        if (purchaseInfo.getSkuProductType() == NON_CONSUMABLE || purchaseInfo.getSkuProductType() == SUBSCRIPTION){
            if (purchaseInfo.getPurchase().getPurchaseState() == Purchase.PurchaseState.PURCHASED) {

                AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchaseInfo.getPurchase().getPurchaseToken()).build();

                billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        findUiHandler().post(() -> billingEventListener.onPurchaseAcknowledged(purchaseInfo));
                    } else {
                        Log("Handling acknowledges: error during acknowledgment attempt: " + billingResult.getDebugMessage());

                        findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                                new BillingResponse(ErrorType.ACKNOWLEDGE_ERROR, billingResult)));
                    }
                });
            } else if (purchaseInfo.getPurchase().getPurchaseState() == Purchase.PurchaseState.PENDING) {
                Log("Handling acknowledges: purchase can not be acknowledged because the state is PENDING. " +
                        "A purchase can be acknowledged only when the state is PURCHASED");

                findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.ACKNOWLEDGE_WARNING,
                        "Warning: purchase can not be acknowledged because the state is PENDING. Please acknowledge the purchase later", DEFAULT_RESPONSE_CODE)));
            }
        }
    }

    /**
     * Called to purchase a non-consumable/consumable product
     */
    public final void purchase(Activity activity, String productId) {
        purchase(activity, productId, 0);
    }

    /**
     * Called to purchase a non-consumable/consumable product
     * <p>
     * The offer index represents the different offers in the subscription.
     */
    private void purchase(Activity activity, String productId, int selectedOfferIndex) {
        if (!checkProductBeforeInteraction(productId)) {
            return;
        }

        Optional<ProductInfo> productInfo = fetchedProductInfoList.stream().filter(it -> it.getProduct().equals(productId)).findFirst();
        if (productInfo.isPresent()) {
            ProductDetails productDetails = productInfo.get().getProductDetails();
            List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList;

            if (productDetails.getProductType().equals(BillingClient.ProductType.SUBS) && productDetails.getSubscriptionOfferDetails() != null) {
                // The offer index represents the different offers in the subscription
                // Offer index is only available for subscriptions starting with Google Billing v5+
                productDetailsParamsList = List.of(BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(productDetails.getSubscriptionOfferDetails().get(selectedOfferIndex).getOfferToken())
                        .build()
                );
            } else {
                productDetailsParamsList = List.of(BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                );
            }

            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build();

            billingClient.launchBillingFlow(activity, billingFlowParams);
        } else {
            Log("Billing client can not launch billing flow because product details are missing");
        }
    }

    /**
     * Called to purchase a subscription with offers
     * <p>
     * To avoid confusion while trying to purchase a subscription
     * Does the same thing as purchase() method
     * <p>
     * For subscription with only one base package, use subscribe(activity, productId) method or selectedOfferIndex = 0
     */
    public final void subscribe(Activity activity, String productId, int selectedOfferIndex) {
        purchase(activity, productId, selectedOfferIndex);
    }

    /**
     * Called to purchase a simple subscription
     * <p>
     * To avoid confusion while trying to purchase a subscription
     * Does the same thing as purchase() method
     * <p>
     * For subscription with multiple offers, use subscribe(activity, productId, selectedOfferIndex) method
     */
    public final void subscribe(Activity activity, String productId) {
        purchase(activity, productId);
    }

    /**
     * Called to cancel a subscription
     */
    public final void unsubscribe(Activity activity, String productId) {
        try {
            String subscriptionUrl = "http://play.google.com/store/account/subscriptions?package=" + activity.getPackageName() + "&sku=" + productId;

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(subscriptionUrl));

            activity.startActivity(intent);
            activity.finish();
        } catch (Exception e) {
            Log("Handling subscription cancellation: error while trying to unsubscribe");
        }
    }

    /**
     * Checks purchase state synchronously
     */
    public final PurchasedResult isPurchased(@NonNull ProductInfo productInfo) {
        return checkPurchased(productInfo.getProduct());
    }

    private PurchasedResult checkPurchased(String productId) {
        if (!isReady()) {
            return PurchasedResult.CLIENT_NOT_READY;
        } else if (!fetchedPurchasedProducts) {
            return PurchasedResult.PURCHASED_PRODUCTS_NOT_FETCHED_YET;
        } else {
            for (PurchaseInfo purchaseInfo : purchasedProductsList) {
                if (purchaseInfo.getProduct().equals(productId)) {
                    return PurchasedResult.YES;
                }
            }
            return PurchasedResult.NO;
        }
    }

    /**
     * Checks purchase signature validity
     */
    private boolean isPurchaseSignatureValid(@NonNull Purchase purchase) {
        return Security.verifyPurchase(base64Key, purchase.getOriginalJson(), purchase.getSignature());
    }

    /**
     * Returns the main thread for operations that need to be executed on the UI thread
     * <p>
     * BillingEventListener runs on it
     */
    @NonNull
    @Contract(" -> new")
    private Handler findUiHandler() {
        return new Handler(Looper.getMainLooper());
    }

    /**
     * To print a log while debugging BillingConnector
     */
    private void Log(String debugMessage) {
        if (shouldEnableLogging) {
            Log.d(TAG, debugMessage);
        }
    }

    /**
     * Called to release the BillingClient instance
     * <p>
     * To avoid leaks this method should be called when BillingConnector is no longer needed
     */
    public void release() {
        if (billingClient != null && billingClient.isReady()) {
            Log("BillingConnector instance release: ending connection...");
            billingClient.endConnection();
        }
    }
}