package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.fragments.AppFlag;

import java.lang.ref.WeakReference;
import java.util.List;

public class AppInfoAdapter extends BaseAdapter {

    private List<AppInfo> applicationInfoList;
    private WeakReference<Context> contextReference;
    private AppFlag appFlag;

    public AppInfoAdapter(List<AppInfo> appInfoList, AppFlag appFlag, Context context) {
        this.applicationInfoList = appInfoList;
        this.contextReference = new WeakReference<>(context);
        this.appFlag = appFlag;
    }

    @Override
    public int getCount() {
        return this.applicationInfoList.size();
    }

    @Override
    public AppInfo getItem(int position) {
        return this.applicationInfoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(contextReference.get()).inflate(R.layout.item_disable_app_list_view, parent, false);
            holder = new ViewHolder();
            holder.nameH = convertView.findViewById(R.id.appName);
            holder.packageH = convertView.findViewById(R.id.packName);
            holder.switchH = convertView.findViewById(R.id.switchDisable);
            holder.imageH = convertView.findViewById(R.id.appIcon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AppInfo appInfo = applicationInfoList.get(position);
        holder.nameH.setText(appInfo.appName);
        holder.packageH.setText(appInfo.packageName);
        boolean checked = false;
        switch (appFlag.getFlag()) {
            case DISABLER_FLAG:
                checked = appInfo.disabled;
                break;
            case RESTRICTED_FLAG:
                checked = appInfo.mobileRestricted;
                break;
        }
        holder.switchH.setChecked(!checked);

        if (appInfo.system) {
            convertView.findViewById(R.id.systemOrNot).setVisibility(View.VISIBLE);
        } else {
            convertView.findViewById(R.id.systemOrNot).setVisibility(View.GONE);
        }
        try {
            holder.imageH.setImageDrawable(appFlag.getPackageManager().getApplicationIcon(appInfo.packageName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return convertView;
    }

    private static class ViewHolder {
        TextView nameH;
        TextView packageH;
        Switch switchH;
        ImageView imageH;
    }
}
