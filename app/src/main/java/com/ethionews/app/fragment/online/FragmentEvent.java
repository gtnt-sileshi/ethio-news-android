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
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.ads.mediation.facebook.FacebookMediationAdapter;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import com.ethionews.app.R;
import com.ethionews.app.activity.VideoDetailsActivity;
import com.ethionews.app.adapter.AdapterEvent;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.callback.Method;
import com.ethionews.app.dialog.DialogUtil;
import com.ethionews.app.executor.LoadEvent;
import com.ethionews.app.interfaces.EventListener;
import com.ethionews.app.item.ItemEvent;
import com.ethionews.app.utils.helper.Helper;
import com.ethionews.app.utils.recycler.EndlessRecyclerViewScrollListener;

public class FragmentEvent extends Fragment {

    private static final String TAG = "FragmentEvent";
    private Helper helper;
    private RecyclerView rv;
    private AdapterEvent adapter;
    private ArrayList<ItemEvent> arrayList;
    private Boolean isOver = false;
    private Boolean isScroll = false;
    private int page = 1;
    private int nativeAdPos = 0;
    private ProgressBar pb;
    private FloatingActionButton fab;
    private String errorMsg;
    private FrameLayout frameLayout;
    private SearchView searchView;

    private AdLoader adLoader;
    private final ArrayList<NativeAd> arrayListNativeAds = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_latest, container, false);

        helper = new Helper(getActivity(), (position, type) -> {
            Intent intent1 = new Intent(getActivity(), VideoDetailsActivity.class);
            intent1.putExtra("post_id", arrayList.get(position).getPostId());
            startActivity(intent1);
        });

        arrayList = new ArrayList<>();

        frameLayout = rootView.findViewById(R.id.fl_empty);
        fab = rootView.findViewById(R.id.fab);
        pb = rootView.findViewById(R.id.pb);
        rv = rootView.findViewById(R.id.rv);

        android.widget.LinearLayout adView = rootView.findViewById(R.id.ll_adView);
        helper.showBannerAd(adView, Callback.PAGE_CAT_DETAILS);

        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        rv.setLayoutManager(llm);
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.setNestedScrollingEnabled(false);

        rv.addOnScrollListener(new EndlessRecyclerViewScrollListener(llm) {
            @Override
            public void onLoadMore(int p, int totalItemsCount) {
                if (getActivity() == null) {
                    return;
                }
                if (Boolean.FALSE.equals(isOver)) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        isScroll = true;
                        getData();
                    }, 0);
                } else {
                    if (adapter != null) adapter.hideHeader();
                }
            }
        });
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int firstVisibleItem = llm.findFirstVisibleItemPosition();
                if (firstVisibleItem > 6) {
                    fab.show();
                } else {
                    fab.hide();
                }
            }
        });

        fab.setOnClickListener(v -> rv.smoothScrollToPosition(0));

        if(Callback.getNativeAdShow()%3 != 0) {
            nativeAdPos = Callback.getNativeAdShow() + 1;
        } else {
            nativeAdPos = Callback.getNativeAdShow();
        }

        getData();

        addMenuProvider();
        return rootView;
    }

    private void getData() {
        if (!helper.isNetworkAvailable()) {
            errorMsg = getString(R.string.err_internet_not_connected);
            setEmpty();
            return;
        }
        LoadEvent loadEvent = new LoadEvent(new EventListener() {
            @Override
            public void onStart() {
                if (arrayList.isEmpty()) {
                    frameLayout.setVisibility(View.GONE);
                    rv.setVisibility(View.GONE);
                    pb.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onEnd(String success, String verifyStatus, String message, ArrayList<ItemEvent> arrayListData) {
                if (getActivity() == null) {
                    return;
                }
                if (success.equals("1")) {
                    loadEventEnd(verifyStatus, message, arrayListData);
                } else {
                    errorMsg = getString(R.string.err_server_not_connected);
                    setEmpty();
                }
            }
        }, helper.getAPIRequest(Method.METHOD_EVENT, page, "", "", "", "",
                "", "", "", "", "", "", "",
                "", null));
        loadEvent.execute();
    }

    private void loadEventEnd(String verifyStatus, String message, ArrayList<ItemEvent> arrayListData) {
        if (verifyStatus.equals("-1")) {
            DialogUtil.verifyDialog(requireActivity(), getString(R.string.err_unauthorized_access), message, () -> {
            });
            errorMsg = getString(R.string.err_no_data_found);
            setEmpty();
            return;
        }
        if (arrayListData.isEmpty()) {
            isOver = true;
            if (adapter != null) adapter.hideHeader();
            errorMsg = getString(R.string.err_no_data_found);
            setEmpty();
        } else {
            for (int i = 0; i < arrayListData.size(); i++) {
                arrayList.add(arrayListData.get(i));
                if (helper.canLoadNativeAds(requireContext(),Callback.PAGE_NATIVE_POST)) {
                    int abc = arrayList.lastIndexOf(null);
                    if (nativeAdPos != 0 && ((arrayList.size() - (abc + 1)) % nativeAdPos == 0)) {
                        arrayList.add(null);
                    }
                }
            }
            page = page + 1;
            setAdapter();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setAdapter() {
        if(Boolean.FALSE.equals(isScroll)) {
            adapter = new AdapterEvent(getContext(), arrayList, position -> helper.showInterAd(position,""));
            rv.setAdapter(adapter);
            rv.scheduleLayoutAnimation();
            setEmpty();
            loadNativeAds();
        } else {
            if (adapter != null) adapter.notifyDataSetChanged();
        }
    }

    private void loadNativeAds() {
        if (helper.canLoadNativeAds(requireContext(),Callback.PAGE_NATIVE_POST)
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
                    if (!adLoader.isLoading() &&adapter != null) {
                        adapter.addAds(arrayListNativeAds);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error addAds", e);
                }
            }).build();
            adLoader.loadAds(adRequest, 5);
        }
    }

    private void setEmpty() {
        if (!arrayList.isEmpty()) {
            rv.setVisibility(View.VISIBLE);
            pb.setVisibility(View.INVISIBLE);
            frameLayout.setVisibility(View.GONE);
        } else {
            rv.setVisibility(View.GONE);
            frameLayout.setVisibility(View.VISIBLE);
            pb.setVisibility(View.INVISIBLE);

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
                    getData();
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
                 searchView = (SearchView) item.getActionView();
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
        public boolean onQueryTextSubmit(String s) {
            return false;
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public boolean onQueryTextChange(String s) {
            if (adapter != null && (!searchView.isIconified())) {
                adapter.getFilter().filter(s);
                adapter.notifyDataSetChanged();
            }
            return true;
        }
    };

}