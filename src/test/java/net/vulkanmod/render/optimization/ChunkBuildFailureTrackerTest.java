package net.vulkanmod.render.optimization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkBuildFailureTrackerTest {
    @BeforeEach
    void reset() {
        ChunkBuildFailureTracker.resetForTests();
    }

    @AfterEach
    void cleanUp() {
        ChunkBuildFailureTracker.resetForTests();
    }

    @Test
    void recordsOnlyStablePrivacySafeFailureTypes() {
        ChunkBuildFailureTracker.record("rend_chk_rebuild",
                new IllegalStateException("world name and coordinates must not be copied"));

        Set<String> failures = ChunkBuildFailureTracker.reportedFailures();
        assertTrue(failures.contains("rend_chk_rebuild:java.lang.IllegalStateException"));
        assertFalse(failures.toString().contains("world name"));
        assertThrows(UnsupportedOperationException.class, () -> failures.add("mutable"));
    }
}
