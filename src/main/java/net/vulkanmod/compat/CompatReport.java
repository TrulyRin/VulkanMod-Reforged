package net.vulkanmod.compat;

import net.vulkanmod.compat.path.RenderPath;
import net.vulkanmod.compat.path.RenderPathOwnership;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CompatReport {
    public static String generateReportString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== VulkanMod Universal Mod Compatibility Diagnostic Report ===\n");
        sb.append("System Coexistence Override Active: ").append(CompatPolicyManager.isCoexistenceIncompatible()).append("\n\n");

        sb.append("--- Mod Discovery Details ---\n");
        for (String modId : CompatMods.REPORT_MOD_IDS) {
            boolean loaded = CompatDetector.isModLoaded(modId);
            sb.append(" * Mod ID: ").append(modId)
              .append(" | Category: ").append(CompatPolicyManager.getCompatCategory(modId))
              .append(" | Loaded: ").append(loaded)
              .append(" | Version: ").append(CompatDetector.getModVersion(modId))
              .append(" | Policy Mode: ").append(CompatPolicyManager.getCompatMode(modId));
            if (loaded && CompatPolicyManager.getCompatCategory(modId) == CompatCategory.RENDERER_GL) {
                sb.append(" | Note: renderer/OpenGL pipeline mods are unsupported");
            }
            sb
              .append("\n");
        }

        sb.append("\n--- Render Path Ownership Allocations ---\n");
        for (RenderPath path : RenderPath.values()) {
            sb.append(" * Path: ").append(path)
              .append(" | Owner: ").append(RenderPathOwnership.getPathOwner(path))
              .append(" | Mode: ").append(RenderPathOwnership.getPathMode(path))
              .append("\n");
        }
        sb.append("===============================================================");
        return sb.toString();
    }

    public static void logReport() {
        logger().info(generateReportString());
    }

    public static String generateRuntimeHintString() {
        if (RuntimeOptions.hudTraceEnabled()) {
            return "";
        }

        StringBuilder activePaths = new StringBuilder();
        for (RenderPath path : RenderPath.values()) {
            if (!RenderPathOwnership.isPathActive(path)) {
                continue;
            }

            if (!activePaths.isEmpty()) {
                activePaths.append(", ");
            }
            activePaths.append(path);
        }

        if (activePaths.isEmpty()) {
            return "";
        }

        if (RuntimeOptions.diagnosticsEnabled()) {
            return "Compatibility render paths are active (" + activePaths
                    + "), but HUD trace is disabled. Add JVM arg -D"
                    + RuntimeOptions.HUD_TRACE_PROPERTY + "=true to capture HUD draw state.";
        }

        return "Compatibility render paths are active (" + activePaths
                + "), but diagnostics are disabled. Add JVM args -D"
                + RuntimeOptions.DIAGNOSTICS_PROPERTY + "=true -D"
                + RuntimeOptions.HUD_TRACE_PROPERTY + "=true to capture HUD draw state.";
    }

    public static void logRuntimeHints() {
        String hint = generateRuntimeHintString();
        if (!hint.isEmpty()) {
            logger().info(hint);
        }
    }

    private static Logger logger() {
        return LogManager.getLogger("VulkanMod-Compat");
    }
}

