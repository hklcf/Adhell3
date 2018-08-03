package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.entity.ReportBlockedUrl;
import com.fusionjack.adhell3.utils.AppCache;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

public class ReportBlockedUrlAdapter extends ArrayAdapter<ReportBlockedUrl> {
    private Map<String, Drawable> appIcons;
    private Map<String, String> appNames;

    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss");

    public ReportBlockedUrlAdapter(@NonNull Context context, @NonNull List<ReportBlockedUrl> objects, Handler handler) {
        super(context, 0, objects);
        AppCache appCache = AppCache.getInstance(context, handler);
        appIcons = appCache.getIcons();
        appNames = appCache.getNames();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_blocked_url_info, parent, false);
        }
        ReportBlockedUrl reportBlockedUrl = getItem(position);
        if (reportBlockedUrl == null) {
            return convertView;
        }

        ImageView blockedDomainIconImageView = convertView.findViewById(R.id.blockedDomainIconImageView);
        TextView blockedDomainAppNameTextView = convertView.findViewById(R.id.blockedDomainAppNameTextView);
        TextView blockedDomainUrlTextView = convertView.findViewById(R.id.blockedDomainUrlTextView);
        TextView blockedDomainTimeTextView = convertView.findViewById(R.id.blockedDomainTimeTextView);

        String appName = appNames.get(reportBlockedUrl.packageName);
        Drawable icon = appIcons.get(reportBlockedUrl.packageName);
        if (icon == null) {
            icon = getContext().getResources().getDrawable(android.R.drawable.sym_def_app_icon);
        }
        blockedDomainIconImageView.setImageDrawable(icon);
        blockedDomainAppNameTextView.setText(appName == null ? "(unknown)" : appName);
        blockedDomainUrlTextView.setText(reportBlockedUrl.url);
        blockedDomainTimeTextView.setText(dateFormatter.format(reportBlockedUrl.blockDate));

        return convertView;
    }
}
