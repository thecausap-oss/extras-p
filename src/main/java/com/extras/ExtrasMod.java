package com.extras;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtrasMod implements ModInitializer {
    public static final String MOD_ID = "extras";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModComponents.register();
        ModEntities.register();
        ModItems.register();
        TotemHandler.register();
        LOGGER.info("Extras cargado.");
    }
}
