package com.ethionews.app.adapter.home;

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
import com.ethionews.app.item.ItemEvent;

public class AdapterHomeEvent extends RecyclerView.Adapter<AdapterHomeEvent.MyViewHolder> {

    private final List<ItemEvent> arrayList;

    public AdapterHomeEvent(List<ItemEvent> arrayList) {
        this.arrayList = arrayList;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        private final ImageView logoOne;
        private final ImageView logoTwo;
        private final TextView eventTitle;
        private final TextView titleOne;
        private final TextView titleTwo;

        MyViewHolder(View view) {
            super(view);

            eventTitle = view.findViewById(R.id.tv_event_title);

            titleOne = view.findViewById(R.id.tv_team_one);
            logoOne = view.findViewById(R.id.iv_team_one);

            titleTwo = view.findViewById(R.id.tv_team_two);
            logoTwo = view.findViewById(R.id.iv_team_two);
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_home_event, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        holder.eventTitle.setText(arrayList.get(position).getTitle());

        holder.titleOne.setText(arrayList.get(position).getTitleOne());
        Picasso.get()
                .load(arrayList.get(position).getThumbOne())
                .centerCrop()
                .resize(300,300)
                .placeholder(R.drawable.material_design_default)
                .error(R.drawable.material_design_default)
                .into(holder.logoOne);

        holder.titleTwo.setText(arrayList.get(position).getTitleTwo());
        Picasso.get()
                .load(arrayList.get(position).getThumbTwo())
                .centerCrop()
                .resize(300,300)
                .placeholder(R.drawable.material_design_default)
                .into(holder.logoTwo);
    }

    @Override
    public long getItemId(int id) {
        return id;
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }
}