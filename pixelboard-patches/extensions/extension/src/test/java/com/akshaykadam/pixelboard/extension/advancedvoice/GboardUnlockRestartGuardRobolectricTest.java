package com.akshaykadam.pixelboard.extension.advancedvoice;

import android.content.Context;
import android.content.Intent;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public final class GboardUnlockRestartGuardRobolectricTest {
    private Context application;
    private AtomicInteger restarts;

    @Before
    public void setUp() {
        GboardUnlockRestartGuard.resetForTest();
        GboardAdvancedVoice1803RuntimeSettings.clearEnabledOverrideForTest();
        application = RuntimeEnvironment.getApplication();
        restarts = new AtomicInteger();
        GboardUnlockRestartGuard.setRestartActionForTest(new Runnable() {
            @Override
            public void run() {
                restarts.incrementAndGet();
            }
        });
    }

    @After
    public void tearDown() {
        GboardUnlockRestartGuard.resetForTest();
        GboardAdvancedVoice1803RuntimeSettings.clearEnabledOverrideForTest();
    }

    @Test
    public void realCheckReportsAnUnlockedUserOnTheTestDevice() {
        Assert.assertTrue(GboardUnlockRestartGuard.isUserUnlocked(application));
    }

    @Test
    public void overrideControlsTheReportedUnlockState() {
        GboardUnlockRestartGuard.setUserUnlockedOverrideForTest(Boolean.FALSE);
        Assert.assertFalse(GboardUnlockRestartGuard.isUserUnlocked(application));
        GboardUnlockRestartGuard.setUserUnlockedOverrideForTest(Boolean.TRUE);
        Assert.assertTrue(GboardUnlockRestartGuard.isUserUnlocked(application));
        GboardUnlockRestartGuard.setUserUnlockedOverrideForTest(null);
        Assert.assertTrue(GboardUnlockRestartGuard.isUserUnlocked(application));
    }

    @Test
    public void processStartedAfterTheUnlockIsNeverRestarted() {
        GboardUnlockRestartGuard.setUserUnlockedOverrideForTest(Boolean.TRUE);

        GboardUnlockRestartGuard.install(application);
        application.sendBroadcast(new Intent(Intent.ACTION_USER_UNLOCKED));
        ShadowLooper.idleMainLooper();
        ShadowLooper.idleMainLooper(GboardUnlockRestartGuard.RESTART_DELAY_MS, TimeUnit.MILLISECONDS);

        Assert.assertFalse(GboardUnlockRestartGuard.isUnlockHandledForTest());
        Assert.assertEquals(0, restarts.get());
    }

    @Test
    public void processStartedBeforeTheUnlockRestartsOnceAfterTheUnlock() {
        GboardUnlockRestartGuard.setUserUnlockedOverrideForTest(Boolean.FALSE);
        GboardUnlockRestartGuard.install(application);

        GboardUnlockRestartGuard.setUserUnlockedOverrideForTest(Boolean.TRUE);
        application.sendBroadcast(new Intent(Intent.ACTION_USER_UNLOCKED));
        ShadowLooper.idleMainLooper();

        Assert.assertTrue(GboardUnlockRestartGuard.isUnlockHandledForTest());
        Assert.assertEquals("the restart is delayed", 0, restarts.get());

        ShadowLooper.idleMainLooper(GboardUnlockRestartGuard.RESTART_DELAY_MS, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, restarts.get());

        // A second unlock broadcast, or a second install, must not schedule another restart.
        application.sendBroadcast(new Intent(Intent.ACTION_USER_UNLOCKED));
        GboardUnlockRestartGuard.install(application);
        ShadowLooper.idleMainLooper(GboardUnlockRestartGuard.RESTART_DELAY_MS, TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, restarts.get());
    }

    @Test
    public void processStartedBeforeTheUnlockKeepsRunningWhenAdvancedVoiceIsDisabled() {
        GboardAdvancedVoice1803RuntimeSettings.setEnabledOverrideForTest(false);
        GboardUnlockRestartGuard.setUserUnlockedOverrideForTest(Boolean.FALSE);
        GboardUnlockRestartGuard.install(application);

        GboardUnlockRestartGuard.setUserUnlockedOverrideForTest(Boolean.TRUE);
        application.sendBroadcast(new Intent(Intent.ACTION_USER_UNLOCKED));
        ShadowLooper.idleMainLooper();
        ShadowLooper.idleMainLooper(GboardUnlockRestartGuard.RESTART_DELAY_MS, TimeUnit.MILLISECONDS);

        Assert.assertTrue(GboardUnlockRestartGuard.isUnlockHandledForTest());
        Assert.assertEquals(0, restarts.get());
    }
}
