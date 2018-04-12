package com.fusionjack.adhell3.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.UserBlockUrl;
import com.fusionjack.adhell3.utils.AdhellFactory;

import java.util.List;

public class BlackUrlViewModel extends ViewModel {
    private LiveData<List<UserBlockUrl>> blockUrls;

    public BlackUrlViewModel() {
    }

    public LiveData<List<UserBlockUrl>> getBlockUrls() {
        if (blockUrls == null) {
            blockUrls = new MutableLiveData<>();
            loadBlockUrls();
        }
        return blockUrls;
    }

    private void loadBlockUrls() {
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        blockUrls = appDatabase.userBlockUrlDao().getAll();
    }
}
