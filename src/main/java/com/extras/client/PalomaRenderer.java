package com.extras.client;

import com.extras.ExtrasMod;
import com.extras.entity.PalomaEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PalomaRenderer extends GeoEntityRenderer<PalomaEntity> {
    public PalomaRenderer(EntityRendererFactory.Context context) {
        super(context, new DefaultedEntityGeoModel<>(ExtrasMod.id("paloma")));
        this.shadowRadius = 0.3f;
        withScale(0.8f);
    }
}
