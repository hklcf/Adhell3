package com.fusionjack.adhell3.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

public abstract class UserListFragment extends Fragment {
    protected Context context;
    protected SingleObserver<String> addObserver;
    protected SingleObserver<String> deleteObserver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.context = getContext();

        addObserver = new SingleObserver<String>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(String item) {
                if (item.indexOf('|') == -1) {
                    Toast.makeText(context, "Domain has been added", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Rule has been added", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        deleteObserver = new SingleObserver<String>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(String item) {
                if (item.indexOf('|') == -1) {
                    Toast.makeText(context, "Domain has been removed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Rule has been removed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
    }
}
