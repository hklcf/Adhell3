package com.fusionjack.adhell3.fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.BlockUrlProviderAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.BlockUrlUtils;
import com.fusionjack.adhell3.viewmodel.BlockUrlProvidersViewModel;

import java.util.Date;
import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class CustomBlockUrlProviderFragment extends Fragment {

    private static final String TAG = CustomBlockUrlProviderFragment.class.getCanonicalName();
    private AppDatabase mDb;
    private EditText blockUrlProviderEditText;
    private Button addBlockUrlProviderButton;
    private ListView blockListView;
    private FragmentManager fragmentManager;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDb = AppDatabase.getAppDatabase(App.get().getApplicationContext());
        fragmentManager = getActivity().getSupportFragmentManager();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_custom_url_provider, container, false);
        blockUrlProviderEditText = (EditText) view.findViewById(R.id.blockUrlProviderEditText);
        addBlockUrlProviderButton = (Button) view.findViewById(R.id.addBlockUrlProviderButton);
        blockListView = (ListView) view.findViewById(R.id.blockUrlProviderListView);

        blockListView.setOnItemClickListener((parent, view1, position, id) -> {
            Maybe.fromCallable(() -> {
                List<BlockUrlProvider> providers = mDb.blockUrlProviderDao().getAll2();
                BlockUrlProvider provider = providers.get(position);

                Bundle bundle = new Bundle();
                bundle.putLong("provider", provider.id);
                ShowBlockUrlFragment fragment = new ShowBlockUrlFragment();
                fragment.setArguments(bundle);

                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragmentContainer, fragment);
                fragmentTransaction.addToBackStack("manage_url_to_add_custom");
                fragmentTransaction.commit();

                return null;
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();
        });

        Button updateBlockUrlProvidersButton = (Button) view.findViewById(R.id.updateBlockUrlProvidersButton);
        updateBlockUrlProvidersButton.setOnClickListener(v -> {
            // TODO: getAll all
            // TODO: then loop and delete and update
            Maybe.fromCallable(() -> {
                List<BlockUrlProvider> blockUrlProviders = mDb.blockUrlProviderDao().getAll2();
                mDb.blockUrlDao().deleteAll();
                for (BlockUrlProvider blockUrlProvider : blockUrlProviders) {
                    try {
                        List<BlockUrl> blockUrls = BlockUrlUtils.loadBlockUrls(blockUrlProvider);
                        blockUrlProvider.count = blockUrls.size();
                        blockUrlProvider.lastUpdated = new Date();
                        mDb.blockUrlProviderDao().updateBlockUrlProviders(blockUrlProvider);
                        mDb.blockUrlDao().insertAll(blockUrls);
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
                return null;
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();

        });
        addBlockUrlProviderButton.setOnClickListener(v -> {
            String urlProvider = blockUrlProviderEditText.getText().toString();
            // Check if normal url
            if (URLUtil.isValidUrl(urlProvider)) {
                Maybe.fromCallable(() -> {
                    BlockUrlProvider blockUrlProvider = new BlockUrlProvider();
                    blockUrlProvider.url = urlProvider;
                    blockUrlProvider.count = 0;
                    blockUrlProvider.deletable = true;
                    blockUrlProvider.lastUpdated = new Date();
                    blockUrlProvider.selected = false;
                    blockUrlProvider.id = mDb.blockUrlProviderDao().insertAll(blockUrlProvider)[0];
                    blockUrlProvider.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;

                    // Try to download and parse urls
                    try {
                        List<BlockUrl> blockUrls = BlockUrlUtils.loadBlockUrls(blockUrlProvider);
                        blockUrlProvider.count = blockUrls.size();
                        Log.d(TAG, "Number of urls to insert: " + blockUrlProvider.count);
                        // Save url provider
                        mDb.blockUrlProviderDao().updateBlockUrlProviders(blockUrlProvider);
                        // Save urls from providers
                        mDb.blockUrlDao().insertAll(blockUrls);
                    } catch (Exception e) {
                        mDb.blockUrlProviderDao().delete(blockUrlProvider);
                        Log.e(TAG, e.getMessage(), e);
                    }

                    return null;
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe();
                blockUrlProviderEditText.setText("");
            } else {
                Toast.makeText(getContext(), "Url is invalid", Toast.LENGTH_LONG).show();
            }
        });

        BlockUrlProvidersViewModel model = ViewModelProviders.of(getActivity()).get(BlockUrlProvidersViewModel.class);
        model.getBlockUrlProviders().observe(this, blockUrlProviders -> {
            BlockUrlProviderAdapter adapter = new BlockUrlProviderAdapter(this.getContext(), blockUrlProviders);
            blockListView.setAdapter(adapter);
        });

        return view;
    }
}
