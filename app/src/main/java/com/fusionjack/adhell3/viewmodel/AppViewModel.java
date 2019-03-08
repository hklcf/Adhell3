package com.fusionjack.adhell3.viewmodel;

import android.arch.lifecycle.ViewModel;

import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;

import java.util.List;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class AppViewModel extends ViewModel {

    private AppRepository repository;

    public AppViewModel() {
        this.repository = new AppRepository();
    }

    public void loadAppList(AppRepository.Type type, SingleObserver<List<AppInfo>> observer) {
        repository.loadAppList("", type)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

    public void loadAppList(String text, AppRepository.Type type, SingleObserver<List<AppInfo>> observer) {
        repository.loadAppList(text, type)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

}
