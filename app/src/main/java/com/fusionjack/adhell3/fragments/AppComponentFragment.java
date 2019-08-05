package com.fusionjack.adhell3.fragments;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppComponentFactory;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class AppComponentFragment extends AppFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initAppModel(AppRepository.Type.COMPONENT);

        if (BuildConfig.SHOW_SYSTEM_APP_COMPONENT) {
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
            TextView titlTextView = dialogView.findViewById(R.id.titleTextView);
            titlTextView.setText(R.string.dialog_system_app_components_title);
            TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
            questionTextView.setText(R.string.dialog_system_app_components_info);
            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, null).show();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.appcomponent_tab_menu, menu);

        initSearchView(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_enable_all:
                enableAllAppComponents();
                break;
            case R.id.action_batch:
                batchOperation();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void batchOperation() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
        TextView titlTextView = dialogView.findViewById(R.id.titleTextView);
        titlTextView.setText(R.string.dialog_appcomponent_batch_title);
        TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
        questionTextView.setText(R.string.dialog_appcomponent_batch_summary);

        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setCancelable(false);

        SingleObserver<String> observer = new SingleObserver<String>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(String s) {
                progressDialog.dismiss();
                Toast.makeText(context, s, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(Throwable e) {
                progressDialog.dismiss();
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        };

        new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton(R.string.button_enable, (dialog, whichButton) -> {
                    progressDialog.setMessage(getString(R.string.dialog_appcomponent_enable_summary));
                    progressDialog.show();
                    AppComponentFactory.getInstance().processAppComponentInBatch(true)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(observer);
                })
                .setNegativeButton(R.string.button_disable, (dialog, whichButton) -> {
                    progressDialog.setMessage(getString(R.string.dialog_appcomponent_disable_summary));
                    progressDialog.show();
                    AppComponentFactory.getInstance().processAppComponentInBatch(false)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(observer);
                })
                .setNeutralButton(android.R.string.no, null).show();
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
                        AsyncTask.execute(() -> {
                            AdhellFactory.getInstance().setAppComponentState(true);
                            AdhellFactory.getInstance().getAppDatabase().appPermissionDao().deleteAll();
                        })
                )
                .setNegativeButton(android.R.string.no, null).show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_app_component, container, false);

        AppFlag appFlag = AppFlag.createComponentFlag();
        ListView listView = view.findViewById(appFlag.getLoadLayout());
        listView.setAdapter(adapter);
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
        swipeContainer.setOnRefreshListener(() -> {
            loadAppList(type);
            swipeContainer.setRefreshing(false);
            resetSearchView();
        });

        return view;
    }
}
