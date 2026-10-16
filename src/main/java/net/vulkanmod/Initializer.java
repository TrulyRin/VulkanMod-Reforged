package net.vulkanmod;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.ModContainer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.ConfigBootstrap;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.VOptionScreen;
import net.vulkanmod.config.Platform;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.compat.CompatBootstrap;
import net.vulkanmod.compat.CompatReport;
import net.vulkanmod.compat.CompatibilityGuard;
import net.vulkanmod.compat.RuntimeOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("vulkanmod")
public class Initializer {
	public static final Logger LOGGER = LogManager.getLogger("VulkanMod");

	private static String VERSION = "0.4.9-dev";
	public static Config CONFIG;

	static {
		try {
			net.vulkanmod.vulkan.LwjglStackSizeConfigurator.configureSystemPropertyForVulkan();
			ConfigBootstrap.initialize(FMLPaths.CONFIGDIR.get(), config -> CONFIG = config, Platform::init);
		} catch (Exception e) {
			LOGGER.error("Failed to initialize VulkanMod configuration; using in-memory defaults", e);
			CONFIG = Config.defaults(FMLPaths.CONFIGDIR.get().resolve(ConfigBootstrap.CONFIG_FILE_NAME));
			Platform.init();
		}
	}

	public Initializer(IEventBus modEventBus, ModContainer modContainer) {
		CompatibilityGuard.failIfIncompatible();
		VERSION = modContainer.getModInfo().getVersion().toString();
		modEventBus.addListener(this::onInitializeClient);
		modEventBus.addListener(net.vulkanmod.diagnostics.VulkanDebugEntry::register);

		modContainer.registerExtensionPoint(IConfigScreenFactory.class,
				(java.util.function.Supplier<IConfigScreenFactory>) () ->
						(container, parent) -> new VOptionScreen(Component.literal("VulkanMod Settings"), parent));
	}

	private void onInitializeClient(FMLClientSetupEvent event) {
		LOGGER.info("== VulkanMod ==");
		event.enqueueWork(() -> {
			CompatBootstrap.init();
			if (RuntimeOptions.diagnosticsEnabled()) {
				CompatReport.logReport();
			}
			CompatReport.logRuntimeHints();
		});
	}

	public static String getVersion() {
		return VERSION;
	}
}
