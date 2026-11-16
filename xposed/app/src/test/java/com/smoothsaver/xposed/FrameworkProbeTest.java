package com.smoothsaver.xposed;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class FrameworkProbeTest {

    @Test
    public void diagnosticFilterMatchesOnlyDisplayRestrictionMethods() {
        assertTrue(FrameworkProbe.isDiagnosticMethod("updateLowPowerModeSettingLocked"));
        assertTrue(FrameworkProbe.isDiagnosticMethod("onChange"));
        assertTrue(FrameworkProbe.isDiagnosticMethod("updateGlobalVote"));
        assertTrue(FrameworkProbe.isDiagnosticMethod("forRenderFrameRates"));
        assertFalse(FrameworkProbe.isDiagnosticMethod("updateRefreshRateSettingLocked"));
        assertFalse(FrameworkProbe.isDiagnosticMethod("onChangeListener"));
        assertFalse(FrameworkProbe.isDiagnosticMethod(null));
    }

    @Test
    public void inspectionReportsSignaturesWithoutReadingPriorityValues() {
        List<String> lines = new ArrayList<>();
        FrameworkProbe.inspect(
                FrameworkProbeTest.class.getClassLoader(),
                List.of(Sample.class.getName(), Vote.class.getName(), "com.absent.NoSuchDirector"),
                lines::add);
        String output = String.join("\n", lines);

        assertTrue(output.contains("Detected framework class: " + Sample.class.getName()));
        assertTrue(output.contains("void updateLowPowerModeSettingLocked()"));
        assertTrue(output.contains("void onChange(boolean, java.lang.String, int)"));
        assertFalse(output.contains("unrelated"));
        assertTrue(output.contains("Detected framework class: com.absent.NoSuchDirector — absent"));
        assertTrue(output.contains("PRIORITY_LOW_POWER_MODE_RENDER_RATE"));
        assertTrue(output.contains("value not read"));
        assertFalse(output.contains("424242"));
    }

    @Test
    public void policyInstallsNoHooks() {
        List<String> lines = new ArrayList<>();
        ModulePolicy.logSkippedHooks(lines::add);
        String output = String.join("\n", lines);

        assertTrue(output.contains("Settings.Global.getInt"));
        assertTrue(output.contains("vote setters"));
        assertTrue(output.contains("priority 19"));
        assertFalse(output.contains("Successfully hooked"));
    }

    public static class Sample {
        @SuppressWarnings("unused")
        private void updateLowPowerModeSettingLocked() {
        }

        @SuppressWarnings("unused")
        public void onChange(boolean selfChange, String uri, int userId) {
        }

        @SuppressWarnings("unused")
        public void unrelated() {
        }
    }

    public static class Vote {
        public static final int PRIORITY_LOW_POWER_MODE_RENDER_RATE = 424242;

        @SuppressWarnings("unused")
        public static Object forRenderFrameRates(float min, float max) {
            return null;
        }
    }
}
