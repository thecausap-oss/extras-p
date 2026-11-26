package com.extras.item;

import com.extras.Balance;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Municion del lanzallamas: cada balon recarga 100 de gas. */
public class GasBalonItem extends Item {
    public GasBalonItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        ItemStack other = user.getStackInHand(hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);

        if (other.getItem() instanceof LanzallamasItem) {
            if (LanzallamasItem.getGas(other) >= Balance.MAX_GAS) {
                if (!world.isClient) user.sendMessage(Text.translatable("message.extras.tanque_lleno"), true);
                return TypedActionResult.fail(stack);
            }
            if (!world.isClient) {
                LanzallamasItem.setGas(other, Balance.MAX_GAS);
                if (!user.isCreative()) stack.decrement(1);
                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        SoundEvents.ITEM_BUCKET_FILL_LAVA, SoundCategory.PLAYERS, 0.8f, 1.2f);
                user.sendMessage(Text.translatable("message.extras.recargado", Balance.MAX_GAS), true);
            }
            return TypedActionResult.success(stack, world.isClient());
        }

        if (!world.isClient) user.sendMessage(Text.translatable("message.extras.como_recargar"), true);
        return TypedActionResult.pass(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.extras.gas_balon.1", Balance.MAX_GAS).formatted(Formatting.GOLD));
        tooltip.add(Text.translatable("tooltip.extras.gas_balon.2").formatted(Formatting.GRAY));
    }
}
