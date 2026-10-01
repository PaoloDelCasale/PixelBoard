package com.akshaykadam.pixelboard.extension.advancedvoice;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Back-off for Gboard's language pack download requests.
 *
 * <p>The eligibility checker requests a language pack download every time a locale reports
 * "language pack not installed". On PixelBoard the on-device speech service refuses silent
 * downloads (the renamed package is not in its allowlist) but still completes the response
 * stream, so the download is reported as a success without installing anything. That
 * completion re-runs the eligibility check, which requests the same download again, and the
 * cycle repeats every few milliseconds for as long as an interaction is open.
 *
 * <p>The first request for a (language, source) pair is always let through; identical repeats
 * inside {@link #MIN_REPEAT_INTERVAL_MS} are suppressed.
 */
public final class GboardLanguageDownloadGuard {
    static final long MIN_REPEAT_INTERVAL_MS = 10L * 60L * 1000L;

    private static final ConcurrentHashMap<String, Long> LAST_ALLOWED_MS =
            new ConcurrentHashMap<String, Long>();

    private GboardLanguageDownloadGuard() {
    }

    /** Returns true when the download request must be dropped. */
    public static boolean shouldSkip(String languageTag, Object source) {
        return shouldSkip(languageTag, source, System.nanoTime() / 1000000L);
    }

    static boolean shouldSkip(String languageTag, Object source, long nowMs) {
        if (languageTag == null) {
            return false;
        }
        String key = languageTag + '|' + source;
        Long previous = LAST_ALLOWED_MS.get(key);
        if (previous != null && nowMs - previous.longValue() < MIN_REPEAT_INTERVAL_MS) {
            return true;
        }
        LAST_ALLOWED_MS.put(key, Long.valueOf(nowMs));
        return false;
    }

    static void resetForTest() {
        LAST_ALLOWED_MS.clear();
    }
}
