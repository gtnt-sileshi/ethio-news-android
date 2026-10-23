package com.ethionews.app.fragment.online;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
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
import com.ethionews.app.adapter.AdapterVideo;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.callback.Method;
import com.ethionews.app.dialog.DialogUtil;
import com.ethionews.app.executor.LoadLive;
import com.ethionews.app.fragment.search.FragmentSearchLive;
import com.ethionews.app.interfaces.LiveListener;
import com.ethionews.app.item.ItemData;
import com.ethionews.app.utils.ApplicationUtil;
import com.ethionews.app.utils.helper.Helper;
import com.ethionews.app.utils.helper.SPHelper;
import com.ethionews.app.utils.recycler.EndlessRecyclerViewScrollListener;
import com.ethionews.app.utils.recycler.RecyclerItemClickListener;

public class FragmentTrending extends Fragment {

    private static final String TAG = "FragmentTrending";
    private Helper helper;
    private SPHelper spHelper;
    private RecyclerView rv;
    private AdapterVideo adapter;
    private ArrayList<ItemData> arrayList;
    private int nativeAdPos = 0;
    private GridLayoutManager grid;
    private ProgressBar pb;
    private FloatingActionButton fab;
    private String errorMsg;
    private FrameLayout frameLayout;

    private AdLoader adLoader;
    private final ArrayList<NativeAd> arrayListNativeAds = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_latest, container, false);

        spHelper = new SPHelper(requireActivity());
        helper = new Helper(getActivity(), (position, type) -> playLive(position));

        arrayList = new ArrayList<>();

        frameLayout = rootView.findViewById(R.id.fl_empty);
        fab = rootView.findViewById(R.id.fab);
        pb = rootView.findViewById(R.id.pb);
        rv = rootView.findViewById(R.id.rv);

        android.widget.LinearLayout adView = rootView.findViewById(R.id.ll_adView);
        helper.showBannerAd(adView, Callback.PAGE_CAT_DETAILS);

        grid = new GridLayoutManager(getActivity(), 1);
        boolean isLandscape = ApplicationUtil.isLandscape(requireContext());
        grid.setSpanCount(isLandscape ? 4 : 3);
        grid.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return (adapter.getItemViewType(position) == -2 || adapter.isHeader(position)) ? grid.getSpanCount() : 1;
            }
        });
        rv.setLayoutManager(grid);
        rv.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), (view, position) -> helper.showInterAd(position, "")));
        rv.addOnScrollListener(new EndlessRecyclerViewScrollListener(grid) {
            @Override
            public void onLoadMore(int p, int totalItemsCount) {
                if (getActivity() == null) {
                    return;
                }
                try {
                    adapter.hideHeader();
                } catch (Exception e) {
                    Log.e(TAG, "Error hideHeader", e);
                }
            }
        });

        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int firstVisibleItem = grid.findFirstVisibleItemPosition();
                if (firstVisibleItem > 6) {
                    fab.show();
                } else {
                    fab.hide();
                }
            }
        });
        fab.setOnClickListener(v -> rv.smoothScrollToPosition(0));

        setNativeShow(isLandscape);
        getData();
        addMenuProvider();
        return rootView;
    }

    private void setNativeShow(boolean isLandscape) {
        if (isLandscape){
            if(Callback.getNativeAdShow()%5 != 0) {
                nativeAdPos = Callback.getNativeAdShow() + 2;
            } else {
                nativeAdPos = Callback.getNativeAdShow();
            }
        } else {
            if(Callback.getNativeAdShow()%3 != 0) {
                nativeAdPos = Callback.getNativeAdShow() + 1;
            } else {
                nativeAdPos = Callback.getNativeAdShow();
            }
        }
    }

    private void getData() {
        if (!helper.isNetworkAvailable()) {
            errorMsg = getString(R.string.err_internet_not_connected);
            setEmpty();
            return;
        }
        LoadLive loadCategory = new LoadLive(new LiveListener() {
            @Override
            public void onStart() {
                if (arrayList.isEmpty()) {
                    frameLayout.setVisibility(View.GONE);
                    rv.setVisibility(View.GONE);
                    pb.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onEnd(String success, String verifyStatus, String message, ArrayList<ItemData> arrayListData) {
                if (getActivity() == null) {
                    return;
                }
                if (success.equals("1")) {
                    if (!verifyStatus.equals("-1")) {
                        loadCategoryEnd(arrayListData);
                    } else {
                        DialogUtil.verifyDialog(requireActivity(), getString(R.string.err_unauthorized_access), message, () -> {
                        });
                        setEmpty();
                    }
                } else {
                    errorMsg = getString(R.string.err_server_not_connected);
                    setEmpty();
                }
            }
        }, helper.getAPIRequest(Method.METHOD_MOST_VIEWED, 0, "", "",
                "", "", "", "", "", "",
                "", "", "", "", null));
        loadCategory.execute();
    }

    private void loadCategoryEnd(ArrayList<ItemData> arrayListData) {
        if (arrayListData.isEmpty()) {
            errorMsg = getString(R.string.err_no_data_found);
            setEmpty();
        } else {
            for (int i = 0; i < arrayListData.size(); i++) {
                arrayList.add(arrayListData.get(i));
                if (Boolean.TRUE.equals(Callback.getIsNativeAdPost() && Callback.getIsAdsStatus())) {
                    int abc = arrayList.lastIndexOf(null);
                    if (nativeAdPos != 0 && ((arrayList.size() - (abc + 1)) % nativeAdPos == 0)) {
                        arrayList.add(null);
                    }
                }
            }
            setAdapter();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setAdapter() {
        adapter = new AdapterVideo(requireActivity(), arrayList, new AdapterVideo.RecyclerItemClickListener() {
            @Override
            public void onClick(int position) {
                helper.showInterAd(position, "");
            }

            @Override
            public void onRewardAds(int position) {
                if (spHelper.getRewardCredit() != 0){
                    spHelper.useRewardCredit(1);
                    playLive(position);
                    Toast.makeText(requireActivity(), "Your Total Credit ("+spHelper.getRewardCredit()+")", Toast.LENGTH_SHORT).show();
                } else {
                    helper.showRewardAds(position, (isLoad, pos) -> {
                        if (isLoad){
                            spHelper.addRewardCredit(Callback.getRewardCredit());
                            spHelper.useRewardCredit(1);
                            playLive(pos);
                            Toast.makeText(requireActivity(), "Your Total Credit ("+spHelper.getRewardCredit()+")", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireActivity(), "Display Failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
        rv.setAdapter(adapter);
        rv.scheduleLayoutAnimation();
        setEmpty();
        loadNativeAds();
    }

    private void playLive(int position) {
        if(adapter.getItem(position) != null) {
            Intent intent1 = new Intent(requireActivity(), VideoDetailsActivity.class);
            intent1.putExtra("post_id", arrayList.get(position).getId());
            startActivity(intent1);
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
                    if (!adLoader.isLoading() && adapter != null) {
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
        public boolean onQueryTextSubmit(String s) {
            Callback.setSearchItem(s.replace(" ", "%20"));
            FragmentSearchLive search = new FragmentSearchLive();
            FragmentManager fm = getParentFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.hide(getParentFragmentManager().getFragments().get(getParentFragmentManager().getBackStackEntryCount()));
            ft.add(R.id.fragment, search, getString(R.string.search));
            ft.addToBackStack(getString(R.string.search));
            ft.commit();
            return false;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            return true;
        }
    };
}