package com.extras.item;

import java.util.List;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ExtraTotemItem extends Item {
    public enum Variant { PALOMA, MIKU }

    private final Variant variant;

    public ExtraTotemItem(Settings settings, Variant variant) {
        super(settings);
        this.variant = variant;
    }

    /** Efectos que da ademas de los del Totem de la Inmortalidad (II = amplificador 1). */
    public List<StatusEffectInstance> getExtraEffects() {
        return switch (variant) {
            case PALOMA -> List.of(
                    new StatusEffectInstance(StatusEffects.RESISTANCE, 3 * 20, 1),   // Resistencia II, 3 s
                    new StatusEffectInstance(StatusEffects.JUMP_BOOST, 5 * 20, 1));  // Salto II, 5 s
            case MIKU -> List.of(
                    new StatusEffectInstance(StatusEffects.STRENGTH, 4 * 20, 1),     // Fuerza II, 4 s
                    new StatusEffectInstance(StatusEffects.SPEED, 6 * 20, 1));       // Velocidad II, 6 s
        };
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.extras.totem.base").formatted(Formatting.GRAY));
        if (variant == Variant.PALOMA) {
            tooltip.add(Text.translatable("tooltip.extras.totem_paloma.1").formatted(Formatting.GOLD));
            tooltip.add(Text.translatable("tooltip.extras.totem_paloma.2").formatted(Formatting.GOLD));
        } else {
            tooltip.add(Text.translatable("tooltip.extras.totem_miku.1").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("tooltip.extras.totem_miku.2").formatted(Formatting.AQUA));
        }
    }
}
