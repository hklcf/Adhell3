package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
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
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

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
        View view = null;
        switch (page) {
            case BLACKLIST_PAGE:
                view = inflater.inflate(R.layout.fragment_blacklist, container, false);

                SwipeRefreshLayout blacklistSwipeContainer = view.findViewById(R.id.blacklistSwipeContainer);
                blacklistSwipeContainer.setOnRefreshListener(() ->
                        new RefreshListAsyncTask(page, context).execute()
                );

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

                FloatingActionsMenu blackFloatMenu = view.findViewById(R.id.blacklist_actions);
                FloatingActionButton actionAddBlackDomain = view.findViewById(R.id.action_add_domain);
                actionAddBlackDomain.setIcon(R.drawable.ic_public_black_24dp);
                actionAddBlackDomain.setOnClickListener(v -> {
                    blackFloatMenu.collapse();
                    View dialogView = inflater.inflate(R.layout.dialog_blacklist_domain, container, false);
                    new AlertDialog.Builder(context)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            EditText domainEditText = dialogView.findViewById(R.id.domainEditText);
                            String domainToAdd = domainEditText.getText().toString().trim().toLowerCase();
                            if (!BlockUrlPatternsMatch.isUrlValid(domainToAdd)) {
                                Toast.makeText(context, "Url not valid. Please check", Toast.LENGTH_SHORT).show();
                            } else {
                                new AddUrlAsyncTask(domainToAdd, page, context).execute();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                });

                FloatingActionButton actionAddFirewallRule = view.findViewById(R.id.action_add_firewall_rule);
                actionAddFirewallRule.setIcon(R.drawable.ic_whatshot_black_24dp);
                actionAddFirewallRule.setOnClickListener(v -> {
                    blackFloatMenu.collapse();
                    View dialogView = inflater.inflate(R.layout.dialog_blacklist_rule, container, false);
                    new AlertDialog.Builder(context)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            EditText ruleEditText = dialogView.findViewById(R.id.ruleEditText);
                            String ruleToAdd = ruleEditText.getText().toString().trim().toLowerCase();
                            StringTokenizer tokens = new StringTokenizer(ruleToAdd, "|");
                            if (tokens.countTokens() != 3) {
                                Toast.makeText(context, "Rule not valid. Please check", Toast.LENGTH_SHORT).show();
                            } else {
                                new AddUrlAsyncTask(ruleToAdd, page, context).execute();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                });

                break;

            case WHITELIST_PAGE:
                view = inflater.inflate(R.layout.fragment_whitelist, container, false);

                SwipeRefreshLayout whitelistSwipeContainer = view.findViewById(R.id.whitelistSwipeContainer);
                whitelistSwipeContainer.setOnRefreshListener(() ->
                        new RefreshListAsyncTask(page, context).execute()
                );

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

                FloatingActionsMenu whiteFloatMenu = view.findViewById(R.id.whitelist_actions);
                FloatingActionButton actionAddWhiteDomain = view.findViewById(R.id.action_add_domain);
                actionAddWhiteDomain.setIcon(R.drawable.ic_public_black_24dp);
                actionAddWhiteDomain.setOnClickListener(v -> {
                    whiteFloatMenu.collapse();
                    View dialogView = inflater.inflate(R.layout.dialog_whitelist_domain, container, false);
                    new AlertDialog.Builder(context)
                            .setView(dialogView)
                            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                                EditText domainEditText = dialogView.findViewById(R.id.domainEditText);
                                String domainToAdd = domainEditText.getText().toString().trim().toLowerCase();
                                if (domainToAdd.indexOf('|') == -1) {
                                    if (!BlockUrlPatternsMatch.isUrlValid(domainToAdd)) {
                                        Toast.makeText(this.getContext(), "Url not valid. Please check", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                } else {
                                    // packageName|url
                                    StringTokenizer tokens = new StringTokenizer(domainToAdd, "|");
                                    if (tokens.countTokens() != 2) {
                                        Toast.makeText(this.getContext(), "Rule not valid. Please check", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                }
                                new AddUrlAsyncTask(domainToAdd, page, context).execute();
                            })
                            .setNegativeButton(android.R.string.no, null).show();
                });

                break;

            case PROVIDER_PAGE:
                view = inflater.inflate(R.layout.fragment_provider, container, false);

                // Set URL limit
                TextView hintTextView = view.findViewById(R.id.providerInfoTextView);
                String strFormat = getResources().getString(R.string.provider_info);
                hintTextView.setText(String.format(strFormat, AdhellAppIntegrity.BLOCK_URL_LIMIT));

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

                FloatingActionButton actionUpdateProviders = view.findViewById(R.id.action_update_providers);
                actionUpdateProviders.setIcon(R.drawable.ic_update_black_24dp);
                actionUpdateProviders.setOnClickListener(v -> {
                    providerFloatMenu.collapse();
                    new UpdateProviderAsyncTask(context).execute();
                });

                break;
        }

        return view;
    }

    private static class RefreshListAsyncTask extends AsyncTask<Void, Void, List<String>> {
        private int page;
        private WeakReference<Context> contextWeakReference;

        RefreshListAsyncTask(int page, Context context) {
            this.page = page;
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            List<String> domainList = new ArrayList<>();
            switch (page) {
                case BLACKLIST_PAGE:
                    List<UserBlockUrl> urlList = appDatabase.userBlockUrlDao().getAll2();
                    for (UserBlockUrl blockUrl : urlList) {
                        domainList.add(blockUrl.url);
                    }
                    break;
                case WHITELIST_PAGE:
                    List<WhiteUrl> whiteUrlList = appDatabase.whiteUrlDao().getAll2();
                    for (WhiteUrl whiteUrl : whiteUrlList) {
                        domainList.add(whiteUrl.url);
                    }
                    break;
            }
            return domainList;
        }

        @Override
        protected void onPostExecute(List<String> domainList) {
            Context context = contextWeakReference.get();
            if (context != null) {
                ListView listView = null;
                SwipeRefreshLayout swipeContainer = null;
                switch (page) {
                    case BLACKLIST_PAGE:
                        listView = ((Activity) context).findViewById(R.id.blackListView);
                        swipeContainer = ((Activity) context).findViewById(R.id.blacklistSwipeContainer);
                        break;
                    case WHITELIST_PAGE:
                        listView = ((Activity) context).findViewById(R.id.whiteListView);
                        swipeContainer = ((Activity) context).findViewById(R.id.whitelistSwipeContainer);
                        break;
                }
                if (listView != null) {
                    ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, domainList);
                    listView.setAdapter(itemsAdapter);
                }
                if (swipeContainer != null) {
                    swipeContainer.setRefreshing(false);
                }
            }
        }
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
                    adapter.add(url);
                    adapter.notifyDataSetChanged();
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
