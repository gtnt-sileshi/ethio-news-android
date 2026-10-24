package com.ethionews.app.fragment.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.ethionews.app.activity.MainActivity;
import com.ethionews.app.adapter.AdapterBlogPost;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.callback.Method;
import com.ethionews.app.executor.LoadBlogPosts;
import com.ethionews.app.interfaces.BlogPostsListener;
import com.ethionews.app.item.ItemBlogPost;
import com.ethionews.app.utils.helper.Helper;

/**
 * Blog post search results fragment.
 * Replaces the old TV-channel search — now uses blog search API.
 */
public class FragmentSearch extends Fragment {

    private Helper helper;
    private ProgressBar progressBar;
    private FrameLayout frameLayout;
    private RecyclerView rv;
    private AdapterBlogPost adapter;
    private ArrayList<ItemBlogPost> arrayList;
    private String errorMsg;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_blog, container, false);

        helper = new Helper(getActivity());
        arrayList = new ArrayList<>();

        ((MainActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.search));
        ((MainActivity) getActivity()).bottomNavigationView(5);

        progressBar = rootView.findViewById(R.id.pb_blog);
        frameLayout = rootView.findViewById(R.id.fl_empty);
        rv = rootView.findViewById(R.id.rv_blog);

        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        rv.setLayoutManager(llm);
        rv.setItemAnimator(new DefaultItemAnimator());

        adapter = new AdapterBlogPost(getActivity(), arrayList, pos -> {
            Intent intent = new Intent(getActivity(), BlogPostActivity.class);
            intent.putExtra(BlogPostActivity.EXTRA_POST_ID, arrayList.get(pos).getId());
            startActivity(intent);
        });
        rv.setAdapter(adapter);

        search();
        addMenuProvider();
        return rootView;
    }

    private void search() {
        if (!helper.isNetworkAvailable()) {
            errorMsg = getString(R.string.err_internet_not_connected);
            setEmpty();
            return;
        }

        new LoadBlogPosts(new BlogPostsListener() {
            @Override public void onStart() {
                frameLayout.setVisibility(View.GONE);
                rv.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }
            @Override public void onEnd(String success, String message, ArrayList<ItemBlogPost> posts) {
                progressBar.setVisibility(View.GONE);
                if (getActivity() == null) return;
                if ("1".equals(success) && !posts.isEmpty()) {
                    arrayList.clear();
                    arrayList.addAll(posts);
                    adapter.hideFooter();
                    adapter.notifyDataSetChanged();
                    setEmpty();
                } else {
                    errorMsg = getString(R.string.err_no_data_found);
                    setEmpty();
                }
            }
        }, helper.getAPIRequest(Method.METHOD_BLOG_SEARCH, 1, "", "",
                Callback.getSearchItem(), "", "", "", "", "",
                "", "", "", "", null)).execute();
    }

    @SuppressLint("InflateParams")
    public void setEmpty() {
        if (!arrayList.isEmpty()) {
            rv.setVisibility(View.VISIBLE);
            frameLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
        } else {
            rv.setVisibility(View.GONE);
            frameLayout.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

            frameLayout.removeAllViews();
            LayoutInflater inflater = (LayoutInflater) requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View myView = inflater.inflate(R.layout.row_empty, null);
            ((TextView) myView.findViewById(R.id.tv_empty_msg)).setText(errorMsg);
            myView.findViewById(R.id.ll_empty_try).setOnClickListener(v -> {
                myView.findViewById(R.id.iv_empty).setVisibility(View.GONE);
                myView.findViewById(R.id.pb_empty).setVisibility(View.VISIBLE);
                new Handler().postDelayed(() -> {
                    myView.findViewById(R.id.iv_empty).setVisibility(View.VISIBLE);
                    myView.findViewById(R.id.pb_empty).setVisibility(View.GONE);
                    search();
                }, 500);
            });
            frameLayout.addView(myView);
        }
    }

    private void addMenuProvider() {
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.menu_search, menu);
                MenuItem item = menu.findItem(R.id.menu_search);
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW | MenuItem.SHOW_AS_ACTION_IF_ROOM);
                SearchView searchView = (SearchView) item.getActionView();
                if (searchView != null) {
                    searchView.setOnQueryTextListener(queryTextListener);
                }
            }
            @Override public boolean onMenuItemSelected(@NonNull MenuItem menuItem) { return false; }
        }, getViewLifecycleOwner());
    }

    SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public boolean onQueryTextSubmit(String s) {
            if (helper.isNetworkAvailable()) {
                Callback.setSearchItem(s.replace(" ", "%20"));
                arrayList.clear();
                adapter.notifyDataSetChanged();
                search();
            } else {
                Toast.makeText(getActivity(), getString(R.string.err_internet_not_connected), Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        @Override public boolean onQueryTextChange(String s) { return false; }
    };
}
