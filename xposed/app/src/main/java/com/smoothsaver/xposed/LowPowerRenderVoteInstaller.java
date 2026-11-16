package com.smoothsaver.xposed;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * Clears only the verified 0–60 Hz render vote. The original method still runs,
 * so the brightness callback and the separate modes vote stay in place.
 */
public final class LowPowerRenderVoteInstaller {

    private static final String VOTE_CLASS = "com.android.server.display.mode.Vote";
    private static final String STORAGE_CLASS = "com.android.server.display.mode.VotesStorage";

    private LowPowerRenderVoteInstaller() {
    }

    public static void install(ClassLoader loader, ModulePolicy.LineSink sink) {
        if (!"stallion".equals(android.os.Build.DEVICE)) {
            sink.line("Skipped hook: low-power render vote. Device codename is "
                    + android.os.Build.DEVICE + ". The verified hook is for stallion.");
            sink.line("Installed hooks: none");
            return;
        }
        try {
            Class<?> voteClass = Class.forName(VOTE_CLASS, false, loader);
            Class<?> storageClass = Class.forName(STORAGE_CLASS, false, loader);
            Method priorityToString = voteClass.getDeclaredMethod("priorityToString", int.class);
            if (!Modifier.isStatic(priorityToString.getModifiers())
                    || priorityToString.getReturnType() != String.class) {
                sink.line("Skipped hook: Vote.priorityToString(int) is not a static method returning String.");
                sink.line("Installed hooks: none");
                return;
            }
            priorityToString.setAccessible(true);
            Object priorityName = priorityToString.invoke(null, RenderVoteGate.RENDER_RATE_PRIORITY);
            if (!RenderVoteGate.priorityConfirmed(priorityName)) {
                sink.line("Skipped hook: priority " + RenderVoteGate.RENDER_RATE_PRIORITY
                        + " is named " + priorityName + ".");
                sink.line("Installed hooks: none");
                return;
            }
            Method updateGlobalVote = storageClass.getDeclaredMethod(
                    "updateGlobalVote", int.class, voteClass);
            if (updateGlobalVote.getReturnType() != void.class) {
                sink.line("Skipped hook: VotesStorage.updateGlobalVote does not return void.");
                sink.line("Installed hooks: none");
                return;
            }
            XposedBridge.hookMethod(updateGlobalVote, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        if (param.args != null
                                && param.args.length == 2
                                && RenderVoteGate.shouldClearVote(
                                        param.args[0], Thread.currentThread().getStackTrace())) {
                            param.args[1] = null;
                        }
                    } catch (Throwable ignored) {
                        // Leave the framework arguments unchanged.
                    }
                }
            });
            sink.line("Installed hook: VotesStorage.updateGlobalVote(int, Vote). "
                    + "Clears priority " + RenderVoteGate.RENDER_RATE_PRIORITY
                    + " (" + RenderVoteGate.RENDER_RATE_PRIORITY_NAME + ") "
                    + "only when the caller is " + RenderVoteGate.CALLER_CLASS
                    + "." + RenderVoteGate.CALLER_METHOD + ".");
        } catch (Throwable error) {
            sink.line("Hook installation exception: " + error.getClass().getName()
                    + ": " + error.getMessage());
            sink.line("Installed hooks: none");
        }
    }
}
