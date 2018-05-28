package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.BlockUrlProviderAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.BlockUrlUtils;
import com.fusionjack.adhell3.viewmodel.BlockUrlProvidersViewModel;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

import static com.fusionjack.adhell3.fragments.DomainTabPageFragment.PROVIDER_CONTENT_PAGE;

public class ProviderListFragment extends Fragment {
    private Context context;
    private FragmentActivity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = getContext();
        this.activity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_provider, container, false);

        // Set URL limit
        TextView hintTextView = view.findViewById(R.id.providerInfoTextView);
        String strFormat = getResources().getString(R.string.provider_info);
        hintTextView.setText(String.format(strFormat, AdhellAppIntegrity.BLOCK_URL_LIMIT));

        // Provider list
        ListView providerListView = view.findViewById(R.id.providerListView);
        BlockUrlProvidersViewModel providersViewModel = ViewModelProviders.of(activity).get(BlockUrlProvidersViewModel.class);
        providersViewModel.getBlockUrlProviders().observe(this, blockUrlProviders -> {
            ListAdapter adapter = providerListView.getAdapter();
            if (adapter == null) {
                BlockUrlProviderAdapter arrayAdapter = new BlockUrlProviderAdapter(context, blockUrlProviders);
                providerListView.setAdapter(arrayAdapter);
            }
        });

        providerListView.setOnItemClickListener((parent, view1, position, id) -> {
            BlockUrlProvider provider = (BlockUrlProvider) parent.getItemAtPosition(position);
            List<Fragment> fragments = getFragmentManager().getFragments();
            for (Fragment fragment : fragments) {
                if (fragment instanceof ProviderContentFragment) {
                    ((ProviderContentFragment) fragment).setProviderId(provider.id);
                }
            }
            TabLayout tabLayout = getParentFragment().getActivity().findViewById(R.id.domains_sliding_tabs);
            if (tabLayout != null) {
                TabLayout.Tab tab = tabLayout.getTabAt(PROVIDER_CONTENT_PAGE);
                if (tab != null) {
                    tab.select();
                }
            }
        });

        SwipeRefreshLayout dnsSwipeContainer = view.findViewById(R.id.providerSwipeContainer);
        dnsSwipeContainer.setOnRefreshListener(() ->
                new UpdateProviderAsyncTask(context).execute()
        );

        FloatingActionsMenu providerFloatMenu = view.findViewById(R.id.provider_actions);
        FloatingActionButton actionAddProvider = view.findViewById(R.id.action_add_provider);
        actionAddProvider.setIcon(R.drawable.ic_event_note_black_24dp);
        actionAddProvider.setOnClickListener(v -> {
            providerFloatMenu.collapse();
            View dialogView = inflater.inflate(R.layout.dialog_add_provider, container, false);
            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        EditText providerEditText = dialogView.findViewById(R.id.providerEditText);
                        String provider = providerEditText.getText().toString();
                        if (URLUtil.isValidUrl(provider)) {
                            new AddProviderAsyncTask(provider, context).execute();
                        } else {
                            Toast.makeText(getContext(), "Url is invalid", Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        });

        return view;
    }

    private static class AddProviderAsyncTask extends AsyncTask<Void, Void, Void> {
        private String provider;
        private WeakReference<Context> contextWeakReference;
        private BlockUrlProvider blockUrlProvider;

        AddProviderAsyncTask(String provider, Context context) {
            this.provider = provider;
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();

            blockUrlProvider = new BlockUrlProvider();
            blockUrlProvider.url = provider;
            blockUrlProvider.count = 0;
            blockUrlProvider.deletable = true;
            blockUrlProvider.lastUpdated = new Date();
            blockUrlProvider.selected = false;
            blockUrlProvider.id = appDatabase.blockUrlProviderDao().insertAll(blockUrlProvider)[0];
            blockUrlProvider.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
            appDatabase.blockUrlProviderDao().updateBlockUrlProviders(blockUrlProvider);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Context context = contextWeakReference.get();
            if (context != null) {
                ListView listView = ((Activity) context).findViewById(R.id.providerListView);
                if (listView != null) {
                    if (listView.getAdapter() instanceof BlockUrlProviderAdapter) {
                        BlockUrlProviderAdapter adapter = (BlockUrlProviderAdapter) listView.getAdapter();
                        adapter.add(blockUrlProvider);
                        adapter.notifyDataSetChanged();

                        new LoadProviderAsyncTask(blockUrlProvider, context).execute();
                    }
                }
            }
        }
    }

    private static class LoadProviderAsyncTask extends AsyncTask<Void, Void, Void> {
        private BlockUrlProvider provider;
        private WeakReference<Context> contextWeakReference;

        LoadProviderAsyncTask(BlockUrlProvider provider, Context context) {
            this.provider = provider;
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            try {
                List<BlockUrl> blockUrls = BlockUrlUtils.loadBlockUrls(provider);
                provider.count = blockUrls.size();
                provider.lastUpdated = new Date();
                appDatabase.blockUrlProviderDao().updateBlockUrlProviders(provider);
                appDatabase.blockUrlDao().insertAll(blockUrls);
            } catch (Exception e) {
                appDatabase.blockUrlProviderDao().delete(provider);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Context context = contextWeakReference.get();
            if (context != null) {
                ListView listView = ((Activity) context).findViewById(R.id.providerListView);
                if (listView != null) {
                    if (listView.getAdapter() instanceof BlockUrlProviderAdapter) {
                        BlockUrlProviderAdapter adapter = (BlockUrlProviderAdapter) listView.getAdapter();
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }

    private static class UpdateProviderAsyncTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<Context> contextWeakReference;

        UpdateProviderAsyncTask(Context context) {
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            AdhellFactory.getInstance().updateAllProviders();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Context context = contextWeakReference.get();
            if (context != null) {
                new SetProviderAsyncTask(context).execute();

                SwipeRefreshLayout swipeContainer = ((Activity) context).findViewById(R.id.providerSwipeContainer);
                if (swipeContainer != null) {
                    swipeContainer.setRefreshing(false);
                }
            }
        }
    }

    private static class SetProviderAsyncTask extends AsyncTask<Void, Void, List<BlockUrlProvider>> {
        private WeakReference<Context> contextWeakReference;

        SetProviderAsyncTask(Context context) {
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected List<BlockUrlProvider> doInBackground(Void... voids) {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            return appDatabase.blockUrlProviderDao().getAll2();
        }

        @Override
        protected void onPostExecute(List<BlockUrlProvider> providers) {
            Context context = contextWeakReference.get();
            if (context != null) {
                ListView listView = ((Activity) context).findViewById(R.id.providerListView);
                if (listView != null) {
                    if (listView.getAdapter() instanceof BlockUrlProviderAdapter) {
                        BlockUrlProviderAdapter adapter = (BlockUrlProviderAdapter) listView.getAdapter();
                        for (int i = 0; i < adapter.getCount(); i++) {
                            BlockUrlProvider provider = adapter.getItem(i);
                            if (provider != null) {
                                BlockUrlProvider dbProvider = getProvider(provider.id, providers);
                                if (dbProvider != null) {
                                    provider.count = dbProvider.count;
                                    provider.lastUpdated = dbProvider.lastUpdated;
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        }

        private BlockUrlProvider getProvider(long id, List<BlockUrlProvider> providers) {
            for (BlockUrlProvider provider : providers) {
                if (provider.id == id) {
                    return provider;
                }
            }
            return null;
        }
    }
}
