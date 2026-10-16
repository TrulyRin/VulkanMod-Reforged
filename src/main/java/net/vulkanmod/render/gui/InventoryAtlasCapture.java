package net.vulkanmod.render.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

                                                                                                
public final class InventoryAtlasCapture<T> {
    public interface Readback<T> {
        byte[] read(T atlas, int width, int height) throws Exception;
    }

    private static final int MAX_DRAWS = 64;
    private static final int MAX_TEXT_LENGTH = 160;
    private static final int MAX_PIXEL_BYTES = 64 * 1024 * 1024;
    private static final Gson JSON = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

    private final boolean enabled;
    private final Path directory;
    private final Readback<T> readback;
    private final List<Draw> draws = new ArrayList<>();
    private boolean collecting;
    private boolean attempted;
    private int itemModelCount;
    private int totalDraws;
    private int completedDraws;
    private int nonTransparentPixels;
    private String failure;

    public InventoryAtlasCapture(boolean enabled, Path directory, Readback<T> readback) {
        this.enabled = enabled;
        this.directory = directory;
        this.readback = readback;
    }

    public boolean beginPass(int itemModelCount) {
        if (!this.enabled || this.attempted || this.collecting || itemModelCount <= 0) return false;
        this.collecting = true;
        this.itemModelCount = itemModelCount;
        this.totalDraws = 0;
        this.completedDraws = 0;
        this.draws.clear();
        return true;
    }

    public boolean isCollecting() { return this.collecting; }
    public boolean hasAttempted() { return this.attempted; }
    public int nonTransparentPixels() { return this.nonTransparentPixels; }
    public String failure() { return this.failure; }

    public void recordDraw(String pipeline, int vertexCount, int indexCount, int[] scissor,
                           float[] vertexBounds, String projectionBuffer, boolean completed) {
        if (!this.collecting) return;
        this.totalDraws++;
        if (completed) this.completedDraws++;
        if (this.draws.size() < MAX_DRAWS) {
            this.draws.add(new Draw(sanitize(pipeline), vertexCount, indexCount,
                    scissor == null ? new int[0] : scissor.clone(),
                    vertexBounds == null ? new float[0] : vertexBounds.clone(),
                    sanitize(projectionBuffer), completed));
        }
    }

    public boolean completePass(T atlas, int width, int height) {
        if (!this.enabled || !this.collecting || this.attempted) return false;
        this.collecting = false;
        if (atlas == null || width <= 0 || height <= 0) return false;
                                                                                          
        this.attempted = true;
        Report report;
        try {
            int byteCount = Math.multiplyExact(Math.multiplyExact(width, height), 4);
            if (byteCount > MAX_PIXEL_BYTES) throw new IllegalArgumentException("RGBA atlas exceeds diagnostic size limit");
            byte[] rgba = this.readback.read(atlas, width, height);
            if (rgba == null || rgba.length != byteCount) {
                throw new IllegalArgumentException("RGBA readback length does not match atlas dimensions");
            }
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int minX = width, minY = height, maxX = -1, maxY = -1;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int offset = (y * width + x) * 4;
                    int red = Byte.toUnsignedInt(rgba[offset]);
                    int green = Byte.toUnsignedInt(rgba[offset + 1]);
                    int blue = Byte.toUnsignedInt(rgba[offset + 2]);
                    int alpha = Byte.toUnsignedInt(rgba[offset + 3]);
                    image.setRGB(x, y, alpha << 24 | red << 16 | green << 8 | blue);
                    if (alpha != 0) {
                        this.nonTransparentPixels++;
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }
                }
            }
            int[] bounds = this.nonTransparentPixels == 0 ? null : new int[]{minX, minY, maxX + 1, maxY + 1};
            report = this.report(width, height, bounds, null);
            Files.createDirectories(this.directory);
            if (!ImageIO.write(image, "PNG", this.directory.resolve("inventory-atlas.png").toFile())) {
                throw new IllegalStateException("PNG writer unavailable");
            }
            Files.writeString(this.directory.resolve("inventory-atlas.json"), JSON.toJson(report));
            return true;
        } catch (Exception exception) {
            this.failure = sanitize(exception.getClass().getSimpleName() + ": " + exception.getMessage());
            try {
                Files.createDirectories(this.directory);
                Files.writeString(this.directory.resolve("inventory-atlas.json"),
                        JSON.toJson(this.report(width, height, null, this.failure)));
            } catch (Exception writeFailure) {
                this.failure = sanitize(this.failure + "; report: " + writeFailure.getClass().getSimpleName());
            }
            return false;
        }
    }

    private Report report(int width, int height, int[] bounds, String error) {
        return new Report(width, height, this.itemModelCount, this.totalDraws, this.completedDraws,
                this.nonTransparentPixels, bounds, List.copyOf(this.draws), error);
    }

    private static String sanitize(String text) {
        if (text == null) return "";
        String clean = text.replaceAll("\\p{Cntrl}", " ");
        return clean.substring(0, Math.min(clean.length(), MAX_TEXT_LENGTH));
    }

    private record Draw(String pipeline, int vertexCount, int indexCount, int[] scissor,
                        float[] vertexBounds, String projectionBuffer, boolean completed) {}
    private record Report(int width, int height, int itemModelCount, int totalDraws, int completedDraws,
                          int nonTransparentPixels, int[] nonTransparentBounds, List<Draw> draws, String error) {}
}
