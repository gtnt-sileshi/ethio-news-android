package com.ethionews.app.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

import com.ethionews.app.R;
import com.ethionews.app.item.ItemData;

public class AdapterSimilar extends RecyclerView.Adapter<AdapterSimilar.ViewHolder> {

    private final RecyclerItemClickListener listener;
    private final List<ItemData> arrayList;

    public AdapterSimilar(Context context, List<ItemData> arrayList, RecyclerItemClickListener listener) {
        this.arrayList = arrayList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_similar_v,parent, false);
        return new ViewHolder(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final ItemData itemPost = arrayList.get(position);

        Picasso.get()
                .load(arrayList.get(position).getThumb())
                .centerCrop()
                .resize(300,300)
                .placeholder(R.drawable.material_design_default)
                .error(R.drawable.material_design_default)
                .into(holder.logo);

        holder.title.setText(itemPost.getTitle());
        holder.bind(itemPost, listener);
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        private final ImageView logo;
        private final TextView title;

        public ViewHolder(View itemView) {
            super(itemView);
            logo  = itemView.findViewById(R.id.iv_similar_img);
            title = itemView.findViewById(R.id.tv_similar_text);
        }

        public void bind(final ItemData itemPost, final RecyclerItemClickListener listener){
            itemView.setOnClickListener(v -> listener.onClickListener(itemPost, getLayoutPosition()));
        }
    }

    public interface RecyclerItemClickListener{
        void onClickListener(ItemData itemPost, int position);
    }

}
