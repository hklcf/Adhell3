package com.fusionjack.adhell3.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;

import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;

import java.util.List;

public class AppViewModel extends ViewModel {

    private AppRepository repository;

    public AppViewModel() {
        this.repository = new AppRepository();
    }

    public LiveData<List<AppInfo>> getAppList(String text, AppRepository.Type type) {
        return repository.getAppList(text, type);
    }

}
