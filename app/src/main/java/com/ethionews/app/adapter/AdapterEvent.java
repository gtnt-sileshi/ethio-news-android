package com.ethionews.app.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.nativeAds.MaxNativeAdListener;
import com.applovin.mediation.nativeAds.MaxNativeAdLoader;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.squareup.picasso.Picasso;
import com.startapp.sdk.ads.nativead.NativeAdPreferences;
import com.startapp.sdk.ads.nativead.StartAppNativeAd;
import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.ethionews.app.R;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.item.ItemEvent;
import com.ethionews.app.utils.ApplicationUtil;
import com.ethionews.app.utils.helper.SPHelper;

public class AdapterEvent extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private List<ItemEvent> arrayList;
    private boolean showFooter = true;
    private final List<ItemEvent> filteredArrayList;
    private final RecyclerItemClickListener listener;
    private NameFilter filter;
    private static final int VIEW_PROG = -1;
    public final Boolean isPurchases;

    Boolean isAdLoaded = false;
    List<NativeAd> mNativeAdsAdmob = new ArrayList<>();

    public AdapterEvent(Context context, List<ItemEvent> arrayList, RecyclerItemClickListener clickListener) {
        this.arrayList = arrayList;
        this.filteredArrayList = arrayList;
        this.context = context;
        this.listener = clickListener;
        isPurchases = new SPHelper(context).getIsSubscribed();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {

        private final ImageView logoOne;
        private final ImageView logoTwo;
        private final TextView eventTitle;
        private final TextView teamOne;
        private final TextView teamTwo;
        private final TextView eventTime;
        private final TextView eventDate;
        private final RelativeLayout nativeAd;
        private final LinearLayout linearLayout;

        MyViewHolder(View view) {
            super(view);
            eventTitle = view.findViewById(R.id.tv_event_title);
            eventDate = view.findViewById(R.id.tv_event_date);
            eventTime = view.findViewById(R.id.tv_event_time);

            teamOne = view.findViewById(R.id.tv_team_one);
            logoOne = view.findViewById(R.id.iv_team_one);

            teamTwo = view.findViewById(R.id.tv_team_two);
            logoTwo = view.findViewById(R.id.iv_team_two);
            linearLayout = view.findViewById(R.id.ll_event_list);

            nativeAd = view.findViewById(R.id.rl_native_ad);
        }
    }

    private static class ProgressViewHolder extends RecyclerView.ViewHolder {

        private final ProgressBar progressBar;

        private ProgressViewHolder(View v) {
            super(v);
            progressBar = v.findViewById(R.id.progressBar);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_PROG) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_progressbar, parent, false);
            return new ProgressViewHolder(v);
        } else {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row_event, parent, false);
            return new MyViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof MyViewHolder myViewHolder) {

            myViewHolder.eventTitle.setText(arrayList.get(position).getTitle());
            myViewHolder.eventDate.setText(arrayList.get(position).getDate());
            myViewHolder.eventTime.setText(arrayList.get(position).getTime());

            myViewHolder.teamOne.setText(arrayList.get(position).getTitleOne());
            Picasso.get()
                    .load(arrayList.get(position).getThumbOne())
                    .centerCrop()
                    .resize(300,300)
                    .placeholder(R.drawable.material_design_default)
                    .error(R.drawable.material_design_default)
                    .into(myViewHolder.logoOne);

            myViewHolder.teamTwo.setText(arrayList.get(position).getTitleTwo());
            Picasso.get()
                    .load(arrayList.get(position).getThumbTwo())
                    .centerCrop()
                    .resize(300,300)
                    .placeholder(R.drawable.material_design_default)
                    .error(R.drawable.material_design_default)
                    .into(myViewHolder.logoTwo);

            myViewHolder.linearLayout.setOnClickListener(view ->
                    listener.onClick(getPosition(arrayList.get(holder.getAbsoluteAdapterPosition()).getId())));

            if (Boolean.TRUE.equals(Callback.getIsAdsStatus()
                    && Callback.getIsNativeAdPost()
                    && (position != arrayList.size() - 1)
                    && (position + 1) % Callback.getNativeAdShow() == 0)
                    && Boolean.TRUE.equals(!isPurchases)) {
                try {
                    if (((MyViewHolder) holder).nativeAd.getChildCount() == 0) {
                        switch (Callback.getAdNetwork()) {
                            case Callback.AD_TYPE_ADMOB, Callback.AD_TYPE_META -> {
                                if (Boolean.TRUE.equals(isAdLoaded) && (mNativeAdsAdmob.size() >= 5)) {

                                    int i = ApplicationUtil.getRandomValue(mNativeAdsAdmob.size() - 1);

                                    @SuppressLint("InflateParams") NativeAdView adView = (NativeAdView) ((Activity) context)
                                            .getLayoutInflater().inflate(R.layout.layout_native_ad_admob, null);
                                    populateUnifiedNativeAdView(mNativeAdsAdmob.get(i), adView);
                                    ((MyViewHolder) holder).nativeAd.removeAllViews();
                                    ((MyViewHolder) holder).nativeAd.addView(adView);

                                    ((MyViewHolder) holder).nativeAd.setVisibility(View.VISIBLE);
                                }
                            }
                            case Callback.AD_TYPE_STARTAPP -> {
                                StartAppNativeAd nativeAd = new StartAppNativeAd(context);
                                nativeAd.loadAd(new NativeAdPreferences().setAdsNumber(1).setAutoBitmapDownload(true)
                                        .setPrimaryImageSize(2), new AdEventListener() {
                                    @Override
                                    public void onReceiveAd(@NonNull Ad ad) {
                                        try {
                                            if (!nativeAd.getNativeAds().isEmpty()) {
                                                @SuppressLint("InflateParams") RelativeLayout nativeAdView = (RelativeLayout) ((Activity) context)
                                                        .getLayoutInflater().inflate(R.layout.layout_native_ad_startapp, null);

                                                ImageView icon = nativeAdView.findViewById(R.id.icon);
                                                TextView title = nativeAdView.findViewById(R.id.title);
                                                TextView description = nativeAdView.findViewById(R.id.description);
                                                Button button = nativeAdView.findViewById(R.id.button);

                                                Picasso.get()
                                                        .load(nativeAd.getNativeAds().get(0).getImageUrl())
                                                        .into(icon);
                                                title.setText(nativeAd.getNativeAds().get(0).getTitle());
                                                description.setText(nativeAd.getNativeAds().get(0).getDescription());
                                                button.setText(nativeAd.getNativeAds().get(0).isApp() ? "Install" : "Open");

                                                ((MyViewHolder) holder).nativeAd.removeAllViews();
                                                ((MyViewHolder) holder).nativeAd.addView(nativeAdView);
                                                ((MyViewHolder) holder).nativeAd.setVisibility(View.VISIBLE);
                                            }
                                        } catch (Exception e) {
                                            Log.e("AdapterEvent", "StartApp native ad failed", e);
                                        }
                                    }

                                    @Override
                                    public void onFailedToReceiveAd(Ad ad) {
                                        // this method is empty
                                    }
                                });
                            }
                            case Callback.AD_TYPE_APPLOVIN -> {
                                MaxNativeAdLoader nativeAdLoader = getMaxNativeAdLoader((MyViewHolder) holder);
                                nativeAdLoader.loadAd();
                            }
                            case Callback.AD_TYPE_WORTISE -> {
                                // Wortise removed
                            }
                            default -> {
                                // this method is empty
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("AdapterEvent", "onBindViewHolder failed", e);
                }
            }
        }
    }

    private @NonNull MaxNativeAdLoader getMaxNativeAdLoader(MyViewHolder holder) {
        MaxNativeAdLoader nativeAdLoader = new MaxNativeAdLoader(Callback.getApplovinNativeAdID(), context);
        nativeAdLoader.setNativeAdListener(new MaxNativeAdListener() {
            @Override
            public void onNativeAdLoaded(final MaxNativeAdView nativeAdView, @NonNull final MaxAd ad) {
                holder.nativeAd.removeAllViews();
                holder.nativeAd.addView(nativeAdView);
                holder.nativeAd.setVisibility(View.VISIBLE);
            }
        });
        return nativeAdLoader;
    }

    @Override
    public long getItemId(int id) {
        return id;
    }

    public void hideHeader() {
        try {
            if (showFooter) {
                showFooter = false;
                notifyItemRemoved(arrayList.size());
            }
        } catch (Exception e) {
            Log.e("AdapterEvent", "hideHeader failed", e);
        }
    }

    public boolean isHeader(int position) {
        return position == arrayList.size();
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (arrayList.get(position) != null) {
            return position;
        } else {
            return VIEW_PROG;
        }
    }

    private int getPosition(String id) {
        int count = 0;
        for (int i = 0; i < filteredArrayList.size(); i++) {
            if (id.equals(filteredArrayList.get(i).getId())) {
                count = i;
                break;
            }
        }
        return count;
    }

    public Filter getFilter() {
        if (filter == null) {
            filter = new NameFilter();
        }
        return filter;
    }

    private class NameFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            constraint = constraint.toString().toLowerCase();
            FilterResults result = new FilterResults();
            if (constraint.toString().length() > 0) {
                ArrayList<ItemEvent> filteredItems = new ArrayList<>();

                for (int i = 0, l = filteredArrayList.size(); i < l; i++) {
                    String nameList = filteredArrayList.get(i).getTitle();
                    if (nameList.toLowerCase().contains(constraint))
                        filteredItems.add(filteredArrayList.get(i));
                }
                result.count = filteredItems.size();
                result.values = filteredItems;
            } else {
                synchronized (this) {
                    result.values = filteredArrayList;
                    result.count = filteredArrayList.size();
                }
            }
            return result;
        }

        @SuppressLint("NotifyDataSetChanged")
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            arrayList = (ArrayList<ItemEvent>) results.values;
            notifyDataSetChanged();
        }
    }

    public void addAds(List<NativeAd> arrayListNativeAds) {
        isAdLoaded = true;
        mNativeAdsAdmob.addAll(arrayListNativeAds);
        for (int i = 0; i < arrayList.size(); i++) {
            if(arrayList.get(i) == null) {
                notifyItemChanged(i);
            }
        }
    }

    private void populateUnifiedNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        MediaView mediaView = adView.findViewById(R.id.ad_media);
        adView.setMediaView(mediaView);

        // Set other ad assets.
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        // The headline is guaranteed to be in every UnifiedNativeAd.
        ((TextView) Objects.requireNonNull(adView.getHeadlineView())).setText(nativeAd.getHeadline());

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.getBody() == null) {
            Objects.requireNonNull(adView.getBodyView()).setVisibility(View.INVISIBLE);
        } else {
            Objects.requireNonNull(adView.getBodyView()).setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            Objects.requireNonNull(adView.getCallToActionView()).setVisibility(View.INVISIBLE);
        } else {
            Objects.requireNonNull(adView.getCallToActionView()).setVisibility(View.VISIBLE);
            ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }

        if (nativeAd.getIcon() == null) {
            Objects.requireNonNull(adView.getIconView()).setVisibility(View.GONE);
        } else {
            ((ImageView) Objects.requireNonNull(adView.getIconView())).setImageDrawable(
                    nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getPrice() == null) {
            Objects.requireNonNull(adView.getPriceView()).setVisibility(View.INVISIBLE);
        } else {
            Objects.requireNonNull(adView.getPriceView()).setVisibility(View.VISIBLE);
            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
        }

        if (nativeAd.getStore() == null) {
            Objects.requireNonNull(adView.getStoreView()).setVisibility(View.INVISIBLE);
        } else {
            Objects.requireNonNull(adView.getStoreView()).setVisibility(View.VISIBLE);
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        }

        if (nativeAd.getStarRating() == null) {
            Objects.requireNonNull(adView.getStarRatingView()).setVisibility(View.INVISIBLE);
        } else {
            ((RatingBar) Objects.requireNonNull(adView.getStarRatingView()))
                    .setRating(nativeAd.getStarRating().floatValue());
            adView.getStarRatingView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getAdvertiser() == null) {
            Objects.requireNonNull(adView.getAdvertiserView()).setVisibility(View.INVISIBLE);
        } else {
            ((TextView) Objects.requireNonNull(adView.getAdvertiserView())).setText(nativeAd.getAdvertiser());
            adView.getAdvertiserView().setVisibility(View.VISIBLE);
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad. The SDK will populate the adView's MediaView
        // with the media content from this native ad.
        adView.setNativeAd(nativeAd);
    }

    public interface RecyclerItemClickListener{
        void onClick(int position);
    }

}