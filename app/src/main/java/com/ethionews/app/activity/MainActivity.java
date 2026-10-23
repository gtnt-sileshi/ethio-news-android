package com.ethionews.app.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import com.ethionews.app.utils.ThemeManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.ethionews.app.R;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.dialog.DialogUtil;
import com.ethionews.app.executor.LoadAbout;
import com.ethionews.app.fragment.FragmentDashBoard;
import com.ethionews.app.fragment.online.FragmentCategories;
import com.ethionews.app.fragment.online.FragmentEvent;
import com.ethionews.app.fragment.online.FragmentLatest;
import com.ethionews.app.fragment.online.FragmentRadio;
import com.ethionews.app.fragment.online.FragmentRecent;
import com.ethionews.app.fragment.online.FragmentTrending;
import com.ethionews.app.interfaces.AboutListener;
import com.ethionews.app.utils.IfSupported;
import com.ethionews.app.utils.advertising.AdManagerInterAdmob;
import com.ethionews.app.utils.advertising.AdManagerInterApplovin;
import com.ethionews.app.utils.advertising.AdManagerInterStartApp;
import com.ethionews.app.utils.advertising.AdManagerInterUnity;
import com.ethionews.app.utils.advertising.AdManagerInterYandex;
import com.ethionews.app.utils.advertising.GDPRChecker;
import com.ethionews.app.utils.advertising.RewardAdAdmob;
import com.ethionews.app.utils.advertising.RewardAdApplovin;
import com.ethionews.app.utils.advertising.RewardAdStartApp;
import com.ethionews.app.utils.advertising.RewardAdUnity;
import com.ethionews.app.utils.helper.DBHelper;
import com.ethionews.app.utils.helper.Helper;
import com.ethionews.app.utils.helper.SPHelper;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    FragmentManager fm;
    MenuItem menuLogin;
    MenuItem menuProfile;
    MenuItem menuSubscription;
    ReviewManager manager;
    ReviewInfo reviewInfo;
    Helper helper;
    DBHelper dbHelper;
    SPHelper spHelper;
    NavigationView navigationView;
    BottomNavigationView bottomNav;
    int currentNavIndex = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IfSupported.applyTheme(this);
        setContentView(R.layout.activity_main);
        IfSupported.isRTL(this);
        IfSupported.isScreenshot(this);

        fm = getSupportFragmentManager();

        helper = new Helper(this);
        dbHelper = new DBHelper(this);
        spHelper = new SPHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Apply status-bar top padding to the nav drawer header.
        // NavigationView consumes window insets itself and does not forward them
        // to the header view, so we read the status bar height directly and apply it.
        View navHeader = navigationView.getHeaderView(0);
        if (navHeader != null) {
            int statusBarHeight = 0;
            int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resId > 0) statusBarHeight = getResources().getDimensionPixelSize(resId);
            final int topPad = statusBarHeight;
            navHeader.post(() -> navHeader.setPadding(
                    navHeader.getPaddingLeft(),
                    topPad,
                    navHeader.getPaddingRight(),
                    navHeader.getPaddingBottom()));
        }
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        toggle.setDrawerIndicatorEnabled(false);
        toggle.setHomeAsUpIndicator(R.drawable.ic_menu_white);
        toggle.setToolbarNavigationClickListener(view -> drawer.openDrawer(GravityCompat.START));
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        Drawable navigationIcon = toolbar.getNavigationIcon();
        if (navigationIcon != null) {
            navigationIcon = DrawableCompat.wrap(navigationIcon);
            DrawableCompat.setTint(navigationIcon, ContextCompat.getColor(MainActivity.this,
                    Boolean.TRUE.equals(new ThemeManager(this).getIsThemeMode()) ? R.color.ns_white : R.color.ns_black));
            toolbar.setNavigationIcon(navigationIcon);
        }

        navigationView.setNavigationItemSelectedListener(this);

        Menu menu = navigationView.getMenu();
        menuLogin = menu.findItem(R.id.nav_login);
        menuProfile = menu.findItem(R.id.nav_profile);
        menuSubscription = menu.findItem(R.id.nav_subscription);

        new GDPRChecker(MainActivity.this).check();
        changeLoginName();
        loadAboutData();

        manager = ReviewManagerFactory.create(this);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                reviewInfo = task.getResult();
            }
        });

        bottomNav = findViewById(R.id.ll_bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.tv_nav_home)      { pageChange(0); currentNavIndex = 0; }
            else if (id == R.id.tv_nav_latest)   { pageChange(1); currentNavIndex = 1; }
            else if (id == R.id.tv_nav_most)     { pageChange(2); currentNavIndex = 2; }
            else if (id == R.id.tv_nav_category) { pageChange(3); currentNavIndex = 3; }
            else if (id == R.id.tv_nav_restore)  { pageChange(4); currentNavIndex = 4; }
            return true;
        });
        loadDashboardFrag();

        // Request POST_NOTIFICATIONS permission on Android 13+ (API 33+).
        // On older versions the permission is granted automatically.
        requestNotificationPermission();

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleOnBack();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }


    private void loadDashboardFrag() {
        FragmentDashBoard f1 = new FragmentDashBoard();
        loadFrag(f1, getResources().getString(R.string.dashboard), fm);
        navigationView.setCheckedItem(R.id.nav_home);
    }

    public void loadFrag(Fragment f1, String name, FragmentManager fm) {
        for (int i = 0; i < fm.getBackStackEntryCount(); ++i) {
            fm.popBackStackImmediate();
        }

        FragmentTransaction ft = fm.beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        if (!name.equals(getString(R.string.dashboard))) {
            ft.hide(fm.getFragments().get(fm.getBackStackEntryCount()));
            ft.add(R.id.fragment, f1, name);
            ft.addToBackStack(name);
        } else {
            ft.replace(R.id.fragment, f1, name);
        }
        ft.commit();

        if (getSupportActionBar() != null){
            getSupportActionBar().setTitle(name);
        }
    }

    private void changeLoginName() {
        if (menuLogin == null) return;

        boolean isLoginEnabled = spHelper.getIsLogin();
        menuLogin.setVisible(isLoginEnabled);

        menuSubscription.setVisible(true);
        if (spHelper.isLogged()) {
            menuProfile.setVisible(true);
            // Show logout option only when login is enabled (user can undo their login)
            if (isLoginEnabled) {
                menuLogin.setTitle(getResources().getString(R.string.logout));
                menuLogin.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_logout));
            }
            if (spHelper.getIsSubscribed()) {
                menuSubscription.setVisible(false);
            }
        } else {
            menuProfile.setVisible(false);
            if (isLoginEnabled) {
                menuLogin.setTitle(getResources().getString(R.string.login));
                menuLogin.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_login));
            }
        }
    }

    public void loadAboutData() {
        if (helper.isNetworkAvailable()) {
            LoadAbout loadAbout = new LoadAbout(MainActivity.this, new AboutListener() {
                @Override
                public void onStart() {
                    // this method is empty
                }

                @Override
                public void onEnd(String success, String verifyStatus, String message) {
                    if (isFinishing() && !success.equals("1")) {
                        return;
                    }
                    dbHelper.addToAbout();
                    helper.initializeAds();
                    initAds();
                }
            });
            loadAbout.execute();
        } else {
            try {
                dbHelper.getAbout();
            } catch (Exception e) {
                Log.e("MainActivity", "Error getAbout", e);
            }
        }
    }

    private void initAds() {
        if (Boolean.TRUE.equals(Callback.getIsInterAd()) && (!spHelper.getIsSubscribed() || spHelper.getIsAdOn())) {
            switch (Callback.getAdNetwork()) {
                case Callback.AD_TYPE_ADMOB :
                    AdManagerInterAdmob adManagerInterAdmob = new AdManagerInterAdmob(getApplicationContext());
                    adManagerInterAdmob.createAd();
                    break;
                case Callback.AD_TYPE_STARTAPP :
                    AdManagerInterStartApp adManagerInterStartApp = new AdManagerInterStartApp(getApplicationContext());
                    adManagerInterStartApp.createAd();
                    break;
                case Callback.AD_TYPE_APPLOVIN :
                    AdManagerInterApplovin adManagerInterApplovin = new AdManagerInterApplovin(MainActivity.this);
                    adManagerInterApplovin.createAd();
                    break;
                case Callback.AD_TYPE_YANDEX :
                    AdManagerInterYandex adManagerInterYandex = new AdManagerInterYandex(MainActivity.this);
                    adManagerInterYandex.createAd();
                    break;
                case Callback.AD_TYPE_WORTISE :
                    // Wortise removed
                    break;
                case Callback.AD_TYPE_UNITY :
                    AdManagerInterUnity adManagerInterUnity = new AdManagerInterUnity();
                    adManagerInterUnity.createAd();
                    break;
                default:
                    break;
            }
        }
        if (Boolean.TRUE.equals(Callback.getIsRewardAd()) && (!spHelper.getIsSubscribed() || spHelper.getIsAdOn())) {
            switch (Callback.getAdNetwork()) {
                case Callback.AD_TYPE_ADMOB :
                    RewardAdAdmob rewardAdAdmob = new RewardAdAdmob(getApplicationContext());
                    rewardAdAdmob.createAd();
                    break;
                case Callback.AD_TYPE_STARTAPP :
                    RewardAdStartApp rewardAdStartApp = new RewardAdStartApp(getApplicationContext());
                    rewardAdStartApp.createAd();
                    break;
                case Callback.AD_TYPE_APPLOVIN :
                    RewardAdApplovin rewardAdApplovin = new RewardAdApplovin(MainActivity.this);
                    rewardAdApplovin.createAd();
                    break;
                case Callback.AD_TYPE_WORTISE :
                    // Wortise removed
                    break;
                case Callback.AD_TYPE_UNITY :
                    RewardAdUnity rewardAdUnity = new RewardAdUnity();
                    rewardAdUnity.createAd();
                    break;
                default:
                    break;
            }
        }
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home){
            if (currentNavIndex != 0) pageChange(0);
            bottomNavigationView(0);
        } else if (id == R.id.nav_latest){
            if (currentNavIndex != 1) pageChange(1);
            bottomNavigationView(1);
        } else if (id == R.id.nav_most){
            if (currentNavIndex != 2) pageChange(2);
            bottomNavigationView(2);
        } else if (id == R.id.nav_category){
            if (currentNavIndex != 3) pageChange(3);
            bottomNavigationView(3);
        } else if (id == R.id.nav_restore){
            if (currentNavIndex != 4) pageChange(4);
            bottomNavigationView(4);
        } else if (id == R.id.nav_radio){
            FragmentRadio radio = new FragmentRadio();
            loadFrag(radio, getString(R.string.live_radio), fm);
            currentNavIndex = -1; // radio is not a bottom-nav tab
        } else if (id == R.id.nav_event){
            FragmentEvent event = new FragmentEvent();
            loadFrag(event, getString(R.string.live_event), fm);
            currentNavIndex = -1; // event is not a bottom-nav tab
        } else if (id == R.id.nav_suggest){
            if (spHelper.isLogged()){
                startActivity(new Intent(MainActivity.this, SuggestionActivity.class));
            } else {
                helper.clickLogin();
            }
        } else if (id == R.id.nav_fav){
            Intent intent = new Intent(MainActivity.this, PostIDActivity.class);
            intent.putExtra("page_type", getString(R.string.favourite));
            intent.putExtra("id", "");
            intent.putExtra("name", getString(R.string.favourite));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else if (id == R.id.nav_subscription){
            startActivity(new Intent(MainActivity.this, BillingSubscribeActivity.class));
        } else if (id == R.id.nav_profile){
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        } else if (id == R.id.nav_settings){
            startActivity(new Intent(MainActivity.this, SettingActivity.class));
        } else if (id == R.id.nav_login){
            helper.clickLogin();
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void bottomNavigationView(int pos) {
        if (bottomNav == null) return;
        int[] ids = {R.id.tv_nav_home, R.id.tv_nav_latest, R.id.tv_nav_most,
                     R.id.tv_nav_category, R.id.tv_nav_restore};
        if (pos >= 0 && pos < ids.length) {
            bottomNav.setSelectedItemId(ids[pos]);
        }
        currentNavIndex = pos;
    }


    @Override
    public void onResume() {
        changeLoginName();
        if (Boolean.TRUE.equals(Callback.isRecreate())) {
            Callback.setRecreate(false);
            recreate();
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        try {
            dbHelper.close();
        } catch (Exception e) {
            Log.e("MainActivity", "Error in closing", e);
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            handleOnBack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleOnBack() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (fm.getBackStackEntryCount() != 0) {
            String title = fm.getFragments().get(fm.getBackStackEntryCount() - 1).getTag();
            if (title != null) {

                // Custom class to hold both navigation ID and bottom navigation index
                class NavInfo {
                    final int navId;
                    final int bottomNavIndex;
                    NavInfo(int navId, int bottomNavIndex) {
                        this.navId = navId;
                        this.bottomNavIndex = bottomNavIndex;
                    }
                }

                // Map to hold titles and corresponding NavInfo
                Map<String, NavInfo> titleToNavInfoMap = new HashMap<>();

                // Initialize the map with titles and corresponding actions
                titleToNavInfoMap.put(getString(R.string.dashboard), new NavInfo(R.id.nav_home, 0));
                titleToNavInfoMap.put(getString(R.string.nav_home), new NavInfo(R.id.nav_home, 0));
                titleToNavInfoMap.put(getString(R.string.latest), new NavInfo(R.id.nav_latest, 1));
                titleToNavInfoMap.put(getString(R.string.trending), new NavInfo(R.id.nav_most, 2));
                titleToNavInfoMap.put(getString(R.string.categories), new NavInfo(R.id.nav_category, 3));
                titleToNavInfoMap.put(getString(R.string.recently), new NavInfo(R.id.nav_restore, 4));
                titleToNavInfoMap.put(getString(R.string.search), new NavInfo(R.id.nav_home, 5));
                titleToNavInfoMap.put(getString(R.string.live_radio), new NavInfo(R.id.nav_home, 0));
                titleToNavInfoMap.put(getString(R.string.live_event), new NavInfo(R.id.nav_home, 0));

                // Update the navigation view and bottom navigation view if the title is in the map
                NavInfo navInfo = titleToNavInfoMap.get(title);
                if (navInfo != null) {
                    navigationView.setCheckedItem(navInfo.navId);
                    bottomNavigationView(navInfo.bottomNavIndex);
                    pageChange(navInfo.bottomNavIndex);
                }
            }
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(Objects.equals(title, getString(R.string.dashboard)) ? getString(R.string.nav_home) : title);
            }
        } else if (reviewInfo != null){
            Task<Void> flow = manager.launchReviewFlow(MainActivity.this, reviewInfo);
            flow.addOnCompleteListener(task1 -> DialogUtil.exitDialog(MainActivity.this));
        } else {
            DialogUtil.exitDialog(MainActivity.this);
        }
    }

    private void pageChange(int bottomNavIndex) {
        if (bottomNavIndex == 0){
            FragmentDashBoard home = new FragmentDashBoard();
            loadFrag(home, getString(R.string.dashboard), fm);
        } else if (bottomNavIndex == 1){
            FragmentLatest latest = new FragmentLatest();
            loadFrag(latest, getString(R.string.latest), fm);
        } else if (bottomNavIndex == 2){
            FragmentTrending most = new FragmentTrending();
            loadFrag(most, getString(R.string.trending), fm);
        } else if (bottomNavIndex == 3){
            FragmentCategories category = new FragmentCategories();
            loadFrag(category, getString(R.string.categories), fm);
        } else if (bottomNavIndex == 4){
            FragmentRecent recent = new FragmentRecent();
            loadFrag(recent, getString(R.string.recently), fm);
        }
    }

    /**
     * Requests POST_NOTIFICATIONS permission on Android 13+ (API 33).
     * On lower API versions the permission is implicitly granted.
     * Uses ActivityResultLauncher — no deprecated onRequestPermissionsResult needed.
     */
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // No action needed — FCM will still deliver notifications silently
                // even if the user denies; the channel still receives them in the tray.
                Log.d("MainActivity", "POST_NOTIFICATIONS granted: " + granted);
            });

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}