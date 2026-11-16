package com.smoothsaver.xposed;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RenderVoteGateTest {

    @Test
    public void clearsOnlyTheVerifiedCallerAndPriority() {
        StackTraceElement[] caller = new StackTraceElement[]{
                new StackTraceElement(
                        RenderVoteGate.CALLER_CLASS,
                        RenderVoteGate.CALLER_METHOD,
                        "DisplayModeDirector.java",
                        100)
        };
        assertTrue(RenderVoteGate.shouldClearVote(20, caller));
        assertFalse(RenderVoteGate.shouldClearVote(19, caller));
        assertFalse(RenderVoteGate.shouldClearVote(20, new StackTraceElement[]{
                new StackTraceElement(
                        "com.android.server.display.mode.DisplayModeDirector$BrightnessObserver",
                        "onLowPowerModeEnabledLocked",
                        "DisplayModeDirector.java",
                        10)
        }));
        assertFalse(RenderVoteGate.shouldClearVote(20, null));
        assertFalse(RenderVoteGate.shouldClearVote("20", caller));
    }

    @Test
    public void priorityNameMustMatchTheFramework() {
        assertTrue(RenderVoteGate.priorityConfirmed("PRIORITY_LOW_POWER_MODE_RENDER_RATE"));
        assertFalse(RenderVoteGate.priorityConfirmed("PRIORITY_LOW_POWER_MODE_MODES"));
        assertFalse(RenderVoteGate.priorityConfirmed(null));
    }
}
