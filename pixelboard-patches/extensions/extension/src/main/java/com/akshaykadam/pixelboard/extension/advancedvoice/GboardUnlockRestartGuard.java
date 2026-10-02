package com.akshaykadam.pixelboard.extension.advancedvoice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.UserManager;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import com.akshaykadam.pixelboard.extension.rambler.GboardRambler1803OfficialSelectionRuntime;

/**
 * Re-initialises the keyboard process when it was started before the user unlocked the device.
 *
 * <p>Gboard is direct-boot aware, so after a reboot with automatic unlock the system can start
 * the keyboard process about a second before the credential-encrypted storage is available.
 * Everything Gboard and the patches decide in that window (dictation extensions, flag values,
 * the selected dictation type) is computed once and kept for the life of the process, and
 * Rambler then never activates until the process is restarted. A process that is started after
 * the unlock is not affected.
 *
 * <p>When the first patched flag read happens while the user is still locked, a receiver for
 * {@link Intent#ACTION_USER_UNLOCKED} is registered. On unlock the cached decisions are dropped
 * and, if Advanced Voice is enabled, the process is killed shortly afterwards; the system binds
 * the input method again, which is what a manual force-stop does. Processes started after the
 * unlock never register the receiver, so the normal path is unchanged.
 */
public final class GboardUnlockRestartGuard {
    static final long RESTART_DELAY_MS = 2000L;

    private static final String TAG = "GboardPatches";
    private static final String LOG_PREFIX = "[unlock-guard] ";
    private static final AtomicBoolean INSTALLED = new AtomicBoolean();
    private static final AtomicBoolean UNLOCK_HANDLED = new AtomicBoolean();
    private static final Runnable KILL_PROCESS = new Runnable() {
        @Override
        public void run() {
            log("restarting the keyboard process now");
            Process.killProcess(Process.myPid());
        }
    };

    private static volatile Runnable restartAction = KILL_PROCESS;
    private static volatile Boolean userUnlockedOverrideForTest;

    private GboardUnlockRestartGuard() {
    }

    /** Cheap enough for hot paths: a single volatile read once installed. */
    public static void ensureInstalled() {
        if (INSTALLED.get()) {
            return;
        }
        try {
            Context context = currentApplication();
            if (context != null) {
                install(context);
            }
        } catch (Throwable failure) {
            log("install failed: " + failure);
        }
    }

    /** False only when the system reports that the current user is still locked. */
    public static boolean isUserUnlocked(Context context) {
        Boolean override = userUnlockedOverrideForTest;
        if (override != null) {
            return override.booleanValue();
        }
        try {
            if (context == null) {
                return true;
            }
            Object service = context.getSystemService(Context.USER_SERVICE);
            return !(service instanceof UserManager) || ((UserManager) service).isUserUnlocked();
        } catch (Throwable ignored) {
            return true;
        }
    }

    static boolean shouldRestart(boolean advancedVoiceEnabled) {
        return advancedVoiceEnabled;
    }

    static void install(Context context) {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }
        if (isUserUnlocked(context)) {
            return;
        }
        final Context application = applicationContext(context);
        log("process started before the user was unlocked; waiting for ACTION_USER_UNLOCKED");
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context receiverContext, Intent intent) {
                try {
                    application.unregisterReceiver(this);
                } catch (Throwable ignored) {
                    // Already unregistered.
                }
                handleUnlocked(application);
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_UNLOCKED);
        if (Build.VERSION.SDK_INT >= 33) {
            application.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            application.registerReceiver(receiver, filter);
        }
        // The unlock may have happened between the check above and the registration.
        if (isUserUnlocked(application)) {
            try {
                application.unregisterReceiver(receiver);
            } catch (Throwable ignored) {
                // Already unregistered.
            }
            handleUnlocked(application);
        }
    }

    private static void handleUnlocked(Context application) {
        if (!UNLOCK_HANDLED.compareAndSet(false, true)) {
            return;
        }
        try {
            GboardRambler1803OfficialSelectionRuntime.invalidateCache();
            GboardAdvancedVoice1803RuntimeSettings.invalidateCachedSnapshot();
            boolean enabled = GboardAdvancedVoice1803RuntimeSettings.isEnabled();
            if (!shouldRestart(enabled)) {
                log("user unlocked; Advanced Voice disabled, keeping the process");
                return;
            }
            log("user unlocked; restarting the keyboard process in " + RESTART_DELAY_MS
                    + " ms to re-initialise dictation");
            new Handler(Looper.getMainLooper()).postDelayed(restartAction, RESTART_DELAY_MS);
        } catch (Throwable failure) {
            log("unlock handling failed: " + failure);
        }
    }

    /** Test hook: forces the answer of {@link #isUserUnlocked}; null restores the real check. */
    public static void setUserUnlockedOverrideForTest(Boolean unlocked) {
        userUnlockedOverrideForTest = unlocked;
    }

    static void setRestartActionForTest(Runnable action) {
        restartAction = action;
    }

    static boolean isUnlockHandledForTest() {
        return UNLOCK_HANDLED.get();
    }

    static void resetForTest() {
        INSTALLED.set(false);
        UNLOCK_HANDLED.set(false);
        restartAction = KILL_PROCESS;
        userUnlockedOverrideForTest = null;
    }

    private static Context applicationContext(Context context) {
        Context application = context.getApplicationContext();
        return application != null ? application : context;
    }

    private static Context currentApplication() {
        try {
            Object application = Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication")
                    .invoke(null);
            return application instanceof Context ? (Context) application : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void log(String message) {
        try {
            Log.i(TAG, LOG_PREFIX + message);
        } catch (Throwable ignored) {
            // Logging must not affect Gboard.
        }
    }
}
