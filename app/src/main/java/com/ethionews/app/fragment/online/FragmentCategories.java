package com.ethionews.app.fragment.online;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.shimmer.ShimmerFrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.ads.mediation.facebook.FacebookMediationAdapter;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.nativead.NativeAd;

import java.util.ArrayList;

import com.ethionews.app.R;
import com.ethionews.app.adapter.AdapterCategories;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.callback.Method;
import com.ethionews.app.dialog.DialogUtil;
import com.ethionews.app.executor.LoadCat;
import com.ethionews.app.fragment.search.FragmentSearchCategories;
import com.ethionews.app.interfaces.CategoryListener;
import com.ethionews.app.item.ItemCat;
import com.ethionews.app.utils.ApplicationUtil;
import com.ethionews.app.utils.helper.Helper;
import com.ethionews.app.utils.recycler.EndlessRecyclerViewScrollListener;

public class FragmentCategories extends Fragment {

    private static final String TAG = "FragmentCategories";
    private Helper helper;
    private RecyclerView rv;
    private AdapterCategories adapterCategories;
    private ArrayList<ItemCat> arrayList;
    private ShimmerFrameLayout shimmerCategories;
    private FrameLayout frameLayout;
    private GridLayoutManager glm;
    private Boolean isLoading = false;
    private String errorMsg = "";
    private String homeSecId = "";
    private int page = 1;
    private int nativeAdPos = 0;
    private Boolean isOver = false;
    private Boolean isScroll = false;
    private Boolean isFromHome = false;

    private AdLoader adLoader;
    private final ArrayList<NativeAd> arrayListNativeAds = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_categories, container, false);

        helper = new Helper(getActivity(), (position, type) -> openByCategory(position));

        try {
            homeSecId = getArguments().getString("id");
            isFromHome = true;
        } catch (Exception e) {
            homeSecId = "";
            isFromHome = false;
        }

        arrayList = new ArrayList<>();

        // Clear any stale search from other pages so categories always loads unfiltered
        Callback.setSearchItem("");

        shimmerCategories = rootView.findViewById(R.id.shimmer_categories);
        frameLayout = rootView.findViewById(R.id.fl_empty);
        rv = rootView.findViewById(R.id.rv);

        android.widget.LinearLayout adView = rootView.findViewById(R.id.ll_adView);
        helper.showBannerAd(adView, Callback.PAGE_CAT_DETAILS);

        glm = new GridLayoutManager(getActivity(), 1);
        rv.setLayoutManager(glm);
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        loadCategories();

        addMenuProvider();
        return rootView;
    }

    private void openByCategory(int position) {
        ItemCat cat = arrayList.get(position);
        String type = cat.getType();

        if (ItemCat.TYPE_LIVE.equals(type)) {
            // Navigate to TV list filtered by this category
            com.ethionews.app.fragment.online.FragmentLatest tvFrag =
                new com.ethionews.app.fragment.online.FragmentLatest();
            android.os.Bundle args = new android.os.Bundle();
            args.putString("cat_id", cat.getId());
            args.putString("cat_name", cat.getName());
            tvFrag.setArguments(args);
            loadChildFrag(tvFrag, cat.getName());

        } else if (ItemCat.TYPE_RADIO.equals(type)) {
            // Navigate to Radio list filtered by this category
            com.ethionews.app.fragment.online.FragmentRadio radioFrag =
                new com.ethionews.app.fragment.online.FragmentRadio();
            android.os.Bundle args = new android.os.Bundle();
            args.putString("cat_id", cat.getId());
            args.putString("cat_name", cat.getName());
            radioFrag.setArguments(args);
            loadChildFrag(radioFrag, cat.getName());

        } else {
            // 'blog' or 'all' → open blog post list for this category
            FragmentBlog blogFrag = FragmentBlog.newInstance(
                Method.METHOD_BLOG_BY_CAT, cat.getId(), "", cat.getName());
            loadChildFrag(blogFrag, cat.getName());
        }
    }

    private void loadChildFrag(androidx.fragment.app.Fragment frag, String name) {
        FragmentManager fm = getParentFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.hide(fm.getFragments().get(fm.getBackStackEntryCount()));
        ft.add(R.id.fragment, frag, name);
        ft.addToBackStack(name);
        ft.commit();
    }

    private void loadCategories() {
        if (!helper.isNetworkAvailable()) {
            errorMsg = getString(R.string.err_internet_not_connected);
            setEmpty();
            return;
        }
        String helperName = Boolean.TRUE.equals(isFromHome) ? Method.METHOD_HOME_DETAILS : Method.METHOD_CAT;
        LoadCat loadCat = new LoadCat(new CategoryListener() {
            @Override
            public void onStart() {
                if (arrayList.isEmpty()) {
                    frameLayout.setVisibility(View.GONE);
                    rv.setVisibility(View.GONE);
                    shimmerCategories.setVisibility(View.VISIBLE);
                    shimmerCategories.startShimmer();
                }
            }

            @Override
            public void onEnd(String success, String verifyStatus, String message, ArrayList<ItemCat> arrayListCat) {
                if (getActivity() == null) {
                    return;
                }
                shimmerCategories.stopShimmer();
                shimmerCategories.setVisibility(View.GONE);
                if (success.equals("1")) {
                    loadCatEnd(verifyStatus, message, arrayListCat);
                } else {
                    isOver = true;
                    try {
                        adapterCategories.hideHeader();
                    } catch (Exception e) {
                        Log.e(TAG, "Error hideHeader", e);
                    }
                    errorMsg = getString(R.string.err_server);
                    setEmpty();
                }
                isLoading = false;
            }
        }, helper.getAPIRequest(helperName, page, homeSecId, "", Callback.getSearchItem(), "",
                "", "", "", "", "", "", "",
                Method.CAT_TYPE_BLOG, null));
        loadCat.execute();
    }

    private void loadCatEnd(String verifyStatus, String message, ArrayList<ItemCat> arrayListCat) {
        if (verifyStatus.equals("-1")) {
            DialogUtil.verifyDialog(requireActivity(), getString(R.string.err_unauthorized_access), message, () -> {
            });
            return;
        }
        if (arrayListCat.isEmpty()) {
            isOver = true;
            errorMsg = getString(R.string.err_no_cat_found);
            if (Boolean.FALSE.equals(isFromHome)) {
                try {
                    adapterCategories.hideHeader();
                } catch (Exception e) {
                    Log.e(TAG, "Error hideHeader", e);
                }
            }
            setEmpty();
            return;
        }

        for (int i = 0; i < arrayListCat.size(); i++) {
            arrayList.add(arrayListCat.get(i));
            if (helper.canLoadNativeAds(requireContext(),Callback.PAGE_NATIVE_CAT)) {
                int abc = arrayList.lastIndexOf(null);
                if (nativeAdPos != 0 && ((arrayList.size() - (abc + 1)) % nativeAdPos == 0)) {
                    arrayList.add(null);
                }
            }
        }
        // Categories API returns everything in one shot — no real pagination.
        // Stop after the first load so endless scroll doesn't re-fetch duplicates.
        isOver = true;
        page = page + 1;
        setAdapter();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setAdapter() {
        if (Boolean.FALSE.equals(isScroll)) {
            adapterCategories = new AdapterCategories(getActivity(), arrayList, (itemCat, position) -> helper.showInterAd(position, ""));
            // No pagination — hide the footer spinner immediately
            adapterCategories.hideHeader();
            rv.setAdapter(adapterCategories);
            setEmpty();
            loadNativeAds();
        } else {
            adapterCategories.notifyDataSetChanged();
        }
    }

    private void loadNativeAds() {
        if (helper.canLoadNativeAds(requireContext(),Callback.PAGE_NATIVE_CAT)
                && Callback.getAdNetwork().equals(Callback.AD_TYPE_ADMOB)
                || Callback.getAdNetwork().equals(Callback.AD_TYPE_META) && arrayList.size() >= 10) {
            AdLoader.Builder builder = new AdLoader.Builder(requireContext(), Callback.getAdmobNativeAdID());

            Bundle extras = new Bundle();

            AdRequest adRequest;
            if(Callback.getAdNetwork().equals(Callback.AD_TYPE_ADMOB)) {
                adRequest = new AdRequest.Builder()
                        .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                        .build();
            } else {
                adRequest = new AdRequest.Builder()
                        .addNetworkExtrasBundle(AdMobAdapter.class, new Bundle())
                        .addNetworkExtrasBundle(FacebookMediationAdapter.class, extras)
                        .build();
            }
            adLoader = builder.forNativeAd(nativeAd -> {
                try {
                    arrayListNativeAds.add(nativeAd);
                    if (!adLoader.isLoading() && adapterCategories != null) {
                        adapterCategories.addAds(arrayListNativeAds);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error addAds", e);
                }
            }).build();
            adLoader.loadAds(adRequest, 5);
        }
    }

    public void setEmpty() {
        if (!arrayList.isEmpty()) {
            rv.setVisibility(View.VISIBLE);
            frameLayout.setVisibility(View.GONE);
            shimmerCategories.stopShimmer();
            shimmerCategories.setVisibility(View.GONE);
        } else {
            rv.setVisibility(View.GONE);
            frameLayout.setVisibility(View.VISIBLE);
            shimmerCategories.stopShimmer();
            shimmerCategories.setVisibility(View.GONE);

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

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public boolean onQueryTextSubmit(@NonNull String s) {
            searchHandler.removeCallbacks(searchRunnable);
            doSearch(s);
            return true;
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public boolean onQueryTextChange(String s) {
            searchHandler.removeCallbacks(searchRunnable);
            if (s.trim().isEmpty()) {
                // Cleared — reset to full unfiltered list
                Callback.setSearchItem("");
                resetAndReload();
                return true;
            }
            searchRunnable = () -> doSearch(s);
            searchHandler.postDelayed(searchRunnable, 400);
            return true;
        }

        @SuppressLint("NotifyDataSetChanged")
        private void doSearch(String s) {
            Callback.setSearchItem(s.replace(" ", "%20"));
            resetAndReload();
        }

        @SuppressLint("NotifyDataSetChanged")
        private void resetAndReload() {
            page = 1;
            isOver = false;
            isScroll = false;
            arrayList.clear();
            if (adapterCategories != null) adapterCategories.notifyDataSetChanged();
            loadCategories();
        }
    };
}