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
    private ProgressBar progressBar;
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
        View rootView = inflater.inflate(R.layout.fragment_latest, container, false);

        helper = new Helper(getActivity(), (position, type) -> openByCategory(position));

        try {
            homeSecId = getArguments().getString("id");
            isFromHome = true;
        } catch (Exception e) {
            homeSecId = "";
            isFromHome = false;
        }

        arrayList = new ArrayList<>();

        progressBar = rootView.findViewById(R.id.pb);
        frameLayout = rootView.findViewById(R.id.fl_empty);
        rv = rootView.findViewById(R.id.rv);

        android.widget.LinearLayout adView = rootView.findViewById(R.id.ll_adView);
        helper.showBannerAd(adView, Callback.PAGE_CAT_DETAILS);

        boolean isLandscape = ApplicationUtil.isLandscape(requireContext());
        glm = new GridLayoutManager(getActivity(), isLandscape ? 4 : 3);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return (adapterCategories.getItemViewType(position) == -2 || adapterCategories.isHeader(position)) ? glm.getSpanCount() : 1;
            }
        });
        rv.setLayoutManager(glm);
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.addOnScrollListener(new EndlessRecyclerViewScrollListener(glm) {
            @Override
            public void onLoadMore(int p, int totalItemsCount) {
                if (getActivity() == null){
                    return;
                }
                if (Boolean.FALSE.equals(isFromHome)) {
                    if (Boolean.FALSE.equals(isOver)) {
                        if (Boolean.FALSE.equals(isLoading)) {
                            isLoading = true;
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                isScroll = true;
                                loadCategories();
                            }, 0);
                        }
                    } else {
                        adapterCategories.hideHeader();
                    }
                } else {
                    adapterCategories.hideHeader();
                }
            }
        });

        setNativeShow(isLandscape);
        loadCategories();

        addMenuProvider();
        return rootView;
    }

    private void setNativeShow(boolean isLandscape) {
        if (isLandscape){
            if(Callback.getNativeAdShow()%5 != 0) {
                Callback.setNativeAdShow(Callback.getNativeAdShow() + 2);
            } else {
                Callback.setNativeAdShow(Callback.getNativeAdShow());
            }
        } else {
            if(Callback.getNativeAdShow()%3 != 0) {
                Callback.setNativeAdShow(Callback.getNativeAdShow() + 1);
            } else {
                Callback.setNativeAdShow(Callback.getNativeAdShow());
            }
        }
    }

    private void openByCategory(int position) {
        // Open blog posts filtered by this category
        FragmentBlog blogFrag = FragmentBlog.newInstance(
            Method.METHOD_BLOG_BY_CAT, arrayList.get(position).getId(), "", arrayList.get(position).getName());
        FragmentManager fm = getParentFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.hide(fm.getFragments().get(fm.getBackStackEntryCount()));
        ft.add(R.id.fragment, blogFrag, arrayList.get(position).getName());
        ft.addToBackStack(arrayList.get(position).getName());
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
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onEnd(String success, String verifyStatus, String message, ArrayList<ItemCat> arrayListCat) {
                if (getActivity() == null) {
                    return;
                }
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
                progressBar.setVisibility(View.GONE);
                isLoading = false;
            }
        }, helper.getAPIRequest(helperName, page, homeSecId, "", "", "",
                "", "", "", "","","","",
                "", null));
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
        page = page + 1;
        setAdapter();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setAdapter() {
        if (Boolean.FALSE.equals(isScroll)) {
            adapterCategories = new AdapterCategories(getActivity(), arrayList, (itemCat, position) -> helper.showInterAd(position, ""));
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
        @Override
        public boolean onQueryTextSubmit(@NonNull String s) {
            Callback.setSearchItem(s.replace(" ", "%20"));
            FragmentSearchCategories fSearch = new FragmentSearchCategories();
            FragmentManager fm = getParentFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.hide(getParentFragmentManager().getFragments().get(getParentFragmentManager().getBackStackEntryCount()));
            ft.add(R.id.fragment, fSearch, getString(R.string.search_categories));
            ft.addToBackStack(getString(R.string.search_categories));
            ft.commit();
            return true;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            return false;
        }
    };
}