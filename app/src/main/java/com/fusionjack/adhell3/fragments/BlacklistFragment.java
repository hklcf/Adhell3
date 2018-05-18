package com.fusionjack.adhell3.fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.entity.UserBlockUrl;
import com.fusionjack.adhell3.tasks.AddUrlAsyncTask;
import com.fusionjack.adhell3.tasks.RefreshListAsyncTask;
import com.fusionjack.adhell3.tasks.RemoveUrlAsyncTask;
import com.fusionjack.adhell3.utils.BlockUrlPatternsMatch;
import com.fusionjack.adhell3.viewmodel.BlackUrlViewModel;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class BlacklistFragment extends Fragment {
    private static final String ARG_PAGE = "page";
    private int page;
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            this.page = bundle.getInt(ARG_PAGE);
        }
        this.context = getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blacklist, container, false);

        SwipeRefreshLayout blacklistSwipeContainer = view.findViewById(R.id.blacklistSwipeContainer);
        blacklistSwipeContainer.setOnRefreshListener(() ->
                new RefreshListAsyncTask(page, context).execute()
        );

        ListView blacklistView = view.findViewById(R.id.blackListView);
        blacklistView.setOnItemClickListener((parent, view1, position, id) -> {
            String url = (String) parent.getItemAtPosition(position);
            new RemoveUrlAsyncTask(url, page, context).execute();
        });

        BlackUrlViewModel blackViewModel = ViewModelProviders.of(getActivity()).get(BlackUrlViewModel.class);
        blackViewModel.getBlockUrls().observe(this, blackUrls -> {
            if (blackUrls == null) {
                return;
            }
            ListAdapter adapter = blacklistView.getAdapter();
            if (adapter == null) {
                List<String> urls = new ArrayList<>();
                for (UserBlockUrl blockUrl : blackUrls) {
                    urls.add(blockUrl.url);
                }
                ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, urls);
                blacklistView.setAdapter(itemsAdapter);
            }
        });

        FloatingActionsMenu blackFloatMenu = view.findViewById(R.id.blacklist_actions);
        FloatingActionButton actionAddBlackDomain = view.findViewById(R.id.action_add_domain);
        actionAddBlackDomain.setIcon(R.drawable.ic_public_black_24dp);
        actionAddBlackDomain.setOnClickListener(v -> {
            blackFloatMenu.collapse();
            View dialogView = inflater.inflate(R.layout.dialog_blacklist_domain, container, false);
            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        EditText domainEditText = dialogView.findViewById(R.id.domainEditText);
                        String domainToAdd = domainEditText.getText().toString().trim().toLowerCase();
                        if (!BlockUrlPatternsMatch.isUrlValid(domainToAdd)) {
                            Toast.makeText(context, "Url not valid. Please check", Toast.LENGTH_SHORT).show();
                        } else {
                            new AddUrlAsyncTask(domainToAdd, page, context).execute();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        });

        FloatingActionButton actionAddFirewallRule = view.findViewById(R.id.action_add_firewall_rule);
        actionAddFirewallRule.setIcon(R.drawable.ic_whatshot_black_24dp);
        actionAddFirewallRule.setOnClickListener(v -> {
            blackFloatMenu.collapse();
            View dialogView = inflater.inflate(R.layout.dialog_blacklist_rule, container, false);
            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        EditText ruleEditText = dialogView.findViewById(R.id.ruleEditText);
                        String ruleToAdd = ruleEditText.getText().toString().trim().toLowerCase();
                        StringTokenizer tokens = new StringTokenizer(ruleToAdd, "|");
                        if (tokens.countTokens() != 3) {
                            Toast.makeText(context, "Rule not valid. Please check", Toast.LENGTH_SHORT).show();
                        } else {
                            new AddUrlAsyncTask(ruleToAdd, page, context).execute();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        });

        return view;
    }
}
