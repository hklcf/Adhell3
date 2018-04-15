package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.model.AdhellPermissionInfo;

import java.util.List;

public class AdhellPermissionInfoAdapter extends ArrayAdapter<AdhellPermissionInfo> {

    public AdhellPermissionInfoAdapter(@NonNull Context context, @NonNull List<AdhellPermissionInfo> permissionInfos) {
        super(context, 0, permissionInfos);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_permission_info, parent, false);
        }

        AdhellPermissionInfo permissionInfo = getItem(position);
        if (permissionInfo == null) {
            return convertView;
        }

        TextView permissionLabelTextView = convertView.findViewById(R.id.permissionLabelTextView);
        TextView permissionNameTextView = convertView.findViewById(R.id.permissionNameTextView);
        TextView protectionLevelTextView = convertView.findViewById(R.id.protectionLevelTextView);
        permissionLabelTextView.setText(permissionInfo.label);
        permissionNameTextView.setText(permissionInfo.name);
        protectionLevelTextView.setText(permissionInfo.getProtectionLevelLabel());

        return convertView;
    }
}
