package com.smoothsaver.xposed;

/**
 * Hooks that 1.1.0 installed, and why this build does not install them.
 * Startup logging and the status screen share this text.
 */
public final class ModulePolicy {

    private ModulePolicy() {
    }

    public static final String[] SKIPPED_HOOKS = {
            "BatterySaverObserver.onChange: skipped. 1.1.0 replaced every onChange method on a guessed observer class. "
                    + "DisplayModeDirector's settings observer also handles peak refresh rate, minimum refresh rate, "
                    + "and content frame-rate matching in onChange. Replacing those callbacks changes unrelated display behavior.",
            "observe/register/startObserving: skipped. 1.1.0 replaced every method with those names. "
                    + "That stops the whole observer, not one Battery Saver callback.",
            "DisplayModeDirector vote setters: skipped. 1.1.0 hooked every method whose name contained both \"vote\" and "
                    + "\"update\" or \"set\", then returned null when the stack contained BatterySaver, LowPower, or PowerSave. "
                    + "Returning null from a primitive or non-null method crashes the caller. "
                    + "The priority was inferred from the stack instead of read from the framework.",
            "Settings.Global.getInt: skipped. 1.1.0 forced the low_power setting to 0 when DisplayManagerService appeared "
                    + "anywhere on the stack. That misreports Battery Saver for ordinary display queries, including Settings search, "
                    + "and it is not limited to the refresh-rate vote.",
            "VotesStorage.updateVote and priority 19: skipped. On this stallion build that call carries "
                    + "low-power supported modes and work durations, separate from the 0–60 Hz render vote."
    };

    public static void logSkippedHooks(LineSink sink) {
        for (String reason : SKIPPED_HOOKS) {
            sink.line("Skipped hook: " + reason);
        }
    }

    public interface LineSink {
        void line(String message);
    }
}
