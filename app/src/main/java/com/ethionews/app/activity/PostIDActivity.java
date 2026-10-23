package com.ethionews.app.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
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
import com.ethionews.app.adapter.AdapterVideo;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.callback.Method;
import com.ethionews.app.dialog.DialogUtil;
import com.ethionews.app.executor.LoadLive;
import com.ethionews.app.interfaces.LiveListener;
import com.ethionews.app.item.ItemData;
import com.ethionews.app.utils.ApplicationUtil;
import com.ethionews.app.utils.IfSupported;
import com.ethionews.app.utils.helper.Helper;
import com.ethionews.app.utils.helper.SPHelper;
import com.ethionews.app.utils.recycler.EndlessRecyclerViewScrollListener;
import okhttp3.RequestBody;

public class PostIDActivity extends AppCompatActivity {

    private static final String TAG = "PostIDActivity";
    private Helper helper;
    private SPHelper spHelper;
    private RecyclerView rv;
    private AdapterVideo adapter;
    private ArrayList<ItemData> arrayList;
    private Boolean isOver = false;
    private Boolean isScroll = false;
    private int page = 1;
    private int nativeAdPos = 0;
    private GridLayoutManager grid;
    private ProgressBar pb;
    private FloatingActionButton fab;
    private String errorMsg;
    private FrameLayout frameLayout;
    private String id = "";
    private String name = "";
    private String pageType = "latest";

    private AdLoader adLoader;
    private final ArrayList<NativeAd> arrayListNativeAds = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IfSupported.applyTheme(this);
        setContentView(R.layout.activity_post_id);
        IfSupported.isRTL(this);
        IfSupported.isScreenshot(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(view -> finish());
        try {
            setTitle(name);
        } catch (Exception e) {
            Log.e(TAG, "Error title: ", e);
        }

        Intent intent = getIntent();
        id = intent.getStringExtra("id");
        name = intent.getStringExtra("name");
        pageType = intent.getStringExtra("page_type");

        helper = new Helper(this);
        helper = new Helper(PostIDActivity.this, (position, type) -> playLive(position));

        spHelper = new SPHelper(this);

        arrayList = new ArrayList<>();

        frameLayout = findViewById(R.id.fl_empty);
        fab = findViewById(R.id.fab);
        pb = findViewById(R.id.pb);
        rv = findViewById(R.id.rv);

        grid = new GridLayoutManager(PostIDActivity.this, 1);
        boolean isLandscape = ApplicationUtil.isLandscape(PostIDActivity.this);
        grid.setSpanCount(isLandscape ? 4 : 3);
        grid.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return (adapter.getItemViewType(position) == -2 || adapter.isHeader(position)) ? grid.getSpanCount() : 1;
            }
        });
        rv.setLayoutManager(grid);
        rv.addOnScrollListener(new EndlessRecyclerViewScrollListener(grid) {
            @Override
            public void onLoadMore(int p, int totalItemsCount) {
                if (Boolean.FALSE.equals(isOver)) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        isScroll = true;
                        getData();
                    }, 0);
                } else {
                    adapter.hideHeader();
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
        getPage();
    }

    private void getPage() {
        if (pageType.equals(getString(R.string.favourite))){
            if(spHelper.isLogged()) {
                getData();
            } else {
                errorMsg = getString(R.string.not_log);
                setEmpty();
            }
        } else {
            getData();
        }
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

    private void getData() {
        if (!helper.isNetworkAvailable()) {
            errorMsg = getString(R.string.err_internet_not_connected);
            setEmpty();
            return;
        }

        RequestBody requestBody;
        if (pageType.equals(getString(R.string.categories))){
            requestBody = helper.getAPIRequest(Method.METHOD_CAT_ID, page, "", id,
                    "", "", "", "", "", "",
                    "", "", "", "", null);
        } else if (pageType.equals(getString(R.string.favourite))){
            requestBody = helper.getAPIRequest(Method.METHOD_POST_BY_FAV, page, "",
                    "", "", "", spHelper.getUserId(), "",
                    "", "", "", "", "", "", null);
        } else if (pageType.equals("banner")) {
            requestBody = helper.getAPIRequest(Method.METHOD_POST_BY_BANNER, page, id, "",
                    "", "","", "", "", "",
                    "", "", "", "", null);
        } else {
            requestBody = helper.getAPIRequest(Method.METHOD_LATEST, page, "", "",
                    "", "", "", "", "", "",
                    "", "", "", "", null);
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
                if (!success.equals("1")) {
                    errorMsg = getString(R.string.err_server_not_connected);
                    setEmpty();
                    return;
                }
                if (verifyStatus.equals("-1")) {
                    DialogUtil.verifyDialog(PostIDActivity.this, getString(R.string.err_unauthorized_access), message, () -> {
                    });
                    errorMsg = getString(R.string.err_no_data_found);
                    setEmpty();
                    return;
                }
                loadCategoryEnd(arrayListData);
            }
        }, requestBody);
        loadCategory.execute();
    }

    private void loadCategoryEnd(ArrayList<ItemData> arrayListData) {
        if (arrayListData.isEmpty()) {
            isOver = true;
            try {
                adapter.hideHeader();
            } catch (Exception e) {
                Log.e(TAG, "Error hide header: ", e);
            }
            errorMsg = getString(R.string.err_no_data_found);
            setEmpty();
        } else {
            for (int i = 0; i < arrayListData.size(); i++) {
                arrayList.add(arrayListData.get(i));
                if (helper.canLoadNativeAds(PostIDActivity.this,Callback.PAGE_NATIVE_POST)) {
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
            adapter = new AdapterVideo(PostIDActivity.this, arrayList, new AdapterVideo.RecyclerItemClickListener() {
                @Override
                public void onClick(int position) {
                    helper.showInterAd(position, "");
                }

                @Override
                public void onRewardAds(int position) {
                    if (spHelper.getRewardCredit() != 0){
                        spHelper.useRewardCredit(1);
                        playLive(position);
                        Toast.makeText(PostIDActivity.this, "Your Total Credit ("+spHelper.getRewardCredit()+")", Toast.LENGTH_SHORT).show();
                    } else {
                        helper.showRewardAds(position, (isLoad, pos) -> {
                            if (isLoad){
                                spHelper.addRewardCredit(Callback.getRewardCredit());
                                spHelper.useRewardCredit(1);
                                playLive(pos);
                                Toast.makeText(PostIDActivity.this, "Your Total Credit ("+spHelper.getRewardCredit()+")", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(PostIDActivity.this, "Display Failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
            rv.setAdapter(adapter);
            rv.scheduleLayoutAnimation();
            setEmpty();
            loadNativeAds();
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private void playLive(int position) {
        if(adapter.getItem(position) != null) {
            Intent intent1 = new Intent(PostIDActivity.this, VideoDetailsActivity.class);
            intent1.putExtra("post_id", arrayList.get(position).getId());
            startActivity(intent1);
        }
    }

    private void loadNativeAds() {
        if (helper.canLoadNativeAds(PostIDActivity.this,Callback.PAGE_NATIVE_POST)
                && Callback.getAdNetwork().equals(Callback.AD_TYPE_ADMOB)
                || Callback.getAdNetwork().equals(Callback.AD_TYPE_META) && arrayList.size() >= 10) {

            AdLoader.Builder builder = new AdLoader.Builder(PostIDActivity.this, Callback.getAdmobNativeAdID());
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
                    Log.e(TAG, "Error add ads: ", e);
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
            return;
        }
        rv.setVisibility(View.GONE);
        frameLayout.setVisibility(View.VISIBLE);
        pb.setVisibility(View.INVISIBLE);

        frameLayout.removeAllViews();
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        @SuppressLint("InflateParams") View myView = inflater.inflate(R.layout.row_empty, null);

        TextView textView = myView.findViewById(R.id.tv_empty_msg);
        textView.setText(errorMsg);

        if (pageType.equals(getString(R.string.favourite)) && !spHelper.isLogged()) {
            TextView tvEmpty = myView.findViewById(R.id.tv_empty);
            tvEmpty.setText(getString(R.string.login));

            ImageView ivEmpty = myView.findViewById(R.id.iv_empty);
            ivEmpty.setImageResource(R.drawable.ic_login);
        }

        myView.findViewById(R.id.ll_empty_try).setOnClickListener(v -> {
            myView.findViewById(R.id.iv_empty).setVisibility(View.GONE);
            myView.findViewById(R.id.pb_empty).setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> {
                myView.findViewById(R.id.iv_empty).setVisibility(View.VISIBLE);
                myView.findViewById(R.id.pb_empty).setVisibility(View.GONE);
                if (pageType.equals(getString(R.string.favourite))) {
                    if (spHelper.isLogged()){
                        getData();
                    } else {
                        helper.clickLogin();
                    }
                } else {
                    getData();
                }
            }, 500);
        });
        frameLayout.addView(myView);
    }
}