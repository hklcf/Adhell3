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
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.utils.AppCache;
import com.fusionjack.adhell3.utils.AppPreferences;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

public class AppInfoAdapter extends BaseAdapter {

    private List<AppInfo> applicationInfoList;
    private WeakReference<Context> contextReference;
    private AppRepository.Type appType;
    private Map<String, Drawable> appIcons;
    private Map<String, String> versionNames;

    public AppInfoAdapter(List<AppInfo> appInfoList, AppRepository.Type appType, boolean reload, Context context) {
        this.applicationInfoList = appInfoList;
        this.contextReference = new WeakReference<>(context);
        this.appType = appType;
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
            this.versionNames = AppCache.getInstance(context, handler).getVersionNames();
        }
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
            convertView = LayoutInflater.from(context).inflate(R.layout.item_app_info, parent, false);
            holder = new ViewHolder();
            holder.nameH = convertView.findViewById(R.id.appName);
            holder.packageH = convertView.findViewById(R.id.packName);
            holder.infoH = convertView.findViewById(R.id.systemOrNot);
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
        switch (appType) {
            case DISABLER:
                checked = !appInfo.disabled;
                boolean enabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
                holder.switchH.setEnabled(enabled);
                break;
            case MOBILE_RESTRICTED:
                checked = !appInfo.mobileRestricted;
                break;
            case WIFI_RESTRICTED:
                checked = !appInfo.wifiRestricted;
                break;
            case WHITELISTED:
                checked = appInfo.adhellWhitelisted;
                break;
            case COMPONENT:
                holder.switchH.setVisibility(View.GONE);
            case DNS:
                boolean isDnsNotEmpty = AppPreferences.getInstance().isDnsNotEmpty();
                if (isDnsNotEmpty) {
                    holder.switchH.setEnabled(true);
                } else {
                    holder.switchH.setEnabled(false);
                }
                checked = appInfo.hasCustomDns;
                break;
        }
        holder.switchH.setChecked(checked);

        String info = appInfo.system ? "System" : "User";
        holder.infoH.setText(String.format("%s (%s)", info, versionNames.get(appInfo.packageName)));

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
        TextView infoH;
        Switch switchH;
        ImageView imageH;
    }
}
