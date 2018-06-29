package com.fusionjack.adhell3.adapter;

import android.content.ComponentName;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.model.IComponentInfo;
import com.fusionjack.adhell3.model.ReceiverInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.util.List;

public class ReceiverInfoAdapter extends ComponentAdapter {

    public ReceiverInfoAdapter(@NonNull Context context, @NonNull List<IComponentInfo> componentInfos) {
        super(context, componentInfos);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_receiver_info, parent, false);
        }

        IComponentInfo componentInfo = getItem(position);
        if (componentInfo == null) {
            return convertView;
        }

        if (componentInfo instanceof ReceiverInfo) {
            ReceiverInfo receiverInfo = (ReceiverInfo) componentInfo;
            String packageName = receiverInfo.getPackageName();
            String receiverName = receiverInfo.getName();
            String permission = receiverInfo.getPermission();
            TextView receiverNameTextView = convertView.findViewById(R.id.receiverNameTextView);
            TextView receiverPermissionTextView = convertView.findViewById(R.id.receiverPermissionTextView);
            Switch permissionSwitch = convertView.findViewById(R.id.switchDisable);
            receiverNameTextView.setText(receiverName);
            receiverPermissionTextView.setText(permission);
            permissionSwitch.setChecked(getComponentState(packageName, receiverName));
        }

        return convertView;
    }

    private boolean getComponentState(String packageName, String serviceName) {
        ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
        if (appPolicy == null) {
            return false;
        }

        ComponentName componentName = new ComponentName(packageName, serviceName);
        return appPolicy.getApplicationComponentState(componentName);
    }
}
