package com.ethionews.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import com.ethionews.app.R;
import com.ethionews.app.item.ItemBlogPost;

/**
 * Adapter for blog post list views (FragmentBlog, saved posts, search results).
 */
public class AdapterBlogPost extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_ITEM     = 0;
    private static final int VIEW_PROGRESS = 1;

    private final Context context;
    private final ArrayList<ItemBlogPost> list;
    private final OnBlogPostClickListener listener;
    private boolean showFooter = true;

    public AdapterBlogPost(Context ctx, ArrayList<ItemBlogPost> list, OnBlogPostClickListener listener) {
        this.context  = ctx;
        this.list     = list;
        this.listener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView  ivCover;
        final TextView   tvTitle;
        final TextView   tvExcerpt;
        final TextView   tvCategory;
        final TextView   tvAuthor;
        final TextView   tvReadTime;

        ViewHolder(@NonNull View v) {
            super(v);
            ivCover    = v.findViewById(R.id.iv_blog_cover);
            tvTitle    = v.findViewById(R.id.tv_blog_title);
            tvExcerpt  = v.findViewById(R.id.tv_blog_excerpt);
            tvCategory = v.findViewById(R.id.tv_blog_category);
            tvAuthor   = v.findViewById(R.id.tv_blog_author);
            tvReadTime = v.findViewById(R.id.tv_blog_read_time);
        }
    }

    static class ProgressHolder extends RecyclerView.ViewHolder {
        ProgressHolder(@NonNull View v) { super(v); }
    }

    @NonNull @Override
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
        vh.tvCategory.setText(post.getCategoryName().isEmpty() ? "" : post.getCategoryName());
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
    }

    @Override public int getItemCount() { return showFooter ? list.size() + 1 : list.size(); }
    @Override public int getItemViewType(int pos) { return (showFooter && pos == list.size()) ? VIEW_PROGRESS : VIEW_ITEM; }
    @Override public long getItemId(int id) { return id; }

    public ItemBlogPost getItem(int pos) { return list.get(pos); }

    public void hideFooter() {
        if (showFooter) {
            showFooter = false;
            notifyItemRemoved(list.size());
        }
    }

    public interface OnBlogPostClickListener {
        void onClick(int position);
    }
}
