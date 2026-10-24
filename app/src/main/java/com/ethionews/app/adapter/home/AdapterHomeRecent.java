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

public class AdapterHomeRecent extends RecyclerView.Adapter<AdapterHomeRecent.MyViewHolder> {

    Context context;
    List<ItemData> arrayList;
    private final RecyclerItemClickListener listener;

    public AdapterHomeRecent(Context context, List<ItemData> arrayList, RecyclerItemClickListener listener) {
        this.context = context;
        this.arrayList = arrayList;
        this.listener = listener;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        private final TextView title;
        private final ImageView logo;
        private final RelativeLayout relativeLayout;

        MyViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.tv_recent_title);
            logo = view.findViewById(R.id.iv_recently);
            relativeLayout = view.findViewById(R.id.rl_recent);
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recently, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        holder.title.setText(arrayList.get(position).getTitle());

        Picasso.get()
                .load(arrayList.get(position).getThumb())
                .resize(300,300)
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