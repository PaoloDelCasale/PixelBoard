package com.akshaykadam.pixelboard.extension.advancedvoice;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public final class GboardLanguageDownloadGuardTest {
    private static final long INTERVAL = GboardLanguageDownloadGuard.MIN_REPEAT_INTERVAL_MS;

    @Before
    public void setUp() {
        GboardLanguageDownloadGuard.resetForTest();
    }

    @After
    public void tearDown() {
        GboardLanguageDownloadGuard.resetForTest();
    }

    @Test
    public void firstRequestIsAllowedAndImmediateRepeatsAreSuppressed() {
        Assert.assertFalse(GboardLanguageDownloadGuard.shouldSkip("en-US", "ELIGIBILITY_CHECKER", 0L));
        for (long now = 1L; now < 1000L; now += 7L) {
            Assert.assertTrue(
                    GboardLanguageDownloadGuard.shouldSkip("en-US", "ELIGIBILITY_CHECKER", now));
        }
    }

    @Test
    public void requestIsAllowedAgainAfterTheBackOffInterval() {
        Assert.assertFalse(GboardLanguageDownloadGuard.shouldSkip("en-US", "src", 100L));
        Assert.assertTrue(
                GboardLanguageDownloadGuard.shouldSkip("en-US", "src", 100L + INTERVAL - 1L));
        Assert.assertFalse(
                GboardLanguageDownloadGuard.shouldSkip("en-US", "src", 100L + INTERVAL));
        // The window restarts from the request that was let through.
        Assert.assertTrue(
                GboardLanguageDownloadGuard.shouldSkip("en-US", "src", 100L + INTERVAL + 1L));
    }

    @Test
    public void languagesAndSourcesAreTrackedIndependently() {
        Assert.assertFalse(GboardLanguageDownloadGuard.shouldSkip("en-US", "A", 0L));
        Assert.assertFalse(GboardLanguageDownloadGuard.shouldSkip("it-IT", "A", 1L));
        Assert.assertFalse(GboardLanguageDownloadGuard.shouldSkip("en-US", "B", 2L));
        Assert.assertTrue(GboardLanguageDownloadGuard.shouldSkip("en-US", "A", 3L));
        Assert.assertTrue(GboardLanguageDownloadGuard.shouldSkip("it-IT", "A", 4L));
        Assert.assertTrue(GboardLanguageDownloadGuard.shouldSkip("en-US", "B", 5L));
    }

    @Test
    public void nullLanguageTagIsNeverSuppressed() {
        Assert.assertFalse(GboardLanguageDownloadGuard.shouldSkip(null, "src", 0L));
        Assert.assertFalse(GboardLanguageDownloadGuard.shouldSkip(null, "src", 1L));
    }

    @Test
    public void nullSourceIsTrackedLikeAnyOtherSource() {
        Assert.assertFalse(GboardLanguageDownloadGuard.shouldSkip("en-US", null, 0L));
        Assert.assertTrue(GboardLanguageDownloadGuard.shouldSkip("en-US", null, 1L));
    }

    @Test
    public void suppressionLoggingIsRateLimitedButNeverSilent() {
        for (int count = 1; count <= 5; count++) {
            Assert.assertTrue(GboardLanguageDownloadGuard.shouldLogSuppression(count));
        }
        for (int count = 6; count < 100; count++) {
            Assert.assertFalse(GboardLanguageDownloadGuard.shouldLogSuppression(count));
        }
        Assert.assertTrue(GboardLanguageDownloadGuard.shouldLogSuppression(100));
        Assert.assertFalse(GboardLanguageDownloadGuard.shouldLogSuppression(101));
        Assert.assertTrue(GboardLanguageDownloadGuard.shouldLogSuppression(1000));
    }
}
