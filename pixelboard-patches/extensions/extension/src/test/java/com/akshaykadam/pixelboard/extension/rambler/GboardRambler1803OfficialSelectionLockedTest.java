package com.akshaykadam.pixelboard.extension.rambler;

import android.content.Context;
import android.os.UserManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowUserManager;

@RunWith(RobolectricTestRunner.class)
public final class GboardRambler1803OfficialSelectionLockedTest {
    @After
    public void tearDown() {
        GboardRambler1803OfficialSelectionRuntime.resetForTests();
    }

    @Test
    public void selectionIsNotCachedWhileTheUserIsLockedAndIsReadAgainAfterTheUnlock() {
        Context application = RuntimeEnvironment.getApplication();
        ShadowUserManager users =
                Shadows.shadowOf((UserManager) application.getSystemService(Context.USER_SERVICE));
        GboardRambler1803OfficialSelectionRuntime.resetForTests();

        users.setUserUnlocked(false);
        Assert.assertFalse(
                GboardRambler1803OfficialSelectionRuntime.shouldEnableAgenticDictation());

        users.setUserUnlocked(true);
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
