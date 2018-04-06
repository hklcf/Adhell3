package com.fusionjack.adhell3.viewmodel;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.pm.PackageManager;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.model.AdhellPermissionInfo;

import javax.inject.Inject;

public class SharedAppPermissionViewModel extends ViewModel {
    private final MutableLiveData<AdhellPermissionInfo> selected = new MutableLiveData<>();

    @Inject
    AppDatabase appDatabase;

    @Inject
    PackageManager packageManager;

    public SharedAppPermissionViewModel() {
        App.get().getAppComponent().inject(this);
    }

    public void select(AdhellPermissionInfo adhellPermissionInfo) {
        selected.setValue(adhellPermissionInfo);
    }

    public LiveData<AdhellPermissionInfo> getSelected() {
        return selected;
    }

}
