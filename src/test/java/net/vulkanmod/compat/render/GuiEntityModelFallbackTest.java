package net.vulkanmod.compat.render;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiEntityModelFallbackTest {
    @Test
    void guiEntityPreviewUsesVanillaModelPartCompileAfterPreparingRenderState() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/vulkanmod/mixin/render/entity/model/ModelPartM.java"));
        int previewCheck = source.indexOf("if (GuiEntityRenderState.isGuiEntityPreview())");
        int prepare = source.indexOf("GuiEntityRenderState.prepareDeferredDraw();", previewCheck);
        int fallback = source.indexOf("return;", prepare);
        int fastPath = source.indexOf("this.renderCubes(pose, vertexConsumer, light, overlay, color);", previewCheck);

        assertTrue(previewCheck >= 0);
        assertTrue(prepare > previewCheck);
        assertTrue(fallback > prepare);
        assertTrue(fastPath > fallback);
    }

    @Test
    void guiPreviewDetectionForModelFallbackDoesNotDependOnPackedLight() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/vulkanmod/compat/render/GuiEntityRenderState.java"));
        int noArgMethod = source.indexOf("public static boolean isGuiEntityPreview()");
        int packedLightMethod = source.indexOf("public static boolean isGuiEntityPreview(int packedLight)");

        assertTrue(noArgMethod >= 0);
        assertTrue(packedLightMethod > noArgMethod);
    }
}
