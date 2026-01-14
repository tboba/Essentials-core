package com.nhulston.essentials.commands.home;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.managers.HomeManager;
import com.nhulston.essentials.managers.TeleportManager;
import com.nhulston.essentials.models.Home;
import com.nhulston.essentials.util.Msg;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;

public class HomeCommand extends AbstractPlayerCommand {
    private final HomeManager homeManager;
    private final TeleportManager teleportManager;

    public HomeCommand(@Nonnull HomeManager homeManager, @Nonnull TeleportManager teleportManager) {
        super("home", "Teleport to your home");
        this.homeManager = homeManager;
        this.teleportManager = teleportManager;

        requirePermission("essentials.home");
        addUsageVariant(new HomeNamedCommand(homeManager, teleportManager));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World currentWorld) {
        UUID playerUuid = playerRef.getUuid();
        Map<String, Home> homes = homeManager.getHomes(playerUuid);

        if (homes.isEmpty()) {
            Msg.fail(context, "You don't have any homes set. Use /sethome to set one.");
            return;
        }

        if (homes.size() == 1) {
            String homeName = homes.keySet().iterator().next();
            doTeleportToHome(context, store, ref, playerRef, homeName, homeManager, teleportManager);
        } else {
            Msg.prefix(context, "Homes", String.join(", ", homes.keySet()));
        }
    }

    static void doTeleportToHome(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                                 @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef,
                                 @Nonnull String homeName, @Nonnull HomeManager homeManager,
                                 @Nonnull TeleportManager teleportManager) {
        Home home = homeManager.getHome(playerRef.getUuid(), homeName);
        if (home == null) {
            Msg.fail(context, "Home '" + homeName + "' not found.");
            return;
        }

        Vector3d startPosition = playerRef.getTransform().getPosition();
        
        teleportManager.queueTeleport(
            playerRef, ref, store, startPosition,
            home.getWorld(), home.getX(), home.getY(), home.getZ(), home.getYaw(), home.getPitch(),
            "Teleported to home '" + homeName + "'."
        );
    }

    private static class HomeNamedCommand extends AbstractPlayerCommand {
        private final HomeManager homeManager;
        private final TeleportManager teleportManager;
        private final RequiredArg<String> nameArg;

        HomeNamedCommand(@Nonnull HomeManager homeManager, @Nonnull TeleportManager teleportManager) {
            super("Teleport to a specific home");
            this.homeManager = homeManager;
            this.teleportManager = teleportManager;
            this.nameArg = withRequiredArg("name", "Home name", ArgTypes.STRING);
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            String homeName = context.get(nameArg);
            doTeleportToHome(context, store, ref, playerRef, homeName, homeManager, teleportManager);
        }
    }
}
