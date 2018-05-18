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
import com.fusionjack.adhell3.db.entity.WhiteUrl;
import com.fusionjack.adhell3.tasks.AddUrlAsyncTask;
import com.fusionjack.adhell3.tasks.RefreshListAsyncTask;
import com.fusionjack.adhell3.tasks.RemoveUrlAsyncTask;
import com.fusionjack.adhell3.utils.BlockUrlPatternsMatch;
import com.fusionjack.adhell3.viewmodel.WhiteUrlViewModel;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class WhitelistFragment extends Fragment {
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
        View view = inflater.inflate(R.layout.fragment_whitelist, container, false);

        SwipeRefreshLayout whitelistSwipeContainer = view.findViewById(R.id.whitelistSwipeContainer);
        whitelistSwipeContainer.setOnRefreshListener(() ->
                new RefreshListAsyncTask(page, context).execute()
        );

        ListView whiteListView = view.findViewById(R.id.whiteListView);
        whiteListView.setOnItemClickListener((parent, view1, position, id) -> {
            String url = (String) parent.getItemAtPosition(position);
            new RemoveUrlAsyncTask(url, page, context).execute();
        });

        WhiteUrlViewModel model = ViewModelProviders.of(getActivity()).get(WhiteUrlViewModel.class);
        model.getWhiteUrls().observe(this, whiteUrls -> {
            if (whiteUrls == null) {
                return;
            }
            ListAdapter adapter = whiteListView.getAdapter();
            if (adapter == null) {
                List<String> urls = new ArrayList<>();
                for (WhiteUrl whiteUrl : whiteUrls) {
                    urls.add(whiteUrl.url);
                }
                ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, urls);
                whiteListView.setAdapter(itemsAdapter);
            }
        });

        FloatingActionsMenu whiteFloatMenu = view.findViewById(R.id.whitelist_actions);
        FloatingActionButton actionAddWhiteDomain = view.findViewById(R.id.action_add_domain);
        actionAddWhiteDomain.setIcon(R.drawable.ic_public_black_24dp);
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
                        new AddUrlAsyncTask(domainToAdd, page, context).execute();
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        });
        return view;
    }
}
