package com.extras;

import com.extras.entity.ChargerEntity;
import com.extras.entity.PalomaEntity;
import com.extras.entity.PoopBombEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModEntities {
    public static final EntityType<ChargerEntity> CHARGER = Registry.register(
            Registries.ENTITY_TYPE, ExtrasMod.id("charger"),
            EntityType.Builder.create(ChargerEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.3f, 2.3f).maxTrackingRange(10).build("charger"));

    public static final EntityType<PalomaEntity> PALOMA = Registry.register(
            Registries.ENTITY_TYPE, ExtrasMod.id("paloma"),
            EntityType.Builder.create(PalomaEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.7f, 0.7f).maxTrackingRange(10).build("paloma"));

    public static final EntityType<PoopBombEntity> POOP_BOMB = Registry.register(
            Registries.ENTITY_TYPE, ExtrasMod.id("caca_explosiva"),
            EntityType.Builder.<PoopBombEntity>create(PoopBombEntity::new, SpawnGroup.MISC)
                    .dimensions(0.3f, 0.3f).maxTrackingRange(4).trackingTickInterval(10).build("caca_explosiva"));

    public static void register() {
        FabricDefaultAttributeRegistry.register(CHARGER, ChargerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(PALOMA, PalomaEntity.createAttributes());
    }
}
