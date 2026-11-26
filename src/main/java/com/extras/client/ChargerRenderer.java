package com.extras.client;

import com.extras.ExtrasMod;
import com.extras.entity.ChargerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ChargerRenderer extends GeoEntityRenderer<ChargerEntity> {
    public ChargerRenderer(EntityRendererFactory.Context context) {
        super(context, new DefaultedEntityGeoModel<>(ExtrasMod.id("charger")));
        this.shadowRadius = 0.9f;
        withScale(0.9f);
    }
}
