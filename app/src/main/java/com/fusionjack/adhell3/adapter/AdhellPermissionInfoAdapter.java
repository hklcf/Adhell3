package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.model.AdhellPermissionInfo;

import java.util.List;

public class AdhellPermissionInfoAdapter extends RecyclerView.Adapter<AdhellPermissionInfoAdapter.ViewHolder> {
    private List<AdhellPermissionInfo> mAdhellPermissionInfos;

    public AdhellPermissionInfoAdapter(List<AdhellPermissionInfo> adhellPermissionInfos) {
        mAdhellPermissionInfos = adhellPermissionInfos;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View permissionInfoView = inflater.inflate(R.layout.item_permission_info, parent, false);
        return new ViewHolder(permissionInfoView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AdhellPermissionInfo adhellPermissionInfo = mAdhellPermissionInfos.get(position);
        holder.permissionLabelTextView.setText(adhellPermissionInfo.label);
        holder.permissionNameTextView.setText(adhellPermissionInfo.name);
    }

    @Override
    public int getItemCount() {
        return mAdhellPermissionInfos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView permissionLabelTextView;
        TextView permissionNameTextView;

        ViewHolder(View itemView) {
            super(itemView);
            permissionLabelTextView = (TextView) itemView.findViewById(R.id.permissionLabelTextView);
            permissionNameTextView = (TextView) itemView.findViewById(R.id.permissionNameTextView);
        }
    }
}
