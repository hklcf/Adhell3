package com.fusionjack.adhell3.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.WhiteUrl;
import com.fusionjack.adhell3.utils.AdhellFactory;

import java.util.List;

public class WhiteUrlViewModel extends ViewModel {
    private LiveData<List<WhiteUrl>> whiteUrls;

    public WhiteUrlViewModel() {
    }

    public LiveData<List<WhiteUrl>> getWhiteUrls() {
        if (whiteUrls == null) {
            whiteUrls = new MutableLiveData<>();
            loadBlockUrls();
        }
        return whiteUrls;
    }

    private void loadBlockUrls() {
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        whiteUrls = appDatabase.whiteUrlDao().getAll();
    }
}
