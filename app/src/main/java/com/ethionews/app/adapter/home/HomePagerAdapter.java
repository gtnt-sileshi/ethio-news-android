package com.ethionews.app.adapter.home;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.squareup.picasso.Picasso;

import java.util.List;

import com.ethionews.app.R;
import com.ethionews.app.interfaces.InterAdListener;
import com.ethionews.app.item.ItemHomeSlider;
import com.ethionews.app.utils.helper.Helper;
import com.ethionews.app.utils.color.LoadColor;

public class HomePagerAdapter extends RecyclerView.Adapter<HomePagerAdapter.BannerViewHolder> {

    private final Context mContext;
    private final List<ItemHomeSlider> arrayList;
    private final Helper helper;

    public HomePagerAdapter(Context context, List<ItemHomeSlider> arrayList) {
        mContext = context;
        this.arrayList = arrayList;
        helper = new Helper(context, interAdListener);
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_home_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        holder.title.setText(arrayList.get(position).getTitle());
        holder.desc.setText(arrayList.get(position).getInfo());

        Picasso.get()
                .load(arrayList.get(position).getImage())
                .placeholder(R.drawable.material_design_default)
                .error(R.drawable.material_design_default)
                .into(holder.banner);

        new LoadColor(holder.gradient).execute(arrayList.get(position).getImage());

        holder.itemView.setOnClickListener(v -> helper.showInterAd(position, ""));
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView desc;
        final ImageView banner;
        final View gradient;

        BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            title    = itemView.findViewById(R.id.tv_home_banner);
            desc     = itemView.findViewById(R.id.tv_home_banner_desc);
            banner   = itemView.findViewById(R.id.iv_home_banner);
            gradient = itemView.findViewById(R.id.view_home_banner);
        }
    }

    final InterAdListener interAdListener = new InterAdListener() {
        @Override
        public void onClick(int position, String type) {
            // Banners now link to blog posts by ID
            Intent intent = new Intent(mContext, com.ethionews.app.activity.BlogPostActivity.class);
            intent.putExtra(com.ethionews.app.activity.BlogPostActivity.EXTRA_POST_ID, arrayList.get(position).getId());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        }
    };
}
