package com.extras.item;

import com.extras.Balance;
import com.extras.ModComponents;
import com.extras.ModItems;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * Lanzallamas del Pyro.
 *  - Mantener clic derecho: echa fuego (5 corazones de dano y prende a lo que toque).
 *  - Gasta 2 de gas cada 3 segundos de uso. Sin gas no se puede usar.
 *  - Shift + clic derecho: recarga con un Balon de gas del inventario.
 *  - No tiene durabilidad: no se rompe.
 */
public class LanzallamasItem extends Item {
    public LanzallamasItem(Settings settings) {
        super(settings);
    }

    public static int getGas(ItemStack stack) {
        return stack.getOrDefault(ModComponents.GAS, 0);
    }

    public static void setGas(ItemStack stack, int value) {
        stack.set(ModComponents.GAS, MathHelper.clamp(value, 0, Balance.MAX_GAS));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (user.isSneaking()) {
            boolean ok = recargarDesdeInventario(user, stack);
            if (!world.isClient) {
                user.sendMessage(Text.translatable(ok ? "message.extras.recargado" : "message.extras.sin_balones",
                        Balance.MAX_GAS), true);
            }
            return ok ? TypedActionResult.success(stack, world.isClient()) : TypedActionResult.fail(stack);
        }

        if (getGas(stack) <= 0) {
            if (!world.isClient) user.sendMessage(Text.translatable("message.extras.sin_gas"), true);
            return TypedActionResult.fail(stack);
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    private boolean recargarDesdeInventario(PlayerEntity user, ItemStack lanzallamas) {
        if (getGas(lanzallamas) >= Balance.MAX_GAS) return false;
        PlayerInventory inv = user.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(ModItems.GAS_BALON)) {
                if (!user.getWorld().isClient) {
                    setGas(lanzallamas, Balance.MAX_GAS);
                    if (!user.isCreative()) s.decrement(1);
                    user.getWorld().playSound(null, user.getX(), user.getY(), user.getZ(),
                            SoundEvents.ITEM_BUCKET_FILL_LAVA, SoundCategory.PLAYERS, 0.8f, 1.2f);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient) return;
        ServerWorld sw = (ServerWorld) world;

        if (getGas(stack) <= 0) {
            user.stopUsingItem();
            return;
        }

        int used = getMaxUseTime(stack, user) - remainingUseTicks;
        Vec3d eye = user.getEyePos();
        Vec3d look = user.getRotationVec(1.0f);

        // ---- Particulas y sonido ----
        for (double d = 1.2; d <= Balance.FLAME_RANGE; d += 1.4) {
            Vec3d p = eye.add(look.multiply(d)).add(0, -0.25, 0);
            double spread = 0.05 + d * 0.035;
            sw.spawnParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 2, spread, spread, spread, 0.01);
            if (used % 3 == 0) sw.spawnParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 1, spread, spread, spread, 0.01);
        }
        if (used % 6 == 0) {
            sw.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.5f, 1.3f);
        }

        // ---- Dano y fuego a lo que este delante (cono) ----
        Box box = user.getBoundingBox().stretch(look.multiply(Balance.FLAME_RANGE)).expand(2.5);
        DamageSource source = user instanceof PlayerEntity p
                ? sw.getDamageSources().playerAttack(p)
                : sw.getDamageSources().mobAttack(user);
        for (Entity e : sw.getOtherEntities(user, box, en -> en instanceof LivingEntity le && le.isAlive())) {
            Vec3d center = e.getBoundingBox().getCenter();
            Vec3d to = center.subtract(eye);
            double dist = to.length();
            if (dist > Balance.FLAME_RANGE || dist < 0.1) continue;
            if (to.normalize().dotProduct(look) < 0.86) continue;
            BlockHitResult hit = sw.raycast(new RaycastContext(eye, center,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, user));
            if (hit.getType() == HitResult.Type.BLOCK) continue;

            e.setOnFireForTicks(Balance.BURN_TICKS);
            if (used % Balance.FLAME_DAMAGE_INTERVAL == 0) {
                e.damage(source, Balance.FLAME_DAMAGE);
            }
        }

        // ---- Consumo de gas: 2 cada 3 segundos ----
        if (used > 0 && used % Balance.GAS_INTERVAL_TICKS == 0) {
            setGas(stack, getGas(stack) - Balance.GAS_COST);
            if (getGas(stack) <= 0) user.stopUsingItem();
        }
    }

    // ---- Barra de gas en el icono ----
    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.round(13.0f * getGas(stack) / (float) Balance.MAX_GAS);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        float f = getGas(stack) / (float) Balance.MAX_GAS;
        return MathHelper.hsvToRgb(0.02f + 0.10f * f, 1.0f, 1.0f);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.extras.gas", getGas(stack), Balance.MAX_GAS).formatted(Formatting.GOLD));
        tooltip.add(Text.translatable("tooltip.extras.lanzallamas.1").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.extras.lanzallamas.2").formatted(Formatting.GRAY));
    }
}
