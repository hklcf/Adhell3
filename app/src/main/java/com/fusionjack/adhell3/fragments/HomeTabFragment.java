package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.ReportBlockedUrlAdapter;
import com.fusionjack.adhell3.blocker.ContentBlocker;
import com.fusionjack.adhell3.blocker.ContentBlocker56;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.ReportBlockedUrl;
import com.fusionjack.adhell3.db.entity.WhiteUrl;
import com.fusionjack.adhell3.dialogfragment.FirewallDialogFragment;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppCache;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.FirewallUtils;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

public class HomeTabFragment extends Fragment {
    private static final String TAG = HomeTabFragment.class.getCanonicalName();

    private FragmentManager fragmentManager;
    private AppCompatActivity parentActivity;
    private TextView domainStatusTextView;
    private Switch domainSwitch;
    private TextView firewallStatusTextView;
    private Switch firewallSwitch;
    private TextView disablerStatusTextView;
    private Switch disablerSwitch;
    private TextView infoTextView;
    private SwipeRefreshLayout swipeContainer;
    private ContentBlocker contentBlocker;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentManager = getActivity().getSupportFragmentManager();
        parentActivity = (AppCompatActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ActionBar actionBar = parentActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);

            View view = inflater.inflate(R.layout.activity_actionbar, container, false);
            TextView subtitleTextView = view.findViewById(R.id.subtitleTextView);
            if (subtitleTextView != null) {
                String versionInfo = getContext().getResources().getString(R.string.version);
                subtitleTextView.setText(String.format(versionInfo, BuildConfig.VERSION_NAME));
            }
            actionBar.setCustomView(view);
            actionBar.setDisplayShowCustomEnabled(true);
        }

        View view = inflater.inflate(R.layout.fragment_blocker, container, false);
        domainSwitch = view.findViewById(R.id.domainRulesSwitch);
        domainStatusTextView = view.findViewById(R.id.domainStatusTextView);
        firewallSwitch = view.findViewById(R.id.firewallRulesSwitch);
        firewallStatusTextView = view.findViewById(R.id.firewallStatusTextView);
        disablerSwitch = view.findViewById(R.id.appDisablerSwitch);
        disablerStatusTextView = view.findViewById(R.id.disablerStatusTextView);
        swipeContainer = view.findViewById(R.id.swipeContainer);
        infoTextView = view.findViewById(R.id.infoTextView);

        contentBlocker = ContentBlocker56.getInstance();

        infoTextView.setVisibility(View.INVISIBLE);
        swipeContainer.setVisibility(View.INVISIBLE);
        domainSwitch.setOnClickListener(v -> {
            Log.d(TAG, "Domain switch button has been clicked");
            new SetFirewallAsyncTask(true, this, fragmentManager).execute();
        });
        firewallSwitch.setOnClickListener(v -> {
            Log.d(TAG, "Firewall switch button has been clicked");
            new SetFirewallAsyncTask(false, this, fragmentManager).execute();
        });
        disablerSwitch.setOnClickListener(v -> {
            Log.d(TAG, "App disabler switch button has been clicked");
            new AppDisablerAsyncTask(this, getActivity()).execute();
        });

        AsyncTask.execute(() -> {
            AdhellAppIntegrity adhellAppIntegrity = AdhellAppIntegrity.getInstance();
            adhellAppIntegrity.checkDefaultPolicyExists();
            adhellAppIntegrity.checkAdhellStandardPackage();
            adhellAppIntegrity.fillPackageDb();
        });

        updateUserInterface();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUserInterface();
    }

    private void updateUserInterface() {
        boolean isDomainRuleEmpty = contentBlocker.isDomainRuleEmpty();
        boolean isFirewallRuleEmpty = contentBlocker.isFirewallRuleEmpty();

        if (contentBlocker == null || isDomainRuleEmpty) {
            domainStatusTextView.setText(R.string.domain_rules_disabled);
            domainSwitch.setChecked(false);
        } else {
            domainStatusTextView.setText(R.string.domain_rules_enabled);
            domainSwitch.setChecked(true);
        }

        if (contentBlocker == null || isFirewallRuleEmpty) {
            firewallStatusTextView.setText(R.string.firewall_rules_disabled);
            firewallSwitch.setChecked(false);
        } else {
            firewallStatusTextView.setText(R.string.firewall_rules_enabled);
            firewallSwitch.setChecked(true);
        }

        if (!isDomainRuleEmpty) {
            infoTextView.setVisibility(View.VISIBLE);
            swipeContainer.setVisibility(View.VISIBLE);
            swipeContainer.setOnRefreshListener(() ->
                    new RefreshAsyncTask(getContext()).execute()
            );
            AppCache.getInstance(getContext(), null);
            new RefreshAsyncTask(getContext()).execute();
        } else {
            infoTextView.setVisibility(View.INVISIBLE);
            swipeContainer.setVisibility(View.INVISIBLE);
        }

        boolean disablerEnabled = AppPreferences.getInstance().isAppDisablerEnabled();
        if (disablerEnabled) {
            disablerStatusTextView.setText(R.string.app_disabler_enabled);
            disablerSwitch.setChecked(true);
        } else {
            disablerStatusTextView.setText(R.string.app_disabler_disabled);
            disablerSwitch.setChecked(false);
        }

        new SetInfoAsyncTask(getContext()).execute();
    }

    private static class SetInfoAsyncTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<Context> contextWeakReference;
        private int mobileSize;
        private int wifiSize;
        private int customSize;
        private int blackListSize;
        private int whiteListSize;
        private int whitelistAppSize;
        private int disablerSize;

        SetInfoAsyncTask(Context context) {
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            Context context = contextWeakReference.get();
            if (context != null) {
                TextView domainInfoTextView = ((Activity) context).findViewById(R.id.domainInfoTextView);
                if (domainInfoTextView != null) {
                    String domainInfo = context.getResources().getString(R.string.domain_rules_info);
                    domainInfoTextView.setText(String.format(domainInfo, 0, 0, 0));
                }
                TextView firewallInfoTextView = ((Activity) context).findViewById(R.id.firewallInfoTextView);
                if (firewallInfoTextView != null) {
                    String firewallInfo = context.getResources().getString(R.string.firewall_rules_info);
                    firewallInfoTextView.setText(String.format(firewallInfo, 0, 0, 0));
                }
                TextView disablerInfoTextView = ((Activity) context).findViewById(R.id.disablerInfoTextView);
                if (disablerInfoTextView != null) {
                    String disablerInfo = context.getResources().getString(R.string.app_disabler_info);
                    disablerInfoTextView.setText(String.format(disablerInfo, 0));
                }
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            disablerSize = appDatabase.disabledPackageDao().getAll().size();

            FirewallUtils.DomainStat domainStat = FirewallUtils.getInstance().getDomainStatFromKnox();
            blackListSize = domainStat.blackListSize;
            whiteListSize = domainStat.whiteListSize;

            whitelistAppSize = FirewallUtils.getInstance().getWhitelistAppCountFromKnox();

            // Dirty solution: Every deny firewall is created for IPv4 and IPv6.
            FirewallUtils.FirewallStat stat = FirewallUtils.getInstance().getFirewallStatFromKnox();
            customSize = stat.allNetworkSize / 2;
            mobileSize = stat.mobileDataSize / 2;
            wifiSize = stat.wifiDataSize / 2;

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Context context = contextWeakReference.get();
            if (context != null) {
                TextView domainInfoTextView = ((Activity) context).findViewById(R.id.domainInfoTextView);
                if (domainInfoTextView != null) {
                    String domainInfo = context.getResources().getString(R.string.domain_rules_info);
                    domainInfoTextView.setText(String.format(domainInfo, blackListSize, whiteListSize, whitelistAppSize));
                }
                TextView firewallInfoTextView = ((Activity) context).findViewById(R.id.firewallInfoTextView);
                if (firewallInfoTextView != null) {
                    String firewallInfo = context.getResources().getString(R.string.firewall_rules_info);
                    firewallInfoTextView.setText(String.format(firewallInfo, mobileSize, wifiSize, customSize));
                }
                TextView disablerInfoTextView = ((Activity) context).findViewById(R.id.disablerInfoTextView);
                if (disablerInfoTextView != null) {
                    String disablerInfo = context.getResources().getString(R.string.app_disabler_info);
                    boolean enabled = AppPreferences.getInstance().isAppDisablerEnabled();
                    disablerInfoTextView.setText(String.format(disablerInfo, enabled ? disablerSize : 0));
                }
            }
        }
    }

    private static class AppDisablerAsyncTask extends AsyncTask<Void, Void, Void> {
        private HomeTabFragment parentFragment;
        private ProgressDialog dialog;

        AppDisablerAsyncTask(HomeTabFragment parentFragment, Activity activity) {
            this.parentFragment = parentFragment;
            this.dialog = new ProgressDialog(activity);
        }

        @Override
        protected void onPreExecute() {
            boolean enabled = AppPreferences.getInstance().isAppDisablerEnabled();
            dialog.setMessage(enabled ? "Enabling apps..." : "Disabling apps...");
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            boolean disablerEnabled = AppPreferences.getInstance().isAppDisablerEnabled();
            AppPreferences.getInstance().setAppDisabler(!disablerEnabled);
            AdhellFactory.getInstance().applyAppDisabler();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            parentFragment.updateUserInterface();
        }
    }

    private static class SetFirewallAsyncTask extends AsyncTask<Void, Void, Void> {
        private FragmentManager fragmentManager;
        private FirewallDialogFragment fragment;
        private HomeTabFragment parentFragment;
        private ContentBlocker contentBlocker;
        private Handler handler;
        private boolean isDomain;
        private boolean isDomainRuleEmpty;
        private boolean isFirewallRuleEmpty;

        SetFirewallAsyncTask(boolean isDomain, HomeTabFragment parentFragment, FragmentManager fragmentManager) {
            this.isDomain = isDomain;
            this.parentFragment = parentFragment;
            this.fragmentManager = fragmentManager;
            this.contentBlocker = ContentBlocker56.getInstance();
            this.isDomainRuleEmpty = contentBlocker.isDomainRuleEmpty();
            this.isFirewallRuleEmpty = contentBlocker.isFirewallRuleEmpty();

            this.handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    fragment.appendText(msg.obj.toString());
                }
            };
        }

        @Override
        protected void onPreExecute() {
            if (isDomain) {
                fragment = FirewallDialogFragment.newInstance(
                        isDomainRuleEmpty ? "Enabling Domain Rules" : "Disabling Domain Rules");
            } else {
                fragment = FirewallDialogFragment.newInstance(
                        isFirewallRuleEmpty ? "Enabling Firewall Rules" : "Disabling Firewall Rules");
            }
            fragment.setCancelable(false);
            fragment.show(fragmentManager, "dialog_firewall");
        }

        @Override
        protected Void doInBackground(Void... args) {
            contentBlocker.setHandler(handler);
            if (isDomain) {
                if (isDomainRuleEmpty) {
                    contentBlocker.enableDomainRules();
                } else {
                    contentBlocker.disableDomainRules();
                }
            } else {
                if (isFirewallRuleEmpty) {
                    contentBlocker.enableFirewallRules();
                } else {
                    contentBlocker.disableFirewallRules();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            fragment.enableCloseButton();
            parentFragment.updateUserInterface();
        }
    }

    private static class RefreshAsyncTask extends AsyncTask<Void, Void, List<ReportBlockedUrl>> {
        private WeakReference<Context> contextReference;

        RefreshAsyncTask(Context context) {
            this.contextReference = new WeakReference<>(context);
        }

        @Override
        protected List<ReportBlockedUrl> doInBackground(Void... voids) {
            return FirewallUtils.getInstance().getReportBlockedUrl();
        }

        @Override
        protected void onPostExecute(List<ReportBlockedUrl> reportBlockedUrls) {
            Context context = contextReference.get();
            if (context != null) {
                ListView listView = ((Activity) context).findViewById(R.id.blockedDomainsListView);
                if (listView != null) {
                    ReportBlockedUrlAdapter adapter = new ReportBlockedUrlAdapter(context, reportBlockedUrls);
                    listView.setAdapter(adapter);
                    listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_question, listView, false);
                        TextView titlTextView = dialogView.findViewById(R.id.titleTextView);
                        titlTextView.setText(R.string.dialog_whitelist_domain_title);
                        TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
                        questionTextView.setText(R.string.dialog_add_to_whitelist_question);
                        new AlertDialog.Builder(context)
                            .setView(dialogView)
                            .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                                AsyncTask.execute(() -> {
                                    AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
                                    final String blockedUrl = reportBlockedUrls.get(position).url;
                                    WhiteUrl whiteUrl = new WhiteUrl(blockedUrl, new Date());
                                    appDatabase.whiteUrlDao().insert(whiteUrl);
                                })
                            )
                            .setNegativeButton(android.R.string.no, null).show();
                    });
                }

                TextView infoTextView = ((Activity) context).findViewById(R.id.infoTextView);
                if (infoTextView != null) {
                    infoTextView.setText(String.format("%s%s",
                            context.getString(R.string.last_day_blocked), String.valueOf(reportBlockedUrls.size())));
                }

                SwipeRefreshLayout swipeContainer = ((Activity) context).findViewById(R.id.swipeContainer);
                if (swipeContainer != null) {
                    swipeContainer.setRefreshing(false);
                }
            }
        }
    }
}
