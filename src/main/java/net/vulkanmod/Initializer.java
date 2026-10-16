package net.vulkanmod;

import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.Platform;
import net.vulkanmod.config.UpdateChecker;
import net.vulkanmod.config.gui.VOptionScreen;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.compat.CompatBootstrap;
import net.vulkanmod.compat.CompatReport;
import net.vulkanmod.compat.RuntimeOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

@Mod("vulkanmod")
public class Initializer {
	public static final Logger LOGGER = LogManager.getLogger("VulkanMod");

	private static String VERSION = "0.4.9-dev";
	public static Config CONFIG;

	static {

		try {
			Platform.init();
			VideoModeManager.init();
			Path configPath = Path.of("config", "vulkanmod_settings.json");
			CONFIG = loadConfig(configPath);
		} catch (Exception e) {
			CONFIG = new Config();
		}
	}

	public Initializer(IEventBus modEventBus, ModContainer modContainer) {
		VERSION = modContainer.getModInfo().getVersion().toString();
		modEventBus.addListener(this::onInitializeClient);
		modContainer.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
				() -> new ConfigScreenHandler.ConfigScreenFactory(
						(minecraft, parent) -> new VOptionScreen(Component.literal("VulkanMod Settings"), parent)));
	}

	private void onInitializeClient(FMLClientSetupEvent event) {
		LOGGER.info("== VulkanMod ==");
		UpdateChecker.checkForUpdates();
		CompatBootstrap.init();
		if (RuntimeOptions.diagnosticsEnabled()) {
			CompatReport.logReport();
		}
		CompatReport.logRuntimeHints();
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

