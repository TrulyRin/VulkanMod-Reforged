package net.vulkanmod.compat.sable;

import java.util.EnumSet;
import java.util.Set;

public final class SableContractProbe {
    public static final String CERTIFIED_VERSION = "2.0.3";

    private SableContractProbe() {
    }

    public static Result evaluate(String version, Set<Contract> availableContracts) {
        boolean supportedLine = version != null && version.matches("2\\.0\\.\\d+(?:[-+].*)?");
        boolean complete = availableContracts != null
                && availableContracts.containsAll(EnumSet.allOf(Contract.class));
        boolean certified = CERTIFIED_VERSION.equals(version);
        EnumSet<Contract> captured = availableContracts == null || availableContracts.isEmpty()
                ? EnumSet.noneOf(Contract.class)
                : EnumSet.copyOf(availableContracts);
        return new Result(supportedLine && complete, certified && complete, captured);
    }

    public static Result inspect(String version, ClassLoader loader) {
        return inspectTypes(version,
                inspectType(loader, "dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher").orElse(null),
                inspectType(loader, "dev.ryanhcode.sable.sublevel.render.SubLevelRenderData").orElse(null),
                inspectType(loader, "dev.ryanhcode.sable.neoforge.mixinterface.compatibility.flywheel.EmbeddedEnvironmentExtension").orElse(null));
    }

    static Result inspectTypes(String version, Class<?> dispatcher, Class<?> renderData, Class<?> embedding) {
        EnumSet<Contract> contracts = EnumSet.noneOf(Contract.class);
        if (dispatcher != null) {
            contracts.add(Contract.DISPATCHER);
            if (hasMethod(dispatcher, "renderSectionLayer", 9)) contracts.add(Contract.SECTION_LAYER);
            if (hasMethod(dispatcher, "renderAfterSections", 7)) contracts.add(Contract.AFTER_SECTIONS);
            if (hasMethod(dispatcher, "renderBlockEntities", 6)) contracts.add(Contract.BLOCK_ENTITIES);
        }
        if (renderData != null) {
            contracts.add(Contract.RENDER_DATA);
            if (hasMethod(renderData, "getTransformation", 4)) contracts.add(Contract.TRANSFORMATION);
        }
        if (embedding != null && hasMethod(embedding, "sable$setLightingInfo", 3)) {
            contracts.add(Contract.FLYWHEEL_EMBEDDING);
        }
        return evaluate(version, contracts);
    }

    private static java.util.Optional<Class<?>> inspectType(ClassLoader loader, String name) {
        try {
            return java.util.Optional.of(Class.forName(name, false, loader));
        } catch (Throwable ignored) {
            return java.util.Optional.empty();
        }
    }

    private static boolean hasMethod(Class<?> type, String name, int parameterCount) {
        for (var method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                return true;
            }
        }
        return false;
    }

    public enum Contract {
        DISPATCHER,
        SECTION_LAYER,
        AFTER_SECTIONS,
        BLOCK_ENTITIES,
        RENDER_DATA,
        TRANSFORMATION,
        FLYWHEEL_EMBEDDING
    }

    public record Result(boolean compatible, boolean certified, Set<Contract> availableContracts) {
        public Result {
            availableContracts = Set.copyOf(availableContracts);
        }
    }
}
