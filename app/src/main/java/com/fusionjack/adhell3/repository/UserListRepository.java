package com.fusionjack.adhell3.repository;

import android.arch.lifecycle.LiveData;

import java.util.List;

import io.reactivex.Single;

public interface UserListRepository {

    LiveData<List<String>> getItems();
    Single<String> addItem(String item);
    Single<String> removeItem(String item);

    void addItemToDatabase(String item);
    void removeItemFromDatabase(String item);
}
