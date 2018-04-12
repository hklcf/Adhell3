package com.fusionjack.adhell3.fragments;

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

                EditText blackUrlEditText = view.findViewById(R.id.addBlockedUrlEditText);
                Button addBlackUrlButton = view.findViewById(R.id.addCustomBlockedUrlButton);
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

                    AsyncTask.execute(() -> {
                        UserBlockUrl userBlockUrl = new UserBlockUrl(urlToAdd);
                        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
                        appDatabase.userBlockUrlDao().insert(userBlockUrl);
                    });

                    blackUrlEditText.setText("");

                    if (urlToAdd.indexOf('|') == -1) {
                        Toast.makeText(context, "Url has been added", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Rule has been added", Toast.LENGTH_SHORT).show();
                    }
                });

                ListView blacklistView = view.findViewById(R.id.customUrlsListView);
                blacklistView.setOnItemClickListener((parent, view1, position, id) -> {
                    AsyncTask.execute(() -> {
                        String item = (String) parent.getItemAtPosition(position);
                        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
                        appDatabase.userBlockUrlDao().deleteByUrl(item);
                    });
                    Toast.makeText(context, "Url removed", Toast.LENGTH_SHORT).show();
                });

                BlackUrlViewModel blackViewModel = ViewModelProviders.of(getActivity()).get(BlackUrlViewModel.class);
                blackViewModel.getBlockUrls().observe(this, blackUrls -> {
                    if (blackUrls == null) {
                        return;
                    }
                    List<String> urls = new ArrayList<>();
                    for (UserBlockUrl blockUrl : blackUrls) {
                        urls.add(blockUrl.url);
                    }
                    ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, urls);
                    blacklistView.setAdapter(itemsAdapter);
                });
                break;

            case WHITELIST_PAGE:
                view = inflater.inflate(R.layout.fragment_white_list, container, false);

                EditText whiteUrlEditText = view.findViewById(R.id.whitelistUrlEditText);
                Button addWhitelistUrlButton = view.findViewById(R.id.addWhitelistUrl);
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

                    AsyncTask.execute(() -> {
                        WhiteUrl whiteUrl = new WhiteUrl(urlToAdd);
                        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
                        appDatabase.whiteUrlDao().insert(whiteUrl);
                    });

                    whiteUrlEditText.setText("");
                    Toast.makeText(this.getContext(), "Url has been added", Toast.LENGTH_SHORT).show();
                });

                ListView whiteListView = view.findViewById(R.id.urlList);
                whiteListView.setOnItemClickListener((parent, view1, position, id) -> {
                    String item = (String) parent.getItemAtPosition(position);
                    AsyncTask.execute(() -> {
                        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
                        appDatabase.whiteUrlDao().deleteByUrl(item);
                    });
                    Toast.makeText(context, "Url removed", Toast.LENGTH_SHORT).show();
                });

                WhiteUrlViewModel model = ViewModelProviders.of(getActivity()).get(WhiteUrlViewModel.class);
                model.getWhiteUrls().observe(this, whiteUrls -> {
                    if (whiteUrls == null) {
                        return;
                    }
                    List<String> urls = new ArrayList<>();
                    for (WhiteUrl whiteUrl : whiteUrls) {
                        urls.add(whiteUrl.url);
                    }
                    ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, urls);
                    whiteListView.setAdapter(itemsAdapter);
                });
                break;

            case PROVIDER_PAGE:
                view = inflater.inflate(R.layout.fragment_custom_url_provider, container, false);

                // Set URL limit
                TextView hintTextView = view.findViewById(R.id.customUrlProviderExpTextView);
                String strFormat = getResources().getString(R.string.custom_url_provider_message);
                hintTextView.setText(String.format(strFormat, AdhellAppIntegrity.BLOCK_URL_LIMIT));

                // Add button
                EditText providerEditText = view.findViewById(R.id.blockUrlProviderEditText);
                Button addProviderButton = view.findViewById(R.id.addBlockUrlProviderButton);
                addProviderButton.setOnClickListener(v -> {
                    String urlProvider = providerEditText.getText().toString();
                    if (URLUtil.isValidUrl(urlProvider)) {
                        AsyncTask.execute(() -> {
                            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();

                            BlockUrlProvider blockUrlProvider = new BlockUrlProvider();
                            blockUrlProvider.url = urlProvider;
                            blockUrlProvider.count = 0;
                            blockUrlProvider.deletable = true;
                            blockUrlProvider.lastUpdated = new Date();
                            blockUrlProvider.selected = false;
                            blockUrlProvider.id = appDatabase.blockUrlProviderDao().insertAll(blockUrlProvider)[0];
                            blockUrlProvider.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;

                            // Try to download and parse urls
                            try {
                                List<BlockUrl> blockUrls = BlockUrlUtils.loadBlockUrls(blockUrlProvider);
                                blockUrlProvider.count = blockUrls.size();
                                appDatabase.blockUrlProviderDao().updateBlockUrlProviders(blockUrlProvider);
                                appDatabase.blockUrlDao().insertAll(blockUrls);
                            } catch (Exception e) {
                                appDatabase.blockUrlProviderDao().delete(blockUrlProvider);
                                e.printStackTrace();
                            }
                        });
                        providerEditText.setText("");
                    } else {
                        Toast.makeText(getContext(), "Url is invalid", Toast.LENGTH_LONG).show();
                    }
                });

                // Update button
                Button updateBlockUrlProvidersButton = view.findViewById(R.id.updateBlockUrlProvidersButton);
                updateBlockUrlProvidersButton.setOnClickListener(v ->
                    AsyncTask.execute(() -> {
                        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
                        List<BlockUrlProvider> blockUrlProviders = appDatabase.blockUrlProviderDao().getAll2();
                        appDatabase.blockUrlDao().deleteAll();
                        for (BlockUrlProvider blockUrlProvider : blockUrlProviders) {
                            try {
                                List<BlockUrl> blockUrls = BlockUrlUtils.loadBlockUrls(blockUrlProvider);
                                blockUrlProvider.count = blockUrls.size();
                                blockUrlProvider.lastUpdated = new Date();
                                appDatabase.blockUrlProviderDao().updateBlockUrlProviders(blockUrlProvider);
                                appDatabase.blockUrlDao().insertAll(blockUrls);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    })
                );

                // Provider list
                ListView providerListView = view.findViewById(R.id.blockUrlProviderListView);
                BlockUrlProvidersViewModel providersViewModel = ViewModelProviders.of(getActivity()).get(BlockUrlProvidersViewModel.class);
                providersViewModel.getBlockUrlProviders().observe(this, blockUrlProviders -> {
                    BlockUrlProviderAdapter adapter = new BlockUrlProviderAdapter(context, blockUrlProviders);
                    providerListView.setAdapter(adapter);
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

                break;
        }

        return view;
    }
}
