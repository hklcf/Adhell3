package com.fusionjack.adhell3.fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.utils.BlockUrlPatternsMatch;
import com.fusionjack.adhell3.viewmodel.UserListViewModel;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class BlacklistFragment extends UserListFragment {
    private ArrayAdapter adapter;
    private UserListViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<String> items = new ArrayList<>();
        adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, items);

        viewModel = ViewModelProviders.of(this, new UserListViewModel.BlackListFactory()).get(UserListViewModel.class);
        viewModel.getItems().observe(this, blackItems -> {
            items.clear();
            items.addAll(blackItems);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blacklist, container, false);

        ListView blacklistView = view.findViewById(R.id.blackListView);
        blacklistView.setAdapter(adapter);
        blacklistView.setOnItemClickListener((parent, view1, position, id) -> {
            String item = (String) parent.getItemAtPosition(position);
            viewModel.removeItem(item, deleteObserver);
        });

        FloatingActionsMenu blackFloatMenu = view.findViewById(R.id.blacklist_actions);
        FloatingActionButton actionAddBlackDomain = view.findViewById(R.id.action_add_domain);
        actionAddBlackDomain.setIcon(R.drawable.ic_public_white_24dp);
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
                            viewModel.addItem(domainToAdd, addObserver);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        });

        FloatingActionButton actionAddFirewallRule = view.findViewById(R.id.action_add_firewall_rule);
        actionAddFirewallRule.setIcon(R.drawable.ic_whatshot_white_24dp);
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
                            viewModel.addItem(ruleToAdd, addObserver);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        });

        return view;
    }
}
