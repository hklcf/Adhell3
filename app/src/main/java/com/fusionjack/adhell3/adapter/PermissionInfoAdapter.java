package com.fusionjack.adhell3.adapter;

import android.app.enterprise.AppPermissionControlInfo;
import android.app.enterprise.ApplicationPermissionControlPolicy;
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
import com.fusionjack.adhell3.model.PermissionInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;

import java.util.List;
import java.util.Set;

public class PermissionInfoAdapter extends ComponentAdapter {

    public PermissionInfoAdapter(@NonNull Context context, @NonNull List<IComponentInfo> componentInfos) {
        super(context, componentInfos);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_permission_info, parent, false);
        }

        IComponentInfo componentInfo = getItem(position);
        if (componentInfo == null) {
            return convertView;
        }

        if (componentInfo instanceof PermissionInfo) {
            PermissionInfo permissionInfo = (PermissionInfo) componentInfo;
            TextView permissionLabelTextView = convertView.findViewById(R.id.permissionLabelTextView);
            TextView permissionNameTextView = convertView.findViewById(R.id.permissionNameTextView);
            TextView protectionLevelTextView = convertView.findViewById(R.id.protectionLevelTextView);
            Switch permissionSwitch = convertView.findViewById(R.id.switchDisable);
            permissionLabelTextView.setText(permissionInfo.label);
            permissionNameTextView.setText(permissionInfo.name);
            protectionLevelTextView.setText(permissionInfo.getProtectionLevelLabel());

            boolean checked = false;
            Set<String> blacklistedPackageNames = getPermissionBlacklistedPackages(permissionInfo.name);
            if (blacklistedPackageNames == null || !blacklistedPackageNames.contains(permissionInfo.getPackageName())) {
                checked = true;
            }
            permissionSwitch.setChecked(checked);
        }

        return convertView;
    }

    private Set<String> getPermissionBlacklistedPackages(String permissionName) {
        ApplicationPermissionControlPolicy permissionPolicy = AdhellFactory.getInstance().getAppControlPolicy();
        if (permissionPolicy == null) {
            return null;
        }

        List<AppPermissionControlInfo> permissionInfos = permissionPolicy.getPackagesFromPermissionBlackList();
        if (permissionInfos == null || permissionInfos.size() == 0) {
            return null;
        }

        for (AppPermissionControlInfo permissionInfo : permissionInfos) {
            if (permissionInfo == null || permissionInfo.mapEntries == null) {
                continue;
            }
            return permissionInfo.mapEntries.get(permissionName);
        }

        return null;
    }
}
