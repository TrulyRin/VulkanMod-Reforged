package net.vulkanmod.mixin;

import net.vulkanmod.compat.RuntimeOptions;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {
    private static final String CREATE_MIXIN_PACKAGE = "net.vulkanmod.mixin.compatibility.create.";
    private static final String CREATE_MAIN_MENU_SCREEN_MIXIN = CREATE_MIXIN_PACKAGE + "CreateMainMenuScreenMixin";
    private static final String CREATE_MAIN_MENU_SCREEN = "com.simibubi.create.infrastructure.gui.CreateMainMenuScreen";
    private static final String TENSURA_MIXIN_PACKAGE = "net.vulkanmod.mixin.compatibility.tensura.";
    private static final String TENSURA_SIMPLE_BUTTON_MIXIN = TENSURA_MIXIN_PACKAGE + "TensuraSimpleButtonMixin";
    private static final String TENSURA_OVERLAY_HANDLER_MIXIN = TENSURA_MIXIN_PACKAGE + "TensuraOverlayHandlerMixin";
    private static final String TENSURA_SIMPLE_BUTTON = "io.github.manasmods.tensura.client.screen.widgets.SimpleButton";
    private static final String TENSURA_OVERLAY_HANDLER = "io.github.manasmods.tensura.handler.client.OverlayHandler";
    private static final String ESSENTIAL_MIXIN_PACKAGE = "net.vulkanmod.mixin.compatibility.essential.";
    private static final String ESSENTIAL_UI_3D_PLAYER_MIXIN = ESSENTIAL_MIXIN_PACKAGE + "EssentialUI3DPlayerMixin";
    private static final String ESSENTIAL_UI_3D_PLAYER = "gg.essential.gui.common.UI3DPlayer";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith(CREATE_MIXIN_PACKAGE)) {
            return CREATE_MAIN_MENU_SCREEN_MIXIN.equals(mixinClassName)
                    && CREATE_MAIN_MENU_SCREEN.equals(targetClassName);
        }

        if (mixinClassName.startsWith(TENSURA_MIXIN_PACKAGE)) {
            if (TENSURA_SIMPLE_BUTTON_MIXIN.equals(mixinClassName)) {
                return TENSURA_SIMPLE_BUTTON.equals(targetClassName);
            }

            if (TENSURA_OVERLAY_HANDLER_MIXIN.equals(mixinClassName)) {
                return TENSURA_OVERLAY_HANDLER.equals(targetClassName);
            }

            return false;
        }

        if (mixinClassName.startsWith(ESSENTIAL_MIXIN_PACKAGE)) {
            return ESSENTIAL_UI_3D_PLAYER_MIXIN.equals(mixinClassName)
                    && ESSENTIAL_UI_3D_PLAYER.equals(targetClassName);
        }

        if (mixinClassName.startsWith("net.vulkanmod.mixin.profiling.")) {
            return RuntimeOptions.profilingMixinsEnabled();
        }

        if (isOldF3Mixin(mixinClassName)) {
            return true;
        }

        if (mixinClassName.startsWith("net.vulkanmod.mixin.debug.")) {
            return RuntimeOptions.debugMixinsEnabled();
        }

        return true;
    }

    private static boolean isOldF3Mixin(String mixinClassName) {
        return mixinClassName.equals("net.vulkanmod.mixin.debug.DebugScreenOverlayM")
                || mixinClassName.equals("net.vulkanmod.mixin.debug.GlDebugInfoM");
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
