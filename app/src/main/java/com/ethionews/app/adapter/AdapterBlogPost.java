package com.ethionews.app.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
import com.ethionews.app.item.ItemBlogPost;
import com.ethionews.app.utils.ApplicationUtil;

/**
 * Adapter for blog post list views (FragmentBlog, saved posts, search results).
 * Supports native ads (AdMob/Meta, StartApp, AppLovin MAX) inserted every N posts
 * as configured by the admin (native_position setting).
 */
public class AdapterBlogPost extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_ITEM     = 0;
    private static final int VIEW_PROGRESS = 1;

    private final Context context;
    private final ArrayList<ItemBlogPost> list;
    private final OnBlogPostClickListener listener;
    private boolean showFooter = true;

    // Native ads
    private boolean isAdLoaded = false;
    private final List<NativeAd> mNativeAdsAdmob = new ArrayList<>();

    public AdapterBlogPost(Context ctx, ArrayList<ItemBlogPost> list, OnBlogPostClickListener listener) {
        this.context  = ctx;
        this.list     = list;
        this.listener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView      ivCover;
        final TextView       tvTitle;
        final TextView       tvExcerpt;
        final TextView       tvCategory;
        final TextView       tvAuthor;
        final TextView       tvReadTime;
        final RelativeLayout rlNativeAd;

        ViewHolder(@NonNull View v) {
            super(v);
            ivCover    = v.findViewById(R.id.iv_blog_cover);
            tvTitle    = v.findViewById(R.id.tv_blog_title);
            tvExcerpt  = v.findViewById(R.id.tv_blog_excerpt);
            tvCategory = v.findViewById(R.id.tv_blog_category);
            tvAuthor   = v.findViewById(R.id.tv_blog_author);
            tvReadTime = v.findViewById(R.id.tv_blog_read_time);
            rlNativeAd = v.findViewById(R.id.rl_native_ad);
        }
    }

    static class ProgressHolder extends RecyclerView.ViewHolder {
        ProgressHolder(@NonNull View v) { super(v); }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_PROGRESS) {
            View v = LayoutInflater.from(context).inflate(R.layout.row_progressbar, parent, false);
            return new ProgressHolder(v);
        }
        View v = LayoutInflater.from(context).inflate(R.layout.item_blog_post, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (!(holder instanceof ViewHolder vh)) return;

        ItemBlogPost post = list.get(position);

        vh.tvTitle.setText(post.getTitle());
        vh.tvExcerpt.setText(post.getExcerpt());
        vh.tvCategory.setText(post.getCategoryName());
        vh.tvAuthor.setText(post.getAuthorName().isEmpty() ? "" : "By " + post.getAuthorName());
        vh.tvReadTime.setText(post.getReadingTime() + " min read");

        String img = post.getFeaturedImage();
        if (img != null && !img.isEmpty() && !img.equals("null")) {
            Picasso.get()
                .load(img)
                .placeholder(R.drawable.material_design_default)
                .error(R.drawable.material_design_default)
                .into(vh.ivCover);
        } else {
            vh.ivCover.setImageResource(R.drawable.material_design_default);
        }

        vh.itemView.setOnClickListener(v -> listener.onClick(position));

        // ── Native ads ────────────────────────────────────────────────────────
        if (Boolean.TRUE.equals(Callback.getIsAdsStatus())
                && Boolean.TRUE.equals(Callback.getIsNativeAdPost())
                && (position != list.size() - 1)
                && (position + 1) % Callback.getNativeAdShow() == 0) {
            try {
                if (vh.rlNativeAd.getChildCount() == 0) {
                    switch (Callback.getAdNetwork()) {
                        case Callback.AD_TYPE_ADMOB, Callback.AD_TYPE_META -> {
                            if (isAdLoaded && mNativeAdsAdmob.size() >= 5) {
                                int i = ApplicationUtil.getRandomValue(mNativeAdsAdmob.size() - 1);
                                @SuppressLint("InflateParams")
                                NativeAdView adView = (NativeAdView) ((Activity) context)
                                        .getLayoutInflater()
                                        .inflate(R.layout.layout_native_ad_admob, null);
                                populateNativeAdView(mNativeAdsAdmob.get(i), adView);
                                vh.rlNativeAd.removeAllViews();
                                vh.rlNativeAd.addView(adView);
                                vh.rlNativeAd.setVisibility(View.VISIBLE);
                            }
                        }
                        case Callback.AD_TYPE_STARTAPP -> {
                            StartAppNativeAd nativeAd = new StartAppNativeAd(context);
                            nativeAd.loadAd(
                                new NativeAdPreferences()
                                    .setAdsNumber(1)
                                    .setAutoBitmapDownload(true)
                                    .setPrimaryImageSize(2),
                                new AdEventListener() {
                                    @Override
                                    public void onReceiveAd(@NonNull Ad ad) {
                                        try {
                                            if (!nativeAd.getNativeAds().isEmpty()) {
                                                @SuppressLint("InflateParams")
                                                RelativeLayout adView = (RelativeLayout)
                                                        ((Activity) context).getLayoutInflater()
                                                        .inflate(R.layout.layout_native_ad_startapp, null);

                                                ImageView icon        = adView.findViewById(R.id.icon);
                                                TextView  title       = adView.findViewById(R.id.title);
                                                TextView  description = adView.findViewById(R.id.description);
                                                Button    button      = adView.findViewById(R.id.button);

                                                Picasso.get().load(nativeAd.getNativeAds().get(0).getImageUrl()).into(icon);
                                                title.setText(nativeAd.getNativeAds().get(0).getTitle());
                                                description.setText(nativeAd.getNativeAds().get(0).getDescription());
                                                button.setText(nativeAd.getNativeAds().get(0).isApp() ? "Install" : "Open");

                                                vh.rlNativeAd.removeAllViews();
                                                vh.rlNativeAd.addView(adView);
                                                vh.rlNativeAd.setVisibility(View.VISIBLE);
                                            }
                                        } catch (Exception e) {
                                            Log.e("AdapterBlogPost", "StartApp native ad failed", e);
                                        }
                                    }
                                    @Override public void onFailedToReceiveAd(Ad ad) {}
                                });
                        }
                        case Callback.AD_TYPE_APPLOVIN -> {
                            MaxNativeAdLoader loader = new MaxNativeAdLoader(
                                    Callback.getApplovinNativeAdID(), context);
                            loader.setNativeAdListener(new MaxNativeAdListener() {
                                @Override
                                public void onNativeAdLoaded(MaxNativeAdView nativeAdView,
                                                             @NonNull MaxAd maxAd) {
                                    vh.rlNativeAd.removeAllViews();
                                    vh.rlNativeAd.addView(nativeAdView);
                                    vh.rlNativeAd.setVisibility(View.VISIBLE);
                                }
                            });
                            loader.loadAd();
                        }
                        default -> { /* unsupported network */ }
                    }
                }
            } catch (Exception e) {
                Log.e("AdapterBlogPost", "Native ad bind failed", e);
            }
        } else {
            vh.rlNativeAd.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return showFooter ? list.size() + 1 : list.size(); }

    @Override
    public int getItemViewType(int pos) {
        return (showFooter && pos == list.size()) ? VIEW_PROGRESS : VIEW_ITEM;
    }

    @Override
    public long getItemId(int id) { return id; }

    public void hideFooter() {
        if (showFooter) {
            showFooter = false;
            notifyItemRemoved(list.size());
        }
    }

    /** Called by FragmentBlog after AdMob native ads are preloaded. */
    public void addAds(List<NativeAd> ads) {
        isAdLoaded = true;
        mNativeAdsAdmob.addAll(ads);
        for (int i = 0; i < list.size(); i++) {
            notifyItemChanged(i);
        }
    }

    // ── Native ad population ──────────────────────────────────────────────────

    private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        MediaView mediaView = adView.findViewById(R.id.ad_media);
        adView.setMediaView(mediaView);
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        ((TextView) Objects.requireNonNull(adView.getHeadlineView()))
                .setText(nativeAd.getHeadline());

        setOrHide(adView.getBodyView(),
                nativeAd.getBody(), v -> ((TextView) v).setText(nativeAd.getBody()));
        setOrHide(adView.getCallToActionView(),
                nativeAd.getCallToAction(), v -> ((Button) v).setText(nativeAd.getCallToAction()));
        setOrHide(adView.getPriceView(),
                nativeAd.getPrice(), v -> ((TextView) v).setText(nativeAd.getPrice()));
        setOrHide(adView.getStoreView(),
                nativeAd.getStore(), v -> ((TextView) v).setText(nativeAd.getStore()));
        setOrHide(adView.getAdvertiserView(),
                nativeAd.getAdvertiser(), v -> ((TextView) v).setText(nativeAd.getAdvertiser()));

        if (nativeAd.getIcon() == null) {
            Objects.requireNonNull(adView.getIconView()).setVisibility(View.GONE);
        } else {
            ((ImageView) Objects.requireNonNull(adView.getIconView()))
                    .setImageDrawable(nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getStarRating() == null) {
            Objects.requireNonNull(adView.getStarRatingView()).setVisibility(View.INVISIBLE);
        } else {
            ((RatingBar) Objects.requireNonNull(adView.getStarRatingView()))
                    .setRating(nativeAd.getStarRating().floatValue());
            adView.getStarRatingView().setVisibility(View.VISIBLE);
        }

        adView.setNativeAd(nativeAd);
    }

    private void setOrHide(View view, Object value, ViewSetter setter) {
        if (view == null) return;
        if (value == null) {
            view.setVisibility(View.INVISIBLE);
        } else {
            view.setVisibility(View.VISIBLE);
            setter.set(view);
        }
    }

    private interface ViewSetter { void set(View v); }

    public interface OnBlogPostClickListener {
        void onClick(int position);
    }
}
