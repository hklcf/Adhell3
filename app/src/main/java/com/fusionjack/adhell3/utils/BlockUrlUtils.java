package com.fusionjack.adhell3.utils;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.webkit.URLUtil;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class BlockUrlUtils {

    private static final String TAG = BlockUrlUtils.class.getCanonicalName();

    // Pattern to detect lines that do not start with a word or wildcard
    private static final Pattern linePattern = Pattern.compile("(?im)^(?![a-z0-9*]|\\|{2}).+$");

    // Pattern to detect 'deadzone' - We only want the domain
    private static final Pattern deadZonePattern = Pattern.compile("(?im)^(?:0|127)\\.0\\.0\\.[0-1]\\s+");

    // Pattern to detect comments
    private static final Pattern commentPattern = Pattern.compile("(?im)(?:^|[^\\S\\n]+)#.*$");

    // Pattern to detect empty lines
    private static final Pattern emptyLinePattern = Pattern.compile("(?im)^\\s*");

    @NonNull
    public static List<BlockUrl> loadBlockUrls(BlockUrlProvider blockUrlProvider) throws IOException, URISyntaxException {
        Date start = new Date();

        // Read the host source and convert it to string
        String hostFileStr = "";
        if (URLUtil.isFileUrl(blockUrlProvider.url)) {
            File file = new File(new URI(blockUrlProvider.url));
            hostFileStr = Files.asCharSource(file, Charsets.UTF_8).read();
        } else {
            URL urlProviderUrl = new URL(blockUrlProvider.url);
            URLConnection connection = urlProviderUrl.openConnection();
            try (final Reader reader = new InputStreamReader(connection.getInputStream(), Charsets.UTF_8)) {
                hostFileStr = CharStreams.toString(reader);
            }
        }

        // If we received any host file data
        if (!hostFileStr.isEmpty()) {
            // Clean up the host string
            hostFileStr = linePattern.matcher(hostFileStr).replaceAll("");
            hostFileStr = deadZonePattern.matcher(hostFileStr).replaceAll("");
            hostFileStr = commentPattern.matcher(hostFileStr).replaceAll("");
            hostFileStr = emptyLinePattern.matcher(hostFileStr).replaceAll("");
            hostFileStr = hostFileStr.toLowerCase();

            // Fetch valid domains
            List<BlockUrl> blockUrls = BlockUrlPatternsMatch.validHostFileDomains(hostFileStr, blockUrlProvider.id);

            Date end = new Date();
            Log.i(TAG, "Domain processing duration: " + (end.getTime() - start.getTime()) + " ms");

            return blockUrls;
        }

        return new ArrayList<>();
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