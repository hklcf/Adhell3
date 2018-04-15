package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import com.fusionjack.adhell3.utils.AppCache;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

public class AppInfoAdapter extends BaseAdapter {

    private List<AppInfo> applicationInfoList;
    private WeakReference<Context> contextReference;
    private AppFlag appFlag;
    private Map<String, Drawable> appIcons;

    public AppInfoAdapter(List<AppInfo> appInfoList, AppFlag appFlag, boolean reload, Context context) {
        this.applicationInfoList = appInfoList;
        this.contextReference = new WeakReference<>(context);
        this.appFlag = appFlag;
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                notifyDataSetChanged();
            }
        };
        if (reload) {
            this.appIcons = AppCache.reload(context, handler).getIcons();
        } else {
            this.appIcons = AppCache.getInstance(context, handler).getIcons();
        }
    }

    public void setItem(int position, AppInfo appInfo) {
        applicationInfoList.set(position, appInfo);
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
        Context context = contextReference.get();
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_disable_app_list_view, parent, false);
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
            case WHITELISTED_FLAG:
                checked = appInfo.adhellWhitelisted;
                break;
        }
        holder.switchH.setChecked(!checked);

        if (appInfo.system) {
            convertView.findViewById(R.id.systemOrNot).setVisibility(View.VISIBLE);
        } else {
            convertView.findViewById(R.id.systemOrNot).setVisibility(View.GONE);
        }

        Drawable icon = appIcons.get(appInfo.packageName);
        if (icon == null) {
            icon = context.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
        }
        holder.imageH.setImageDrawable(icon);
        return convertView;
    }

    private static class ViewHolder {
        TextView nameH;
        TextView packageH;
        Switch switchH;
        ImageView imageH;
    }
}
