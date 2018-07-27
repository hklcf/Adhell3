package com.fusionjack.adhell3.utils;

import android.util.Log;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.db.entity.BlockUrl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BlockUrlPatternsMatch {
    private static final String TAG = BlockUrlPatternsMatch.class.getCanonicalName();

    private static final String WILDCARD_PATTERN = "(?im)^(?=\\*|.+\\*$)(?:\\*[.-]?)?[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)*(?:[.-]?\\*)?$";
    private static final Pattern wildcard_r = Pattern.compile(WILDCARD_PATTERN);

    private static final String DOMAIN_PATTERN = "(?im)^(?=.{4,253}$)(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z]{2,}$";
    private static final Pattern domain_r = Pattern.compile(DOMAIN_PATTERN);

    // Knox URL - Must contain a letter in prefix / domain
    private static final String KNOX_VALID_PATTERN = "(?i)^(?=.*[a-z]).*$";
    private static final Pattern knox_valid_r = Pattern.compile(KNOX_VALID_PATTERN);

    private static String domainPrefix = BuildConfig.DOMAIN_PREFIX.trim();
    private static final String WILDCARD_PREFIX = "*";

    private BlockUrlPatternsMatch() {
    }

    private static boolean wildcardValid (String domain) {
        return wildcard_r.matcher(domain).matches();
    }

    private static boolean domainValid (String domain){
        return domain_r.matcher(domain).matches();
    }

    public static List<BlockUrl> validHostFileDomains(String hostFileStr, long providerId) {
        Date start = new Date();

        List<BlockUrl> blockUrls = new ArrayList<>();

        final Matcher domainPatternMatch = domain_r.matcher(hostFileStr);
        final Matcher wildcardPatternMatch = wildcard_r.matcher(hostFileStr);

        // Standard domains
        while (domainPatternMatch.find()) {
            String standardDomain = domainPatternMatch.group();
            processPrefixingOptions(standardDomain, blockUrls, providerId);
        }
        // Wildcards
        while (wildcardPatternMatch.find()) {
            String wildcard = wildcardPatternMatch.group();
            blockUrls.add(new BlockUrl(wildcard, providerId));
        }

        Date end = new Date();
        Log.i(TAG, "Domain validation duration: " + (end.getTime() - start.getTime()) + " ms");

        return blockUrls;
    }

    public static boolean isUrlValid(String url) {
        if (url.contains("*")) {
            return BlockUrlPatternsMatch.wildcardValid(url);
        }
        return BlockUrlPatternsMatch.domainValid(url);
    }

    private static void processPrefixingOptions(String url, List<BlockUrl> blockUrls, long providerId) {
        switch (domainPrefix) {
            case "*":
                blockUrls.add(new BlockUrl(conditionallyPrefix(url), providerId));
                break;
            case "*.":
                blockUrls.add(new BlockUrl(getValidKnoxUrl(url), providerId));
                blockUrls.add(new BlockUrl(conditionallyPrefix(url), providerId));
                break;
            case "":
                blockUrls.add(new BlockUrl(getValidKnoxUrl(url), providerId));
                break;
            default:
                break;
        }
    }

    private static String conditionallyPrefix(String url) {
        return (url.startsWith(domainPrefix) ? "" : domainPrefix) + url;
    }

    public static String getValidKnoxUrl(String url) {
        // Knox seems invalidate a domain if the prefix does not contain any letters.
        // We will programmatically prefix domains such as 123.test.com, but not t123.test.com

        // If the url is a wildcard, return it as is.
        if (url.contains(WILDCARD_PREFIX)) {
            return url;
        }

        // Grab the prefix
        String prefix = url.split("\\Q.\\E")[0];
        // Regex: must contain a letter (excl wildcards)
        final Matcher prefix_valid = knox_valid_r.matcher(prefix);

        // If we don't have any letters in the prefix
        // Add a wildcard prefix as a safety net
        return (prefix_valid.matches() ? "" : WILDCARD_PREFIX) + url;
    }

}