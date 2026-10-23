package com.ethionews.app.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import com.ethionews.app.R;
import com.ethionews.app.item.ItemPlan;

public class PlanAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ItemPlan> dataList;
    private final Context mContext;
    private RvOnClickListener clickListener;
    private int rowIndex = -1;

    public PlanAdapter(Context context, List<ItemPlan> dataList) {
        this.dataList = dataList;
        this.mContext = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.row_select_plan, parent, false);
        return new ItemRowHolder(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder,
                                 @SuppressLint("RecyclerView") final int position) {
        final ItemRowHolder holder = (ItemRowHolder) viewHolder;
        final ItemPlan singleItem = dataList.get(position);
        holder.textPlanName.setText(singleItem.getPlanName());
        holder.textPlanPrice.setText(singleItem.getPlanPrice());
        holder.textPlanCurrency.setText(singleItem.getPlanCurrencyCode());
        holder.textPlanDuration.setText(mContext.getString(R.string.plan_day_for, singleItem.getPlanDuration())+" Days");
        holder.lytPlan.setOnClickListener(v -> clickListener.onItemClick(position));
        holder.radioButton.setOnClickListener(view -> clickListener.onItemClick(position));

        if (rowIndex > -1) {
            if (rowIndex == position) {
                holder.lytPlan.setBackgroundResource(R.drawable.bg_card_select);
                holder.radioButton.setChecked(true);
            } else {
                holder.lytPlan.setBackgroundResource(R.drawable.bg_card);
                holder.radioButton.setChecked(false);
            }
        } else {
            holder.lytPlan.setBackgroundResource(R.drawable.bg_card);
            holder.radioButton.setChecked(false);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void select(int position) {
        rowIndex = position;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return (null != dataList ? dataList.size() : 0);
    }

    public void setOnItemClickListener(RvOnClickListener clickListener) {
        this.clickListener = clickListener;
    }

    static class ItemRowHolder extends RecyclerView.ViewHolder {

        private final TextView textPlanName;
        private final TextView textPlanPrice;
        private final TextView textPlanDuration;
        private final TextView textPlanCurrency;
        private final RadioButton radioButton;
        private final RelativeLayout lytPlan;

        ItemRowHolder(View itemView) {
            super(itemView);
            textPlanName = itemView.findViewById(R.id.textPackName);
            textPlanPrice = itemView.findViewById(R.id.textPrice);
            textPlanDuration = itemView.findViewById(R.id.textDay);
            textPlanCurrency = itemView.findViewById(R.id.textCurrency);
            radioButton = itemView.findViewById(R.id.radioButton);
            lytPlan = itemView.findViewById(R.id.lytPlan);
        }
    }

    public interface RvOnClickListener {
        void onItemClick(int position);
    }
}
