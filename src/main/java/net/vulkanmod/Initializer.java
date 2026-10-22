package net.vulkanmod;

import net.minecraft.network.chat.Component;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.ModContainer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.Platform;
import net.vulkanmod.config.UpdateChecker;
import net.vulkanmod.config.gui.VOptionScreen;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.compat.CompatBootstrap;
import net.vulkanmod.compat.CompatReport;
import net.vulkanmod.compat.RuntimeOptions;
import net.vulkanmod.compat.sable.flywheel.VulkanFlywheelBootstrap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

@Mod("vulkanmod")
public class Initializer {
	public static final Logger LOGGER = LogManager.getLogger("VulkanMod");

	private static String VERSION = "0.3.5-ALPHA+1.21.1";
	public static Config CONFIG;

	static {

		try {
			Path configPath = Path.of("config", "vulkanmod_settings.json");
			Platform.init(Config.readWaylandPreference(configPath));
			VideoModeManager.init();
			CONFIG = loadConfig(configPath);
		} catch (Exception e) {
			CONFIG = new Config();
		}
	}

	public Initializer(IEventBus modEventBus, ModContainer modContainer) {
		VERSION = modContainer.getModInfo().getVersion().toString();

		VulkanFlywheelBootstrap.registerIfPresent();
		modEventBus.addListener(this::onInitializeClient);
		modContainer.registerExtensionPoint(IConfigScreenFactory.class,
				(java.util.function.Supplier<IConfigScreenFactory>) () ->
						(container, parent) -> new VOptionScreen(Component.literal("VulkanMod Settings"), parent));
	}

	private void onInitializeClient(FMLClientSetupEvent event) {
		LOGGER.info("== VulkanMod ==");
		UpdateChecker.checkForUpdates();
		event.enqueueWork(() -> {
			CompatBootstrap.init();
			if (RuntimeOptions.diagnosticsEnabled()) {
				CompatReport.logReport();
			}
			CompatReport.logRuntimeHints();
		});
	}

	private static Config loadConfig(Path path) {
		Config config = Config.load(path);

		if(config == null) {
			config = new Config();
			config.write();
		}

		return config;
	}

	public static String getVersion() {
		return VERSION;
	}
}
