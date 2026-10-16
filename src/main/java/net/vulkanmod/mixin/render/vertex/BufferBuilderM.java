package net.vulkanmod.mixin.render.vertex;

import com.mojang.blaze3d.vertex.*;
import net.vulkanmod.interfaces.ExtendedVertexBuilder;
import net.vulkanmod.interfaces.VertexFormatMixed;
import net.vulkanmod.render.vertex.VertexUtil;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;
import java.util.List;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderM extends DefaultedVertexConsumer
        implements BufferVertexConsumer, ExtendedVertexBuilder {

    @Shadow(remap = false) public abstract void m_5752_();

    @Shadow(remap = false) private ByteBuffer f_85648_;

    @Shadow(remap = false) private int f_85652_;
    @Shadow(remap = false) private boolean f_85659_;
    @Shadow(remap = false) private boolean f_85660_;
    @Shadow(remap = false) private int f_85656_;
    @Shadow(remap = false) private @Nullable VertexFormatElement f_85655_;
    @Shadow(remap = false) private VertexFormat f_85658_;

                                                         
                                  
                                  
                                  
                                             

    private long bufferPtr;
    private long ptr;
    private int offset;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void setPtrC(int initialCapacity, CallbackInfo ci) {
        this.bufferPtr = MemoryUtil.memAddress0(this.f_85648_);
    }

    @Inject(method = "m_85722_", at = @At(value = "RETURN"), remap = false)
    private void setPtr(int initialCapacity, CallbackInfo ci) {
        this.bufferPtr = MemoryUtil.memAddress0(this.f_85648_);
    }

    public void vertex(float x, float y, float z, int packedColor, float u, float v, int overlay, int light, int packedNormal) {
        this.ptr = this.nextElementPtr();

        if(this.f_85658_ == DefaultVertexFormat.NEW_ENTITY) {
            MemoryUtil.memPutFloat(ptr + 0, x);
            MemoryUtil.memPutFloat(ptr + 4, y);
            MemoryUtil.memPutFloat(ptr + 8, z);

            MemoryUtil.memPutInt(ptr + 12, packedColor);

            MemoryUtil.memPutFloat(ptr + 16, u);
            MemoryUtil.memPutFloat(ptr + 20, v);

            MemoryUtil.memPutInt(ptr + 24, overlay);

            MemoryUtil.memPutInt(ptr + 28, light);
            MemoryUtil.memPutInt(ptr + 32, packedNormal);

            this.f_85652_ += 36;
            this.m_5752_();

        }
        else {
            this.position(x, y, z);
            this.fastColor(packedColor);
            this.fastUv(u, v);
            this.fastOverlay(overlay);
            this.light(light);
            this.fastNormal(packedNormal);
            this.m_5752_();
                                                                              
        }

    }

    public void vertex(float x, float y, float z, float u, float v, int packedColor, int light) {
        this.ptr = this.nextElementPtr();

        MemoryUtil.memPutFloat(ptr + 0, x);
        MemoryUtil.memPutFloat(ptr + 4, y);
        MemoryUtil.memPutFloat(ptr + 8, z);

        MemoryUtil.memPutFloat(ptr + 12, u);
        MemoryUtil.memPutFloat(ptr + 16, v);

        MemoryUtil.memPutInt(ptr + 20, packedColor);

        MemoryUtil.memPutInt(ptr + 24, light);

        this.f_85652_ += 28;
        this.m_5752_();

    }

    public void position(float x, float y, float z) {
        MemoryUtil.memPutFloat(ptr + 0, x);
        MemoryUtil.memPutFloat(ptr + 4, y);
        MemoryUtil.memPutFloat(ptr + 8, z);

        this.m_5751_();
    }

    public void fastColor(int packedColor) {
        if (this.f_85655_.getUsage() != VertexFormatElement.Usage.COLOR)
            return;

        MemoryUtil.memPutInt(ptr + 12, packedColor);

        this.m_5751_();
    }

    public void fastUv(float u, float v) {
        if (this.f_85655_.getUsage() != VertexFormatElement.Usage.UV)
            return;

        MemoryUtil.memPutFloat(ptr + 16, u);
        MemoryUtil.memPutFloat(ptr + 20, v);

        this.m_5751_();
    }

    public void fastOverlay(int o) {
        if (this.f_85655_.getUsage() != VertexFormatElement.Usage.UV)
            return;

        MemoryUtil.memPutInt(ptr + 24, o);

        this.m_5751_();
    }

    public void light(int l) {
        if (this.f_85655_.getUsage() != VertexFormatElement.Usage.UV)
            return;

        MemoryUtil.memPutInt(ptr + 28, l);

        this.m_5751_();
    }

    public void fastNormal(int packedNormal) {
        if (this.f_85655_.getUsage() != VertexFormatElement.Usage.NORMAL)
            return;

        MemoryUtil.memPutInt(ptr + 32, packedNormal);

        this.m_5751_();
    }

       
              
       
    @Overwrite(remap = false)
    public void m_5954_(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        if (this.f_85659_) {
            long ptr = this.nextElementPtr();
            MemoryUtil.memPutFloat(ptr + 0, x);
            MemoryUtil.memPutFloat(ptr + 4, y);
            MemoryUtil.memPutFloat(ptr + 8, z);

            int temp = ColorUtil.RGBA.pack(red, green, blue, alpha);
            MemoryUtil.memPutInt(ptr + 12, temp);

            MemoryUtil.memPutFloat(ptr + 16, u);
            MemoryUtil.memPutFloat(ptr + 20, v);

            byte i;
            if (this.f_85660_) {
                MemoryUtil.memPutInt(ptr + 24, overlay);
                i = 28;
            } else {
                i = 24;
            }

            MemoryUtil.memPutInt(ptr + i, light);

            temp = VertexUtil.packNormal(normalX, normalY, normalZ);
            MemoryUtil.memPutInt(ptr + i + 4, temp);

            this.f_85652_ += i + 8;
            this.m_5752_();
        } else {
            super.vertex(x, y, z, red, green, blue, alpha, u, v, overlay, light, normalX, normalY, normalZ);
        }
    }

       
              
       
    @Overwrite(remap = false)
    public void m_5751_() {
        VertexFormatElement vertexFormatElement;
        List<VertexFormatElement> list = ((VertexFormatMixed)(this.f_85658_)).getFastList();

        this.f_85656_ = (this.f_85656_ + 1) % list.size();
        this.f_85652_ += this.f_85655_.getByteSize();

        this.f_85655_ = vertexFormatElement = list.get(this.f_85656_);

        if (vertexFormatElement.getUsage() == VertexFormatElement.Usage.PADDING) {
            this.m_5751_();
        }
        if (this.defaultColorSet && this.f_85655_.getUsage() == VertexFormatElement.Usage.COLOR) {
            BufferVertexConsumer.super.color(this.defaultR, this.defaultG, this.defaultB, this.defaultA);
        }
    }

         
                
         
                
                                                                           
                                                                   
                                                        
  
                                                                      
                                                               
                                                               
                                                               
                                                   
           
  
    		                                                                                  
                                                                                            
                                            
                                               
  
                                                                                      
  
                                              
                            
                                                                    
                                                                    
                                                                    
                                                                    
                                                                    
                                                                    
           
       

    public void putByte(int index, byte value) {
        MemoryUtil.memPutByte(this.bufferPtr + this.f_85652_ + index, value);
    }

    public void putShort(int index, short value) {
        MemoryUtil.memPutShort(this.bufferPtr + this.f_85652_ + index, value);
    }

    public void putFloat(int index, float value) {
        MemoryUtil.memPutFloat(this.bufferPtr + this.f_85652_ + index, value);
    }

    private long nextElementPtr() {
        return (this.bufferPtr + this.f_85652_);
    }

    protected void setNextElementByte(int i) {
        this.f_85652_ = i;
    }
}

