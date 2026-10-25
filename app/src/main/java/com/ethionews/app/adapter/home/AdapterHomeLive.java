package com.ethionews.app.adapter.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

import com.ethionews.app.R;
import com.ethionews.app.item.ItemData;

public class AdapterHomeLive extends RecyclerView.Adapter<AdapterHomeLive.MyViewHolder> {

    private final List<ItemData> arrayList;
    private final RecyclerItemClickListener listener;

    public AdapterHomeLive(Context context, List<ItemData> arrayList, RecyclerItemClickListener listener) {
        this.arrayList = arrayList;
        this.listener = listener;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        ImageView logo;
        TextView title;
        RelativeLayout relativeLayout;

        MyViewHolder(View view) {
            super(view);
            title         = view.findViewById(R.id.tv_live);
            logo          = view.findViewById(R.id.iv_live);
            relativeLayout = view.findViewById(R.id.rl_live_tv);
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_home_live, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        holder.title.setText(arrayList.get(position).getTitle());
        Picasso.get()
                .load(arrayList.get(position).getThumb())
                .centerCrop()
                .resize(300, 300)
                .placeholder(R.drawable.material_design_default)
                .into(holder.logo);

        holder.relativeLayout.setOnClickListener(v -> listener.onClick(holder.getAbsoluteAdapterPosition()));
    }

    @Override
    public long getItemId(int id) {
        return id;
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public interface RecyclerItemClickListener{
        void onClick(int position);
        void onRewardAds(int position);
    }
}