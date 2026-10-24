package com.ethionews.app.fragment.search;

import android.annotation.SuppressLint;
import android.content.Context;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import com.ethionews.app.R;
import com.ethionews.app.activity.MainActivity;
import com.ethionews.app.adapter.AdapterCategories;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.callback.Method;
import com.ethionews.app.dialog.DialogUtil;
import com.ethionews.app.executor.LoadCat;
import com.ethionews.app.fragment.online.FragmentBlog;
import com.ethionews.app.interfaces.CategoryListener;
import com.ethionews.app.item.ItemCat;
import com.ethionews.app.utils.ApplicationUtil;
import com.ethionews.app.utils.helper.Helper;
import com.ethionews.app.utils.recycler.EndlessRecyclerViewScrollListener;

public class FragmentSearchCategories extends Fragment {

    private Helper helper;
    private RecyclerView rv;
    private AdapterCategories adapterCat;
    private ArrayList<ItemCat> arrayList;
    private ProgressBar progressBar;
    private FrameLayout frameLayout;
    private GridLayoutManager glmManner;
    private Boolean isLoading = false;
    private String errorMsg="";
    private int page = 1;
    private Boolean isOver = false;
    private Boolean isScroll = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_latest, container, false);

        helper = new Helper(getActivity(), (position, type) -> {
            if (adapterCat.getItem(position) != null) {
                FragmentBlog blogFrag = FragmentBlog.newInstance(
                    Method.METHOD_BLOG_BY_CAT, arrayList.get(position).getId(), "", arrayList.get(position).getName());
                androidx.fragment.app.FragmentManager fm = getParentFragmentManager();
                androidx.fragment.app.FragmentTransaction ft = fm.beginTransaction();
                ft.setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                ft.hide(fm.getFragments().get(fm.getBackStackEntryCount()));
                ft.add(R.id.fragment, blogFrag, arrayList.get(position).getName());
                ft.addToBackStack(arrayList.get(position).getName());
                ft.commit();
            }
        });

        ((MainActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.search_categories));
        ((MainActivity) getActivity()).bottomNavigationView(5);

        arrayList = new ArrayList<>();

        progressBar = rootView.findViewById(R.id.pb);
        frameLayout = rootView.findViewById(R.id.fl_empty);
        rv = rootView.findViewById(R.id.rv);

        android.widget.LinearLayout adView = rootView.findViewById(R.id.ll_adView);
        helper.showBannerAd(adView, Callback.PAGE_SEARCH);
        boolean isLandscape = ApplicationUtil.isLandscape(requireContext());
        glmManner = new GridLayoutManager(getActivity(), isLandscape ? 4 : 3);
        glmManner.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapterCat.isHeader(position) ? glmManner.getSpanCount() : 1;
            }
        });
        glmManner.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return (adapterCat.getItemViewType(position) == -2 || adapterCat.isHeader(position)) ? glmManner.getSpanCount() : 1;
            }
        });
        rv.setLayoutManager(glmManner);
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.addOnScrollListener(new EndlessRecyclerViewScrollListener(glmManner) {
            @Override
            public void onLoadMore(int p, int totalItemsCount) {
                if (Boolean.FALSE.equals(isOver)) {
                    if (Boolean.FALSE.equals(isLoading)) {
                        isLoading = true;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            isScroll = true;
                            loadCategories();
                        }, 0);
                    }
                } else {
                    adapterCat.hideHeader();
                }
            }
        });

        loadCategories();

        addMenuProvider();
        return rootView;
    }

    private void addMenuProvider() {
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.menu_search, menu);

                // Configure the search menu item
                MenuItem item = menu.findItem(R.id.menu_search);
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW | MenuItem.SHOW_AS_ACTION_IF_ROOM);
                SearchView searchView = (SearchView) item.getActionView();
                if (searchView != null) {
                    searchView.setOnQueryTextListener(queryTextListener);
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                // Handle menu item selection if necessary
                return false;
            }
        }, getViewLifecycleOwner());
    }
    SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public boolean onQueryTextSubmit(String s) {
            if (helper.isNetworkAvailable()) {
                page = 1;
                isScroll = false;
                Callback.setSearchItem(s.replace(" ", "%20"));
                arrayList.clear();
                if (adapterCat != null) {
                    adapterCat.notifyDataSetChanged();
                }
                loadCategories();
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.err_internet_not_connected), Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            return false;
        }
    };

    private void loadCategories() {
        if (!helper.isNetworkAvailable()) {
            errorMsg = getString(R.string.err_internet_not_connected);
            setEmpty();
            return;
        }
        LoadCat loadCat = new LoadCat(new CategoryListener() {
            @Override
            public void onStart() {
                if (arrayList.isEmpty()) {
                    frameLayout.setVisibility(View.GONE);
                    rv.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onEnd(String success, String verifyStatus, String message,
                              ArrayList<ItemCat> arrayListCat) {
                if (getActivity() == null) {
                    return;
                }
                if (success.equals("1")) {
                    loadCatEnd(verifyStatus, message, arrayListCat);
                } else {
                    errorMsg = getString(R.string.err_server);
                    setEmpty();
                }
                progressBar.setVisibility(View.GONE);
                isLoading = false;
            }
        }, helper.getAPIRequest(Method.METHOD_CAT, page, "", "",
                Callback.getSearchItem(), "","", "", "", "",
                "","","","search", null));
        loadCat.execute();
    }

    private void loadCatEnd(String verifyStatus, String message, ArrayList<ItemCat> arrayListCat) {
        if (!verifyStatus.equals("-1")) {
            if (arrayListCat.isEmpty()) {
                isOver = true;
                errorMsg = getString(R.string.err_no_cat_found);
                setEmpty();
            } else {
                page = page + 1;
                arrayList.addAll(arrayListCat);
                setAdapter();
            }
        } else {
            DialogUtil.verifyDialog(requireActivity(), getString(R.string.err_unauthorized_access), message, () -> {
            });
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setAdapter() {
        if (Boolean.FALSE.equals(isScroll)) {
            adapterCat = new AdapterCategories(getActivity(), arrayList, (itemCat, position) -> helper.showInterAd(position, ""));
            rv.setAdapter(adapterCat);
            setEmpty();
        } else {
            adapterCat.notifyDataSetChanged();
        }
    }

    public void setEmpty() {
        if (!arrayList.isEmpty()) {
            rv.setVisibility(View.VISIBLE);
            frameLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
        } else {
            rv.setVisibility(View.GONE);
            frameLayout.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.INVISIBLE);

            frameLayout.removeAllViews();
            LayoutInflater inflater = (LayoutInflater) requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            @SuppressLint("InflateParams") View myView = inflater.inflate(R.layout.row_empty, null);

            TextView textView = myView.findViewById(R.id.tv_empty_msg);
            textView.setText(errorMsg);

            myView.findViewById(R.id.ll_empty_try).setOnClickListener(v -> {
                myView.findViewById(R.id.iv_empty).setVisibility(View.GONE);
                myView.findViewById(R.id.pb_empty).setVisibility(View.VISIBLE);
                new Handler().postDelayed(() -> {
                    myView.findViewById(R.id.iv_empty).setVisibility(View.VISIBLE);
                    myView.findViewById(R.id.pb_empty).setVisibility(View.GONE);
                    loadCategories();
                }, 500);
            });

            frameLayout.addView(myView);
        }
    }
}