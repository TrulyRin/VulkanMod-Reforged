package net.vulkanmod.mixin.compatibility;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.Program;
import net.vulkanmod.gl.GlUtil;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Mixin(value = Program.class, priority = 900)
public class ProgramM {

    @Inject(method = "compileShaderInternal", at = @At("HEAD"), cancellable = true)
    private static void compileShaderInternal(Program.Type type, String string, InputStream inputStream, String string2, GlslPreprocessor glslPreprocessor, CallbackInfoReturnable<Integer> cir) throws IOException {
        String string3 = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        if (string3 == null) {
            throw new IOException("Could not load program " + type.getName());
        } else {

        }
        cir.setReturnValue(0);
    }
}
