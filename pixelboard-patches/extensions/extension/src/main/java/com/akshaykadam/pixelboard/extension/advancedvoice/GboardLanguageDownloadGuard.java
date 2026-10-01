package com.akshaykadam.pixelboard.extension.advancedvoice;

import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
    private static final String TAG = "GboardPatches";
    private static final int MAX_ALLOWED_LOGS = 20;

    private static final ConcurrentHashMap<String, Long> LAST_ALLOWED_MS =
            new ConcurrentHashMap<String, Long>();

    // Dedicated counters: the runtime's shared info-log budget is exhausted at start-up by the
    // "forced <flag>" lines, so decisions of this guard must not depend on it.
    private static final AtomicInteger ALLOWED_COUNT = new AtomicInteger();
    private static final AtomicInteger SUPPRESSED_COUNT = new AtomicInteger();

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
        Long now = Long.valueOf(nowMs);
        // Atomic: of several concurrent identical requests exactly one is let through.
        Long previous = LAST_ALLOWED_MS.putIfAbsent(key, now);
        if (previous == null) {
            return false;
        }
        if (nowMs - previous.longValue() < MIN_REPEAT_INTERVAL_MS) {
            return true;
        }
        // The window has elapsed: the thread that wins the swap opens the next window.
        return !LAST_ALLOWED_MS.replace(key, previous, now);
    }

    /** Logs the first few decisions, then every 100th suppression. Never throws. */
    public static void logDecision(String languageTag, Object source, boolean skipped) {
        try {
            String prefix;
            int count;
            if (skipped) {
                count = SUPPRESSED_COUNT.incrementAndGet();
                if (!shouldLogSuppression(count)) {
                    return;
                }
                prefix = "suppressed repeated language download for ";
            } else {
                count = ALLOWED_COUNT.incrementAndGet();
                if (count > MAX_ALLOWED_LOGS) {
                    return;
                }
                prefix = "allowed language download for ";
            }
            Log.i(TAG, prefix + languageTag + " from " + source + " (#" + count + ")");
        } catch (Throwable ignored) {
            // Logging must not affect Gboard.
        }
    }

    static boolean shouldLogSuppression(int count) {
        return count <= 5 || count % 100 == 0;
    }

    static void resetForTest() {
        LAST_ALLOWED_MS.clear();
    }
}
