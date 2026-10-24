package com.ethionews.app.fragment.online;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

import com.ethionews.app.R;
import com.ethionews.app.activity.RadioPlayerActivity;
import com.ethionews.app.adapter.AdapterRadio;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.callback.Method;
import com.ethionews.app.executor.LoadCat;
import com.ethionews.app.executor.LoadRadio;
import com.ethionews.app.interfaces.CategoryListener;
import com.ethionews.app.interfaces.RadioListener;
import com.ethionews.app.item.ItemCat;
import com.ethionews.app.item.ItemRadio;
import com.ethionews.app.utils.ApplicationUtil;
import com.ethionews.app.utils.helper.Helper;
import com.ethionews.app.utils.helper.SPHelper;
import com.ethionews.app.utils.recycler.EndlessRecyclerViewScrollListener;
import com.ethionews.app.utils.recycler.RecyclerItemClickListener;

import okhttp3.RequestBody;

/**
 * Radio station browser with category filtering and favourites.
 *
 * A scrollable {@link TabLayout} at the top provides:
 * <ul>
 *   <li><b>All Stations</b> – every radio (METHOD_RADIO_LIST)</li>
 *   <li>One tab per category – radios in that category (METHOD_RADIO_BY_CAT)</li>
 *   <li><b>Favorites</b> – the user's favourited radios (METHOD_RADIO_FAV)</li>
 * </ul>
 * Each tab switches the underlying data source while reusing the same
 * RecyclerView, adapter and pagination logic.
 */
public class FragmentRadio extends Fragment {

    private static final String TAG = "FragmentRadio";

    // Tab identifiers (stored in Tab.getTag)
    private static final int TAB_ALL = 0;
    private static final int TAB_FAV = -1;

    private Helper helper;
    private SPHelper spHelper;
    private RecyclerView rv;
    private AdapterRadio adapter;
    private ArrayList<ItemRadio> arrayList;
    private Boolean isOver = false;
    private Boolean isScroll = false;
    private int page = 1;
    private GridLayoutManager grid;
    private ProgressBar pb;
    private FloatingActionButton fab;
    private FrameLayout frameLayout;
    private String errorMsg;

    private TabLayout tabLayout;
    private final ArrayList<ItemCat> categoryList = new ArrayList<>();
    private boolean isRebuildingTabs = false;
    private boolean categoriesLoaded = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_radio, container, false);

        spHelper = new SPHelper(requireActivity());
        helper   = new Helper(getActivity(), (position, type) -> openRadio(position));

        arrayList = new ArrayList<>();

        tabLayout   = rootView.findViewById(R.id.tab_layout);
        frameLayout = rootView.findViewById(R.id.fl_empty);
        fab         = rootView.findViewById(R.id.fab);
        pb          = rootView.findViewById(R.id.pb);
        rv          = rootView.findViewById(R.id.rv);

        android.widget.LinearLayout adView = rootView.findViewById(R.id.ll_adView);
        helper.showBannerAd(adView, Callback.PAGE_CAT_DETAILS);

        setupTabs();
        setupRecyclerView();

        getData();
        loadCategories();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload when returning to the Favorites tab (e.g. after toggling a favourite
        // in RadioPlayerActivity) so the list reflects the latest server state.
        TabLayout.Tab currentTab = tabLayout.getTabAt(tabLayout.getSelectedTabPosition());
        if (currentTab != null && Integer.valueOf(TAB_FAV).equals(currentTab.getTag())) {
            onTabChanged(currentTab);
        }
    }

    // ── Tab layout ───────────────────────────────────────────────────

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.radio_all_stations)).setTag(TAB_ALL));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.radio_favorites)).setTag(TAB_FAV));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (isRebuildingTabs) return;
                onTabChanged(tab);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { /* no-op */ }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { /* no-op */ }
        });
    }

    private void loadCategories() {
        if (!helper.isNetworkAvailable()) return;
        LoadCat loadCat = new LoadCat(new CategoryListener() {
            @Override
            public void onStart() { /* no-op */ }

            @Override
            public void onEnd(String success, String verifyStatus, String message,
                              ArrayList<ItemCat> arrayListCat) {
                if (getActivity() == null) return;
                if (success.equals("1") && !arrayListCat.isEmpty()) {
                    categoryList.clear();
                    categoryList.addAll(arrayListCat);
                    rebuildTabs();
                }
            }
        }, helper.getAPIRequest(Method.METHOD_CAT, 1, "", "", "",
                "", "", "", "", "", "", "",
                "", "", null));
        loadCat.execute();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void rebuildTabs() {
        isRebuildingTabs = true;

        // Remember which tab tag was active so we can restore the exact tab after rebuild
        TabLayout.Tab previousTab = tabLayout.getTabAt(tabLayout.getSelectedTabPosition());
        Object previousTag = previousTab != null ? previousTab.getTag() : TAB_ALL;

        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.radio_all_stations)).setTag(TAB_ALL));
        for (ItemCat cat : categoryList) {
            tabLayout.addTab(tabLayout.newTab().setText(cat.getName()).setTag(cat.getId()));
        }
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.radio_favorites)).setTag(TAB_FAV));

        // Restore the previously active tab by matching its tag
        boolean restoredExactTab = false;
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab t = tabLayout.getTabAt(i);
            if (t != null && previousTag != null && previousTag.equals(t.getTag())) {
                tabLayout.selectTab(t);
                restoredExactTab = true;
                break;
            }
        }
        if (!restoredExactTab) {
            // Fall back to "All Stations" if the previous tab no longer exists
            tabLayout.selectTab(tabLayout.getTabAt(0));
        }

        isRebuildingTabs = false;
        categoriesLoaded = true;

        // onTabSelected is suppressed while isRebuildingTabs is true, so if the user
        // was already on the Favorites tab we need to trigger the data load manually.
        TabLayout.Tab restoredTab = tabLayout.getTabAt(tabLayout.getSelectedTabPosition());
        if (restoredTab != null && Integer.valueOf(TAB_FAV).equals(restoredTab.getTag())) {
            onTabChanged(restoredTab);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void onTabChanged(TabLayout.Tab tab) {
        Object tag = tab.getTag();

        // Reset data state for the new tab
        if (adapter != null) {
            arrayList.clear();
            adapter.notifyDataSetChanged();
        } else {
            arrayList.clear();
        }
        page = 1;
        isOver = false;
        isScroll = false;
        adapter = null;
        rv.setVisibility(View.GONE);

        // Favorites tab requires login
        if (tag instanceof Integer && (Integer) tag == TAB_FAV) {
            if (!spHelper.isLogged()) {
                errorMsg = getString(R.string.radio_login_to_fav);
                setEmpty();
                return;
            }
        }

        getData();
    }

    // ── RecyclerView setup ───────────────────────────────────────────

    private void setupRecyclerView() {
        boolean isLandscape = ApplicationUtil.isLandscape(requireContext());
        grid = new GridLayoutManager(getActivity(), isLandscape ? 4 : 3);
        grid.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter != null && adapter.isHeader(position) ? grid.getSpanCount() : 1;
            }
        });
        rv.setLayoutManager(grid);
        rv.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(),
                (view, position) -> helper.showInterAd(position, "")));
        rv.addOnScrollListener(new EndlessRecyclerViewScrollListener(grid) {
            @Override
            public void onLoadMore(int p, int totalItemsCount) {
                if (getActivity() == null) return;
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
                if (grid.findFirstVisibleItemPosition() > 6) fab.show();
                else fab.hide();
            }
        });
        fab.setOnClickListener(v -> rv.smoothScrollToPosition(0));
    }

    // ── Data loading ─────────────────────────────────────────────────

    private void getData() {
        if (!helper.isNetworkAvailable()) {
            errorMsg = getString(R.string.err_internet_not_connected);
            setEmpty();
            return;
        }

        TabLayout.Tab currentTab = tabLayout.getTabAt(tabLayout.getSelectedTabPosition());
        Object tag = currentTab != null ? currentTab.getTag() : TAB_ALL;

        RequestBody request;
        String userId = spHelper.getUserId();

        if (tag instanceof Integer) {
            int tagInt = (Integer) tag;
            if (tagInt == TAB_FAV) {
                // Favourites tab
                request = helper.getAPIRequest(Method.METHOD_RADIO_FAV, page, "", "",
                        "", "", userId, "", "", "", "",
                        "", "", "", null);
            } else {
                // "All" tab
                request = helper.getAPIRequest(Method.METHOD_RADIO_LIST, page, "", "",
                        "", "", userId, "", "", "", "",
                        "", "", "", null);
            }
        } else if (tag instanceof String) {
            // Category tab — tag holds the category id
            String catId = (String) tag;
            request = helper.getAPIRequest(Method.METHOD_RADIO_BY_CAT, page, "", catId,
                    "", "", userId, "", "", "", "",
                    "", "", "", null);
        } else {
            request = helper.getAPIRequest(Method.METHOD_RADIO_LIST, page, "", "",
                    "", "", userId, "", "", "", "",
                    "", "", "", null);
        }

        LoadRadio loadRadio = new LoadRadio(new RadioListener() {
            @Override
            public void onStart() {
                if (arrayList.isEmpty()) {
                    frameLayout.setVisibility(View.GONE);
                    rv.setVisibility(View.GONE);
                    pb.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onEnd(String success, String verifyStatus, String message,
                             ArrayList<ItemRadio> arrayListData) {
                if (getActivity() == null) return;
                if (success.equals("1")) {
                    loadEnd(arrayListData);
                } else {
                    errorMsg = getString(R.string.err_server_not_connected);
                    setEmpty();
                }
            }
        }, request);
        loadRadio.execute();
    }

    private void loadEnd(ArrayList<ItemRadio> data) {
        if (data.isEmpty()) {
            isOver = true;
            if (adapter != null) adapter.hideHeader();
            errorMsg = getString(R.string.err_no_data_found);
            setEmpty();
        } else {
            arrayList.addAll(data);
            page++;
            setAdapter();
        }
    }

    // ── Adapter ───────────────────────────────────────────────────────

    @SuppressLint("NotifyDataSetChanged")
    private void setAdapter() {
        if (Boolean.FALSE.equals(isScroll)) {
            adapter = new AdapterRadio(requireActivity(), arrayList, new AdapterRadio.RecyclerItemClickListener() {
                @Override
                public void onClick(int position) {
                    helper.showInterAd(position, "");
                }

                @Override
                public void onRewardAds(int position) {
                    if (spHelper.getRewardCredit() != 0) {
                        spHelper.useRewardCredit(1);
                        openRadio(position);
                        Toast.makeText(requireActivity(),
                                "Your Total Credit (" + spHelper.getRewardCredit() + ")",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        helper.showRewardAds(position, (isLoad, pos) -> {
                            if (isLoad) {
                                spHelper.addRewardCredit(Callback.getRewardCredit());
                                spHelper.useRewardCredit(1);
                                openRadio(pos);
                                Toast.makeText(requireActivity(),
                                        "Your Total Credit (" + spHelper.getRewardCredit() + ")",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireActivity(), "Display Failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
            rv.setAdapter(adapter);
            rv.scheduleLayoutAnimation();
        } else {
            if (adapter != null) adapter.notifyDataSetChanged();
        }
        setEmpty();
    }

    // ── Navigation ────────────────────────────────────────────────────

    private void openRadio(int position) {
        if (adapter == null || adapter.getItem(position) == null) return;
        ItemRadio item = arrayList.get(position);
        Intent intent = new Intent(getActivity(), RadioPlayerActivity.class);
        intent.putExtra("radio_id", item.getId());
        intent.putExtra("radio_name", item.getName());
        intent.putExtra("stream_url", item.getStreamUrl());
        intent.putExtra("logo", item.getLogo());
        intent.putExtra("genre", item.getGenre());
        intent.putExtra("description", item.getDescription());
        intent.putExtra("isPremium", item.getIsPremium());
        intent.putExtra("is_favourite", item.getIsFav());
        startActivity(intent);
    }

    // ── Empty state ───────────────────────────────────────────────────

    private void setEmpty() {
        if (getActivity() == null) return;
        pb.setVisibility(View.GONE);
        if (!arrayList.isEmpty()) {
            rv.setVisibility(View.VISIBLE);
            frameLayout.setVisibility(View.GONE);
        } else {
            rv.setVisibility(View.GONE);
            frameLayout.setVisibility(View.VISIBLE);
            frameLayout.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            @SuppressLint("InflateParams")
            View emptyView = inflater.inflate(R.layout.row_empty, null);
            android.widget.TextView tv = emptyView.findViewById(R.id.tv_empty_msg);
            tv.setText(errorMsg);
            emptyView.findViewById(R.id.ll_empty_try).setOnClickListener(v -> {
                emptyView.findViewById(R.id.iv_empty).setVisibility(View.GONE);
                emptyView.findViewById(R.id.pb_empty).setVisibility(View.VISIBLE);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    emptyView.findViewById(R.id.iv_empty).setVisibility(View.VISIBLE);
                    emptyView.findViewById(R.id.pb_empty).setVisibility(View.GONE);
                    getData();
                }, 500);
            });
            frameLayout.addView(emptyView);
        }
    }
}
