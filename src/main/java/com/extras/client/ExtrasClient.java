package com.extras.client;

import com.extras.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

public class ExtrasClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.CHARGER, ChargerRenderer::new);
        EntityRendererRegistry.register(ModEntities.PALOMA, PalomaRenderer::new);
        EntityRendererRegistry.register(ModEntities.POOP_BOMB, FlyingItemEntityRenderer::new);
    }
}
