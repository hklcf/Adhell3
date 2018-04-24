package com.fusionjack.adhell3.fragments;

import android.app.Activity;
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

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.ReportBlockedUrlAdapter;
import com.fusionjack.adhell3.blocker.ContentBlocker;
import com.fusionjack.adhell3.blocker.ContentBlocker56;
import com.fusionjack.adhell3.blocker.ContentBlocker57;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.ReportBlockedUrl;
import com.fusionjack.adhell3.db.entity.WhiteUrl;
import com.fusionjack.adhell3.dialogfragment.FirewallDialogFragment;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppCache;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;
import com.sec.enterprise.firewall.DomainFilterReport;
import com.sec.enterprise.firewall.Firewall;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BlockerFragment extends Fragment {
    private static final String TAG = BlockerFragment.class.getCanonicalName();

    private FragmentManager fragmentManager;
    private AppCompatActivity parentActivity;
    private TextView statusTextView;
    private Switch turnOnSwitch;
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
        if (parentActivity.getSupportActionBar() != null) {
            parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            parentActivity.getSupportActionBar().setHomeButtonEnabled(false);
        }
        getActivity().setTitle(getString(R.string.blocker_fragment_title));

        View view = inflater.inflate(R.layout.fragment_blocker, container, false);
        turnOnSwitch = view.findViewById(R.id.switchDisable);
        statusTextView = view.findViewById(R.id.statusTextView);
        swipeContainer = view.findViewById(R.id.swipeContainer);
        infoTextView = view.findViewById(R.id.infoTextView);
        TextView warningMessageTextView = view.findViewById(R.id.warningMessageTextView);

        contentBlocker = DeviceAdminInteractor.getInstance().getContentBlocker();
        if (contentBlocker instanceof ContentBlocker56 || contentBlocker instanceof ContentBlocker57) {
            warningMessageTextView.setVisibility(View.GONE);
        } else {
            warningMessageTextView.setVisibility(View.VISIBLE);
        }

        infoTextView.setVisibility(View.INVISIBLE);
        swipeContainer.setVisibility(View.INVISIBLE);
        turnOnSwitch.setOnClickListener(v -> {
            Log.d(TAG, "Adhell switch button has been clicked");
            new SetFirewallAsyncTask(this, fragmentManager).execute();
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
        if (contentBlocker != null && contentBlocker.isEnabled()) {
            statusTextView.setText(R.string.block_enabled);
            turnOnSwitch.setChecked(true);
        } else {
            statusTextView.setText(R.string.block_disabled);
            turnOnSwitch.setChecked(false);
        }

        if (contentBlocker instanceof ContentBlocker56 || contentBlocker instanceof ContentBlocker57) {
            if (contentBlocker.isEnabled()) {
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
        }
    }

    private static class SetFirewallAsyncTask extends AsyncTask<Void, Void, Void> {
        private FragmentManager fragmentManager;
        private FirewallDialogFragment fragment;
        private BlockerFragment parentFragment;
        private ContentBlocker contentBlocker;
        private Handler handler;

        SetFirewallAsyncTask(BlockerFragment parentFragment, FragmentManager fragmentManager) {
            this.parentFragment = parentFragment;
            this.fragmentManager = fragmentManager;
            this.contentBlocker = DeviceAdminInteractor.getInstance().getContentBlocker();

            this.handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    fragment.appendText(msg.obj.toString());
                }
            };
        }

        @Override
        protected void onPreExecute() {
            if (contentBlocker.isEnabled()) {
                fragment = FirewallDialogFragment.newInstance("Disabling Adhell...");
            } else {
                fragment = FirewallDialogFragment.newInstance("Enabling Adhell...");
            }
            fragment.setCancelable(false);
            fragment.show(fragmentManager, "dialog_firewall");
        }

        @Override
        protected Void doInBackground(Void... args) {
            contentBlocker.setHandler(handler);
            if (contentBlocker.isEnabled()) {
                contentBlocker.disableBlocker();
            } else {
                contentBlocker.enableBlocker();
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
        private Firewall firewall;

        RefreshAsyncTask(Context context) {
            this.contextReference = new WeakReference<>(context);
            this.firewall = AdhellFactory.getInstance().getFirewall();
        }

        @Override
        protected List<ReportBlockedUrl> doInBackground(Void... voids) {
            List<ReportBlockedUrl> reportBlockedUrls = new ArrayList<>();
            List<DomainFilterReport> reports = firewall.getDomainFilterReport(null);
            if (reports == null) {
                return reportBlockedUrls;
            }

            long yesterday = yesterday();
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            appDatabase.reportBlockedUrlDao().deleteBefore(yesterday);

            ReportBlockedUrl lastBlockedUrl = appDatabase.reportBlockedUrlDao().getLastBlockedDomain();
            long lastBlockedTimestamp = 0;
            if (lastBlockedUrl != null) {
                lastBlockedTimestamp = lastBlockedUrl.blockDate / 1000;
            }

            for (DomainFilterReport b : reports) {
                if (b.getTimeStamp() > lastBlockedTimestamp) {
                    ReportBlockedUrl reportBlockedUrl =
                            new ReportBlockedUrl(b.getDomainUrl(), b.getPackageName(), b.getTimeStamp() * 1000);
                    reportBlockedUrls.add(reportBlockedUrl);
                }
            }
            appDatabase.reportBlockedUrlDao().insertAll(reportBlockedUrls);

            return appDatabase.reportBlockedUrlDao().getReportBlockUrlBetween(yesterday(), System.currentTimeMillis());
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
                        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_to_whitelist, listView, false);
                        new AlertDialog.Builder(context)
                            .setView(dialogView)
                            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                                AsyncTask.execute(() -> {
                                    AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
                                    WhiteUrl whiteUrl = new WhiteUrl();
                                    whiteUrl.url = reportBlockedUrls.get(position).url;
                                    whiteUrl.insertedAt = new Date();
                                    appDatabase.whiteUrlDao().insert(whiteUrl);
                                });
                            })
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

        private long yesterday() {
            final Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);
            return cal.getTimeInMillis();
        }
    }
}
