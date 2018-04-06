package com.fusionjack.adhell3.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;

import java.util.List;

public class BlockUrlProvidersViewModel extends ViewModel {
    private LiveData<List<BlockUrlProvider>> blockUrlProviders;
    private AppDatabase mDb;

    public BlockUrlProvidersViewModel() {
        mDb = AppDatabase.getAppDatabase(App.get().getApplicationContext());
    }

    public LiveData<List<BlockUrlProvider>> getBlockUrlProviders() {
        if (blockUrlProviders == null) {
            blockUrlProviders = new MutableLiveData<>();
            loadBlockUrlProviders();
        }
        return blockUrlProviders;
    }

    private void loadBlockUrlProviders() {
        blockUrlProviders = mDb.blockUrlProviderDao().getAll();
    }
}
