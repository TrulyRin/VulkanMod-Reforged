package net.vulkanmod.mixin;

import net.vulkanmod.compat.RuntimeOptions;
import net.vulkanmod.compat.sable.SableCompatibilityOptions;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {
    private static final String SCGUNS_MIXIN = "net.vulkanmod.mixin.compatibility.scguns.GunRenderingHandlerMixin";
    private static final String SCGUNS_HANDLER = "top.ribs.scguns.client.handler.GunRenderingHandler";
    private static final String SCGUNS_DRAW = "(Lnet/minecraft/client/Minecraft;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V";
    private static final String SUBSCRIBE_EVENT = "Lnet/neoforged/bus/api/SubscribeEvent;";
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
    private static final String CODECHICKEN_MIXIN_PACKAGE = "net.vulkanmod.mixin.compatibility.codechicken.";
    private static final String CODECHICKEN_UNIFORM_ENTRY = "codechicken.lib.render.shader.CCUniform$UniformEntry";
    private static final String SABLE_MIXIN_PACKAGE = "net.vulkanmod.mixin.compatibility.sable.";
    private static final Set<String> VEIL_CORE_MIXINS = Set.of(
            SABLE_MIXIN_PACKAGE + "VeilVertexAttribDivisorMixin",
            SABLE_MIXIN_PACKAGE + "VeilSamplerObjectMixin",
            SABLE_MIXIN_PACKAGE + "VeilMultiBindLegacySamplerMixin",
            SABLE_MIXIN_PACKAGE + "VeilFirstPersonRendererMixin",
            SABLE_MIXIN_PACKAGE + "VeilRenderSystemMixin");

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (SCGUNS_MIXIN.equals(mixinClassName)) {
            return SCGUNS_HANDLER.equals(targetClassName) && supportsScgunsCooldownAbi();
        }
        if (mixinClassName.startsWith(CODECHICKEN_MIXIN_PACKAGE)) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            return (CODECHICKEN_MIXIN_PACKAGE + "CCUniformEntryMixin").equals(mixinClassName)
                    && CODECHICKEN_UNIFORM_ENTRY.equals(targetClassName) && loader != null
                    && loader.getResource("codechicken/lib/render/shader/CCUniform$UniformEntry.class") != null;
        }
        if (VEIL_CORE_MIXINS.contains(mixinClassName)) {
            return isVeilPresent();
        }
        if (mixinClassName.startsWith(SABLE_MIXIN_PACKAGE)) {
            return SableCompatibilityOptions.mode().enabled() && isSablePresent();
        }
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

    private static boolean supportsScgunsCooldownAbi() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) return false;
        String owner = SCGUNS_HANDLER.replace('.', '/');
        try (InputStream stream = loader.getResourceAsStream(owner + ".class")) {
            if (stream == null) return false;
            ClassNode target = new ClassNode();
            new ClassReader(stream).accept(target, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            if (!owner.equals(target.name)) return false;

            MethodNode constructor = scgunsMethod(target, "<init>", "()V", -1);
            boolean constructorReturns = false;
            if (constructor != null) {
                for (var instruction : constructor.instructions) {
                    if (instruction.getOpcode() == Opcodes.RETURN) constructorReturns = true;
                }
            }
            return constructorReturns
                    && scgunsMethod(target, "handleRenderTick", "()V", Opcodes.ACC_PRIVATE) != null
                    && scgunsMethod(target, "renderCooldownIndicator", SCGUNS_DRAW, Opcodes.ACC_PRIVATE) != null
                    && subscribedScgunsMethod(target, "(Lnet/neoforged/neoforge/client/event/ClientTickEvent$Post;)V")
                    && subscribedScgunsMethod(target, "(Lnet/neoforged/neoforge/client/event/RenderFrameEvent$Post;)V");
        } catch (IOException | RuntimeException unsupportedClass) {
            return false;
        }
    }

    private static MethodNode scgunsMethod(ClassNode target, String name, String descriptor, int visibility) {
        int visibilityMask = Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE;
        for (MethodNode method : target.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)
                    && (method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0
                    && (visibility < 0 || (method.access & visibilityMask) == visibility)) {
                return method;
            }
        }
        return null;
    }

    private static boolean subscribedScgunsMethod(ClassNode target, String descriptor) {
        MethodNode method = scgunsMethod(target, "onTick", descriptor, Opcodes.ACC_PUBLIC);
        return method != null && method.visibleAnnotations != null
                && method.visibleAnnotations.stream().anyMatch(annotation -> SUBSCRIBE_EVENT.equals(annotation.desc));
    }

    private static boolean isOldF3Mixin(String mixinClassName) {
        return mixinClassName.equals("net.vulkanmod.mixin.debug.DebugScreenOverlayM")
                || mixinClassName.equals("net.vulkanmod.mixin.debug.GlDebugInfoM");
    }

    private static boolean isSablePresent() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader != null && loader.getResource(
                "dev/ryanhcode/sable/sublevel/render/dispatcher/SubLevelRenderDispatcher.class") != null;
    }

    private static boolean isVeilPresent() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader != null && loader.getResource(
                "foundry/veil/api/client/render/VeilRenderSystem.class") != null;
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
