package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.ComponentAdapter;
import com.fusionjack.adhell3.adapter.PermissionInfoAdapter;
import com.fusionjack.adhell3.adapter.ReceiverInfoAdapter;
import com.fusionjack.adhell3.adapter.ServiceInfoAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.model.AppComponent;
import com.fusionjack.adhell3.model.IComponentInfo;
import com.fusionjack.adhell3.model.PermissionInfo;
import com.fusionjack.adhell3.model.ReceiverInfo;
import com.fusionjack.adhell3.model.ServiceInfo;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPermissionUtils;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.samsung.android.knox.application.ApplicationPolicy.ERROR_NONE;
import static com.samsung.android.knox.application.ApplicationPolicy.PERMISSION_POLICY_STATE_DENY;

public class ComponentTabPageFragment extends Fragment {

    private static final String ARG_PAGE = "page";
    private static final String ARG_PACKAGENAME = "packageName";
    private int page;
    private String packageName;
    private Context context;

    public static final int PERMISSIONS_PAGE = 0;
    public static final int SERVICES_PAGE = 1;
    public static final int RECEIVERS_PAGE = 2;

    public static ComponentTabPageFragment newInstance(int page, String packageName) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        args.putString(ARG_PACKAGENAME, packageName);
        ComponentTabPageFragment fragment = new ComponentTabPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.page = getArguments().getInt(ARG_PAGE);
        this.packageName = getArguments().getString(ARG_PACKAGENAME);
        this.context = getContext();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.enable_all_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_enable_all:
                enableComponent();
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableComponent() {
        new EnableComponentAsyncTask(page, packageName, context).execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View view = null;
        boolean toggleEnabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
        switch (page) {
            case PERMISSIONS_PAGE:
                view = inflater.inflate(R.layout.fragment_app_permission, container, false);
                ListView listView = view.findViewById(R.id.permissionInfoListView);
                if (listView != null && toggleEnabled) {
                    listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                        PermissionInfoAdapter adapter = (PermissionInfoAdapter) adView.getAdapter();
                        new SetComponentAsyncTask(PERMISSIONS_PAGE, packageName, adapter.getItem(position), context).execute();
                    });
                }
                new CreateComponentAsyncTask(PERMISSIONS_PAGE, packageName, context).execute();
                break;

            case SERVICES_PAGE:
                view = inflater.inflate(R.layout.fragment_app_service, container, false);
                listView = view.findViewById(R.id.serviceInfoListView);
                if (listView != null && toggleEnabled) {
                    listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                        ServiceInfoAdapter adapter = (ServiceInfoAdapter) adView.getAdapter();
                        new SetComponentAsyncTask(SERVICES_PAGE, packageName, adapter.getItem(position), context).execute();
                    });
                }
                new CreateComponentAsyncTask(SERVICES_PAGE, packageName, context).execute();
                break;

            case RECEIVERS_PAGE:
                view = inflater.inflate(R.layout.fragment_app_receiver, container, false);
                listView = view.findViewById(R.id.receiverInfoListView);
                if (listView != null && toggleEnabled) {
                    listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                        ReceiverInfoAdapter adapter = (ReceiverInfoAdapter) adView.getAdapter();
                        new SetComponentAsyncTask(RECEIVERS_PAGE, packageName, adapter.getItem(position), context).execute();
                    });
                }
                new CreateComponentAsyncTask(RECEIVERS_PAGE, packageName, context).execute();
                break;
        }

        return view;
    }

    private static class EnableComponentAsyncTask extends AsyncTask<Void, Void, Void> {
        private int page;
        private String packageName;
        private WeakReference<Context> contextWeakReference;

        EnableComponentAsyncTask(int page, String packageName, Context context) {
            this.page = page;
            this.packageName = packageName;
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
            switch (page) {
                case PERMISSIONS_PAGE:
                    if (appPolicy != null) {
                        List<String> deniedPermissions = appPolicy.getRuntimePermissions(packageName, PERMISSION_POLICY_STATE_DENY);
                        int errorCode = AdhellFactory.getInstance().setAppPermission(packageName, deniedPermissions, true);
                        if (errorCode == ERROR_NONE) {
                            appDatabase.appPermissionDao().deletePermissions(packageName);
                        }
                    }
                    break;
                case SERVICES_PAGE:
                    if (appPolicy != null) {
                        List<IComponentInfo> componentInfos = AppComponent.getServices(packageName);
                        for (IComponentInfo componentInfo : componentInfos) {
                            ServiceInfo serviceInfo = (ServiceInfo) componentInfo;
                            ComponentName serviceCompName = new ComponentName(packageName, serviceInfo.getName());
                            appPolicy.setApplicationComponentState(serviceCompName, true);
                        }
                        appDatabase.appPermissionDao().deleteServices(packageName);
                    }
                    break;
                case RECEIVERS_PAGE:
                    if (appPolicy != null) {
                        List<IComponentInfo> componentInfos = AppComponent.getReceivers(packageName);
                        for (IComponentInfo componentInfo : componentInfos) {
                            ReceiverInfo receiverInfo = (ReceiverInfo) componentInfo;
                            ComponentName serviceCompName = new ComponentName(packageName, receiverInfo.getName());
                            appPolicy.setApplicationComponentState(serviceCompName, true);
                        }
                        appDatabase.appPermissionDao().deleteReceivers(packageName);
                    }
                    break;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Context context = contextWeakReference.get();
            if (context != null) {
                int listViewId = -1;
                switch (page) {
                    case PERMISSIONS_PAGE:
                        listViewId = R.id.permissionInfoListView;
                        break;
                    case SERVICES_PAGE:
                        listViewId = R.id.serviceInfoListView;
                        break;
                    case RECEIVERS_PAGE:
                        listViewId = R.id.receiverInfoListView;
                        break;
                }

                ListView listView = ((Activity) context).findViewById(listViewId);
                if (listView != null) {
                    if (listView.getAdapter() instanceof ComponentAdapter) {
                        ((ComponentAdapter) listView.getAdapter()).notifyDataSetChanged();
                    }
                }
            }
        }
    }

    private static class SetComponentAsyncTask extends AsyncTask<Void, Void, Void> {
        private int page;
        private String packageName;
        private IComponentInfo componentInfo;
        private WeakReference<Context> contextWeakReference;
        private ApplicationPolicy appPolicy;

        SetComponentAsyncTask(int page, String packageName, IComponentInfo componentInfo, Context context) {
            this.page = page;
            this.packageName = packageName;
            this.componentInfo = componentInfo;
            this.contextWeakReference = new WeakReference<>(context);
            this.appPolicy = AdhellFactory.getInstance().getAppPolicy();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            switch (page) {
                case PERMISSIONS_PAGE:
                    if (appPolicy == null) {
                        return null;
                    }

                    PermissionInfo permissionInfo = (PermissionInfo) componentInfo;
                    String permissionName = permissionInfo.name;
                    List<String> permissions = new ArrayList<>();
                    permissions.add(permissionName);

                    List<String> deniedPermissions = appPolicy.getRuntimePermissions(packageName, PERMISSION_POLICY_STATE_DENY);
                    if (deniedPermissions.contains(permissionName)) {
                        int errorCode = AdhellFactory.getInstance().setAppPermission(packageName, permissions, true);
                        if (errorCode == ApplicationPolicy.ERROR_NONE) {
                            List<String> siblingPermissionNames = AppPermissionUtils.getSiblingPermissions(permissionName);
                            for (String name : siblingPermissionNames) {
                                appDatabase.appPermissionDao().delete(packageName, name);
                            }
                        }
                    } else {
                        int errorCode = AdhellFactory.getInstance().setAppPermission(packageName, permissions, false);
                        if (errorCode == ApplicationPolicy.ERROR_NONE) {
                            AppPermission newAppPermission = new AppPermission();
                            newAppPermission.packageName = packageName;
                            newAppPermission.permissionName = permissionName;
                            newAppPermission.permissionStatus = AppPermission.STATUS_PERMISSION;
                            newAppPermission.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                            appDatabase.appPermissionDao().insert(newAppPermission);
                        }
                    }
                    break;

                case SERVICES_PAGE:
                    if (appPolicy == null) {
                        return null;
                    }
                    ServiceInfo serviceInfo = (ServiceInfo) componentInfo;
                    String serviceName = serviceInfo.getName();
                    ComponentName serviceCompName = new ComponentName(packageName, serviceName);
                    boolean state = !getComponentState(packageName, serviceName);
                    try {
                        boolean success = appPolicy.setApplicationComponentState(serviceCompName, state);
                        if (success) {
                            if (state) {
                                appDatabase.appPermissionDao().delete(packageName, serviceName);
                            } else {
                                AppPermission appService = new AppPermission();
                                appService.packageName = packageName;
                                appService.permissionName = serviceName;
                                appService.permissionStatus = AppPermission.STATUS_SERVICE;
                                appService.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                                appDatabase.appPermissionDao().insert(appService);
                            }
                        }
                    } catch (SecurityException e) {
                        Log.w("", "Failed talking with application policy", e);
                    }
                    break;

                case RECEIVERS_PAGE:
                    if (appPolicy == null) {
                        return null;
                    }
                    ReceiverInfo receiverInfo = (ReceiverInfo) componentInfo;
                    String receiverName = receiverInfo.getName();
                    String receiverPermission = receiverInfo.getPermission();
                    ComponentName receiverCompName = new ComponentName(packageName, receiverName);
                    boolean receiverState = !getComponentState(packageName, receiverName);
                    try {
                        String receiverPair = receiverName + "|" + receiverPermission;
                        boolean success = appPolicy.setApplicationComponentState(receiverCompName, receiverState);
                        if (success) {
                            if (receiverState) {
                                appDatabase.appPermissionDao().delete(packageName, receiverPair);
                            } else {
                                AppPermission appReceiver = new AppPermission();
                                appReceiver.packageName = packageName;
                                appReceiver.permissionName = receiverPair;
                                appReceiver.permissionStatus = AppPermission.STATUS_RECEIVER;
                                appReceiver.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                                appDatabase.appPermissionDao().insert(appReceiver);
                            }
                        }
                    } catch (SecurityException e) {
                        Log.w("", "Failed talking with application policy", e);
                    }
                    break;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Context context = contextWeakReference.get();
            if (context != null) {
                int listViewId = -1;
                switch (page) {
                    case PERMISSIONS_PAGE:
                        listViewId = R.id.permissionInfoListView;
                        break;
                    case SERVICES_PAGE:
                        listViewId = R.id.serviceInfoListView;
                        break;
                    case RECEIVERS_PAGE:
                        listViewId = R.id.receiverInfoListView;
                        break;
                }

                ListView listView = ((Activity) context).findViewById(listViewId);
                if (listView != null) {
                    if (listView.getAdapter() instanceof ComponentAdapter) {
                        ((ComponentAdapter) listView.getAdapter()).notifyDataSetChanged();
                    }
                }
            }
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

    private static class CreateComponentAsyncTask extends AsyncTask<Void, Void, List<IComponentInfo>> {
        private int page;
        private String packageName;
        private WeakReference<Context> contextReference;

        CreateComponentAsyncTask(int page, String packageName, Context context) {
            this.page = page;
            this.packageName = packageName;
            this.contextReference = new WeakReference<>(context);
        }

        @Override
        protected List<IComponentInfo> doInBackground(Void... voids) {
            switch (page) {
                case PERMISSIONS_PAGE:
                    return AppComponent.getPermissions(packageName);
                case SERVICES_PAGE:
                    return AppComponent.getServices(packageName);
                case RECEIVERS_PAGE:
                    return AppComponent.getReceivers(packageName);
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<IComponentInfo> componentInfos) {
            Context context = contextReference.get();
            if (context != null) {
                ComponentAdapter adapter = null;
                int listViewId = -1;
                switch (page) {
                    case PERMISSIONS_PAGE:
                        listViewId = R.id.permissionInfoListView;
                        adapter = new PermissionInfoAdapter(context, componentInfos);
                        break;
                    case SERVICES_PAGE:
                        listViewId = R.id.serviceInfoListView;
                        adapter = new ServiceInfoAdapter(context, componentInfos);
                        break;
                    case RECEIVERS_PAGE:
                        listViewId = R.id.receiverInfoListView;
                        adapter = new ReceiverInfoAdapter(context, componentInfos);
                        break;
                }

                ListView listView = ((Activity) context).findViewById(listViewId);
                if (listView != null && adapter != null) {
                    listView.setAdapter(adapter);
                }
            }
        }
    }
}
