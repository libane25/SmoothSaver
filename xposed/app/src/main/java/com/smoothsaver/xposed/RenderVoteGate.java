package com.smoothsaver.xposed;

/**
 * Decision for the stallion CP3A.261005.005 render-rate vote.
 * The values were read from that build's services.jar, and priorityToString
 * must still confirm them before a hook is installed.
 */
public final class RenderVoteGate {

    public static final int RENDER_RATE_PRIORITY = 20;
    public static final String RENDER_RATE_PRIORITY_NAME = "PRIORITY_LOW_POWER_MODE_RENDER_RATE";
    public static final String CALLER_CLASS =
            "com.android.server.display.mode.DisplayModeDirector$SettingsObserver";
    public static final String CALLER_METHOD = "updateLowPowerModeSettingLocked";

    private RenderVoteGate() {
    }

    public static boolean priorityConfirmed(Object priorityName) {
        return RENDER_RATE_PRIORITY_NAME.equals(priorityName);
    }

    public static boolean shouldClearVote(Object priority, StackTraceElement[] stack) {
        if (!(priority instanceof Integer) || ((Integer) priority) != RENDER_RATE_PRIORITY) {
            return false;
        }
        if (stack == null) {
            return false;
        }
        for (StackTraceElement frame : stack) {
            if (frame != null
                    && CALLER_CLASS.equals(frame.getClassName())
                    && CALLER_METHOD.equals(frame.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}
