package com.fusionjack.adhell3.db.repository;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.utils.AdhellFactory;

import io.reactivex.Single;

public abstract class UserListRepositoryImpl implements UserListRepository {

    protected AppDatabase appDatabase;

    UserListRepositoryImpl() {
        this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
    }

    @Override
    public Single<String> addItem(String item) {
        if (item == null || item.isEmpty()) {
            return Single.error(new IllegalArgumentException("Item cannot be null or empty"));
        }
        return Single.create(emitter -> {
            addItemToDatabase(item);
            emitter.onSuccess(item);
        });
    }

    @Override
    public Single<String> removeItem(String item) {
        if (item == null || item.isEmpty()) {
            return Single.error(new IllegalArgumentException("Item cannot be null or empty"));
        }
        return Single.create(emitter -> {
            removeItemFromDatabase(item);
            emitter.onSuccess(item);
        });
    }
}
