package com.extras;

import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModComponents {
    /** Gas que le queda al lanzallamas. */
    public static final ComponentType<Integer> GAS = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            ExtrasMod.id("gas"),
            ComponentType.<Integer>builder().codec(Codec.INT).packetCodec(PacketCodecs.VAR_INT).build());

    public static void register() {
        // fuerza la carga de la clase
    }
}
