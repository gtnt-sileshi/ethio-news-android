package com.ethionews.app.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

import com.ethionews.app.R;
import com.ethionews.app.item.ItemRadio;
import com.ethionews.app.utils.ApplicationUtil;

public class AdapterRadio extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final List<ItemRadio> arrayList;
    private boolean showFooter = true;
    private final RecyclerItemClickListener listener;
    private final int columnWidth;

    private static final int VIEW_ITEM = 0;
    private static final int VIEW_PROG = -1;

    public AdapterRadio(Context context, List<ItemRadio> arrayList, RecyclerItemClickListener listener) {
        this.context = context;
        this.arrayList = arrayList;
        this.listener = listener;
        boolean isLandscape = ApplicationUtil.isLandscape(context);
        this.columnWidth = ApplicationUtil.getColumnWidth(isLandscape ? 4 : 3, 0, context);
    }

    // ── View holders ─────────────────────────────────────────────────

    private static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView logo;
        private final TextView name;
        private final TextView premiumBadge;
        private final RelativeLayout container;

        private ItemViewHolder(View view) {
            super(view);
            logo        = view.findViewById(R.id.iv_live_tv);
            name        = view.findViewById(R.id.tv_title);
            premiumBadge = view.findViewById(R.id.tv_live_pre);
            container   = view.findViewById(R.id.rl_live_tv);
        }
    }

    private static class ProgressViewHolder extends RecyclerView.ViewHolder {

        private final ProgressBar progressBar;

        private ProgressViewHolder(View v) {
            super(v);
            progressBar = v.findViewById(R.id.progressBar);
        }
    }

    // ── Adapter overrides ────────────────────────────────────────────

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_PROG) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_progressbar, parent, false);
            return new ProgressViewHolder(v);
        }
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_live, parent, false);
        return new ItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemViewHolder vh) {
            ItemRadio item = arrayList.get(position);

            vh.name.setText(item.getName());
            vh.logo.setLayoutParams(new RelativeLayout.LayoutParams(columnWidth, columnWidth));

            Picasso.get()
                    .load(item.getLogo())
                    .placeholder(R.drawable.material_design_default)
                    .error(R.drawable.material_design_default)
                    .into(vh.logo);

            vh.premiumBadge.setVisibility(View.GONE);
            vh.container.setOnClickListener(v -> listener.onClick(holder.getAbsoluteAdapterPosition()));

        } else if (holder instanceof ProgressViewHolder) {
            if (getItemCount() == 1) {
                notifyItemChanged(arrayList.size());
            }
        }
    }

    @Override
    public int getItemCount() {
        return showFooter ? arrayList.size() + 1 : arrayList.size(); // +1 for progress footer
    }

    @Override
    public int getItemViewType(int position) {
        return isHeader(position) ? VIEW_PROG : VIEW_ITEM;
    }

    @Override
    public long getItemId(int id) {
        return id;
    }

    // ── Helpers ──────────────────────────────────────────────────────

    public boolean isHeader(int position) {
        return position == arrayList.size();
    }

    public ItemRadio getItem(int position) {
        return arrayList.get(position);
    }

    public void hideHeader() {
        try {
            if (showFooter) {
                showFooter = false;
                notifyItemRemoved(arrayList.size());
            }
        } catch (Exception e) {
            // ignore
        }
    }

    // ── Click interface ──────────────────────────────────────────────

    public interface RecyclerItemClickListener {
        void onClick(int position);
        void onRewardAds(int position);
    }
}
