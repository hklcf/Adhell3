package com.fusionjack.adhell3.db.repository;

import android.arch.lifecycle.LiveData;

import com.fusionjack.adhell3.db.entity.WhiteUrl;

import java.util.Date;
import java.util.List;

public class WhiteListRepository extends UserListRepositoryImpl {

    @Override
    public LiveData<List<String>> getItems() {
        return appDatabase.whiteUrlDao().getAll();
    }

    @Override
    public void addItemToDatabase(String item) {
        WhiteUrl whiteUrl = new WhiteUrl(item, new Date());
        appDatabase.whiteUrlDao().insert(whiteUrl);
    }

    @Override
    public void removeItemFromDatabase(String item) {
        appDatabase.whiteUrlDao().deleteByUrl(item);
    }

}
