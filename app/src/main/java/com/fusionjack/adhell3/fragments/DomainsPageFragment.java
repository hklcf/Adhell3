package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import com.fusionjack.adhell3.db.entity.UserBlockUrl;
import com.fusionjack.adhell3.db.entity.WhiteUrl;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.BlockUrlPatternsMatch;
import com.fusionjack.adhell3.utils.BlockUrlUtils;
import com.fusionjack.adhell3.viewmodel.BlackUrlViewModel;
import com.fusionjack.adhell3.viewmodel.BlockUrlProvidersViewModel;
import com.fusionjack.adhell3.viewmodel.WhiteUrlViewModel;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

public class DomainsPageFragment extends Fragment {
    private static final String ARG_PAGE = "page";
    private int page;
    private Context context;

    public static final int BLACKLIST_PAGE = 0;
    public static final int WHITELIST_PAGE = 1;
    public static final int PROVIDER_PAGE = 2;
    public static final int LIST_PAGE = 3;

    public static DomainsPageFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        DomainsPageFragment fragment = page == LIST_PAGE ? new ProviderListPageFragment(): new DomainsPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.page = getArguments().getInt(ARG_PAGE);
        this.context = getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = null;
        switch (page) {
            case BLACKLIST_PAGE:
                view = inflater.inflate(R.layout.fragment_manual_url_block, container, false);

                EditText blackUrlEditText = view.findViewById(R.id.blackUrlEditText);
                Button addBlackUrlButton = view.findViewById(R.id.addBlackUrlButton);
                addBlackUrlButton.setOnClickListener(v -> {
                    String urlToAdd = blackUrlEditText.getText().toString().trim().toLowerCase();
                    if (urlToAdd.indexOf('|') == -1) {
                        if (!BlockUrlPatternsMatch.isUrlValid(urlToAdd)) {
                            Toast.makeText(context, "Url not valid. Please check", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else {
                        // packageName|ipAddress|port
                        StringTokenizer tokens = new StringTokenizer(urlToAdd, "|");
                        if (tokens.countTokens() != 3) {
                            Toast.makeText(context, "Rule not valid. Please check", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    new AddUrlAsyncTask(urlToAdd, page, context).execute();
                });

                ListView blacklistView = view.findViewById(R.id.blackListView);
                blacklistView.setOnItemClickListener((parent, view1, position, id) -> {
                    String url = (String) parent.getItemAtPosition(position);
                    new RemoveUrlAsyncTask(url, page, context).execute();
                });

                BlackUrlViewModel blackViewModel = ViewModelProviders.of(getActivity()).get(BlackUrlViewModel.class);
                blackViewModel.getBlockUrls().observe(this, blackUrls -> {
                    if (blackUrls == null) {
                        return;
                    }
                    ListAdapter adapter = blacklistView.getAdapter();
                    if (adapter == null) {
                        List<String> urls = new ArrayList<>();
                        for (UserBlockUrl blockUrl : blackUrls) {
                            urls.add(blockUrl.url);
                        }
                        ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, urls);
                        blacklistView.setAdapter(itemsAdapter);
                    }
                });
                break;

            case WHITELIST_PAGE:
                view = inflater.inflate(R.layout.fragment_white_list, container, false);

                EditText whiteUrlEditText = view.findViewById(R.id.whiteUrlEditText);
                Button addWhitelistUrlButton = view.findViewById(R.id.addWhiteUrlButton);
                addWhitelistUrlButton.setOnClickListener(v -> {
                    String urlToAdd = whiteUrlEditText.getText().toString();
                    if (urlToAdd.indexOf('|') == -1) {
                        if (!BlockUrlPatternsMatch.isUrlValid(urlToAdd)) {
                            Toast.makeText(this.getContext(), "Url not valid. Please check", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else {
                        // packageName|url
                        StringTokenizer tokens = new StringTokenizer(urlToAdd, "|");
                        if (tokens.countTokens() != 2) {
                            Toast.makeText(this.getContext(), "Rule not valid. Please check", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    new AddUrlAsyncTask(urlToAdd, page, context).execute();
                });

                ListView whiteListView = view.findViewById(R.id.whiteListView);
                whiteListView.setOnItemClickListener((parent, view1, position, id) -> {
                    String url = (String) parent.getItemAtPosition(position);
                    new RemoveUrlAsyncTask(url, page, context).execute();
                });

                WhiteUrlViewModel model = ViewModelProviders.of(getActivity()).get(WhiteUrlViewModel.class);
                model.getWhiteUrls().observe(this, whiteUrls -> {
                    if (whiteUrls == null) {
                        return;
                    }
                    ListAdapter adapter = whiteListView.getAdapter();
                    if (adapter == null) {
                        List<String> urls = new ArrayList<>();
                        for (WhiteUrl whiteUrl : whiteUrls) {
                            urls.add(whiteUrl.url);
                        }
                        ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, urls);
                        whiteListView.setAdapter(itemsAdapter);
                    }
                });
                break;

            case PROVIDER_PAGE:
                view = inflater.inflate(R.layout.fragment_custom_url_provider, container, false);

                // Set URL limit
                TextView hintTextView = view.findViewById(R.id.providerInfoTextView);
                String strFormat = getResources().getString(R.string.provider_info);
                hintTextView.setText(String.format(strFormat, AdhellAppIntegrity.BLOCK_URL_LIMIT));

                // Add button
                EditText providerEditText = view.findViewById(R.id.addProviderEditText);
                Button addProviderButton = view.findViewById(R.id.addProviderButton);
                addProviderButton.setOnClickListener(v -> {
                    String provider = providerEditText.getText().toString();
                    if (URLUtil.isValidUrl(provider)) {
                        new AddProviderAsyncTask(provider, context).execute();
                    } else {
                        Toast.makeText(getContext(), "Url is invalid", Toast.LENGTH_LONG).show();
                    }
                });

                // Provider list
                ListView providerListView = view.findViewById(R.id.providerListView);
                BlockUrlProvidersViewModel providersViewModel = ViewModelProviders.of(getActivity()).get(BlockUrlProvidersViewModel.class);
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
                        if (fragment instanceof ProviderListPageFragment) {
                            ((ProviderListPageFragment) fragment).setProviderId(provider.id);
                        }
                    }
                    TabLayout tabLayout = getParentFragment().getActivity().findViewById(R.id.domains_sliding_tabs);
                    if (tabLayout != null) {
                        TabLayout.Tab tab = tabLayout.getTabAt(LIST_PAGE);
                        if (tab != null) {
                            tab.select();
                        }
                    }
                });

                // Update button
                Button updateBlockUrlProvidersButton = view.findViewById(R.id.updateProviderButton);
                updateBlockUrlProvidersButton.setOnClickListener(v -> {
                    new UpdateProviderAsyncTask(context).execute();
                });

                break;
        }

        return view;
    }

    private static class AddUrlAsyncTask extends AsyncTask<Void, Void, Void> {
        private String url;
        private int page;
        private WeakReference<Context> contextWeakReference;

        AddUrlAsyncTask(String url, int page, Context context) {
            this.url = url;
            this.page = page;
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            switch (page) {
                case BLACKLIST_PAGE:
                    UserBlockUrl userBlockUrl = new UserBlockUrl(url);
                    appDatabase.userBlockUrlDao().insert(userBlockUrl);
                    break;
                case WHITELIST_PAGE:
                    WhiteUrl whiteUrl = new WhiteUrl(url);
                    appDatabase.whiteUrlDao().insert(whiteUrl);
                    break;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Context context = contextWeakReference.get();
            if (context != null) {
                ListView listView = null;
                EditText editText = null;
                switch (page) {
                    case BLACKLIST_PAGE:
                        listView = ((Activity) context).findViewById(R.id.blackListView);
                        editText = ((Activity) context).findViewById(R.id.blackUrlEditText);
                        break;

                    case WHITELIST_PAGE:
                        listView = ((Activity) context).findViewById(R.id.whiteListView);
                        editText = ((Activity) context).findViewById(R.id.whiteUrlEditText);
                        break;
                }

                if (listView != null) {
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) listView.getAdapter();
                    adapter.add(url);
                    adapter.notifyDataSetChanged();
                }

                if (editText != null) {
                    editText.setText("");
                }

                if (url.indexOf('|') == -1) {
                    Toast.makeText(context, "Url has been added", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Rule has been added", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private static class RemoveUrlAsyncTask extends AsyncTask<Void, Void, Void> {
        private String url;
        private int page;
        private WeakReference<Context> contextWeakReference;

        RemoveUrlAsyncTask(String url, int page, Context context) {
            this.url = url;
            this.page = page;
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            switch (page) {
                case BLACKLIST_PAGE:
                    appDatabase.userBlockUrlDao().deleteByUrl(url);
                    break;

                case WHITELIST_PAGE:
                    appDatabase.whiteUrlDao().deleteByUrl(url);
                    break;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Context context = contextWeakReference.get();
            if (context != null) {
                ListView listView = null;
                switch (page) {
                    case BLACKLIST_PAGE:
                        listView = ((Activity) context).findViewById(R.id.blackListView);
                        break;

                    case WHITELIST_PAGE:
                        listView = ((Activity) context).findViewById(R.id.whiteListView);
                        break;
                }

                if (listView != null) {
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) listView.getAdapter();
                    adapter.remove(url);
                    adapter.notifyDataSetChanged();
                }

                if (url.indexOf('|') == -1) {
                    Toast.makeText(context, "Url has been removed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Rule has been removed", Toast.LENGTH_SHORT).show();
                }
            }
        }
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

                EditText editText = ((Activity) context).findViewById(R.id.addProviderEditText);
                if (editText != null) {
                    editText.setText("");
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
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            appDatabase.blockUrlDao().deleteAll();
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
                        for (int i = 0; i < adapter.getCount(); i++) {
                            BlockUrlProvider provider = adapter.getItem(i);
                            new LoadProviderAsyncTask(provider, context).execute();
                        }
                    }
                }
            }
        }
    }
}
