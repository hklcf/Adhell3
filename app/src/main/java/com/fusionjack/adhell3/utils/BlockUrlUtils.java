package com.fusionjack.adhell3.utils;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.webkit.URLUtil;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.db.entity.UserBlockUrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockUrlUtils {

    @NonNull
    public static List<BlockUrl> loadBlockUrls(BlockUrlProvider blockUrlProvider) throws IOException, URISyntaxException {
        BufferedReader bufferedReader;
        if (URLUtil.isFileUrl(blockUrlProvider.url)) {
            File file = new File(new URI(blockUrlProvider.url));
            bufferedReader = new BufferedReader(new FileReader(file));
        } else {
            URL urlProviderUrl = new URL(blockUrlProvider.url);
            URLConnection connection = urlProviderUrl.openConnection();
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        }

        List<BlockUrl> blockUrls = new ArrayList<>();
        String inputLine;
        while ((inputLine = bufferedReader.readLine()) != null) {
            inputLine = getDomain(inputLine).trim().toLowerCase();
            if (BlockUrlPatternsMatch.isUrlValid(inputLine)) {
                BlockUrl blockUrl = new BlockUrl(inputLine, blockUrlProvider.id);
                blockUrls.add(blockUrl);
            }
        }
        bufferedReader.close();
        return blockUrls;
    }

    private static String getDomain(String inputLine) {
        return inputLine
                // Remove 'deadzone' - We only want the domain
                .replace("127.0.0.1", "")
                .replace("0.0.0.0", "")

                // Remove whitespace
                .replaceAll("\\s","")

                // Remove comments
                .replaceAll("(#.*)|((\\s)+#.*)","")

                // Remove WWW, WWW1 etc. prefix
                .replaceAll("^(www)([0-9]{0,3})?(\\.)","");
    }

    public static Set<String> getUniqueBlockedUrls(AppDatabase appDatabase, Handler handler, boolean enableLog) {
        Set<String> denyList = new HashSet<>();

        // Process user-defined blocked URLs
        int userBlockUrlCount = 0;
        List<UserBlockUrl> userBlockUrls = appDatabase.userBlockUrlDao().getAll2();
        for (UserBlockUrl userBlockUrl : userBlockUrls) {
            if (userBlockUrl.url.indexOf('|') == -1) {
                final String url = BlockUrlPatternsMatch.getValidatedUrl(userBlockUrl.url);
                denyList.add(url);
                if (enableLog) {
                    LogUtils.getInstance().writeInfo("UserBlockUrl: " + url, handler);
                }
                userBlockUrlCount++;
            }
        }
        if (enableLog) {
            LogUtils.getInstance().writeInfo("User blocked URL size: " + userBlockUrlCount, handler);
        }

        // Process all blocked URL providers
        List<BlockUrlProvider> blockUrlProviders = appDatabase.blockUrlProviderDao().getBlockUrlProviderBySelectedFlag(1);
        for (BlockUrlProvider blockUrlProvider : blockUrlProviders) {
            List<BlockUrl> blockUrls = appDatabase.blockUrlDao().getUrlsByProviderId(blockUrlProvider.id);
            if (enableLog) {
                LogUtils.getInstance().writeInfo("Included url provider: " + blockUrlProvider.url + ", size: " + blockUrls.size(), handler);
            }

            for (BlockUrl blockUrl : blockUrls) {
                denyList.add(BlockUrlPatternsMatch.getValidatedUrl(blockUrl.url));
            }
        }

        if (enableLog) {
            LogUtils.getInstance().writeInfo("Total unique domains to block: " + denyList.size(), handler);
        }

        return denyList;
    }

    public static List<String> getAllBlockedUrls(AppDatabase appDatabase) {
        List<String> result = new ArrayList<>();
        List<BlockUrlProvider> blockUrlProviders = appDatabase.blockUrlProviderDao().getBlockUrlProviderBySelectedFlag(1);
        for (BlockUrlProvider blockUrlProvider : blockUrlProviders) {
            List<BlockUrl> blockUrls = appDatabase.blockUrlDao().getUrlsByProviderId(blockUrlProvider.id);
            for (BlockUrl blockUrl : blockUrls) {
                result.add(blockUrl.url);
            }
        }
        return result;
    }

    public static List<String> getBlockedUrls(long providerId, AppDatabase appDatabase) {
        List<String> result = new ArrayList<>();
        List<BlockUrl> blockUrls = appDatabase.blockUrlDao().getUrlsByProviderId(providerId);
        for (BlockUrl blockUrl : blockUrls) {
            result.add(blockUrl.url);
        }
        return result;
    }

    public static List<String> getFilteredBlockedUrls(String filterText, AppDatabase appDatabase) {
        List<String> result = new ArrayList<>();
        List<BlockUrlProvider> blockUrlProviders = appDatabase.blockUrlProviderDao().getBlockUrlProviderBySelectedFlag(1);
        for (BlockUrlProvider blockUrlProvider : blockUrlProviders) {
            List<BlockUrl> blockUrls = appDatabase.blockUrlDao().getByUrl(blockUrlProvider.id, filterText);
            for (BlockUrl blockUrl: blockUrls) {
                result.add(blockUrl.url);
            }
        }
        return result;
    }

    public static List<String> getFilteredBlockedUrls(String filterText, long providerId, AppDatabase appDatabase) {
        List<String> result = new ArrayList<>();
        List<BlockUrl> blockUrls = appDatabase.blockUrlDao().getByUrl(providerId, filterText);
        for (BlockUrl blockUrl : blockUrls) {
            result.add(blockUrl.url);
        }
        return result;
    }

}
