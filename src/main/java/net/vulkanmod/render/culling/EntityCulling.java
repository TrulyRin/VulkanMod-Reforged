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

public final class EntityCulling {
    private static final byte UNKNOWN = -1;
    private static final byte HIDDEN = 0;
    private static final byte VISIBLE = 1;
    private static final int MAX_SECTIONS_TO_TEST = 32;
    private static final double MOB_CULL_DISTANCE = 96.0D;
    private static final double MISC_CULL_DISTANCE = 128.0D;

    private static short cachedFrame = Short.MIN_VALUE;
    private static final Long2ByteOpenHashMap sectionVisibilityCache = new Long2ByteOpenHashMap();

    static {
        sectionVisibilityCache.defaultReturnValue(UNKNOWN);
    }

    private EntityCulling() {
    }

    public static boolean isVisible(Entity entity, Frustum frustum, AABB aabb, double cameraX, double cameraY, double cameraZ) {
        if (!Initializer.CONFIG.entityCulling) {
            return frustum.isVisible(aabb);
        }

        if (!isVisible(frustum, aabb)) {
            return false;
        }

        return isWithinAggressiveDistance(entity, aabb, cameraX, cameraY, cameraZ);
    }

    public static boolean isVisible(Frustum frustum, AABB aabb) {
        if (!Initializer.CONFIG.entityCulling) {
            return frustum.isVisible(aabb);
        }

        if (!frustum.isVisible(aabb)) {
            return false;
        }

        WorldRenderer worldRenderer = WorldRenderer.getInstance();
        if (worldRenderer == null || worldRenderer.getSectionGrid() == null || aabb.hasNaN()) {
            return true;
        }

        int minSectionX = sectionCoord(aabb.minX);
        int maxSectionX = sectionCoord(aabb.maxX);
        int minSectionY = sectionCoord(aabb.minY);
        int maxSectionY = sectionCoord(aabb.maxY);
        int minSectionZ = sectionCoord(aabb.minZ);
        int maxSectionZ = sectionCoord(aabb.maxZ);

        int sectionCount = (maxSectionX - minSectionX + 1)
                * (maxSectionY - minSectionY + 1)
                * (maxSectionZ - minSectionZ + 1);
        if (sectionCount <= 0 || sectionCount > MAX_SECTIONS_TO_TEST) {
            return true;
        }

        short frame = worldRenderer.getLastFrame();
        resetCacheIfFrameChanged(frame);

        SectionGrid sectionGrid = worldRenderer.getSectionGrid();
        for (int sectionX = minSectionX; sectionX <= maxSectionX; sectionX++) {
            for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; sectionZ++) {
                    if (isSectionVisible(sectionGrid, frame, sectionX, sectionY, sectionZ)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isWithinAggressiveDistance(Entity entity, AABB aabb, double cameraX, double cameraY, double cameraZ) {
        if (entity.noCulling || isImportantEntity(entity)) {
            return true;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Entity cameraEntity = minecraft.getCameraEntity();
        if (entity == cameraEntity || entity.hasIndirectPassenger(cameraEntity)) {
            return true;
        }

        double limit = getAggressiveCullDistance(entity);
        double dx = (aabb.minX + aabb.maxX) * 0.5D - cameraX;
        double dy = (aabb.minY + aabb.maxY) * 0.5D - cameraY;
        double dz = (aabb.minZ + aabb.maxZ) * 0.5D - cameraZ;

        return dx * dx + dy * dy + dz * dz <= limit * limit;
    }

    private static boolean isImportantEntity(Entity entity) {
        return entity instanceof Player
                || entity instanceof Projectile
                || entity instanceof EnderDragon
                || entity instanceof WitherBoss
                || entity.isCurrentlyGlowing()
                || entity.hasCustomName()
                || entity.getBoundingBox().getSize() > 4.0D;
    }

    private static double getAggressiveCullDistance(Entity entity) {
        MobCategory category = entity.getType().getCategory();
        if (category == MobCategory.MISC) {
            return MISC_CULL_DISTANCE;
        }

        return MOB_CULL_DISTANCE;
    }

    private static int sectionCoord(double coordinate) {
        return SectionPos.blockToSectionCoord(Mth.floor(coordinate));
    }

    private static void resetCacheIfFrameChanged(short frame) {
        if (cachedFrame == frame) {
            return;
        }

        cachedFrame = frame;
        sectionVisibilityCache.clear();
    }

    private static boolean isSectionVisible(SectionGrid sectionGrid, short frame, int sectionX, int sectionY, int sectionZ) {
        long key = SectionPos.asLong(sectionX, sectionY, sectionZ);
        byte cached = sectionVisibilityCache.get(key);
        if (cached != UNKNOWN) {
            return cached == VISIBLE;
        }

        RenderSection section = sectionGrid.getSectionAtBlockPos(
                SectionPos.sectionToBlockCoord(sectionX),
                SectionPos.sectionToBlockCoord(sectionY),
                SectionPos.sectionToBlockCoord(sectionZ));
        boolean visible = section == null || section.getLastFrame() == frame;
        sectionVisibilityCache.put(key, visible ? VISIBLE : HIDDEN);

        return visible;
    }
}

