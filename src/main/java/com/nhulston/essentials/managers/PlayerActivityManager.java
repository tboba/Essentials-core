package com.nhulston.essentials.managers;

import com.hypixel.hytale.server.core.universe.Universe;
import com.nhulston.essentials.models.PlayerData;
import com.nhulston.essentials.util.ConfigManager;
import com.nhulston.essentials.util.StorageManager;

import javax.annotation.Nonnull;
import java.util.UUID;

public class PlayerActivityManager {

    private final StorageManager storageManager;

    private final boolean isAfkKickEnabled;
    private final long kickThreshold;
    private final String kickMessage;

    public PlayerActivityManager(StorageManager storageManager, ConfigManager configManager) {
        this.storageManager = storageManager;

        isAfkKickEnabled = configManager.isAfkKickEnabled();
        kickMessage = configManager.getAfkKickMessage();
        kickThreshold = configManager.getAfkKickTime();
    }

    public void checkActivity(@Nonnull UUID playerUuid) {
        // If AFK feature is disabled, don't check activity.
        if (!isAfkKickEnabled) return;

        PlayerData playerData = storageManager.getPlayerData(playerUuid);
        long currentTime = System.currentTimeMillis();

        long playerLastActivity = playerData.getLastActivity();

        if (currentTime - playerLastActivity >= kickThreshold * 1000L) {
            // Mark player as AFK
            Universe.get().getPlayer(playerUuid).getPacketHandler().disconnect(kickMessage);
        }
    }

}
