package com.fusionjack.adhell3.viewmodel;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import com.fusionjack.adhell3.model.AdhellPermissionInfo;

public class SharedAppPermissionViewModel extends ViewModel {
    private final MutableLiveData<AdhellPermissionInfo> selected = new MutableLiveData<>();

    public SharedAppPermissionViewModel() {
    }

    public void select(AdhellPermissionInfo adhellPermissionInfo) {
        selected.setValue(adhellPermissionInfo);
    }

    public LiveData<AdhellPermissionInfo> getSelected() {
        return selected;
    }

}
