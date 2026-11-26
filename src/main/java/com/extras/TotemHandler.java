package com.extras;

import com.extras.item.ExtraTotemItem;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

/** Hace que nuestros totems funcionen como el Totem de la Inmortalidad: evitan la muerte y dan efectos. */
public class TotemHandler {
    public static void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, damageAmount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;

            for (Hand hand : Hand.values()) {
                ItemStack stack = player.getStackInHand(hand);
                if (stack.getItem() instanceof ExtraTotemItem totem) {
                    stack.decrement(1);

                    // Mismos efectos que el totem normal
                    player.setHealth(1.0f);
                    player.clearStatusEffects();
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
                    // Efectos extra de cada totem
                    for (StatusEffectInstance extra : totem.getExtraEffects()) {
                        player.addStatusEffect(extra);
                    }

                    ServerWorld world = (ServerWorld) player.getWorld();
                    world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,
                            player.getX(), player.getBodyY(0.5), player.getZ(), 40, 0.4, 0.5, 0.4, 0.3);
                    world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_TOTEM_USE,
                            SoundCategory.PLAYERS, 1.0f, 1.0f);
                    return false; // cancela la muerte
                }
            }
            return true;
        });
    }
}
