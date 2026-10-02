package com.akshaykadam.pixelboard.extension.rambler;

import com.akshaykadam.pixelboard.extension.advancedvoice.GboardUnlockRestartGuard;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class GboardRambler1803OfficialSelectionLockedTest {
    @After
    public void tearDown() {
        GboardRambler1803OfficialSelectionRuntime.resetForTests();
        GboardUnlockRestartGuard.setUserUnlockedOverrideForTest(null);
    }

    @Test
    public void selectionIsNotCachedWhileTheUserIsLockedAndIsReadAgainAfterTheUnlock() {
        GboardRambler1803OfficialSelectionRuntime.resetForTests();

        GboardUnlockRestartGuard.setUserUnlockedOverrideForTest(Boolean.FALSE);
        Assert.assertFalse(
                GboardRambler1803OfficialSelectionRuntime.shouldEnableAgenticDictation());

        GboardUnlockRestartGuard.setUserUnlockedOverrideForTest(Boolean.TRUE);
        Assert.assertTrue(
                "the locked answer must not have been cached",
                GboardRambler1803OfficialSelectionRuntime.shouldEnableAgenticDictation());
    }

    @Test
    public void invalidateCacheDropsAnExplicitlyCachedSelection() {
        GboardRambler1803OfficialSelectionRuntime.updateOfficialSelection(false);
        Assert.assertFalse(
                GboardRambler1803OfficialSelectionRuntime.shouldEnableAgenticDictation());

        GboardRambler1803OfficialSelectionRuntime.invalidateCache();
        Assert.assertTrue(
                "an unset selection is read from the (default) preferences again",
                GboardRambler1803OfficialSelectionRuntime.shouldEnableAgenticDictation());
    }
}
