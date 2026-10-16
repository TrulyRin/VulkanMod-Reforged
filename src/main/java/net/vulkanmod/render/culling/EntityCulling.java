package net.vulkanmod.render.culling;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.SectionGrid;
import net.vulkanmod.render.chunk.WorldRenderer;

import java.lang.ref.WeakReference;

                                                                                            
public final class EntityCulling {
    private static final byte UNKNOWN = -1;
    private static final byte HIDDEN = 0;
    private static final byte VISIBLE = 1;
    private static final int MAX_SECTIONS_TO_TEST = 32;
    private static final Long2ByteOpenHashMap SECTION_VISIBILITY = new Long2ByteOpenHashMap();
    private static short cachedFrame;
    private static WeakReference<SectionGrid> cachedGrid = new WeakReference<>(null);

    static { SECTION_VISIBILITY.defaultReturnValue(UNKNOWN); }

    private EntityCulling() { }

    public static boolean isVisible(Entity entity, Frustum frustum, AABB aabb,
                                    double cameraX, double cameraY, double cameraZ) {
        if (!frustum.isVisible(aabb)) {
            return false;
        }
        if (Initializer.CONFIG == null || !Initializer.CONFIG.entityCulling) {
            return true;
        }
        double dx = (aabb.minX + aabb.maxX) * 0.5 - cameraX;
        double dy = (aabb.minY + aabb.maxY) * 0.5 - cameraY;
        double dz = (aabb.minZ + aabb.maxZ) * 0.5 - cameraZ;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
                                                                                    
        if (Double.isFinite(distanceSquared) && distanceSquared > 96.0 * 96.0) {
            boolean miscellaneous = entity.getType().getCategory() == MobCategory.MISC;
            if (!EntityCullingPolicy.isWithinDistance(distanceSquared, miscellaneous, false)) {
                boolean important = entity instanceof Player || entity instanceof Projectile
                        || entity instanceof EnderDragon || entity instanceof WitherBoss
                        || entity.isCurrentlyGlowing() || entity.hasCustomName()
                        || entity.getBoundingBox().getSize() > 4.0;
                if (!important) {
                    Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
                    important = entity == cameraEntity
                            || (cameraEntity != null && entity.hasIndirectPassenger(cameraEntity));
                }
                if (!important) {
                    return false;
                }
            }
        }
        return isVisibleInGraph(aabb);
    }

    public static boolean isVisible(Frustum frustum, AABB aabb) {
        if (!frustum.isVisible(aabb)) {
            return false;
        }
        if (Initializer.CONFIG == null || !Initializer.CONFIG.entityCulling) {
            return true;
        }
        return isVisibleInGraph(aabb);
    }

    private static boolean isVisibleInGraph(AABB aabb) {
        if (aabb.hasNaN()) {
            return true;
        }
        WorldRenderer renderer = WorldRenderer.getInstance();
        if (renderer == null || renderer.getSectionGrid() == null) {
            return true;
        }
        if (!Double.isFinite(aabb.minX) || !Double.isFinite(aabb.maxX)
                || !Double.isFinite(aabb.minY) || !Double.isFinite(aabb.maxY)
                || !Double.isFinite(aabb.minZ) || !Double.isFinite(aabb.maxZ)) {
            return true;
        }
        int minX = SectionPos.blockToSectionCoord(Mth.floor(aabb.minX));
        int maxX = SectionPos.blockToSectionCoord(Mth.floor(aabb.maxX));
        int minY = SectionPos.blockToSectionCoord(Mth.floor(aabb.minY));
        int maxY = SectionPos.blockToSectionCoord(Mth.floor(aabb.maxY));
        int minZ = SectionPos.blockToSectionCoord(Mth.floor(aabb.minZ));
        int maxZ = SectionPos.blockToSectionCoord(Mth.floor(aabb.maxZ));
        long spanX = (long) maxX - minX + 1;
        long spanY = (long) maxY - minY + 1;
        long spanZ = (long) maxZ - minZ + 1;
        if (spanX <= 0 || spanY <= 0 || spanZ <= 0 || spanX > MAX_SECTIONS_TO_TEST
                || spanY > MAX_SECTIONS_TO_TEST || spanZ > MAX_SECTIONS_TO_TEST) {
            return true;
        }
        long sectionCount = spanX * spanY * spanZ;
        if (sectionCount <= 0 || sectionCount > MAX_SECTIONS_TO_TEST) {
            return true;
        }
        SectionGrid grid = renderer.getSectionGrid();
        short frame = renderer.getLastFrame();
        if (cachedGrid.get() != grid) {
            cachedGrid = new WeakReference<>(grid);
            cachedFrame = frame;
            SECTION_VISIBILITY.clear();
        } else if (cachedFrame != frame) {
            cachedFrame = frame;
            SECTION_VISIBILITY.clear();
        }
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (isSectionVisible(grid, frame, x, y, z)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isSectionVisible(SectionGrid grid, short frame, int x, int y, int z) {
        long key = SectionPos.asLong(x, y, z);
        byte cached = SECTION_VISIBILITY.get(key);
        if (cached != UNKNOWN) {
            return cached == VISIBLE;
        }
        RenderSection section = grid.getSectionAtBlockPos(
                SectionPos.sectionToBlockCoord(x), SectionPos.sectionToBlockCoord(y), SectionPos.sectionToBlockCoord(z));
        boolean originMatches = section != null && section.xOffset == SectionPos.sectionToBlockCoord(x)
                && section.yOffset == SectionPos.sectionToBlockCoord(y)
                && section.zOffset == SectionPos.sectionToBlockCoord(z);
        boolean visible = section == null || EntityCullingPolicy.isSectionVisible(originMatches, section.getLastFrame(), frame);
        SECTION_VISIBILITY.put(key, visible ? VISIBLE : HIDDEN);
        return visible;
    }
}
