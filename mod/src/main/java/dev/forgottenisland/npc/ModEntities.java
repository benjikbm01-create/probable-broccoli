package dev.forgottenisland.npc;

import dev.forgottenisland.ForgottenIsland;
import dev.forgottenisland.game.WardenEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {
    private ModEntities() {}
    public static final EntityType<IslandNpcEntity> NPC = Registry.register(Registries.ENTITY_TYPE, Identifier.of(ForgottenIsland.MOD_ID, "resident"),
            EntityType.Builder.create(IslandNpcEntity::new, SpawnGroup.MISC).dimensions(0.6f, 1.8f).eyeHeight(1.62f).maxTrackingRange(10).build());
    public static final EntityType<WardenEntity> WARDEN = Registry.register(Registries.ENTITY_TYPE, Identifier.of(ForgottenIsland.MOD_ID, "drowned_warden"),
            EntityType.Builder.create(WardenEntity::new, SpawnGroup.MONSTER).dimensions(0.7f, 2.1f).eyeHeight(1.85f).maxTrackingRange(10).build());

    public static void init() {
        FabricDefaultAttributeRegistry.register(NPC, IslandNpcEntity.attributes());
        FabricDefaultAttributeRegistry.register(WARDEN, WardenEntity.attributes());
    }
}
