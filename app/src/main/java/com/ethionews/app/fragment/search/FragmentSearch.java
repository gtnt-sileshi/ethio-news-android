package com.ethionews.app.fragment.search;

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
import com.ethionews.app.activity.MainActivity;
import com.ethionews.app.adapter.AdapterSearch;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.callback.Method;
import com.ethionews.app.dialog.DialogUtil;
import com.ethionews.app.executor.LoadSearch;
import com.ethionews.app.interfaces.HomeListener;
import com.ethionews.app.item.ItemPost;
import com.ethionews.app.utils.helper.Helper;

public class FragmentSearch extends Fragment {

    Helper helper;
    ProgressBar progressBar;
    FrameLayout frameLayout;
    RecyclerView rv;
    AdapterSearch adapterSearch;
    ArrayList<ItemPost> arrayList;
    private String errorMsg;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        helper = new Helper(getActivity());

        arrayList = new ArrayList<>();

        ((MainActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.search));
        ((MainActivity) getActivity()).bottomNavigationView(5);

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
        if (!helper.isNetworkAvailable()) {
            errorMsg = getString(R.string.err_internet_not_connected);
            setEmpty();
            return;
        }
        LoadSearch loadSearch = new LoadSearch(new HomeListener() {
            @Override
            public void onStart() {
                frameLayout.setVisibility(View.GONE);
                rv.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onEnd(String success, String message, ArrayList<ItemPost> arrayListPost) {
                if (getActivity() == null) {
                    return;
                }
                if (success.equals("1")) {
                    loadSearchEnd(arrayListPost);
                } else if (success.equals("-2")) {
                    DialogUtil.verifyDialog(requireActivity(), getString(R.string.err_unauthorized_access), message, () -> {
                    });
                } else {
                    errorMsg = getString(R.string.err_server);
                    setEmpty();
                }
                progressBar.setVisibility(View.GONE);
            }
        }, helper.getAPIRequest(Method.METHOD_SEARCH, 0, "", "",
                Callback.getSearchItem(), "", "", "", "", "",
                "", "", "", "", null));
        loadSearch.execute();
    }

    private void loadSearchEnd(ArrayList<ItemPost> arrayListPost) {
        if (!arrayListPost.isEmpty()){
            arrayList.addAll(arrayListPost);
            if (Boolean.TRUE.equals(Callback.getIsAdsStatus())){
                arrayList.add(new ItemPost("100","ads","ads", "ads"));
            }
            adapterSearch = new AdapterSearch(getActivity(), arrayList);
            rv.setAdapter(adapterSearch);
            setEmpty();
        } else {
            errorMsg = getString(R.string.err_no_data_found);
            setEmpty();
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
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public boolean onQueryTextSubmit(String s) {
            if (helper.isNetworkAvailable()) {
                Callback.setSearchItem(s.replace(" ", "%20"));
                arrayList.clear();
                if (adapterSearch != null){
                    adapterSearch.notifyDataSetChanged();
                }
                loadHome();
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
}