package com.fusionjack.adhell3.fragments;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.tasks.LoadAppAsyncTask;
import com.fusionjack.adhell3.tasks.RefreshAppAsyncTask;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppCache;

public class AppComponentFragment extends Fragment {
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = getContext();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.app_menu, menu);
        AppFlag appFlag = AppFlag.createComponentFlag();

        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String text) {
                if (searchView.isIconified()) {
                    return false;
                }
                new LoadAppAsyncTask(text, appFlag, getContext()).execute();
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_enable_all:
                enableAllAppComponents();
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableAllAppComponents() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
        TextView titlTextView = dialogView.findViewById(R.id.titleTextView);
        titlTextView.setText(R.string.dialog_enable_components_title);
        TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
        questionTextView.setText(R.string.dialog_enable_components_info);
        new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                        AsyncTask.execute(() ->
                                AdhellFactory.getInstance().setAppComponentState(true)
                        )
                )
                .setNegativeButton(android.R.string.no, null).show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_app_component, container, false);

        AppFlag appFlag = AppFlag.createComponentFlag();
        ListView listView = view.findViewById(appFlag.getLoadLayout());
        listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
            AppInfoAdapter adapter = (AppInfoAdapter) adView.getAdapter();

            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            Bundle bundle = new Bundle();
            AppInfo appInfo = adapter.getItem(position);
            bundle.putString("packageName", appInfo.packageName);
            bundle.putString("appName", appInfo.appName);
            ComponentTabFragment fragment = new ComponentTabFragment();
            fragment.setArguments(bundle);

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragmentContainer, fragment);
            fragmentTransaction.addToBackStack("appComponents");
            fragmentTransaction.commit();
        });

        SwipeRefreshLayout swipeContainer = view.findViewById(appFlag.getRefreshLayout());
        swipeContainer.setOnRefreshListener(() ->
                new RefreshAppAsyncTask(appFlag, context).execute()
        );

        AppCache.getInstance(context, null);
        new LoadAppAsyncTask("", appFlag, context).execute();

        return view;
    }
}
