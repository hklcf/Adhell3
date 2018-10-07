package com.fusionjack.adhell3.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.fusionjack.adhell3.repository.BlackListRepository;
import com.fusionjack.adhell3.repository.UserListRepository;
import com.fusionjack.adhell3.repository.WhiteListRepository;

import java.util.List;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class UserListViewModel extends ViewModel {
    private LiveData<List<String>> items;
    private UserListRepository repository;

    public UserListViewModel(UserListRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<String>> getItems() {
        if (items == null) {
            items = repository.getItems();
        }
        return items;
    }

    public void addItem(String item, SingleObserver<String> observer) {
        repository.addItem(item)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

    public void removeItem(String item, SingleObserver<String> observer) {
        repository.removeItem(item)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

    public static class BlackListFactory extends ViewModelProvider.NewInstanceFactory {
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new UserListViewModel(new BlackListRepository());
        }
    }

    public static class WhiteListFactory extends ViewModelProvider.NewInstanceFactory {
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new UserListViewModel(new WhiteListRepository());
        }
    }
}
