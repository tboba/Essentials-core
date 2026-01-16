package com.nhulston.essentials.tasks;

import com.hypixel.hytale.server.core.universe.Universe;
import com.nhulston.essentials.managers.PlayerActivityManager;

public class PlayerActivityTask implements Runnable {

    private final PlayerActivityManager playerActivityManager;

    public PlayerActivityTask(PlayerActivityManager playerActivityManager) {
        this.playerActivityManager = playerActivityManager;
    }

    @Override
    public void run() {
        for (var player : Universe.get().getPlayers()) {
            if (player.isValid()) {
                playerActivityManager.checkActivity(player.getUuid());
            }
        }
    }
}
