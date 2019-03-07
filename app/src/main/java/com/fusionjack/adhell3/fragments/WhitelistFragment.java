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

public class WhitelistFragment extends UserListFragment {
    private ArrayAdapter adapter;
    private UserListViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<String> items = new ArrayList<>();
        adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, items);
        viewModel = ViewModelProviders.of(this, new UserListViewModel.WhiteListFactory()).get(UserListViewModel.class);;
        viewModel.getItems().observe(this, whiteItems -> {
            items.clear();
            items.addAll(whiteItems);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_whitelist, container, false);

        ListView whiteListView = view.findViewById(R.id.whiteListView);
        whiteListView.setAdapter(adapter);
        whiteListView.setOnItemClickListener((parent, view1, position, id) -> {
            String item = (String) parent.getItemAtPosition(position);
            viewModel.removeItem(item, deleteObserver);
        });

        FloatingActionsMenu whiteFloatMenu = view.findViewById(R.id.whitelist_actions);
        FloatingActionButton actionAddWhiteDomain = view.findViewById(R.id.action_add_domain);
        actionAddWhiteDomain.setIcon(R.drawable.ic_public_white_24dp);
        actionAddWhiteDomain.setOnClickListener(v -> {
            whiteFloatMenu.collapse();
            View dialogView = inflater.inflate(R.layout.dialog_whitelist_domain, container, false);
            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        EditText domainEditText = dialogView.findViewById(R.id.domainEditText);
                        String domainToAdd = domainEditText.getText().toString().trim().toLowerCase();
                        if (domainToAdd.indexOf('|') == -1) {
                            if (!BlockUrlPatternsMatch.isUrlValid(domainToAdd)) {
                                Toast.makeText(this.getContext(), "Url not valid. Please check", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } else {
                            // packageName|url
                            StringTokenizer tokens = new StringTokenizer(domainToAdd, "|");
                            if (tokens.countTokens() != 2) {
                                Toast.makeText(this.getContext(), "Rule not valid. Please check", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        viewModel.addItem(domainToAdd, addObserver);
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        });
        return view;
    }
}
