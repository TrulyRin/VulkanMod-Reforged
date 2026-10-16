package net.vulkanmod.vulkan.framebuffer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.pass.DefaultMainPass;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkImageCopy;
import org.lwjgl.vulkan.VkImageFormatProperties;

import java.util.Objects;

import static org.lwjgl.vulkan.VK10.*;

                                                                                        
public final class MainDepthSnapshot implements AutoCloseable {
    private static final int DEPTH_STAGES = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT;
    private static final int DEPTH_ACCESS = VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
    private static final int SNAPSHOT_USAGE = VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT;

    public record DepthSource(VulkanImage image, int format, int width, int height, int aspects) {}

    public interface Scope extends AutoCloseable {
        @Override void close();
    }

                                                                                                  
    public interface Backend {
        DepthSource currentWritableMainDepth();
        default DepthSource currentWritableMainDepth(VulkanImage sampled) { return currentWritableMainDepth(); }
        VulkanImage createSnapshot(DepthSource source, int usage, int sampledViewAspect);
        int currentLayout(VulkanImage image);
        Scope suspend();
        void barrier(VulkanImage image, int oldLayout, int newLayout,
                     int sourceStages, int sourceAccess, int destinationStages, int destinationAccess, int aspects);
        void copy(VulkanImage source, VulkanImage destination, int width, int height, int aspects);
        void retire(VulkanImage image);
    }

    private final Backend backend;
    private DepthSource owner;
    private VulkanImage snapshot;
    private boolean closed;

    public MainDepthSnapshot() { this(new NativeBackend()); }

    public MainDepthSnapshot(Backend backend) { this.backend = Objects.requireNonNull(backend, "backend"); }

    public static int depthAttachmentUsage() {
        return VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
    }

                                                                                        
    public static DepthSource selectWritableMainDepth(VulkanImage sampled, DepthSource mainDepth,
                                                       VulkanImage boundDepth, boolean supportedPass) {
        if (sampled == null || mainDepth == null || sampled != mainDepth.image() || boundDepth != mainDepth.image()) return null;
        if (!supportedPass) throw new IllegalStateException("Cannot sample writable main depth from an unsupported render pass owner");
        return mainDepth;
    }

    public VulkanImage resolveForSampling(VulkanImage sampled) {
        return resolveForSampling(sampled, VK_SHADER_STAGE_FRAGMENT_BIT);
    }

    public VulkanImage resolveForSampling(VulkanImage sampled, int samplingStages) {
        if (closed) throw new IllegalStateException("Main depth snapshot is closed");
        if (sampled == null) return null;
        DepthSource source = backend.currentWritableMainDepth(sampled);
        if (source == null || source.image() != sampled) return sampled;
        if (samplingStages != VK_SHADER_STAGE_FRAGMENT_BIT) {
            throw new IllegalStateException("Writable main depth snapshots support fragment-only sampling");
        }
        validateSource(source);
        if (backend.currentLayout(sampled) != VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {
            throw new IllegalStateException("Writable main depth is not in attachment layout");
        }

        boolean replace = snapshot == null || !source.equals(owner);
        VulkanImage candidate = replace
                ? backend.createSnapshot(source, SNAPSHOT_USAGE, VK_IMAGE_ASPECT_DEPTH_BIT) : snapshot;
        if (candidate == null) throw new IllegalStateException("Main depth snapshot allocation is unavailable");
        boolean completed = false;
        Throwable primaryFailure = null;
        try {
            if (candidate == sampled || candidate.getImageView() == 0) {
                throw new IllegalStateException("Main depth snapshot must have a separate sampled view");
            }
            try (Scope pass = backend.suspend(); Scope restoreSource = () -> restoreSource(source)) {
                backend.barrier(sampled, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                        DEPTH_STAGES, DEPTH_ACCESS, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_ACCESS_TRANSFER_READ_BIT, source.aspects());
                int oldSnapshotLayout = backend.currentLayout(candidate);
                if (oldSnapshotLayout != VK_IMAGE_LAYOUT_UNDEFINED && oldSnapshotLayout != VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
                    throw new IllegalStateException("Main depth snapshot has an unexpected layout");
                }
                backend.barrier(candidate, oldSnapshotLayout, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        oldSnapshotLayout == VK_IMAGE_LAYOUT_UNDEFINED ? VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT : VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                        oldSnapshotLayout == VK_IMAGE_LAYOUT_UNDEFINED ? 0 : VK_ACCESS_SHADER_READ_BIT,
                        VK_PIPELINE_STAGE_TRANSFER_BIT, VK_ACCESS_TRANSFER_WRITE_BIT, source.aspects());
                backend.copy(sampled, candidate, source.width(), source.height(), VK_IMAGE_ASPECT_DEPTH_BIT);
                backend.barrier(candidate, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                        VK_PIPELINE_STAGE_TRANSFER_BIT, VK_ACCESS_TRANSFER_WRITE_BIT,
                        VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, VK_ACCESS_SHADER_READ_BIT, source.aspects());
            }
            completed = true;
        } catch (RuntimeException | Error failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            if (!completed) {
                if (candidate == snapshot) {
                    snapshot = null;
                    owner = null;
                }
                if (candidate != sampled) {
                    try {
                        backend.retire(candidate);
                    } catch (RuntimeException | Error cleanup) {
                        if (primaryFailure == null) throw cleanup;
                        if (primaryFailure != cleanup) primaryFailure.addSuppressed(cleanup);
                    }
                }
            }
        }

        if (replace) {
            VulkanImage previous = snapshot;
            snapshot = candidate;
            owner = source;
            if (previous != null) backend.retire(previous);
        }
        return candidate;
    }

    private static void validateSource(DepthSource source) {
        int expectedAspects = switch (source.format()) {
            case VK_FORMAT_D24_UNORM_S8_UINT, VK_FORMAT_D32_SFLOAT_S8_UINT -> VK_IMAGE_ASPECT_DEPTH_BIT | VK_IMAGE_ASPECT_STENCIL_BIT;
            case VK_FORMAT_X8_D24_UNORM_PACK32, VK_FORMAT_D32_SFLOAT, VK_FORMAT_D16_UNORM -> VK_IMAGE_ASPECT_DEPTH_BIT;
            default -> throw new IllegalStateException("Main depth format cannot be sampled as depth");
        };
        if (source.width() <= 0 || source.height() <= 0 || source.aspects() != expectedAspects || source.image().getImageView() == 0) {
            throw new IllegalStateException("Main depth attachment is unavailable");
        }
    }

    private void restoreSource(DepthSource source) {
        int layout = backend.currentLayout(source.image());
        if (layout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) return;
        if (layout != VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL) throw new IllegalStateException("Main depth transfer did not preserve its layout");
        backend.barrier(source.image(), layout, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                VK_PIPELINE_STAGE_TRANSFER_BIT, VK_ACCESS_TRANSFER_READ_BIT, DEPTH_STAGES, DEPTH_ACCESS, source.aspects());
    }

                                                                                                 
    @Override public void close() {
        if (closed) return;
        closed = true;
        VulkanImage previous = snapshot;
        snapshot = null;
        owner = null;
        if (previous != null) backend.retire(previous);
    }

    private static final class NativeBackend implements Backend {
        private VkCommandBuffer commandBuffer;
        private DepthSource cachedMainDepth;

        @Override public DepthSource currentWritableMainDepth() {
            RenderSystem.assertOnRenderThread();
            Renderer renderer = Renderer.getInstance();
            if (renderer == null || !Renderer.isRecording() || renderer.getMainPass() == null) {
                cachedMainDepth = null;
                return null;
            }
            VulkanImage depth = renderer.getMainPass().getDepthAttachment();
            if (depth == null) {
                cachedMainDepth = null;
                return null;
            }
            if (cachedMainDepth == null || cachedMainDepth.image() != depth || cachedMainDepth.format() != depth.format
                    || cachedMainDepth.width() != depth.width || cachedMainDepth.height() != depth.height || cachedMainDepth.aspects() != depth.aspect) {
                cachedMainDepth = new DepthSource(depth, depth.format, depth.width, depth.height, depth.aspect);
            }
            return cachedMainDepth;
        }

        @Override public DepthSource currentWritableMainDepth(VulkanImage sampled) {
            DepthSource mainDepth = currentWritableMainDepth();
            if (mainDepth == null || mainDepth.image() != sampled) return null;
            Renderer renderer = Renderer.getInstance();
            RenderPass pass = renderer.getBoundRenderPass();
            VulkanImage boundDepth = pass != null ? pass.getFramebuffer().getDepthAttachment() : null;
            boolean supported = renderer.getMainPass() instanceof DefaultMainPass main && main.isMainRenderPass(pass);
            return selectWritableMainDepth(sampled, mainDepth, boundDepth, supported);
        }

        @Override public VulkanImage createSnapshot(DepthSource source, int usage, int sampledViewAspect) {
            if (usage != SNAPSHOT_USAGE || sampledViewAspect != VK_IMAGE_ASPECT_DEPTH_BIT) {
                throw new IllegalArgumentException("Invalid sampled depth allocation contract");
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkImageFormatProperties properties = VkImageFormatProperties.calloc(stack);
                int result = vkGetPhysicalDeviceImageFormatProperties(Vulkan.getVkDevice().getPhysicalDevice(), source.format(),
                        VK_IMAGE_TYPE_2D, VK_IMAGE_TILING_OPTIMAL, usage, 0, properties);
                if (result != VK_SUCCESS || properties.maxExtent().width() < source.width()
                        || properties.maxExtent().height() < source.height() || (properties.sampleCounts() & VK_SAMPLE_COUNT_1_BIT) == 0) {
                    throw new IllegalStateException("Main depth format does not support a sampled transfer snapshot");
                }
            }
            return VulkanImage.createDepthSnapshot(source.format(), source.width(), source.height());
        }

        @Override public int currentLayout(VulkanImage image) { return image.getCurrentLayout(); }

        @Override public Scope suspend() {
            if (commandBuffer != null) throw new IllegalStateException("Main depth snapshot scope is already active");
            Renderer renderer = Renderer.getInstance();
            DefaultMainPass main = (DefaultMainPass) renderer.getMainPass();
            boolean viewportInitialized = Renderer.isViewportInitialized();
            int vx = Renderer.getViewportBox(0), vy = Renderer.getViewportBox(1);
            int vw = Renderer.getViewportBox(2), vh = Renderer.getViewportBox(3);
            boolean scissorEnabled = Renderer.isScissorEnabled();
            int sx = Renderer.getScissorBox(0), sy = Renderer.getScissorBox(1);
            int sw = Renderer.getScissorBox(2), sh = Renderer.getScissorBox(3);
            commandBuffer = Renderer.getCommandBuffer();
            try {
                renderer.endRenderPass(commandBuffer);
            } catch (RuntimeException | Error failure) {
                commandBuffer = null;
                throw failure;
            }
            return () -> {
                try {
                                                                                            
                    main.rebindMainTarget();
                    if (viewportInitialized) Renderer.setViewport(vx, vy, vw, vh);
                    Renderer.setScissor(sx, sy, sw, sh);
                    Renderer.setScissorEnabled(scissorEnabled);
                } finally {
                    commandBuffer = null;
                }
            };
        }

        @Override public void barrier(VulkanImage image, int oldLayout, int newLayout, int sourceStages, int sourceAccess,
                                      int destinationStages, int destinationAccess, int aspects) {
            if (commandBuffer == null || image.aspect != aspects || image.getCurrentLayout() != oldLayout) {
                throw new IllegalStateException("Main depth barrier is outside its tracked command scope");
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VulkanImage.transitionLayout(stack, commandBuffer, image, oldLayout, newLayout,
                        sourceStages, sourceAccess, destinationStages, destinationAccess);
            }
        }

        @Override public void copy(VulkanImage source, VulkanImage destination, int width, int height, int aspects) {
            if (commandBuffer == null || source.getId() == 0 || destination.getId() == 0) {
                throw new IllegalStateException("Main depth copy is outside its command scope or has unavailable images");
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkImageCopy.Buffer region = VkImageCopy.calloc(1, stack);
                region.srcSubresource().aspectMask(aspects).mipLevel(0).baseArrayLayer(0).layerCount(1);
                region.dstSubresource().aspectMask(aspects).mipLevel(0).baseArrayLayer(0).layerCount(1);
                region.extent().set(width, height, 1);
                vkCmdCopyImage(commandBuffer, source.getId(), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                        destination.getId(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);
            }
        }

        @Override public void retire(VulkanImage image) { image.free(); }
    }
}
