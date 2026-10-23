package com.ethionews.app.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
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

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Objects;

import com.ethionews.app.R;
import com.ethionews.app.adapter.home.AdapterHome;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.callback.Method;
import com.ethionews.app.dialog.DialogUtil;
import com.ethionews.app.executor.LoadHome;
import com.ethionews.app.fragment.search.FragmentSearch;
import com.ethionews.app.interfaces.HomeListener;
import com.ethionews.app.item.ItemPost;
import com.ethionews.app.utils.helper.DBHelper;
import com.ethionews.app.utils.helper.Helper;

public class FragmentHome extends Fragment {

    DBHelper dbHelper;
    Helper helper;
    ProgressBar progressBar;
    FrameLayout frameLayout;
    RecyclerView rv;
    AdapterHome adapterHome;
    ArrayList<ItemPost> arrayList;
    private String errorMsg;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        helper = new Helper(getActivity());

        arrayList = new ArrayList<>();
        dbHelper = new DBHelper(getActivity());

        progressBar = rootView.findViewById(R.id.pb_home);
        frameLayout = rootView.findViewById(R.id.fl_empty);

        rv = rootView.findViewById(R.id.rv_home);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        rv.setLayoutManager(llm);
        rv.setItemAnimator(new DefaultItemAnimator());

        loadHome();

        addMenuProvider();
        return rootView;
    }

    private void loadHome() {
        if (helper.isNetworkAvailable()) {
            LoadHome loadHome = new LoadHome(getContext(), new HomeListener() {
                @Override
                public void onStart() {
                    frameLayout.setVisibility(View.GONE);
                    rv.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void onEnd(String success, String message, ArrayList<ItemPost> arrayListPost) {
                    progressBar.setVisibility(View.GONE);
                    if (getActivity() == null) {
                        return;
                    }
                    if (success.equals("1")) {
                        loadHomeEnd(arrayListPost);
                    } else if (success.equals("-2")) {
                        DialogUtil.verifyDialog(requireActivity(), getString(R.string.err_unauthorized_access), message, () -> {
                        });
                    } else {
                        errorMsg = getString(R.string.err_server);
                        setEmpty();
                    }
                }
            }, helper.getAPIRequest(Method.METHOD_HOME, 0, dbHelper.getRecentIDs("10"),
                    "", "", "", "", "", "", "",
                    "", "", "", "", null));
            loadHome.execute();
        } else {
            errorMsg = getString(R.string.err_internet_not_connected);
            setEmpty();
        }
    }

    private void loadHomeEnd(ArrayList<ItemPost> arrayListPost) {
        if (arrayListPost.isEmpty()){
            errorMsg = getString(R.string.err_no_data_found);
            setEmpty();
            return;
        }
        arrayList.addAll(arrayListPost);
        if (Boolean.TRUE.equals(Callback.getIsAdsStatus())){
            arrayList.add(new ItemPost("100","ads","ads", "ads"));
        }
        adapterHome = new AdapterHome(getActivity(), arrayList);
        rv.setAdapter(adapterHome);
        setEmpty();
    }

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
            LayoutInflater inflater = (LayoutInflater) requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            @SuppressLint("InflateParams") View myView = inflater.inflate(R.layout.row_empty, null);

            TextView textView = myView.findViewById(R.id.tv_empty_msg);
            textView.setText(errorMsg);

            myView.findViewById(R.id.ll_empty_try).setOnClickListener(v -> {
                myView.findViewById(R.id.iv_empty).setVisibility(View.GONE);
                myView.findViewById(R.id.pb_empty).setVisibility(View.VISIBLE);
                new Handler().postDelayed(() -> {
                    myView.findViewById(R.id.iv_empty).setVisibility(View.VISIBLE);
                    myView.findViewById(R.id.pb_empty).setVisibility(View.GONE);
                    loadHome();
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
            FragmentSearch fSearch = new FragmentSearch();
            FragmentManager fm = getParentFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.hide(Objects.requireNonNull(fm.findFragmentByTag(getString(R.string.nav_home))));
            ft.add(R.id.fragment, fSearch, getString(R.string.search));
            ft.addToBackStack(getString(R.string.search));
            ft.commit();
            return true;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            return false;
        }
    };
}