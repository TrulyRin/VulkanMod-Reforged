package net.vulkanmod.vulkan;

import com.mojang.blaze3d.buffers.GpuFence;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.vulkanmod.Initializer;
import net.vulkanmod.gl.VkGlFramebuffer;
import net.vulkanmod.render.texture.ImageUploadHelper;
import net.vulkanmod.render.optimization.FramePerformanceMetrics;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.pass.DefaultMainPass;
import net.vulkanmod.vulkan.pass.MainPass;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.PipelineState;
import net.vulkanmod.vulkan.shader.Uniforms;
import net.vulkanmod.vulkan.shader.layout.PushConstants;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.util.VUtil;
import net.vulkanmod.vulkan.util.VkResult;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static net.vulkanmod.vulkan.Vulkan.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class Renderer {
    private static Renderer INSTANCE;

    private static VkDevice device;

    private static boolean swapChainUpdate = false;

    public static void initRenderer() {
        INSTANCE = new Renderer();
        INSTANCE.init();
    }

    public static Renderer getInstance() {
        return INSTANCE;
    }

    public static Drawer getDrawer() {
        return INSTANCE.drawer;
    }

    public static int getCurrentFrame() {
        return currentFrame;
    }

    public static int getCurrentImage() {
        return imageIndex;
    }

    private final Set<Pipeline> usedPipelines = new ObjectOpenHashSet<>();
    private Pipeline boundPipeline;
    private long boundPipelineHandle;

    private Drawer drawer;

    private SwapChain swapChain;

    private int framesNum;
    private List<VkCommandBuffer> mainCommandBuffers;
    private ArrayList<Long> imageAvailableSemaphores;
    private ArrayList<Long> renderFinishedSemaphores;
    private ArrayList<Long> inFlightFences;
    private List<CommandPool.CommandBuffer> transferCbs;

    private Framebuffer boundFramebuffer;
    private RenderPass boundRenderPass;

    private static int currentFrame = 0;
    private static int imageIndex;
    private static int lastReset = -1;
    private VkCommandBuffer currentCmdBuffer;
    private boolean recordingCmds = false;
    private boolean acquireWaitPending = false;
    private SubmissionFenceTracker submissionFences;
    int recursion = 0;

    MainPass mainPass;

    private final List<Runnable> onResizeCallbacks = new ObjectArrayList<>();

    public Renderer() {
        device = Vulkan.getVkDevice();
        framesNum = Initializer.CONFIG.frameQueueSize;
    }

    public static void setLineWidth(float width) {
        if (INSTANCE.boundFramebuffer == null) {
            return;
        }
        vkCmdSetLineWidth(INSTANCE.currentCmdBuffer, width);
    }

    private void init() {
        MemoryManager.createInstance(Renderer.getFramesNum());
        Vulkan.createStagingBuffers();
        net.vulkanmod.render.chunk.buffer.UploadManager.createInstance();

        swapChain = new SwapChain();
        mainPass = DefaultMainPass.create();

        drawer = new Drawer();
        drawer.createResources(framesNum);

        Uniforms.setupDefaultUniforms();

        allocateCommandBuffers();
        createSyncObjects();
    }

    private void allocateCommandBuffers() {
        if (mainCommandBuffers != null) {
            mainCommandBuffers.forEach(commandBuffer -> vkFreeCommandBuffers(device, Vulkan.getCommandPool(), commandBuffer));
        }

        mainCommandBuffers = new ArrayList<>(framesNum);

        try (MemoryStack stack = stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(getCommandPool());
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(framesNum);

            PointerBuffer pCommandBuffers = stack.mallocPointer(framesNum);

            int vkResult = vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers);
            if (vkResult != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate command buffers: %s".formatted(VkResult.decode(vkResult)));
            }

            for (int i = 0; i < framesNum; i++) {
                mainCommandBuffers.add(new VkCommandBuffer(pCommandBuffers.get(i), device));
            }
        }

        if (transferCbs != null) {
            transferCbs.forEach(commandBuffer -> {
                vkResetCommandBuffer(commandBuffer.handle, 0);
                commandBuffer.reset();
            });
        }

        transferCbs = new ArrayList<>(framesNum);

        for (int i = 0; i < framesNum; i++) {
            transferCbs.add(DeviceManager.getTransferQueue().getCommandPool().getCommandBuffer());
        }
    }

    private void createSyncObjects() {
                                                                                                      
                                                                                                              
        int swapChainImages = swapChain.getImagesNum();
        renderFinishedSemaphores = new ArrayList<>(swapChainImages);

        imageAvailableSemaphores = new ArrayList<>(framesNum);
        inFlightFences = new ArrayList<>(framesNum);

        try (MemoryStack stack = stackPush()) {
            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
            semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            LongBuffer pImageAvailableSemaphore = stack.mallocLong(1);
            LongBuffer pRenderFinishedSemaphore = stack.mallocLong(1);
            LongBuffer pFence = stack.mallocLong(1);

            for (int i = 0; i < framesNum; i++) {

                if (vkCreateSemaphore(device, semaphoreInfo, null, pImageAvailableSemaphore) != VK_SUCCESS
                    || vkCreateFence(device, fenceInfo, null, pFence) != VK_SUCCESS) {

                    throw new RuntimeException("Failed to create synchronization objects for the frame: " + i);
                }

                imageAvailableSemaphores.add(pImageAvailableSemaphore.get(0));
                inFlightFences.add(pFence.get(0));
            }

            for (int i = 0; i < swapChain.getImagesNum(); ++i) {
                if (vkCreateSemaphore(device, semaphoreInfo, null, pRenderFinishedSemaphore) != VK_SUCCESS) {

                    throw new RuntimeException("Failed to create synchronization objects for the image: " + i);
                }

                renderFinishedSemaphores.add(pRenderFinishedSemaphore.get(0));
            }
        }
    }

    public void preInitFrame() {

        drawer.resetBuffers(currentFrame);

    }

    public void beginFrame() {
        beginFrame(0);
    }

    private void beginFrame(int acquisitionRetries) {
        this.recursion++;

        if (swapChainUpdate && recursion <= 1) {
            recreateSwapChain();
            swapChainUpdate = false;
        }

                                                          
        if (this.recursion > 1) {
            this.endFrame();
        }


        long inFlightWaitStartedAt = System.nanoTime();
        long frameFence = inFlightFences.get(currentFrame);
        int fenceWaitResult = vkWaitForFences(device, frameFence, true, VUtil.UINT64_MAX);
        if (fenceWaitResult != VK_SUCCESS) {
            throw new IllegalStateException("Failed to wait for frame fence: " + VkResult.decode(fenceWaitResult));
        }
        submissionFences().completed(frameFence);
        FramePerformanceMetrics.INSTANCE.recordInFlightWait(System.nanoTime() - inFlightWaitStartedAt,
                Initializer.CONFIG != null && Initializer.CONFIG.adaptiveChunkUploads);


        submitUploads();

        MemoryManager.getInstance().initFrame(currentFrame);
        drawer.setCurrentFrame(currentFrame);
        Vulkan.getStagingBuffers().beginFrame(currentFrame);

        this.preInitFrame();

        resetDescriptors();

        currentCmdBuffer = mainCommandBuffers.get(currentFrame);
        vkResetCommandBuffer(currentCmdBuffer, 0);
        acquireWaitPending = false;
        swapChain.setAcquired(false);

        try (MemoryStack stack = stackPush()) {
                                                             
            if (swapChain.hasImages()) {
                IntBuffer pImageIndex = stack.mallocInt(1);
                long semaphore = imageAvailableSemaphores.get(currentFrame);

                long acquireStartedAt = System.nanoTime();
                int vkResult = vkAcquireNextImageKHR(device, swapChain.getId(), VUtil.UINT64_MAX,
                                                     semaphore, VK_NULL_HANDLE, pImageIndex);
                FramePerformanceMetrics.INSTANCE.recordAcquireWait(System.nanoTime() - acquireStartedAt, false);

                if (vkResult == VK_ERROR_OUT_OF_DATE_KHR) {
                                                                                   
                                                                                         
                    swapChainUpdate = true;
                    this.recursion--;
                    if (acquisitionRetries >= 2) {
                        throw new RuntimeException("Swapchain remained out of date after recreation");
                    }
                    this.beginFrame(acquisitionRetries + 1);
                    return;
                }
                if (vkResult == VK_SUBOPTIMAL_KHR || swapChainUpdate) {
                    swapChainUpdate = true;
                }
                else if (vkResult != VK_SUCCESS) {
                    throw new RuntimeException("Cannot acquire next swap chain image: %s".formatted(VkResult.decode(vkResult)));
                }

                imageIndex = pImageIndex.get(0);
                swapChain.setAcquired(true);
                acquireWaitPending = true;
            }

            this.beginMainRenderPass(stack);
        }

    }

    private void beginMainRenderPass(MemoryStack stack) {
        beginCommandBuffer(stack);
        mainPass.begin(currentCmdBuffer, stack);
    }

    private void beginCommandBuffer(MemoryStack stack) {
        VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
        beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
        beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

        VkCommandBuffer commandBuffer = currentCmdBuffer;

        int vkResult = vkBeginCommandBuffer(commandBuffer, beginInfo);
        if (vkResult != VK_SUCCESS) {
            throw new RuntimeException("Failed to begin recording command buffer: %s".formatted(VkResult.decode(vkResult)));
        }

        recordingCmds = true;
                                                                                  
                                                                                  
        boundPipeline = null;
        boundPipelineHandle = 0;

        resetDynamicState(commandBuffer);
    }

    public void endFrame() {
        if (!recordingCmds)
            return;

        if (this.recursion == 0) {
            return;
        }
        this.recursion--;


        mainPass.end(currentCmdBuffer);

        submitUploads();
        waitFences();

        submitFrame();
        recordingCmds = false;
        this.boundRenderPass = null;
        this.boundFramebuffer = null;

        FramePerformanceMetrics.INSTANCE.finishFrame(Initializer.CONFIG != null && Initializer.CONFIG.adaptiveChunkUploads);

    }

    private void submitFrame() {
        try (MemoryStack stack = stackPush()) {
            int vkResult;

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

            populateSubmissionWaits(stack, submitInfo);
            submitInfo.pCommandBuffers(stack.pointers(currentCmdBuffer));

            if (swapChain.isAcquired()) {
                submitInfo.pSignalSemaphores(stack.longs(renderFinishedSemaphores.get(imageIndex)));
            }

            long frameFence = inFlightFences.get(currentFrame);
            submissionFences().requireRetiredGeneration(frameFence);
            vkResetFences(device, frameFence);

            if ((vkResult = vkQueueSubmit(DeviceManager.getGraphicsQueue().vkQueue(), submitInfo, frameFence)) != VK_SUCCESS) {
                vkResetFences(device, frameFence);
                throw new RuntimeException("Failed to submit draw command buffer: %s".formatted(VkResult.decode(vkResult)));
            }
            submissionFences().submitted(device, frameFence);
            acquireWaitPending = false;

                                                                                                             
            Synchronization.INSTANCE.scheduleCbReset();

            if (swapChain.isAcquired()) {
                VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
                presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);

                presentInfo.pWaitSemaphores(stack.longs(renderFinishedSemaphores.get(imageIndex)));

                presentInfo.swapchainCount(1);
                presentInfo.pSwapchains(stack.longs(swapChain.getId()));

                presentInfo.pImageIndices(stack.ints(imageIndex));

                long presentStartedAt = System.nanoTime();
                vkResult = vkQueuePresentKHR(DeviceManager.getPresentQueue().vkQueue(), presentInfo);
                FramePerformanceMetrics.INSTANCE.recordPresentWait(System.nanoTime() - presentStartedAt, false);

                if (vkResult == VK_ERROR_OUT_OF_DATE_KHR || vkResult == VK_SUBOPTIMAL_KHR || swapChainUpdate) {
                    swapChainUpdate = true;
                    return;
                } else if (vkResult != VK_SUCCESS) {
                    throw new RuntimeException("Failed to present rendered frame: %s".formatted(VkResult.decode(vkResult)));
                }
            }

            Vulkan.getStagingBuffers().endFrame(currentFrame);

            currentFrame = (currentFrame + 1) % framesNum;
            swapChain.setAcquired(false);
        }
    }

       
                                                                         
       
    public void flushCmds() {
        if (!this.recordingCmds)
            return;

        try (MemoryStack stack = stackPush()) {
            int vkResult;

            this.endRenderPass(currentCmdBuffer);
            vkEndCommandBuffer(currentCmdBuffer);

                                                                                  
                                                                                      
            submitUploads();
            waitFences();

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

            submitInfo.pCommandBuffers(stack.pointers(currentCmdBuffer));

            populateSubmissionWaits(stack, submitInfo);

            long frameFence = inFlightFences.get(currentFrame);
            submissionFences().requireRetiredGeneration(frameFence);
            vkResetFences(device, frameFence);
            if ((vkResult = vkQueueSubmit(DeviceManager.getGraphicsQueue().vkQueue(), submitInfo, frameFence)) != VK_SUCCESS) {
                vkResetFences(device, frameFence);
                throw new RuntimeException("Failed to submit draw command buffer: %s".formatted(VkResult.decode(vkResult)));
            }
            submissionFences().submitted(device, frameFence);
            acquireWaitPending = false;

            int fenceWaitResult = vkWaitForFences(device, frameFence, true, VUtil.UINT64_MAX);
            if (fenceWaitResult != VK_SUCCESS) {
                throw new IllegalStateException("Failed to wait for flushed commands: " + VkResult.decode(fenceWaitResult));
            }
            submissionFences().completed(frameFence);

                                                                                
                                                                             
                                                               
            this.beginCommandBuffer(stack);
        }
    }

    private void populateSubmissionWaits(MemoryStack stack, VkSubmitInfo submitInfo) {
        int uploadWaits = Synchronization.INSTANCE.getWaitSemaphoreCount();
        boolean waitForAcquisition = acquireWaitPending && swapChain.isAcquired();
        int total = uploadWaits + (waitForAcquisition ? 1 : 0);
        LongBuffer semaphores = stack.mallocLong(total);
        IntBuffer stages = stack.mallocInt(total);
        Synchronization.INSTANCE.getWaitSemaphores(semaphores);
        for (int i = 0; i < uploadWaits; i++) {
            stages.put(i, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT);
        }
        if (waitForAcquisition) {
            semaphores.put(uploadWaits, imageAvailableSemaphores.get(currentFrame));
                                                                               
                                                                                      
            stages.put(uploadWaits, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT);
        }
        semaphores.position(0).limit(total);
        submitInfo.pWaitSemaphores(semaphores).waitSemaphoreCount(total).pWaitDstStageMask(stages);
    }

    public void submitUploads() {
        var transferCb = transferCbs.get(currentFrame);

        if (transferCb.isRecording()) {
            final var transferQueue = DeviceManager.getTransferQueue();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                transferCb.submitCommands(stack, transferQueue.vkQueue(), true);
            }

            Synchronization.INSTANCE.addCommandBuffer(transferCb, true);

            transferCbs.set(currentFrame, transferQueue.getCommandPool().getCommandBuffer());
        }

        ImageUploadHelper.INSTANCE.submitCommands();
        if (net.vulkanmod.render.chunk.buffer.UploadManager.INSTANCE != null) {
            net.vulkanmod.render.chunk.buffer.UploadManager.INSTANCE.submitUploads();
        }
    }

    private SubmissionFenceTracker submissionFences() {
        if (this.submissionFences == null) {
            this.submissionFences = new SubmissionFenceTracker();
        }
        return this.submissionFences;
    }

    public GpuFence createGpuFence(Runnable blockingCompletion) {
        boolean pendingUploads = transferCbs != null && currentFrame < transferCbs.size()
                && transferCbs.get(currentFrame).isRecording();
        pendingUploads |= ImageUploadHelper.INSTANCE.getCommandBuffer() != null;
        pendingUploads |= net.vulkanmod.render.chunk.buffer.UploadManager.INSTANCE != null
                && net.vulkanmod.render.chunk.buffer.UploadManager.INSTANCE.hasPendingUploads();
        pendingUploads |= Synchronization.INSTANCE.hasPendingFences()
                || Synchronization.INSTANCE.getWaitSemaphoreCount() != 0;
        return submissionFences().createFence(this.recordingCmds || pendingUploads, blockingCompletion);
    }

    public void completeGpuSubmissions() {
                                                                                            
        submissionFences().completeAll();
    }

    public void endRenderPass() {
        endRenderPass(currentCmdBuffer);
    }

    public void endRenderPass(VkCommandBuffer commandBuffer) {
        if (!recordingCmds || this.boundFramebuffer == null)
            return;

        this.boundRenderPass.endRenderPass(commandBuffer);

        this.boundRenderPass = null;
        this.boundFramebuffer = null;

        VkGlFramebuffer.resetBoundFramebuffer();
    }

    public boolean beginRenderPass(RenderPass renderPass, Framebuffer framebuffer) {
        if (!recordingCmds) {
            this.beginFrame();

            recordingCmds = true;
        }

        if (this.boundFramebuffer != framebuffer) {
            this.endRenderPass(currentCmdBuffer);

            try (MemoryStack stack = stackPush()) {
                framebuffer.beginRenderPass(currentCmdBuffer, renderPass, stack);
            }

            this.boundFramebuffer = framebuffer;
            this.boundRenderPass = renderPass;

            Renderer.setViewportState(0, 0, framebuffer.getWidth(), framebuffer.getHeight());
            Renderer.setScissor(0, 0, framebuffer.getWidth(), framebuffer.getHeight());
        }

        return true;
    }

    public void addUsedPipeline(Pipeline pipeline) {
        usedPipelines.add(pipeline);
    }

    public void removeUsedPipeline(Pipeline pipeline) {
        usedPipelines.remove(pipeline);
    }

    private void waitFences() {
                                                               
        long transferWaitStartedAt = System.nanoTime();
        Synchronization.INSTANCE.waitFences();
        FramePerformanceMetrics.INSTANCE.recordTransferWait(System.nanoTime() - transferWaitStartedAt,
                Initializer.CONFIG != null && Initializer.CONFIG.adaptiveChunkUploads);
        Vulkan.getStagingBuffer().reset();
    }

    private void resetDescriptors() {
        for (Pipeline pipeline : usedPipelines) {
            pipeline.resetDescriptorPool(currentFrame);
        }

        usedPipelines.clear();
        boundPipeline = null;
        boundPipelineHandle = 0;
    }

    void waitForSwapChain() {
        vkResetFences(device, inFlightFences.get(currentFrame));

                                                                                         
        try (MemoryStack stack = MemoryStack.stackPush()) {
                          
            VkSubmitInfo info = VkSubmitInfo.calloc(stack)
                                            .sType$Default()
                                            .pWaitSemaphores(stack.longs(imageAvailableSemaphores.get(currentFrame)))
                                            .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_ALL_COMMANDS_BIT));

            vkQueueSubmit(DeviceManager.getGraphicsQueue().vkQueue(), info, inFlightFences.get(currentFrame));
            vkWaitForFences(device, inFlightFences.get(currentFrame), true, -1);
        }
    }

    @SuppressWarnings("UnreachableCode")
    private void recreateSwapChain() {
        submitUploads();
        waitFences();
        Vulkan.waitIdle();
        completeGpuSubmissions();

        mainCommandBuffers.forEach(commandBuffer -> vkResetCommandBuffer(commandBuffer, 0));
        recordingCmds = false;

        swapChain.recreate();

                                                                          
        destroySyncObjects();

        int newFramesNum = Initializer.CONFIG.frameQueueSize;

        if (framesNum != newFramesNum) {
    
            framesNum = newFramesNum;
            MemoryManager.getInstance().freeAllBuffers();
            MemoryManager.createInstance(newFramesNum);
            createStagingBuffers();
            allocateCommandBuffers();

            Pipeline.recreateDescriptorSets(framesNum);

            drawer.createResources(framesNum);
        }

        createSyncObjects();
        this.mainPass.onResize();

        this.onResizeCallbacks.forEach(Runnable::run);
                                                                              
                                                                             

        currentFrame = 0;
    }

    public void cleanUpResources() {
                                                                                           
        completeGpuSubmissions();
        destroySyncObjects();

        drawer.cleanUpResources();
        mainPass.cleanUp();
        swapChain.cleanUp();

        VTextureSelector.getWhiteTexture().free();
    }

    private void destroySyncObjects() {
                                                                            
                                                                                
        if (this.submissionFences != null) {
            this.submissionFences.retireNativeFences();
        }
        inFlightFences.forEach(fence -> vkDestroyFence(device, fence, null));
        imageAvailableSemaphores.forEach(semaphore -> vkDestroySemaphore(device, semaphore, null));
        renderFinishedSemaphores.forEach(semaphore -> vkDestroySemaphore(device, semaphore, null));
        inFlightFences.clear();
        imageAvailableSemaphores.clear();
        renderFinishedSemaphores.clear();
    }

    public void addOnResizeCallback(Runnable runnable) {
        this.onResizeCallbacks.add(runnable);
    }

    public void bindGraphicsPipeline(GraphicsPipeline pipeline) {
        VkCommandBuffer commandBuffer = currentCmdBuffer;

        PipelineState currentState = PipelineState.getCurrentPipelineState(boundRenderPass);
        final long handle = pipeline.getHandle(currentState);

        if (boundPipelineHandle == handle) {
            return;
        }

        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, handle);
        boundPipelineHandle = handle;
        boundPipeline = pipeline;
        addUsedPipeline(pipeline);
    }

    public void uploadAndBindUBOs(Pipeline pipeline) {
        VkCommandBuffer commandBuffer = currentCmdBuffer;
        pipeline.bindDescriptorSets(commandBuffer, currentFrame);
    }

    public void pushConstants(Pipeline pipeline) {
        VkCommandBuffer commandBuffer = currentCmdBuffer;

        PushConstants pushConstants = pipeline.getPushConstants();

        try (MemoryStack stack = stackPush()) {
            ByteBuffer buffer = stack.malloc(pushConstants.getSize());
            long ptr = MemoryUtil.memAddress0(buffer);
            pushConstants.update(ptr);

            nvkCmdPushConstants(commandBuffer, pipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstants.getSize(), ptr);
        }

    }

    public Pipeline getBoundPipeline() {
        return boundPipeline;
    }

    public void setBoundFramebuffer(Framebuffer framebuffer) {
        this.boundFramebuffer = framebuffer;
    }

    public Framebuffer getBoundFramebuffer() {
        return boundFramebuffer;
    }

    public void setBoundRenderPass(RenderPass boundRenderPass) {
        this.boundRenderPass = boundRenderPass;
    }

    public RenderPass getBoundRenderPass() {
        return boundRenderPass;
    }

    public void setMainPass(MainPass mainPass) {
        this.mainPass = mainPass;
    }

    public MainPass getMainPass() {
        return this.mainPass;
    }

    public SwapChain getSwapChain() {
        return swapChain;
    }

    public CommandPool.CommandBuffer getTransferCb() {
        return transferCbs.get(currentFrame);
    }

    private static void resetDynamicState(VkCommandBuffer commandBuffer) {
        vkCmdSetDepthBias(commandBuffer, 0.0F, 0.0F, 0.0F);

        vkCmdSetLineWidth(commandBuffer, 1.0F);
    }

    public static void setDepthBias(float constant, float slope) {
        VkCommandBuffer commandBuffer = INSTANCE.currentCmdBuffer;

        vkCmdSetDepthBias(commandBuffer, constant, 0.0f, slope);
    }

    public static void clearAttachments(int attachments) {
        clearAttachments(INSTANCE.currentCmdBuffer, attachments);
    }

    public static void clearAttachments(VkCommandBuffer commandBuffer, int attachments) {
        Framebuffer framebuffer = Renderer.getInstance().boundFramebuffer;
        if (framebuffer == null)
            return;

        clearAttachments(commandBuffer, attachments, framebuffer.getWidth(), framebuffer.getHeight());
    }

    public static void clearAttachments(int attachments, int width, int height) {
        clearAttachments(INSTANCE.currentCmdBuffer, attachments, width , height);
    }

    public static void clearAttachments(int attachments, int x, int y, int width, int height) {
        clearAttachments(INSTANCE.currentCmdBuffer, attachments, x, y, width , height);
    }

    public static void clearAttachments(VkCommandBuffer commandBuffer, int attachments, int width, int height) {
        clearAttachments(commandBuffer, attachments, 0, 0, width, height);
    }

    public static void clearAttachments(VkCommandBuffer commandBuffer, int attachments, int x, int y, int width, int height) {
        try (MemoryStack stack = stackPush()) {
                                                                            
                                                                                          
            VkClearValue colorValue = VkClearValue.calloc(stack);
            colorValue.color().float32(VRenderSystem.clearColor);

            VkClearValue depthValue = VkClearValue.calloc(stack);
            depthValue.depthStencil().set(VRenderSystem.clearDepthValue, 0);                                    

            int attachmentsCount = attachments == (GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT) ? 2 : 1;
            final VkClearAttachment.Buffer pAttachments = VkClearAttachment.malloc(attachmentsCount, stack);
            switch (attachments) {
                case GL_DEPTH_BUFFER_BIT -> {

                    VkClearAttachment clearDepth = pAttachments.get(0);
                    clearDepth.aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);
                    clearDepth.colorAttachment(0);
                    clearDepth.clearValue(depthValue);
                }
                case GL_COLOR_BUFFER_BIT -> {

                    VkClearAttachment clearColor = pAttachments.get(0);
                    clearColor.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                    clearColor.colorAttachment(0);
                    clearColor.clearValue(colorValue);
                }
                case GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT -> {

                    VkClearAttachment clearColor = pAttachments.get(0);
                    clearColor.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                    clearColor.colorAttachment(0);
                    clearColor.clearValue(colorValue);

                    VkClearAttachment clearDepth = pAttachments.get(1);
                    clearDepth.aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);
                    clearDepth.colorAttachment(0);
                    clearDepth.clearValue(depthValue);
                }
                default -> throw new RuntimeException("unexpected value");
            }

                           
            VkRect2D renderArea = VkRect2D.malloc(stack);
            renderArea.offset().set(x, y);
            renderArea.extent().set(width, height);

            VkClearRect.Buffer pRect = VkClearRect.malloc(1, stack);
            pRect.rect(renderArea);
            pRect.baseArrayLayer(0);
            pRect.layerCount(1);

            vkCmdClearAttachments(commandBuffer, pAttachments, pRect);
        }
    }

    public static void setInvertedViewport(int x, int y, int width, int height) {
        setViewportState(x, y + height, width, -height);
    }

    public static void resetViewport() {
        Framebuffer framebuffer = INSTANCE.getMainPass().getMainFramebuffer();

        if (framebuffer == null) {
            return;
        }

        int width = framebuffer.getWidth();
        int height = framebuffer.getHeight();

        if (width > 0 && height > 0) {
            setViewportState(0, 0, width, height);
        }
    }

    public static void setViewportState(int x, int y, int width, int height) {
        setViewport(x, y, width, height);
    }

    public static void setViewport(int x, int y, int width, int height) {
        try (MemoryStack stack = stackPush()) {
            setViewport(x, y, width, height, stack);
        }
    }

    public static void setViewport(int x, int y, int width, int height, MemoryStack stack) {
        if (!INSTANCE.recordingCmds)
            return;

        VkViewport.Buffer viewport = VkViewport.malloc(1, stack);
        viewport.x(x);
        viewport.y(height + y);
        viewport.width(width);
        viewport.height(-height);
        viewport.minDepth(0.0f);
        viewport.maxDepth(1.0f);

        vkCmdSetViewport(INSTANCE.currentCmdBuffer, 0, viewport);
    }

    public static void setScissor(int x, int y, int width, int height) {
        if (!INSTANCE.recordingCmds || INSTANCE.boundFramebuffer == null)
            return;

        try (MemoryStack stack = stackPush()) {
            Framebuffer framebuffer = INSTANCE.boundFramebuffer;
            int framebufferWidth = framebuffer.getWidth();
            int framebufferHeight = framebuffer.getHeight();

                                                                                              
                                                                                             
                                                                           
            int left = Math.max(0, x);
            int bottom = Math.max(0, y);
            int right = Math.min(framebufferWidth, x + Math.max(0, width));
            int top = Math.min(framebufferHeight, y + Math.max(0, height));
            int scissorWidth = Math.max(0, right - left);
            int scissorHeight = Math.max(0, top - bottom);

            VkRect2D.Buffer scissor = VkRect2D.malloc(1, stack);
            scissor.offset().set(left, framebufferHeight - top);
            scissor.extent().set(scissorWidth, scissorHeight);

            vkCmdSetScissor(INSTANCE.currentCmdBuffer, 0, scissor);
        }
    }

    public static void resetScissor() {
        if (INSTANCE.boundFramebuffer == null)
            return;

        try (MemoryStack stack = stackPush()) {
            VkRect2D.Buffer scissor = INSTANCE.boundFramebuffer.scissor(stack);
            vkCmdSetScissor(INSTANCE.currentCmdBuffer, 0, scissor);
        }
    }

    public static void pushDebugSection(String s) {
        if (Vulkan.ENABLE_VALIDATION_LAYERS) {
            VkCommandBuffer commandBuffer = INSTANCE.currentCmdBuffer;

            try (MemoryStack stack = stackPush()) {
                VkDebugUtilsLabelEXT markerInfo = VkDebugUtilsLabelEXT.calloc(stack);
                markerInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_LABEL_EXT);
                ByteBuffer string = stack.UTF8(s);
                markerInfo.pLabelName(string);
                vkCmdBeginDebugUtilsLabelEXT(commandBuffer, markerInfo);
            }
        }
    }

    public static void popDebugSection() {
        if (Vulkan.ENABLE_VALIDATION_LAYERS) {
            VkCommandBuffer commandBuffer = INSTANCE.currentCmdBuffer;

            vkCmdEndDebugUtilsLabelEXT(commandBuffer);
        }
    }

    public static void popPushDebugSection(String s) {
        popDebugSection();
        pushDebugSection(s);
    }

    public static int getFramesNum() {
        return INSTANCE.framesNum;
    }

    public static VkCommandBuffer getCommandBuffer() {
        return INSTANCE.currentCmdBuffer;
    }

    public static boolean isRecording() {
        return INSTANCE.recordingCmds;
    }

    public static void scheduleSwapChainUpdate() {
        swapChainUpdate = true;
    }
}
