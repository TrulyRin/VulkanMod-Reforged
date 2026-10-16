package net.vulkanmod.render.gui;

import net.minecraft.client.gui.render.state.BlitRenderState;

                                                                                       
public final class OffscreenGuiBlitCoordinates {
    private OffscreenGuiBlitCoordinates() {}

    public static BlitRenderState toTopLeft(BlitRenderState state) {
        return new BlitRenderState(state.pipeline(), state.textureSetup(), state.pose(),
                state.x0(), state.y0(), state.x1(), state.y1(), state.u0(), state.u1(),
                1.0f - state.v0(), 1.0f - state.v1(), state.color(), state.scissorArea(), state.bounds());
    }
}
