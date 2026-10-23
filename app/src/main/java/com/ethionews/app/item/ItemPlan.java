package com.ethionews.app.item;

import java.io.Serializable;

public class ItemPlan implements Serializable {

    private final String planId;
    private final String planName;
    private final String planDuration;
    private final String planPrice;
    private final String planCurrencyCode;
    private final String subscriptionID;
    private final String baseKey;

    public ItemPlan(String planId, String planName, String planDuration, String planPrice,
                    String planCurrencyCode, String subscriptionID, String baseKey) {
        this.planId = planId;
        this.planName = planName;
        this.planDuration = planDuration;
        this.planPrice = planPrice;
        this.planCurrencyCode = planCurrencyCode;
        this.subscriptionID = subscriptionID;
        this.baseKey = baseKey;
    }

    public String getPlanId() {
        return planId;
    }

    public String getPlanName() {
        return planName;
    }

    public String getPlanDuration() {
        return planDuration;
    }

    public String getPlanPrice() {
        return planPrice;
    }

    public String getPlanCurrencyCode() {
        return planCurrencyCode;
    }

    public String getSubscriptionID() {
        return subscriptionID;
    }

    public String getBaseKey() {
        return baseKey;
    }
}