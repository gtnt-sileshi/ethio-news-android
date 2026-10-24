package com.ethionews.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import com.ethionews.app.R;
import com.ethionews.app.item.ItemBlogComment;

public class AdapterBlogComment extends RecyclerView.Adapter<AdapterBlogComment.ViewHolder> {

    private final Context context;
    private final ArrayList<ItemBlogComment> list;

    public AdapterBlogComment(Context ctx, ArrayList<ItemBlogComment> list) {
        this.context = ctx;
        this.list    = list;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvAvatar;
        final TextView tvAuthor;
        final TextView tvContent;
        final TextView tvDate;

        ViewHolder(@NonNull View v) {
            super(v);
            tvAvatar  = v.findViewById(R.id.tv_comment_avatar);
            tvAuthor  = v.findViewById(R.id.tv_comment_author);
            tvContent = v.findViewById(R.id.tv_comment_content);
            tvDate    = v.findViewById(R.id.tv_comment_date);
        }
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_blog_comment, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemBlogComment c = list.get(position);
        holder.tvAuthor.setText(c.getAuthorName());
        holder.tvContent.setText(c.getContent());
        String initial = c.getAuthorName().isEmpty() ? "?" : String.valueOf(c.getAuthorName().charAt(0)).toUpperCase();
        holder.tvAvatar.setText(initial);
        // Format date: show only the date portion if it contains a space
        String date = c.getCreatedAt();
        if (date.contains(" ")) date = date.split(" ")[0];
        holder.tvDate.setText(date);
    }

    @Override public int getItemCount() { return list.size(); }
}
