package com.extras.entity;

import com.extras.Balance;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.FlyGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
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
 * Paloma bombardera: vuela alto, elige a un jugador al azar y le suelta excremento explosivo.
 * Vida: 7 corazones.
 */
public class PalomaEntity extends HostileEntity implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.paloma.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.paloma.walk");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.paloma.fly");
    private static final RawAnimation DROP = RawAnimation.begin().then("animation.paloma.drop", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private PlayerEntity bombTarget;
    private int retargetTicks = 0;
    private int bombCooldown = 60;
    private float orbitAngle = 0f;

    public PalomaEntity(EntityType<? extends PalomaEntity> type, World world) {
        super(type, world);
        this.moveControl = new FlightMoveControl(this, 20, true);
        this.experiencePoints = 5;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, Balance.PALOMA_HEALTH)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.7)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0);
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        BirdNavigation navigation = new BirdNavigation(this, world);
        navigation.setCanPathThroughDoors(false);
        navigation.setCanSwim(false);
        navigation.setCanEnterOpenDoors(true);
        return navigation;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new BombRunGoal(this));
        this.goalSelector.add(4, new FlyGoal(this, 1.0));
        this.goalSelector.add(5, new LookAroundGoal(this));
    }

    // ---------- Sin dano por caida (vuela) ----------
    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
    }

    // ---------- Objetivo ----------
    /** Devuelve el jugador al que va a bombardear (elige uno al azar si hace falta). */
    PlayerEntity findTarget() {
        if (bombTarget != null && retargetTicks > 0 && isValidTarget(bombTarget)) {
            return bombTarget;
        }
        List<? extends PlayerEntity> players = this.getWorld().getPlayers();
        List<PlayerEntity> candidates = players.stream()
                .filter(this::isValidTarget)
                .map(p -> (PlayerEntity) p)
                .toList();
        if (candidates.isEmpty()) {
            bombTarget = null;
            return null;
        }
        bombTarget = candidates.get(this.getRandom().nextInt(candidates.size()));
        retargetTicks = 400; // cambia de victima cada 20 s
        return bombTarget;
    }

    private boolean isValidTarget(PlayerEntity p) {
        return p.isAlive() && !p.isSpectator() && !p.isCreative() && p.squaredDistanceTo(this) < 64.0 * 64.0;
    }

    void dropBomb() {
        PoopBombEntity bomb = new PoopBombEntity(this.getWorld(), this);
        bomb.setPosition(this.getX(), this.getY() - 0.3, this.getZ());
        bomb.setVelocity(0.0, -0.4, 0.0);
        this.getWorld().spawnEntity(bomb);
        this.playSound(SoundEvents.ENTITY_CHICKEN_EGG, 1.0f, 0.7f);
        this.triggerAnim("action", "drop");
        this.bombCooldown = Balance.BOMB_COOLDOWN_MIN
                + this.getRandom().nextInt(Balance.BOMB_COOLDOWN_MAX - Balance.BOMB_COOLDOWN_MIN + 1);
    }

    /** Vuela en circulos sobre la victima y suelta la bomba cuando esta justo encima. */
    private static class BombRunGoal extends Goal {
        private final PalomaEntity paloma;

        BombRunGoal(PalomaEntity paloma) {
            this.paloma = paloma;
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return paloma.findTarget() != null;
        }

        @Override
        public boolean shouldContinue() {
            return paloma.findTarget() != null;
        }

        @Override
        public void tick() {
            PlayerEntity target = paloma.findTarget();
            if (target == null) return;

            if (paloma.retargetTicks > 0) paloma.retargetTicks--;
            if (paloma.bombCooldown > 0) paloma.bombCooldown--;

            paloma.orbitAngle += 0.07f;
            double radius = 3.0;
            double tx = target.getX() + Math.cos(paloma.orbitAngle) * radius;
            double tz = target.getZ() + Math.sin(paloma.orbitAngle) * radius;
            double ty = target.getY() + Balance.PALOMA_HEIGHT;
            paloma.getMoveControl().moveTo(tx, ty, tz, 1.3);

            double dx = target.getX() - paloma.getX();
            double dz = target.getZ() - paloma.getZ();
            boolean encima = dx * dx + dz * dz < 4.5 * 4.5 && paloma.getY() > target.getY() + 3.0;
            if (encima && paloma.bombCooldown <= 0) {
                paloma.dropBomb();
            }
        }
    }

    // ---------- Animaciones ----------
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move", 4, this::movePredicate));
        controllers.add(new AnimationController<>(this, "action", 0, state -> PlayState.STOP)
                .triggerableAnim("drop", DROP));
    }

    private PlayState movePredicate(AnimationState<PalomaEntity> state) {
        if (!this.isOnGround()) return state.setAndContinue(FLY);
        if (state.isMoving()) return state.setAndContinue(WALK);
        return state.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ---------- Sonidos ----------
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_PARROT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PARROT_DEATH;
    }

    @Override
    public float getSoundPitch() {
        return super.getSoundPitch() + 0.4f;
    }
}
