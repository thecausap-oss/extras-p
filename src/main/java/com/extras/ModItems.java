package com.extras;

import com.extras.item.ExtraTotemItem;
import com.extras.item.GasBalonItem;
import com.extras.item.LanzallamasItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

public class ModItems {
    private static NbtComponent rareza() {
        // Loot Beams Up: rareza personalizada "extras_dorado" -> destello dorado
        NbtCompound tag = new NbtCompound();
        tag.putString("custom_rarity_id", "extras_dorado");
        return NbtComponent.of(tag);
    }

    public static final Item LANZALLAMAS = reg("lanzallamas", new LanzallamasItem(new Item.Settings()
            .maxCount(1).rarity(Rarity.EPIC).fireproof()
            .component(ModComponents.GAS, Balance.MAX_GAS)
            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            .component(DataComponentTypes.CUSTOM_DATA, rareza())));

    public static final Item GAS_BALON = reg("gas_balon", new GasBalonItem(new Item.Settings()
            .maxCount(16).rarity(Rarity.UNCOMMON)));

    public static final Item TOTEM_PALOMA = reg("totem_paloma", new ExtraTotemItem(new Item.Settings()
            .maxCount(1).rarity(Rarity.UNCOMMON), ExtraTotemItem.Variant.PALOMA));

    public static final Item TOTEM_MIKU = reg("totem_miku", new ExtraTotemItem(new Item.Settings()
            .maxCount(1).rarity(Rarity.UNCOMMON), ExtraTotemItem.Variant.MIKU));

    /** Proyectil de la paloma (no aparece en el creativo). */
    public static final Item CACA_EXPLOSIVA = reg("caca_explosiva", new Item(new Item.Settings()));

    public static final Item CHARGER_SPAWN_EGG = reg("charger_spawn_egg",
            new SpawnEggItem(ModEntities.CHARGER, 0x5B6B4A, 0xB02020, new Item.Settings()));

    public static final Item PALOMA_SPAWN_EGG = reg("paloma_spawn_egg",
            new SpawnEggItem(ModEntities.PALOMA, 0x8A93A6, 0x4DBA84, new Item.Settings()));

    public static final ItemGroup GRUPO = Registry.register(
            Registries.ITEM_GROUP, ExtrasMod.id("extras"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(LANZALLAMAS))
                    .displayName(Text.translatable("itemGroup.extras"))
                    .entries((context, entries) -> {
                        entries.add(LANZALLAMAS);
                        entries.add(GAS_BALON);
                        entries.add(TOTEM_PALOMA);
                        entries.add(TOTEM_MIKU);
                        entries.add(CHARGER_SPAWN_EGG);
                        entries.add(PALOMA_SPAWN_EGG);
                    })
                    .build());

    private static <T extends Item> T reg(String name, T item) {
        return Registry.register(Registries.ITEM, ExtrasMod.id(name), item);
    }

    public static void register() {
        // fuerza la carga de la clase
    }
}
