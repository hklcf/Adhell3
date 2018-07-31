package com.fusionjack.adhell3.utils;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BlockUrlPatternsMatch {

    private static final String WILDCARD_PATTERN = "(?im)^(?=\\*|.+\\*$)(?:\\*[.-]?)?[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)*(?:[.-]?\\*)?$";
    private static final Pattern wildcard_r = Pattern.compile(WILDCARD_PATTERN);

    private static final String DOMAIN_PATTERN = "(?im)^(?=.{4,253}$)(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z]{2,}$";
    private static final Pattern domain_r = Pattern.compile(DOMAIN_PATTERN);

    private static final String FILTER_PATTERN = "(?im)^\\|{2}((?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z]{2,})\\^(?:\\$(?:[a-z]+,)?third-party)?$";
    private static final Pattern filter_r = Pattern.compile(FILTER_PATTERN);

    // Knox URL - Must contain a letter in prefix / domain
    private static final String KNOX_VALID_PATTERN = "(?i)^(?=.*[a-z]).*$";
    private static final Pattern knox_valid_r = Pattern.compile(KNOX_VALID_PATTERN);

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
        // Set for unique domains
        Set<String> uniqueBlockUrls = new HashSet<>();
        // BlockUrl list to return
        List<BlockUrl> blockUrls = new ArrayList<>();

        final Matcher filterPatternMatch = filter_r.matcher(hostFileStr);
        final Matcher domainPatternMatch = domain_r.matcher(hostFileStr);
        final Matcher wildcardPatternMatch = wildcard_r.matcher(hostFileStr);

        // Filter domains - something.com and *.something.com
        while (filterPatternMatch.find()) {
            String filterDomain = getValidKnoxUrl(filterPatternMatch.group(1));
            // Add something.com
            uniqueBlockUrls.add(filterDomain);
            // Conditionally add *.something.com
            if (!filterDomain.startsWith(WILDCARD_PREFIX)) {
                uniqueBlockUrls.add("*." + filterDomain);
            }
        }

        // Standard domains (conditionally prefix)
        while (domainPatternMatch.find()) {
            String standardDomain = getValidKnoxUrl(domainPatternMatch.group());
            String prefix = BuildConfig.DOMAIN_PREFIX ? (standardDomain.startsWith(WILDCARD_PREFIX) ? "" : WILDCARD_PREFIX) : "";
            uniqueBlockUrls.add(prefix + standardDomain);
        }

        // Wildcards
        while (wildcardPatternMatch.find()) {
            String wildcard = wildcardPatternMatch.group();
            uniqueBlockUrls.add(wildcard);
        }

        // Add unique urls to block urls
        for (String url : uniqueBlockUrls) {
            blockUrls.add(new BlockUrl(url, providerId));
        }

        return blockUrls;
    }

    public static boolean isUrlValid(String url) {
        if (url.contains(WILDCARD_PREFIX)) {
            return BlockUrlPatternsMatch.wildcardValid(url);
        }
        return BlockUrlPatternsMatch.domainValid(url);
    }

    public static String getValidKnoxUrl(String url) {
        // Knox seems invalidate a domain if the prefix does not contain any letters.
        // We will programmatically prefix domains such as 123.test.com, but not t123.test.com

        // If the url is a wildcard, return it as is.
        if (url.contains(WILDCARD_PREFIX)) {
            return url;
        }

        // Grab the prefix
        String prefix = url.split("\\.")[0];
        // Regex: must contain a letter (excl wildcards)
        final Matcher prefix_valid = knox_valid_r.matcher(prefix);

        // If we don't have any letters in the prefix
        // Add a wildcard prefix as a safety net
        return (prefix_valid.matches() ? "" : WILDCARD_PREFIX) + url;
    }

}