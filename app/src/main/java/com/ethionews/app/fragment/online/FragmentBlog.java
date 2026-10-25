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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.ads.mediation.facebook.FacebookMediationAdapter;
import com.google.android.gms.ads.AdRequest;

import com.ethionews.app.R;
import com.ethionews.app.activity.BlogPostActivity;
import com.ethionews.app.adapter.AdapterBlogPost;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.callback.Method;
import com.ethionews.app.executor.LoadBlogPosts;
import com.ethionews.app.interfaces.BlogPostsListener;
import com.ethionews.app.item.ItemBlogPost;
import com.ethionews.app.utils.helper.Helper;

/**
 * Paginated blog post list with infinite-scroll and search.
 */
public class FragmentBlog extends Fragment {

    private static final String ARG_METHOD = "method";
    private static final String ARG_CAT_ID = "cat_id";
    private static final String ARG_TAG_ID = "tag_id";
    private static final String ARG_TITLE  = "title";

    private Helper helper;
    private RecyclerView rvBlog;
    private ShimmerFrameLayout shimmerBlog;
    private FrameLayout flEmpty;
    private AdapterBlogPost adapter;
    private ArrayList<ItemBlogPost> postList = new ArrayList<>();

    private String apiMethod;
    private String originalMethod;  // remembers the method this instance was created with
    private String catId  = "";
    private String tagId  = "";
    private String searchQuery = "";
    private String errorMsg;
    private int page = 1;
    private boolean isLoading = false;
    private boolean hasMore   = true;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

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

        helper = new Helper(getActivity(), (pos, type) -> {
            if (getActivity() == null || pos < 0 || pos >= postList.size()) return;
            Intent intent = new Intent(getActivity(), BlogPostActivity.class);
            intent.putExtra(BlogPostActivity.EXTRA_POST_ID, postList.get(pos).getId());
            startActivity(intent);
        });

        if (getArguments() != null) {
            apiMethod     = getArguments().getString(ARG_METHOD, Method.METHOD_BLOG_POSTS);
            originalMethod = apiMethod;
            catId         = getArguments().getString(ARG_CAT_ID, "");
            tagId         = getArguments().getString(ARG_TAG_ID, "");
        } else {
            apiMethod     = Method.METHOD_BLOG_POSTS;
            originalMethod = apiMethod;
        }

        shimmerBlog = root.findViewById(R.id.shimmer_blog);
        flEmpty     = root.findViewById(R.id.fl_empty);
        rvBlog      = root.findViewById(R.id.rv_blog);

        LinearLayout adView = root.findViewById(R.id.ll_adView);
        helper.showBannerAd(adView, Callback.PAGE_HOME);

        if (helper.canLoadNativeAds(getContext(), Callback.PAGE_NATIVE_POST)
                && (Callback.getAdNetwork().equals(Callback.AD_TYPE_ADMOB)
                    || Callback.getAdNetwork().equals(Callback.AD_TYPE_META))) {
            com.google.android.gms.ads.AdLoader adLoader = new com.google.android.gms.ads.AdLoader.Builder(
                    requireContext(), Callback.getAdmobNativeAdID())
                .forNativeAd(nativeAd -> {
                    if (adapter != null) {
                        java.util.List<com.google.android.gms.ads.nativead.NativeAd> ads = new java.util.ArrayList<>();
                        ads.add(nativeAd);
                        adapter.addAds(ads);
                    }
                })
                .build();
            Bundle extras = new Bundle();
            AdRequest adRequest;
            if (Callback.getAdNetwork().equals(Callback.AD_TYPE_META)) {
                adRequest = new AdRequest.Builder()
                        .addNetworkExtrasBundle(AdMobAdapter.class, new Bundle())
                        .addNetworkExtrasBundle(FacebookMediationAdapter.class, extras)
                        .build();
            } else {
                adRequest = new AdRequest.Builder()
                        .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                        .build();
            }
            adLoader.loadAds(adRequest, 5);
        }

        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        rvBlog.setLayoutManager(llm);
        rvBlog.setItemAnimator(new DefaultItemAnimator());

        adapter = new AdapterBlogPost(getActivity(), postList, pos -> {
            helper.showInterAd(pos, "blog_post");
        });
        rvBlog.setAdapter(adapter);

        rvBlog.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 0 && !isLoading && hasMore) {
                    int visible  = llm.getChildCount();
                    int total    = llm.getItemCount();
                    int firstVis = llm.findFirstVisibleItemPosition();
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
                    shimmerBlog.setVisibility(View.VISIBLE);
                    shimmerBlog.startShimmer();
                    rvBlog.setVisibility(View.GONE);
                }
            }
            @Override public void onEnd(String success, String message, ArrayList<ItemBlogPost> posts) {
                shimmerBlog.stopShimmer();
                shimmerBlog.setVisibility(View.GONE);
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
        // Pass search query for both blog search and saved posts filtering
        String search = searchQuery;
        if (Method.METHOD_BLOG_SEARCH.equals(apiMethod)) {
            search = searchQuery;
        } else if (Method.METHOD_SAVED_POSTS.equals(apiMethod)) {
            search = searchQuery;
        } else {
            search = "";
        }
        String deviceId = "";
        if (Method.METHOD_SAVED_POSTS.equals(apiMethod)) {
            deviceId = android.provider.Settings.Secure.getString(
                requireActivity().getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);
        }
        return helper.getAPIRequest(
            apiMethod, page,
            "", catId.isEmpty() ? tagId : catId,
            search, "", deviceId,
            "", "", "", "", "", "", "", null
        );
    }

    @SuppressLint("InflateParams")
    public void setEmpty() {
        if (!postList.isEmpty()) {
            rvBlog.setVisibility(View.VISIBLE);
            flEmpty.setVisibility(View.GONE);
            shimmerBlog.stopShimmer();
            shimmerBlog.setVisibility(View.GONE);
            return;
        }
        rvBlog.setVisibility(View.GONE);
        flEmpty.setVisibility(View.VISIBLE);
        shimmerBlog.stopShimmer();
        shimmerBlog.setVisibility(View.GONE);
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
                        searchHandler.removeCallbacks(searchRunnable);
                        searchQuery = query;
                        if (Method.METHOD_BLOG_POSTS.equals(originalMethod)
                                || Method.METHOD_BLOG_SEARCH.equals(originalMethod)) {
                            apiMethod = Method.METHOD_BLOG_SEARCH;
                        }
                        // Saved/category/tag scoped methods keep originalMethod
                        // and pass query as a filter within that scope
                        page = 1;
                        loadPosts(true);
                        return true;
                    }
                    @Override public boolean onQueryTextChange(String s) {
                        searchHandler.removeCallbacks(searchRunnable);
                        if (s.trim().isEmpty()) {
                            searchQuery = "";
                            apiMethod = originalMethod;
                            page = 1;
                            loadPosts(true);
                            return true;
                        }
                        searchRunnable = () -> {
                            searchQuery = s;
                            if (Method.METHOD_BLOG_POSTS.equals(originalMethod)
                                    || Method.METHOD_BLOG_SEARCH.equals(originalMethod)) {
                                apiMethod = Method.METHOD_BLOG_SEARCH;
                            }
                            page = 1;
                            loadPosts(true);
                        };
                        searchHandler.postDelayed(searchRunnable, 400);
                        return true;
                    }
                });
            }
            @Override public boolean onMenuItemSelected(@NonNull MenuItem item) { return false; }
        }, getViewLifecycleOwner());
    }
}
