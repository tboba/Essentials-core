package com.nhulston.essentials.events;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.managers.SpawnProtectionManager;
import com.nhulston.essentials.util.Log;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class SpawnProtectionEvent {
    private static final String PROTECTED_MESSAGE = "This area is protected.";
    private static final String PROTECTED_COLOR = "#FF5555";
    private static final String PVP_MESSAGE = "PvP is disabled in spawn.";

    private final SpawnProtectionManager spawnProtectionManager;

    public SpawnProtectionEvent(@Nonnull SpawnProtectionManager spawnProtectionManager) {
        this.spawnProtectionManager = spawnProtectionManager;
    }

    private static void sendProtectedMessage(PlayerRef playerRef) {
        if (playerRef != null) {
            playerRef.sendMessage(Message.raw(PROTECTED_MESSAGE).color(PROTECTED_COLOR));
        }
    }

    private static void sendPvpMessage(PlayerRef playerRef) {
        if (playerRef != null) {
            playerRef.sendMessage(Message.raw(PVP_MESSAGE).color(PROTECTED_COLOR));
        }
    }

    public void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        if (!spawnProtectionManager.isEnabled()) {
            return;
        }

        // Register block break protection
        registry.registerSystem(new BreakBlockProtectionSystem(spawnProtectionManager));

        // Register block place protection
        registry.registerSystem(new PlaceBlockProtectionSystem(spawnProtectionManager));

        // Register block damage protection (mining progress)
        registry.registerSystem(new DamageBlockProtectionSystem(spawnProtectionManager));

        // Register PvP protection using FilterDamageGroup
        if (spawnProtectionManager.isInvulnerableEnabled()) {
            registry.registerSystem(new SpawnDamageFilterSystem(spawnProtectionManager));
        }

        Log.info("Spawn protection enabled.");
    }

    /**
     * Prevents block breaking in spawn area.
     */
    private static class BreakBlockProtectionSystem 
            extends EntityEventSystem<EntityStore, BreakBlockEvent> {
        
        private final SpawnProtectionManager manager;

        BreakBlockProtectionSystem(SpawnProtectionManager manager) {
            super(BreakBlockEvent.class);
            this.manager = manager;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }

        @Override
        public void handle(int index, @NotNull ArchetypeChunk<EntityStore> chunk,
                           @NotNull Store<EntityStore> store,
                           @NotNull CommandBuffer<EntityStore> buffer,
                           BreakBlockEvent event) {
            if (event.isCancelled()) {
                return;
            }

            // Check if block is in protected area
            if (!manager.isInProtectedArea(event.getTargetBlock())) {
                return;
            }

            // Get player and check bypass permission
            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef != null && manager.canBypass(playerRef.getUuid())) {
                return;
            }

            // Cancel the event and notify player
            event.setCancelled(true);
            sendProtectedMessage(playerRef);
        }
    }

    /**
     * Prevents block placing in spawn area.
     */
    private static class PlaceBlockProtectionSystem 
            extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
        
        private final SpawnProtectionManager manager;

        PlaceBlockProtectionSystem(SpawnProtectionManager manager) {
            super(PlaceBlockEvent.class);
            this.manager = manager;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }

        @Override
        public void handle(int index, @NotNull ArchetypeChunk<EntityStore> chunk,
                           @NotNull Store<EntityStore> store,
                           @NotNull CommandBuffer<EntityStore> buffer,
                           PlaceBlockEvent event) {
            if (event.isCancelled()) {
                return;
            }

            if (!manager.isInProtectedArea(event.getTargetBlock())) {
                return;
            }

            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef != null && manager.canBypass(playerRef.getUuid())) {
                return;
            }

            event.setCancelled(true);
            sendProtectedMessage(playerRef);
        }
    }

    /**
     * Prevents block damage (mining progress) in spawn area.
     */
    private static class DamageBlockProtectionSystem 
            extends EntityEventSystem<EntityStore, DamageBlockEvent> {
        
        private final SpawnProtectionManager manager;

        DamageBlockProtectionSystem(SpawnProtectionManager manager) {
            super(DamageBlockEvent.class);
            this.manager = manager;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }

        @Override
        public void handle(int index, @NotNull ArchetypeChunk<EntityStore> chunk,
                           @NotNull Store<EntityStore> store,
                           @NotNull CommandBuffer<EntityStore> buffer,
                           DamageBlockEvent event) {
            if (event.isCancelled()) {
                return;
            }

            if (!manager.isInProtectedArea(event.getTargetBlock())) {
                return;
            }

            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef != null && manager.canBypass(playerRef.getUuid())) {
                return;
            }

            event.setCancelled(true);
        }
    }

    /**
     * Filters damage in spawn area by running in the FilterDamageGroup.
     * This should run before damage is actually applied.
     */
    private static class SpawnDamageFilterSystem extends DamageEventSystem {
        
        private final SpawnProtectionManager manager;

        SpawnDamageFilterSystem(SpawnProtectionManager manager) {
            super();
            this.manager = manager;
        }

        @Override
        public SystemGroup<EntityStore> getGroup() {
            // Use the FilterDamageGroup to run in the filtering phase
            return DamageModule.get().getFilterDamageGroup();
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }

        @Override
        public void handle(int index, @NotNull ArchetypeChunk<EntityStore> chunk,
                           @NotNull Store<EntityStore> store,
                           @NotNull CommandBuffer<EntityStore> buffer,
                           Damage event) {
            if (event.isCancelled()) {
                return;
            }

            // Check if the victim is a player
            PlayerRef victimRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (victimRef == null) {
                return;
            }

            // Check if victim is in protected area
            if (!manager.isInProtectedArea(victimRef.getTransform().getPosition())) {
                return;
            }

            // Check if the attacker is a player (only block PvP, not PvE)
            Damage.Source source = event.getSource();
            if (!(source instanceof Damage.EntitySource entitySource)) {
                return;
            }

            // Get attacker's PlayerRef
            Ref<EntityStore> attackerRef = entitySource.getRef();
            if (!attackerRef.isValid()) {
                return;
            }
            
            PlayerRef attackerPlayerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());
            if (attackerPlayerRef == null) {
                return; // Attacker is not a player (NPC, mob, etc.) - allow damage
            }

            // Both are players, victim is in spawn - cancel PvP damage
            event.setCancelled(true);
            event.setAmount(0);
            sendPvpMessage(attackerPlayerRef);
        }
    }
}
