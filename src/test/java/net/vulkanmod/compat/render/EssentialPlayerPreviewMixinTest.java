package net.vulkanmod.compat.render;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EssentialPlayerPreviewMixinTest {
    @Test
    void essentialUi3dPlayerPreviewUsesExplicitRenderStateBoundary() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/vulkanmod/mixin/compatibility/essential/EssentialUI3DPlayerMixin.java"));

        assertTrue(source.contains("@Mixin(targets = \"gg.essential.gui.common.UI3DPlayer\", remap = false)"));
        assertTrue(source.contains("method = \"drawPlayer()V\""));
        assertTrue(source.contains("RenderStateSnapshot"));
        assertTrue(source.contains("GuiEntityRenderState.beginHudEntityPreview();"));
        assertTrue(source.contains("GuiEntityRenderState.prepare();"));
        assertTrue(source.contains("vulkanMod$beginComponentScissor();"));
        assertTrue(source.contains("setRenderShadow(false);"));
        assertTrue(source.contains("GuiEntityRenderState.endHudEntityPreview();"));
        assertTrue(source.contains("setRenderShadow(shouldRenderShadow);"));
        assertTrue(source.contains("vulkanMod$restoreComponentScissor();"));
        assertTrue(source.contains("snapshot.restore();"));
    }

    @Test
    void essentialPlayerPreviewMixinIsRegisteredAndGatedAsOptionalCompatibility() throws IOException {
        String mixinConfig = Files.readString(Path.of("src/main/resources/vulkanmod.mixins.json"));
        String plugin = Files.readString(Path.of("src/main/java/net/vulkanmod/mixin/MixinPlugin.java"));

        assertTrue(mixinConfig.contains("\"compatibility.essential.EssentialUI3DPlayerMixin\""));
        assertTrue(plugin.contains("ESSENTIAL_MIXIN_PACKAGE"));
        assertTrue(plugin.contains("ESSENTIAL_UI_3D_PLAYER_MIXIN"));
        assertTrue(plugin.contains("gg.essential.gui.common.UI3DPlayer"));
    }

    @Test
    void essentialPreviewCanRestorePreviousEntityShadowState() throws IOException {
        String accessor = Files.readString(Path.of(
                "src/main/java/net/vulkanmod/mixin/render/entity/EntityRenderDispatcherAccessor.java"));
        String mixinConfig = Files.readString(Path.of("src/main/resources/vulkanmod.mixins.json"));

        assertTrue(accessor.contains("@Accessor(\"shouldRenderShadow\")"));
        assertTrue(accessor.contains("boolean vulkanMod$shouldRenderShadow();"));
        assertTrue(mixinConfig.contains("\"render.entity.EntityRenderDispatcherAccessor\""));
    }

    @Test
    void screenRenderClearsDepthSoGuiPreviewsDoNotZFight() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/vulkanmod/mixin/screen/ScreenM.java"));

        assertTrue(source.contains("method = \"renderWithTooltip\""));
        assertTrue(source.contains("Renderer.clearAttachments(GL11.GL_DEPTH_BUFFER_BIT)"));
    }

    @Test
    void essentialPreviewClipsPlayerDrawToComponentBounds() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/vulkanmod/mixin/compatibility/essential/EssentialUI3DPlayerMixin.java"));

        assertTrue(source.contains("vulkanMod$componentFloat(\"getLeft\")"));
        assertTrue(source.contains("vulkanMod$componentFloat(\"getTop\")"));
        assertTrue(source.contains("vulkanMod$componentFloat(\"getWidth\")"));
        assertTrue(source.contains("vulkanMod$componentFloat(\"getHeight\")"));
        assertTrue(source.contains("getClass().getMethod(methodName)"));
        assertTrue(source.contains("Renderer.isScissorEnabled()"));
        assertTrue(source.contains("Renderer.getScissorBox(0)"));
        assertTrue(source.contains("RenderSystem.enableScissor("));
        assertTrue(source.contains("Renderer.setScissorEnabled(false);"));
    }
}
