package com.fusionjack.adhell3.adapter;

import android.app.enterprise.ApplicationPolicy;
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
import com.fusionjack.adhell3.model.ServiceInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;

import java.util.List;

public class ServiceInfoAdapter extends ComponentAdapter {

    public ServiceInfoAdapter(@NonNull Context context, @NonNull List<IComponentInfo> componentInfos) {
        super(context, componentInfos);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_service_info, parent, false);
        }

        IComponentInfo componentInfo = getItem(position);
        if (componentInfo == null) {
            return convertView;
        }

        if (componentInfo instanceof ServiceInfo) {
            ServiceInfo serviceInfo = (ServiceInfo) componentInfo;
            String packageName = serviceInfo.getPackageName();
            String serviceName = serviceInfo.getName();
            TextView serviceNameTextView = convertView.findViewById(R.id.serviceNameTextView);
            Switch permissionSwitch = convertView.findViewById(R.id.switchDisable);
            serviceNameTextView.setText(serviceName);
            permissionSwitch.setChecked(getComponentState(packageName, serviceName));
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
