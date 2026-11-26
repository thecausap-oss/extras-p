package com.extras.entity;

import com.extras.Balance;
import com.extras.ModEntities;
import com.extras.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;

/** El excremento explosivo de la paloma: cae y explota como TNT (4 corazones de dano). */
public class PoopBombEntity extends ThrownItemEntity {
    public PoopBombEntity(EntityType<? extends PoopBombEntity> type, World world) {
        super(type, world);
    }

    public PoopBombEntity(World world, LivingEntity owner) {
        super(ModEntities.POOP_BOMB, owner, world);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CACA_EXPLOSIVA;
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!this.getWorld().isClient) {
            explode();
            this.discard();
        }
    }

    private void explode() {
        ServerWorld world = (ServerWorld) this.getWorld();
        Entity owner = this.getOwner();

        // Explosion del tamano del TNT (rompe bloques segun la regla mobGriefing), sin el dano normal:
        // el dano lo aplicamos nosotros para que sean exactamente 4 corazones.
        ExplosionBehavior sinDano = new ExplosionBehavior() {
            @Override
            public boolean shouldDamage(Explosion explosion, Entity entity) {
                return false;
            }
        };
        world.createExplosion(this, world.getDamageSources().explosion(this, owner), sinDano,
                this.getX(), this.getY(), this.getZ(), Balance.BOMB_POWER, false,
                Balance.BOMB_BREAKS_BLOCKS ? World.ExplosionSourceType.MOB : World.ExplosionSourceType.NONE);

        double r = Balance.BOMB_DAMAGE_RADIUS;
        Box area = this.getBoundingBox().expand(r);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, area,
                e -> e.isAlive() && !(e instanceof PalomaEntity))) {
            if (target.squaredDistanceTo(this) <= r * r) {
                target.damage(world.getDamageSources().explosion(this, owner), Balance.BOMB_DAMAGE);
            }
        }
    }
}
