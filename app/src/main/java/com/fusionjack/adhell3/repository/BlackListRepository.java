package com.fusionjack.adhell3.repository;

import android.arch.lifecycle.LiveData;

import com.fusionjack.adhell3.db.entity.UserBlockUrl;

import java.util.Date;
import java.util.List;

public class BlackListRepository extends UserListRepositoryImpl {

    @Override
    public LiveData<List<String>> getItems() {
        return appDatabase.userBlockUrlDao().getAll();
    }

    @Override
    public void addItemToDatabase(String item) {
        UserBlockUrl userBlockUrl = new UserBlockUrl(item, new Date());
        appDatabase.userBlockUrlDao().insert(userBlockUrl);
    }

    @Override
    public void removeItemFromDatabase(String item) {
        appDatabase.userBlockUrlDao().deleteByUrl(item);
    }

}
