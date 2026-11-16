package com.smoothsaver.xposed;

import android.os.Build;
import android.util.Log;

import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LSPosed entry for system_server.
 *
 * 1.1.0 hooked observer callbacks, vote setters, and Settings.Global.getInt.
 * Those hooks run in system_server and crash Settings search. This build
 * clears one verified render-rate vote and leaves every other call unchanged.
 */
public class SmoothSaverLegacy implements IXposedHookLoadPackage {

    static final String TAG = "SmoothSaver";

    private static final List<String> FRAMEWORK_CLASSES = List.of(
            "com.android.server.display.mode.DisplayModeDirector",
            "com.android.server.display.DisplayModeDirector",
            "com.android.server.display.mode.VotesStorage",
            "com.android.server.display.mode.Vote"
    );

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null) {
            return;
        }
        if (!"android".equals(lpparam.packageName)) {
            if ("com.android.settings".equals(lpparam.packageName)
                    || "com.android.settings.intelligence".equals(lpparam.packageName)) {
                log("Skipped " + lpparam.packageName
                        + ". Remove Settings from the Vector scope. The only target is System Framework.");
            }
            return;
        }

        try {
            log("SmoothSaver " + BuildConfig.VERSION_NAME + " loaded in system framework");
            log("Android version: " + Build.VERSION.RELEASE);
            log("SDK version: " + Build.VERSION.SDK_INT);
            log("Build fingerprint: " + Build.FINGERPRINT);
            log("Device codename: " + Build.DEVICE);
            log("Product: " + Build.PRODUCT);
            log("Model: " + Build.MODEL);
            log("SoC: " + Build.SOC_MANUFACTURER + " " + Build.SOC_MODEL);
            log("Codename is stallion: " + "stallion".equals(Build.DEVICE));
            FrameworkProbe.inspect(lpparam.classLoader, FRAMEWORK_CLASSES, SmoothSaverLegacy::log);
            ModulePolicy.logSkippedHooks(SmoothSaverLegacy::log);
            LowPowerRenderVoteInstaller.install(lpparam.classLoader, SmoothSaverLegacy::log);
        } catch (Throwable error) {
            log("Hook installation exception: " + error.getClass().getName() + ": " + error.getMessage());
            log("Installed hooks: none");
        }
    }

    private static void log(String message) {
        Log.i(TAG, message);
        try {
            XposedBridge.log("[" + TAG + "] " + message);
        } catch (Throwable ignored) {
            // Logcat already has the line. A logging failure must not escape into system_server.
        }
    }
}
