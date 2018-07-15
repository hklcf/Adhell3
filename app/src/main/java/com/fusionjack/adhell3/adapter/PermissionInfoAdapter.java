package com.fusionjack.adhell3.adapter;

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
import com.fusionjack.adhell3.utils.AppPermissionUtils;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.util.List;

import static com.samsung.android.knox.application.ApplicationPolicy.PERMISSION_POLICY_STATE_DENY;

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
            protectionLevelTextView.setText(AppPermissionUtils.getProtectionLevelLabel(permissionInfo.getLevel()));

            boolean checked = false;
            ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
            List<String> deniedPermissions = appPolicy.getRuntimePermissions(permissionInfo.getPackageName(), PERMISSION_POLICY_STATE_DENY);
            if (!deniedPermissions.contains(permissionInfo.name)) {
                checked = true;
            }
            permissionSwitch.setChecked(checked);

            boolean enabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
            permissionSwitch.setEnabled(enabled);
        }

        return convertView;
    }
}
