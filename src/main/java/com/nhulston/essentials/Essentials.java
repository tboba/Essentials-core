package com.nhulston.essentials;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class Essentials extends JavaPlugin {
    public Essentials(@Nonnull JavaPluginInit init) {
        super(init);
    }

    protected void setup() {
        getLogger().at(Level.INFO).log("Plugin is setting up...");
    }

    protected void start() {
        getLogger().at(Level.INFO).log("Plugin is starting...");
        getLogger().at(Level.INFO).log("Plugin started up!");
    }

    protected void shutdown() {
        getLogger().at(Level.INFO).log("Plugin is shutting down.");
    }
}
