package net.vulkanmod.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticsUiSourceTest {
    @Test
    void diagnosticsAndDoctorAreDetachedFromTheSettingsScreen() throws IOException {
        String screen = source("src/main/java/net/vulkanmod/config/gui/VOptionScreen.java");

        assertFalse(screen.contains("DiagnosticsPageContent"));
        assertFalse(screen.contains("diagnosticsPage"));
        assertFalse(screen.contains("pendingDoctorDraft"));
        assertFalse(screen.contains("DoctorAction"));
        assertFalse(screen.contains("isDiagnosticsSelected"));
        assertTrue(screen.contains("capturePendingOptionDrafts()"));
        assertTrue(screen.contains("restoreOptionDrafts(pendingDraftsToRestore)"));
    }

    @Test
    void diagnosticsBackendsRemainAvailableOutsideTheRemovedPage() throws IOException {
        String service = source("src/main/java/net/vulkanmod/compat/diagnostics/CompatDiagnosticsService.java");
        String report = source("src/main/java/net/vulkanmod/compat/CompatReport.java");
        String doctor = source("src/main/java/net/vulkanmod/compat/doctor/CompatibilityDoctor.java");

        assertTrue(service.contains("class CompatDiagnosticsService"));
        assertTrue(report.contains("class CompatReport"));
        assertTrue(doctor.contains("class CompatibilityDoctor"));
    }

    @Test
    void settingsApplyStillPreservesStableDraftAndFocusBehavior() throws IOException {
        String screen = source("src/main/java/net/vulkanmod/config/gui/VOptionScreen.java");

        assertTrue(screen.contains("draft.optionIdentity().equals(option.draftIdentity())"));
        assertTrue(screen.contains("option.acceptsDraftValue(value)"));
        assertTrue(screen.contains("preservedDrafts = applyingSettings"));
        assertTrue(screen.contains("this.applyingSettings = true"));
        assertTrue(screen.contains("this.applyingSettings = false"));
        assertTrue(screen.contains("focusedBeforeClick"));
        assertTrue(screen.contains("this.getFocused() == focusedBeforeClick"));
    }

    private static String source(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
