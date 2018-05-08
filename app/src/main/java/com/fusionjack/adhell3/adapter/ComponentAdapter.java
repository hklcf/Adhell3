package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;

import com.fusionjack.adhell3.model.IComponentInfo;

import java.util.List;

public class ComponentAdapter extends ArrayAdapter<IComponentInfo> {

    ComponentAdapter(@NonNull Context context, @NonNull List<IComponentInfo> componentInfos) {
        super(context, 0, componentInfos);
    }

}
