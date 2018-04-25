package com.fusionjack.adhell3.adapter;

import android.app.enterprise.AppPermissionControlInfo;
import android.app.enterprise.ApplicationPermissionControlPolicy;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppCache;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdhellPermissionInAppsAdapter extends RecyclerView.Adapter<AdhellPermissionInAppsAdapter.ViewHolder> {
    private static final String TAG = AdhellPermissionInAppsAdapter.class.getCanonicalName();

    private String permissionName;
    private Set<String> blacklistedPackageNames;
    private List<AppInfo> appInfos;
    private AppDatabase appDatabase;
    private ApplicationPermissionControlPolicy appControlPolicy;
    private Map<String, Drawable> appIcons;
    private WeakReference<Context> contextReference;

    public AdhellPermissionInAppsAdapter(String permissionName, List<AppInfo> packageInfos, Context context) {
        this.permissionName = permissionName;
        this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
        this.appControlPolicy = AdhellFactory.getInstance().getAppControlPolicy();
        this.appInfos = packageInfos;
        this.contextReference = new WeakReference<>(context);

        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                notifyDataSetChanged();
            }
        };
        this.appIcons = AppCache.getInstance(context, handler).getIcons();

        updatePermissionBlacklistedPackages();
    }

    private void updatePermissionBlacklistedPackages() {
        if (appControlPolicy == null) {
            return;
        }

        List<AppPermissionControlInfo> permissionInfos = appControlPolicy.getPackagesFromPermissionBlackList();
        if (permissionInfos == null || permissionInfos.size() == 0) {
            blacklistedPackageNames = null;
            return;
        }

        for (AppPermissionControlInfo permissionInfo : permissionInfos) {
            if (permissionInfo == null || permissionInfo.mapEntries == null) {
                continue;
            }
            blacklistedPackageNames = permissionInfo.mapEntries.get(permissionName);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View permissionInApp = inflater.inflate(R.layout.item_app_info, parent, false);
        return new AdhellPermissionInAppsAdapter.ViewHolder(permissionInApp);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AppInfo appInfo = appInfos.get(position);
        holder.appNameTextView.setText(appInfo.appName);
        holder.appPackageNameTextView.setText(appInfo.packageName);
        if (appInfo.system) {
            holder.systemOrNotTextView.setVisibility(View.VISIBLE);
        } else {
            holder.systemOrNotTextView.setVisibility(View.GONE);
        }
        Drawable icon = appIcons.get(appInfo.packageName);
        if (icon == null) {
            icon = contextReference.get().getResources().getDrawable(android.R.drawable.sym_def_app_icon);
        }
        holder.appIconImageView.setImageDrawable(icon);

        boolean checked = false;
        if (blacklistedPackageNames == null || !blacklistedPackageNames.contains(appInfo.packageName)) {
            checked = true;
        }
        holder.appPermissionSwitch.setChecked(checked);
    }

    @Override
    public int getItemCount() {
        if (appInfos == null) {
            return 0;
        }
        return appInfos.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView appIconImageView;
        TextView appNameTextView;
        TextView appPackageNameTextView;
        TextView systemOrNotTextView;
        Switch appPermissionSwitch;

        ViewHolder(View itemView) {
            super(itemView);
            appIconImageView = itemView.findViewById(R.id.appIcon);
            appNameTextView = itemView.findViewById(R.id.appName);
            appPackageNameTextView = itemView.findViewById(R.id.packName);
            systemOrNotTextView = itemView.findViewById(R.id.systemOrNot);
            appPermissionSwitch = itemView.findViewById(R.id.switchDisable);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (appControlPolicy == null) {
                return;
            }

            int position = getAdapterPosition();
            AppInfo appInfo = appInfos.get(position);
            List<String> list = new ArrayList<>();
            list.add(appInfo.packageName);

            if (blacklistedPackageNames != null && blacklistedPackageNames.contains(appInfo.packageName)) {
                boolean isBlacklisted = appControlPolicy.removePackagesFromPermissionBlackList(permissionName, list);
                Log.d(TAG, "Is removed: " + isBlacklisted);
                if (isBlacklisted) {
                    appPermissionSwitch.setChecked(true);
                    AsyncTask.execute(() -> appDatabase.appPermissionDao().delete(appInfo.packageName, permissionName));
                }
            } else {
                boolean success = appControlPolicy.addPackagesToPermissionBlackList(permissionName, list);
                Log.d(TAG, "Is added to blacklist: " + success);
                if (success) {
                    appPermissionSwitch.setChecked(false);
                    AsyncTask.execute(() -> {
                        AppPermission appPermission = new AppPermission();
                        appPermission.packageName = appInfo.packageName;
                        appPermission.permissionName = permissionName;
                        appPermission.permissionStatus = AppPermission.STATUS_DISALLOW;
                        appPermission.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                        appDatabase.appPermissionDao().insert(appPermission);
                    });
                }
            }
            updatePermissionBlacklistedPackages();
        }
    }
}
