package com.fusionjack.adhell3.utils;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.webkit.URLUtil;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;

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
import java.util.List;

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

        // Create a new StringBuilder object to hold our host file
        StringBuilder hostFile = new StringBuilder();
        String inputLine;

        // Add all lines to the StringBuilder
        while ((inputLine = bufferedReader.readLine()) != null) {
            hostFile.append(getDomain(inputLine.trim().toLowerCase()));
            hostFile.append("\n");
        }
        bufferedReader.close();

        // Convert host file to string
        String hostFileStr = hostFile.toString();

        // If we received any host file data
        if (!hostFileStr.isEmpty()) {
            // Fetch valid domains
            String[] validated_hosts = BlockUrlPatternsMatch.getValidHostFileDomains(hostFileStr).split("\n");

            // Add each domain to blockUrls
            for (String validatedDomain : validated_hosts) {
                BlockUrl blockUrl = new BlockUrl(validatedDomain, blockUrlProvider.id);
                blockUrls.add(blockUrl);
            }
        }

        return blockUrls;
    }

    private static String getDomain(String inputLine) {
        return inputLine
                // Remove 'deadzone' - We only want the domain
                .replace("127.0.0.1", "")
                .replace("0.0.0.0", "")
                // Remove comments
                .replaceAll("\\s*(?:#.*)$","")
                // Remove whitespace
                .replaceAll("\\s+","")
                // Remove WWW
                .replaceAll("^www(?:[0-9]{1,3})?(?:\\.)", "");
    }

    public static List<String> getUserBlockedUrls(AppDatabase appDatabase, boolean enableLog, Handler handler) {
        List<String> list = new ArrayList<>();
        int userBlockUrlCount = 0;
        List<String> urls = appDatabase.userBlockUrlDao().getAll3();
        for (String url : urls) {
            if (url.indexOf('|') == -1) {
                list.add(url);
                if (enableLog) {
                    LogUtils.getInstance().writeInfo("Domain: " + url, handler);
                }
                userBlockUrlCount++;
            }
        }
        if (enableLog) {
            LogUtils.getInstance().writeInfo("Size: " + userBlockUrlCount, handler);
        }
        return list;
    }

    public static int getAllBlockedUrlsCount(AppDatabase appDatabase) {
        return appDatabase.blockUrlProviderDao().getUniqueBlockedUrlsCount();
    }

    public static List<String> getAllBlockedUrls(AppDatabase appDatabase) {
        return appDatabase.blockUrlProviderDao().getUniqueBlockedUrls();
    }

    public static List<String> getBlockedUrls(long providerId, AppDatabase appDatabase) {
        return appDatabase.blockUrlDao().getUrlsByProviderId(providerId);
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

    public static boolean isDomainLimitAboveDefault() {
        int defaultDomainLimit = 15000;
        int domainLimit = AdhellAppIntegrity.BLOCK_URL_LIMIT;
        return domainLimit > defaultDomainLimit;
    }

}