package net.vulkanmod.config;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

   
                                                                         
                                                                            
                      
   
public final class ConfigBootstrap {
    public static final String CONFIG_FILE_NAME = "vulkanmod_settings.json";

    private ConfigBootstrap() {
    }

    public static Config initialize(Path configDirectory,
                                    Consumer<Config> configBinder,
                                    Runnable platformInitializer) {
        Objects.requireNonNull(configDirectory, "configDirectory");
        Objects.requireNonNull(configBinder, "configBinder");
        Objects.requireNonNull(platformInitializer, "platformInitializer");

        Config config = Config.load(configDirectory.resolve(CONFIG_FILE_NAME));
        configBinder.accept(config);
        platformInitializer.run();
        return config;
    }
}
