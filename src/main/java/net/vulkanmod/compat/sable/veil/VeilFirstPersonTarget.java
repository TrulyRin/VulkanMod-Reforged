package net.vulkanmod.compat.sable.veil;

import net.vulkanmod.Initializer;
import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import net.vulkanmod.vulkan.pass.MainPass;
import net.vulkanmod.vulkan.texture.VulkanImage;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;


public final class VeilFirstPersonTarget {
    private static boolean disabled;
    private static boolean warned;

    private VeilFirstPersonTarget() { }

    public static int width() {
        VulkanImage color=mainColor();
        if(color!=null) return color.width;
        warn("Veil first-person main color is unavailable; target refresh cannot run.",null);
        return 1;
    }

    public static int height() {
        VulkanImage color=mainColor();
        if(color!=null) return color.height;
        warn("Veil first-person main color is unavailable; target refresh cannot run.",null);
        return 1;
    }

    private static VulkanImage mainColor() {
        Renderer renderer=Renderer.getInstance();
        MainPass main=renderer!=null?renderer.getMainPass():null;
        return main!=null?main.getColorAttachment():null;
    }

    public static void refresh() {
        if(disabled) return;
        Renderer renderer=Renderer.getInstance();
        MainPass main=renderer!=null?renderer.getMainPass():null;
        VulkanImage color=main!=null?main.getColorAttachment():null;
        if(color==null) {
            warn("Veil first-person main color is unavailable; original attachments were retained.",null);
            return;
        }
        try {
            Object target=(Object)Linkage.FIRST_PERSON.invokeExact();
            if(target==null) return;
            Object colorAttachment=(Object)Linkage.COLOR_ATTACHMENT.invokeExact(target,0);
            Object depthAttachment=(Object)Linkage.DEPTH_ATTACHMENT.invokeExact(target);
            if(!Linkage.MUTABLE_TEXTURE.isInstance(colorAttachment) || depthAttachment==null) {
                disabled=true;
                warn("Veil first-person attachment ABI is incompatible; refresh disabled and original attachments retained.",null);
                return;
            }
            int framebufferId=(int)Linkage.TARGET_ID.invokeExact(target);
            int depthTextureId=(int)Linkage.TEXTURE_ID.invokeExact(depthAttachment);
            int colorTextureId=main.getColorAttachmentGlId();
            int capacity=1;
            SwapChain swapChain=Vulkan.getSwapChain();
            int imageIndex=Renderer.getCurrentImage();
            if(swapChain!=null && imageIndex>=0 && imageIndex<swapChain.getImagesNum()
                    && swapChain.getImages().get(imageIndex)==color) {
                capacity=swapChain.getImagesNum();
            }
            if(!GlFramebuffer.refreshBorrowedAttachments(framebufferId,colorTextureId,depthTextureId,capacity)) {
                warn("Veil first-person borrowed attachment refresh was rejected; original wrapper metadata retained.",null);
                return;
            }

            Linkage.SET_TEXTURE.invokeExact(colorAttachment,colorTextureId,-1);
        } catch(Throwable failure) {
            disabled=true;
            warn("Veil first-person target linkage failed; refresh disabled and the original Veil lifecycle retained.",failure);
        }
    }

    private static void warn(String message,Throwable failure) {
        if(warned) return;
        warned=true;
        if(failure==null) Initializer.LOGGER.warn(message);
        else Initializer.LOGGER.warn(message,failure);
    }


    private static final class Linkage {
        static final Class<?> MUTABLE_TEXTURE;
        static final MethodHandle FIRST_PERSON;
        static final MethodHandle COLOR_ATTACHMENT;
        static final MethodHandle DEPTH_ATTACHMENT;
        static final MethodHandle TARGET_ID;
        static final MethodHandle TEXTURE_ID;
        static final MethodHandle SET_TEXTURE;

        static {
            try {
                ClassLoader loader=VeilFirstPersonTarget.class.getClassLoader();
                Class<?> renderer=Class.forName("foundry.veil.impl.client.render.pipeline.VeilFirstPersonRenderer",false,loader);
                Class<?> framebuffer=Class.forName("foundry.veil.api.client.render.framebuffer.AdvancedFbo",false,loader);
                Class<?> texture=Class.forName("foundry.veil.api.client.render.framebuffer.AdvancedFboTextureAttachment",false,loader);
                MUTABLE_TEXTURE=Class.forName("foundry.veil.impl.client.render.framebuffer.AdvancedFboMutableTextureAttachment",false,loader);
                MethodHandles.Lookup lookup=MethodHandles.lookup();
                FIRST_PERSON=MethodHandles.privateLookupIn(renderer,lookup).findStaticGetter(renderer,"firstPerson",framebuffer)
                        .asType(MethodType.methodType(Object.class));
                COLOR_ATTACHMENT=lookup.findVirtual(framebuffer,"getColorTextureAttachment",MethodType.methodType(texture,int.class))
                        .asType(MethodType.methodType(Object.class,Object.class,int.class));
                DEPTH_ATTACHMENT=lookup.findVirtual(framebuffer,"getDepthTextureAttachment",MethodType.methodType(texture))
                        .asType(MethodType.methodType(Object.class,Object.class));
                TARGET_ID=lookup.findVirtual(framebuffer,"getId",MethodType.methodType(int.class))
                        .asType(MethodType.methodType(int.class,Object.class));
                TEXTURE_ID=lookup.findVirtual(texture,"getId",MethodType.methodType(int.class))
                        .asType(MethodType.methodType(int.class,Object.class));
                SET_TEXTURE=lookup.findVirtual(MUTABLE_TEXTURE,"setTexture",MethodType.methodType(void.class,int.class,int.class))
                        .asType(MethodType.methodType(void.class,Object.class,int.class,int.class));
            } catch(ReflectiveOperationException failure) {
                throw new ExceptionInInitializerError(failure);
            }
        }
    }
}
