package com.extras.entity;

import com.extras.Balance;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Charger (Left 4 Dead 2).
 *  - Vida: 26 corazones. Golpe normal: 3 corazones. Mas rapido que un jugador.
 *  - Embestida: si agarra a un jugador le quita 6 corazones cada 2 segundos.
 *  - Si falla y choca con un bloque, queda aturdido unos segundos.
 */
public class ChargerEntity extends HostileEntity implements GeoEntity {
    public static final int NORMAL = 0, WINDUP = 1, CHARGING = 2, GRABBING = 3, STUNNED = 4;
    private static final TrackedData<Integer> STATE =
            DataTracker.registerData(ChargerEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.charger.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.charger.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.charger.run");
    private static final RawAnimation CHARGE = RawAnimation.begin().thenLoop("animation.charger.charge");
    private static final RawAnimation ATTACK_LOOP = RawAnimation.begin().thenLoop("animation.charger.attack");
    private static final RawAnimation STUN = RawAnimation.begin().thenLoop("animation.charger.stun");
    private static final RawAnimation ATTACK_ONCE =
            RawAnimation.begin().then("animation.charger.attack", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int windupTicks = 0;
    private int chargeTicks = 0;
    private int stunTicks = 0;
    private int grabTicks = 0;
    private int cooldown = 60;
    private Vec3d chargeDir = Vec3d.ZERO;
    private UUID victimId = null;

    public ChargerEntity(EntityType<? extends ChargerEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 30;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, Balance.CHARGER_HEALTH)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, Balance.CHARGER_SPEED)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, Balance.CHARGER_HIT_DAMAGE)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.7);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(STATE, NORMAL);
    }

    public int getChargerState() {
        return this.dataTracker.get(STATE);
    }

    private void setChargerState(int state) {
        this.dataTracker.set(STATE, state);
    }

    // ------------------------------------------------------------------
    //  IA
    // ------------------------------------------------------------------
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new ChargeGoal(this));
        this.goalSelector.add(2, new GuardedMeleeGoal(this, 1.1));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 10.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    /** Ataque cuerpo a cuerpo que solo funciona en estado normal. */
    private static class GuardedMeleeGoal extends MeleeAttackGoal {
        private final ChargerEntity charger;

        GuardedMeleeGoal(ChargerEntity charger, double speed) {
            super(charger, speed, false);
            this.charger = charger;
        }

        @Override
        public boolean canStart() {
            return charger.getChargerState() == NORMAL && super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            return charger.getChargerState() == NORMAL && super.shouldContinue();
        }
    }

    /** Decide cuando empezar una embestida. El resto del proceso lo maneja tick(). */
    private static class ChargeGoal extends Goal {
        private final ChargerEntity charger;

        ChargeGoal(ChargerEntity charger) {
            this.charger = charger;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = charger.getTarget();
            if (target == null || !target.isAlive()) return false;
            if (charger.getChargerState() != NORMAL || charger.cooldown > 0) return false;
            if (!charger.isOnGround()) return false;
            double d = charger.squaredDistanceTo(target);
            if (d < 5.0 * 5.0 || d > 24.0 * 24.0) return false;
            if (!charger.canSee(target)) return false;
            return charger.getRandom().nextInt(8) == 0;
        }

        @Override
        public boolean shouldContinue() {
            int s = charger.getChargerState();
            return s == WINDUP || s == CHARGING;
        }

        @Override
        public void start() {
            charger.beginWindup();
        }
    }

    // ------------------------------------------------------------------
    //  Maquina de estados (solo servidor)
    // ------------------------------------------------------------------
    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) return;

        if (cooldown > 0) cooldown--;
        switch (getChargerState()) {
            case WINDUP -> windupTick();
            case CHARGING -> chargingTick();
            case GRABBING -> grabTick();
            case STUNNED -> stunnedTick();
            default -> { }
        }
    }

    private void beginWindup() {
        setChargerState(WINDUP);
        windupTicks = Balance.CHARGE_WINDUP_TICKS;
        this.getNavigation().stop();
        this.playSound(SoundEvents.ENTITY_RAVAGER_ROAR, 1.5f, 0.8f);
    }

    private void windupTick() {
        freezeHorizontal();
        LivingEntity target = this.getTarget();
        if (target != null) {
            this.getLookControl().lookAt(target, 30.0f, 30.0f);
            faceYaw(target.getX() - this.getX(), target.getZ() - this.getZ());
        }
        if (--windupTicks <= 0) {
            Vec3d dir = target != null
                    ? new Vec3d(target.getX() - this.getX(), 0, target.getZ() - this.getZ())
                    : Vec3d.fromPolar(0, this.getYaw());
            if (dir.lengthSquared() < 1.0E-4) dir = Vec3d.fromPolar(0, this.getYaw());
            chargeDir = new Vec3d(dir.x, 0, dir.z).normalize();
            chargeTicks = 0;
            setChargerState(CHARGING);
        }
    }

    private void chargingTick() {
        chargeTicks++;
        this.getNavigation().stop();
        Vec3d v = this.getVelocity();
        this.setVelocity(chargeDir.x * Balance.CHARGE_SPEED, v.y, chargeDir.z * Balance.CHARGE_SPEED);
        this.velocityModified = true;
        faceYaw(chargeDir.x, chargeDir.z);

        // Agarra al primer jugador que toque
        List<PlayerEntity> hits = this.getWorld().getEntitiesByClass(PlayerEntity.class,
                this.getBoundingBox().expand(0.6, 0.1, 0.6),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative());
        if (!hits.isEmpty()) {
            grab(hits.get(0));
            return;
        }
        // Falla y choca con un bloque: se aturde
        if (chargeTicks > 5 && this.horizontalCollision) {
            stun(Balance.STUN_TICKS);
            return;
        }
        // Se acabo la carrera sin chocar
        if (chargeTicks >= Balance.CHARGE_MAX_TICKS) {
            setChargerState(NORMAL);
            cooldown = Balance.CHARGE_COOLDOWN / 2;
        }
    }

    private void grab(PlayerEntity player) {
        victimId = player.getUuid();
        grabTicks = 0;
        setChargerState(GRABBING);
        this.playSound(SoundEvents.ENTITY_RAVAGER_ATTACK, 1.2f, 0.7f);
    }

    private void grabTick() {
        PlayerEntity victim = victimId == null ? null : this.getWorld().getPlayerByUuid(victimId);
        if (victim == null || !victim.isAlive() || victim.isSpectator() || victim.squaredDistanceTo(this) > 6.0 * 6.0) {
            releaseVictim();
            return;
        }
        freezeHorizontal();

        // Mantiene al jugador pegado delante del Charger
        Vec3d front = this.getPos().add(Vec3d.fromPolar(0, this.getYaw()).multiply(1.1));
        victim.requestTeleport(front.x, this.getY(), front.z);
        victim.setVelocity(Vec3d.ZERO);
        victim.velocityModified = true;
        victim.fallDistance = 0;

        grabTicks++;
        if (grabTicks >= Balance.GRAB_FIRST_HIT && (grabTicks - Balance.GRAB_FIRST_HIT) % Balance.GRAB_INTERVAL == 0) {
            victim.damage(this.getDamageSources().mobAttack(this), Balance.GRAB_DAMAGE);
            this.triggerAnim("attack_controller", "attack");
            this.playSound(SoundEvents.ENTITY_RAVAGER_ATTACK, 1.0f, 0.8f);
        }
    }

    private void releaseVictim() {
        victimId = null;
        setChargerState(NORMAL);
        cooldown = Balance.CHARGE_COOLDOWN;
    }

    private void stun(int ticks) {
        setChargerState(STUNNED);
        stunTicks = ticks;
        this.setVelocity(0, this.getVelocity().y, 0);
        this.getNavigation().stop();
        this.playSound(SoundEvents.ENTITY_RAVAGER_STUNNED, 1.5f, 0.9f);
    }

    private void stunnedTick() {
        freezeHorizontal();
        if (stunTicks % 10 == 0 && this.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 2.2, this.getZ(), 6, 0.4, 0.2, 0.4, 0.05);
        }
        if (--stunTicks <= 0) {
            setChargerState(NORMAL);
            cooldown = Balance.CHARGE_COOLDOWN;
        }
    }

    private void freezeHorizontal() {
        this.getNavigation().stop();
        Vec3d v = this.getVelocity();
        this.setVelocity(0, v.y, 0);
    }

    private void faceYaw(double dx, double dz) {
        float yaw = (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        this.setYaw(yaw);
        this.setBodyYaw(yaw);
        this.setHeadYaw(yaw);
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (getChargerState() != NORMAL) return false;
        boolean ok = super.tryAttack(target);
        if (ok && !this.getWorld().isClient) this.triggerAnim("attack_controller", "attack");
        return ok;
    }

    // ------------------------------------------------------------------
    //  Animaciones (GeckoLib)
    // ------------------------------------------------------------------
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move", 5, this::movePredicate));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK_ONCE));
    }

    private PlayState movePredicate(AnimationState<ChargerEntity> state) {
        switch (getChargerState()) {
            case STUNNED: return state.setAndContinue(STUN);
            case GRABBING: return state.setAndContinue(ATTACK_LOOP);
            case CHARGING: return state.setAndContinue(CHARGE);
            case WINDUP: return state.setAndContinue(IDLE);
            default: break;
        }
        if (state.isMoving()) return state.setAndContinue(this.getTarget() != null ? RUN : WALK);
        return state.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ------------------------------------------------------------------
    //  Sonidos
    // ------------------------------------------------------------------
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_RAVAGER_DEATH;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_RAVAGER_AMBIENT;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ENTITY_RAVAGER_STEP, 0.6f, 1.0f);
    }
}
