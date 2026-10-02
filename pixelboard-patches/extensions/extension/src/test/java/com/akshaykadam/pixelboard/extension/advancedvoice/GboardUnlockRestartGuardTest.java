package com.akshaykadam.pixelboard.extension.advancedvoice;

import org.junit.Assert;
import org.junit.Test;

public final class GboardUnlockRestartGuardTest {
    @Test
    public void restartsOnlyWhenAdvancedVoiceIsEnabled() {
        Assert.assertTrue(GboardUnlockRestartGuard.shouldRestart(true));
        Assert.assertFalse(GboardUnlockRestartGuard.shouldRestart(false));
    }

    @Test
    public void restartDelayLeavesTimeForTheUnlockToSettle() {
        Assert.assertTrue(GboardUnlockRestartGuard.RESTART_DELAY_MS >= 1000L);
        Assert.assertTrue(GboardUnlockRestartGuard.RESTART_DELAY_MS <= 10000L);
    }

    @Test
    public void nullContextIsTreatedAsUnlockedAndDoesNotThrow() {
        Assert.assertTrue(GboardUnlockRestartGuard.isUserUnlocked(null));
    }

    @Test
    public void ensureInstalledWithoutAnApplicationIsANoOp() {
        // No android.app.ActivityThread on the JVM: must neither throw nor mark itself installed.
        GboardUnlockRestartGuard.ensureInstalled();
        GboardUnlockRestartGuard.ensureInstalled();
    }
}
