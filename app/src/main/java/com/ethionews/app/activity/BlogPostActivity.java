package com.ethionews.app.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.shimmer.ShimmerFrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Objects;

import com.ethionews.app.R;
import com.ethionews.app.adapter.AdapterBlogComment;
import com.ethionews.app.adapter.AdapterBlogPost;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.callback.Method;
import com.ethionews.app.executor.LoadBlogComments;
import com.ethionews.app.executor.LoadBlogPost;
import com.ethionews.app.executor.LoadBlogPosts;
import com.ethionews.app.executor.LoadStatus;
import com.ethionews.app.interfaces.BlogCommentsListener;
import com.ethionews.app.interfaces.BlogPostDetailListener;
import com.ethionews.app.interfaces.BlogPostsListener;
import com.ethionews.app.interfaces.SuccessListener;
import com.ethionews.app.item.ItemBlogComment;
import com.ethionews.app.item.ItemBlogPost;
import com.ethionews.app.item.ItemBlogTag;
import com.ethionews.app.utils.IfSupported;
import com.ethionews.app.utils.helper.Helper;
import com.ethionews.app.utils.helper.SPHelper;

public class BlogPostActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "post_id";

    private Helper helper;
    private SPHelper spHelper;
    private String postId;
    private String deviceId;
    private boolean isSaved = false;

    // Views
    private ImageView ivCover;
    private ShimmerFrameLayout shimmerDetail;
    private LinearLayout llContent;
    private TextView tvCategory, tvReadTime, tvTitle, tvAuthor, tvViews;
    private WebView wvContent;
    private ChipGroup cgTags;
    private MaterialButton btnSave, btnShare;
    private ProgressBar pbComments;
    private RecyclerView rvComments, rvRelated;
    private TextView tvRelatedTitle;
    private TextInputEditText etCommentName, etCommentBody;

    private ArrayList<ItemBlogComment> commentList = new ArrayList<>();
    private ArrayList<ItemBlogPost>    relatedList  = new ArrayList<>();
    private AdapterBlogComment commentAdapter;
    private AdapterBlogPost    relatedAdapter;

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IfSupported.applyTheme(this);
        setContentView(R.layout.activity_blog_post);

        helper   = new Helper(this);
        spHelper = new SPHelper(this);
        postId   = getIntent().getStringExtra(EXTRA_POST_ID);

        // Use Android ID as a stable device identifier for saves (no user accounts)
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        bindViews();
        setupRecyclers();
        loadPost();

        // Banner ad
        LinearLayout adView = findViewById(R.id.ll_adView);
        helper.showBannerAd(adView, Callback.PAGE_POST_DETAILS);
    }

    private void bindViews() {
        ivCover       = findViewById(R.id.iv_blog_cover);
        shimmerDetail = findViewById(R.id.shimmer_blog_detail);
        llContent     = findViewById(R.id.ll_blog_content);
        tvCategory    = findViewById(R.id.tv_category);
        tvReadTime    = findViewById(R.id.tv_read_time);
        tvTitle       = findViewById(R.id.tv_title);
        tvAuthor      = findViewById(R.id.tv_author);
        tvViews       = findViewById(R.id.tv_views);
        wvContent     = findViewById(R.id.wv_content);
        cgTags        = findViewById(R.id.cg_tags);
        btnSave       = findViewById(R.id.btn_save);
        btnShare      = findViewById(R.id.btn_share);
        pbComments    = findViewById(R.id.pb_comments);
        rvComments    = findViewById(R.id.rv_comments);
        rvRelated     = findViewById(R.id.rv_related);
        tvRelatedTitle = findViewById(R.id.tv_related_title);
        etCommentName = findViewById(R.id.et_comment_name);
        etCommentBody = findViewById(R.id.et_comment_body);

        MaterialButton btnSubmit = findViewById(R.id.btn_submit_comment);
        btnSubmit.setOnClickListener(v -> submitComment());
    }

    private void setupRecyclers() {
        // Comments
        commentAdapter = new AdapterBlogComment(this, commentList);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setItemAnimator(new DefaultItemAnimator());
        rvComments.setAdapter(commentAdapter);

        // Related posts — hide footer immediately since related posts are loaded
        // all at once (not paginated); avoids a permanent spinner at the bottom
        relatedAdapter = new AdapterBlogPost(this, relatedList, pos -> {
            Intent i = new Intent(this, BlogPostActivity.class);
            i.putExtra(EXTRA_POST_ID, relatedList.get(pos).getId());
            startActivity(i);
        });
        relatedAdapter.hideFooter();
        rvRelated.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRelated.setItemAnimator(new DefaultItemAnimator());
        rvRelated.setAdapter(relatedAdapter);
    }

    private void loadPost() {
        if (!helper.isNetworkAvailable()) {
            Toast.makeText(this, getString(R.string.err_internet_not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        new LoadBlogPost(new BlogPostDetailListener() {
            @Override public void onStart() {
                shimmerDetail.setVisibility(View.VISIBLE);
                shimmerDetail.startShimmer();
                llContent.setVisibility(View.GONE);
            }
            @Override public void onEnd(String success, String message, ItemBlogPost post) {
                shimmerDetail.stopShimmer();
                shimmerDetail.setVisibility(View.GONE);
                if ("1".equals(success) && post != null) {
                    bindPost(post);
                } else {
                    Toast.makeText(BlogPostActivity.this, message, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }, helper.getAPIRequest(
            Method.METHOD_BLOG_POST, 0, postId, "", "", "", deviceId,
            "", "", "", "", "", "", "", null
        )).execute();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void bindPost(ItemBlogPost post) {
        this.isSaved = post.isSaved();

        if (getSupportActionBar() != null) getSupportActionBar().setTitle(post.getTitle());

        // Cover image
        String img = post.getFeaturedImage();
        if (img != null && !img.isEmpty() && !img.equals("null")) {
            Picasso.get().load(img).placeholder(R.drawable.material_design_default).into(ivCover);
        }

        tvCategory.setText(post.getCategoryName());
        tvReadTime.setText(post.getReadingTime() + " min read");
        tvTitle.setText(post.getTitle());
        tvAuthor.setText("By " + post.getAuthorName());
        tvViews.setText(post.getTotalViews() + " views");

        // Render rich HTML content in WebView
        WebSettings ws = wvContent.getSettings();
        ws.setJavaScriptEnabled(false);
        ws.setBuiltInZoomControls(false);
        ws.setDisplayZoomControls(false);
        String html = buildContentHtml(post.getContent());
        wvContent.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        wvContent.setBackgroundColor(Color.TRANSPARENT);

        // Tags
        cgTags.removeAllViews();
        for (ItemBlogTag tag : post.getTags()) {
            Chip chip = new Chip(this);
            chip.setText(tag.getName());
            chip.setClickable(true);
            cgTags.addView(chip);
        }

        // Save button
        updateSaveButton();
        btnSave.setOnClickListener(v -> toggleSave(post.getId()));

        // Share
        btnShare.setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, post.getTitle());
            share.putExtra(Intent.EXTRA_TEXT, post.getTitle() + "\n\n" + post.getExcerpt());
            startActivity(Intent.createChooser(share, getString(R.string.share)));
        });

        // Related posts
        if (!post.getRelated().isEmpty()) {
            tvRelatedTitle.setVisibility(View.VISIBLE);
            relatedList.addAll(post.getRelated());
            relatedAdapter.hideFooter();
            relatedAdapter.notifyDataSetChanged();
        }

        llContent.setVisibility(View.VISIBLE);

        // Load comments
        loadComments();
    }

    private String buildContentHtml(String content) {
        // Minimal responsive HTML wrapper — respects system dark mode via CSS media query
        return "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>"
            + "body{margin:0;padding:0;font-family:sans-serif;font-size:15px;line-height:1.7;word-wrap:break-word;}"
            + "img{max-width:100%;height:auto;}"
            + "p{margin-bottom:0.8em;}h2,h3{font-weight:700;margin:1em 0 0.4em;}"
            + "a{color:#1565C0;}"
            + "@media(prefers-color-scheme:dark){body{background:#0A0D12;color:#F0F4F8;}a{color:#42A5F5;}}"
            + "</style></head><body>"
            + content
            + "</body></html>";
    }

    private void loadComments() {
        pbComments.setVisibility(View.VISIBLE);
        new LoadBlogComments(new BlogCommentsListener() {
            @Override public void onStart() {}
            @Override public void onEnd(String success, String message, ArrayList<ItemBlogComment> comments) {
                pbComments.setVisibility(View.GONE);
                if ("1".equals(success) && !comments.isEmpty()) {
                    commentList.addAll(comments);
                    commentAdapter.notifyDataSetChanged();
                }
            }
        }, helper.getAPIRequest(
            Method.METHOD_BLOG_COMMENTS, 1, postId, "", "", "", "",
            "", "", "", "", "", "", "", null
        )).execute();
    }

    private void submitComment() {
        String name    = Objects.requireNonNull(etCommentName.getText()).toString().trim();
        String content = Objects.requireNonNull(etCommentBody.getText()).toString().trim();

        if (name.isEmpty()) {
            etCommentName.setError(getString(R.string.err_field_required));
            return;
        }
        if (content.isEmpty()) {
            etCommentBody.setError(getString(R.string.err_field_required));
            return;
        }

        new LoadStatus(new SuccessListener() {
            @Override public void onStart() {}
            @Override public void onEnd(String success, String verifyStatus, String message) {
                // success = network result ("1"/"0"), verifyStatus = API success field
                if ("1".equals(success) && "1".equals(verifyStatus)) {
                    Toast.makeText(BlogPostActivity.this,
                        message.isEmpty() ? getString(R.string.submit_comment) : message,
                        Toast.LENGTH_LONG).show();
                    etCommentName.setText("");
                    etCommentBody.setText("");
                } else if ("1".equals(success)) {
                    // Network ok but API returned an error
                    Toast.makeText(BlogPostActivity.this,
                        message.isEmpty() ? getString(R.string.err_server) : message,
                        Toast.LENGTH_SHORT).show();
                } else {
                    // doInBackground returned "0" — network/connection error
                    Toast.makeText(BlogPostActivity.this,
                        getString(R.string.err_server_not_connected),
                        Toast.LENGTH_SHORT).show();
                }
            }
        }, helper.getAPIRequest(
            Method.METHOD_BLOG_COMMENT_POST, 0, postId, "",
            "", content, deviceId,
            name, "", "", "", "", "", "", null
        )).execute();
    }

    private void toggleSave(String id) {
        new LoadStatus(new SuccessListener() {
            @Override public void onStart() {}
            @Override public void onEnd(String success, String verifyStatus, String message) {
                if ("1".equals(success)) {
                    // success=1 means "saved", success=2 means "removed"
                    isSaved = "1".equals(verifyStatus);
                    updateSaveButton();
                    Toast.makeText(BlogPostActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        }, helper.getAPIRequest(
            Method.METHOD_SAVE_POST, 0, id, "", "", "", deviceId,
            "", "", "", "", "", "", "", null
        )).execute();
    }

    private void updateSaveButton() {
        btnSave.setText(isSaved ? R.string.unsave : R.string.save);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
