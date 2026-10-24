package com.ethionews.app.fragment.online;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import com.ethionews.app.R;
import com.ethionews.app.activity.BlogPostActivity;
import com.ethionews.app.adapter.AdapterBlogPost;
import com.ethionews.app.callback.Method;
import com.ethionews.app.executor.LoadBlogPosts;
import com.ethionews.app.interfaces.BlogPostsListener;
import com.ethionews.app.item.ItemBlogPost;
import com.ethionews.app.utils.helper.Helper;

/**
 * Paginated blog post list with infinite-scroll and search.
 *
 * Pass the method name via newInstance() to reuse this fragment for
 * Latest, Trending, By-Category, Saved posts, etc.
 */
public class FragmentBlog extends Fragment {

    private static final String ARG_METHOD = "method";
    private static final String ARG_CAT_ID = "cat_id";
    private static final String ARG_TAG_ID = "tag_id";
    private static final String ARG_TITLE  = "title";

    private Helper helper;
    private RecyclerView rvBlog;
    private ProgressBar pbBlog;
    private FrameLayout flEmpty;
    private AdapterBlogPost adapter;
    private ArrayList<ItemBlogPost> postList = new ArrayList<>();

    private String apiMethod;
    private String catId  = "";
    private String tagId  = "";
    private String searchQuery = "";
    private String errorMsg;
    private int page = 1;
    private boolean isLoading = false;
    private boolean hasMore   = true;

    public static FragmentBlog newInstance(String method, String catId, String tagId, String title) {
        FragmentBlog f = new FragmentBlog();
        Bundle args = new Bundle();
        args.putString(ARG_METHOD, method);
        args.putString(ARG_CAT_ID, catId != null ? catId : "");
        args.putString(ARG_TAG_ID, tagId != null ? tagId : "");
        args.putString(ARG_TITLE,  title  != null ? title  : "");
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_blog, container, false);

        helper = new Helper(getActivity());

        if (getArguments() != null) {
            apiMethod = getArguments().getString(ARG_METHOD, Method.METHOD_BLOG_POSTS);
            catId     = getArguments().getString(ARG_CAT_ID, "");
            tagId     = getArguments().getString(ARG_TAG_ID, "");
        } else {
            apiMethod = Method.METHOD_BLOG_POSTS;
        }

        pbBlog   = root.findViewById(R.id.pb_blog);
        flEmpty  = root.findViewById(R.id.fl_empty);
        rvBlog   = root.findViewById(R.id.rv_blog);

        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        rvBlog.setLayoutManager(llm);
        rvBlog.setItemAnimator(new DefaultItemAnimator());

        adapter = new AdapterBlogPost(getActivity(), postList, pos -> {
            Intent intent = new Intent(getActivity(), BlogPostActivity.class);
            intent.putExtra(BlogPostActivity.EXTRA_POST_ID, postList.get(pos).getId());
            startActivity(intent);
        });
        rvBlog.setAdapter(adapter);

        // Infinite scroll
        rvBlog.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 0 && !isLoading && hasMore) {
                    int visible   = llm.getChildCount();
                    int total     = llm.getItemCount();
                    int firstVis  = llm.findFirstVisibleItemPosition();
                    if ((visible + firstVis) >= total - 3) {
                        page++;
                        loadPosts(false);
                    }
                }
            }
        });

        loadPosts(true);
        addSearchMenu();
        return root;
    }

    private void loadPosts(boolean fresh) {
        if (!helper.isNetworkAvailable()) {
            errorMsg = getString(R.string.err_internet_not_connected);
            setEmpty();
            return;
        }
        isLoading = true;

        new LoadBlogPosts(new BlogPostsListener() {
            @Override public void onStart() {
                if (fresh) {
                    pbBlog.setVisibility(View.VISIBLE);
                    rvBlog.setVisibility(View.GONE);
                }
            }
            @Override public void onEnd(String success, String message, ArrayList<ItemBlogPost> posts) {
                pbBlog.setVisibility(View.GONE);
                isLoading = false;
                if (getActivity() == null) return;

                if ("1".equals(success) && !posts.isEmpty()) {
                    if (fresh) postList.clear();
                    adapter.hideFooter();
                    postList.addAll(posts);
                    adapter.notifyDataSetChanged();
                    hasMore = true;
                } else {
                    hasMore = false;
                    adapter.hideFooter();
                    if (postList.isEmpty()) {
                        errorMsg = getString(R.string.err_no_data_found);
                        setEmpty();
                        return;
                    }
                }
                rvBlog.setVisibility(View.VISIBLE);
                flEmpty.setVisibility(View.GONE);
            }
        }, buildRequest()).execute();
    }

    private okhttp3.RequestBody buildRequest() {
        String search = Method.METHOD_BLOG_SEARCH.equals(apiMethod) ? searchQuery : "";
        return helper.getAPIRequest(
            apiMethod, page,
            /* itemID   */ "",
            /* catID    */ catId.isEmpty() ? tagId : catId,
            /* search   */ search,
            /* report   */ "",
            /* userID   */ "",
            /* name     */ "", "", "", "", "", "", "", null
        );
    }

    @SuppressLint("InflateParams")
    public void setEmpty() {
        if (!postList.isEmpty()) {
            rvBlog.setVisibility(View.VISIBLE);
            flEmpty.setVisibility(View.GONE);
            pbBlog.setVisibility(View.GONE);
            return;
        }
        rvBlog.setVisibility(View.GONE);
        flEmpty.setVisibility(View.VISIBLE);
        pbBlog.setVisibility(View.GONE);
        flEmpty.removeAllViews();
        LayoutInflater inf = (LayoutInflater) requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View mv = inf.inflate(R.layout.row_empty, null);
        ((TextView) mv.findViewById(R.id.tv_empty_msg)).setText(errorMsg);
        mv.findViewById(R.id.ll_empty_try).setOnClickListener(v -> {
            mv.findViewById(R.id.iv_empty).setVisibility(View.GONE);
            mv.findViewById(R.id.pb_empty).setVisibility(View.VISIBLE);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                mv.findViewById(R.id.iv_empty).setVisibility(View.VISIBLE);
                mv.findViewById(R.id.pb_empty).setVisibility(View.GONE);
                page = 1; hasMore = true;
                loadPosts(true);
            }, 500);
        });
        flEmpty.addView(mv);
    }

    private void addSearchMenu() {
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                menu.clear();
                inflater.inflate(R.menu.menu_search, menu);
                MenuItem item = menu.findItem(R.id.menu_search);
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW | MenuItem.SHOW_AS_ACTION_IF_ROOM);
                SearchView sv = (SearchView) item.getActionView();
                if (sv != null) sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override public boolean onQueryTextSubmit(String query) {
                        searchQuery = query;
                        apiMethod   = Method.METHOD_BLOG_SEARCH;
                        page        = 1;
                        loadPosts(true);
                        return true;
                    }
                    @Override public boolean onQueryTextChange(String s) { return false; }
                });
            }
            @Override public boolean onMenuItemSelected(@NonNull MenuItem item) { return false; }
        }, getViewLifecycleOwner());
    }
}
