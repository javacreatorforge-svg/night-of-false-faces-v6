package com.redstonedev.nightoffalsefaces.entity;

import com.redstonedev.nightoffalsefaces.init.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib3.core.AnimationState;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType.EDefaultLoopTypes;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

public class FaceThiefEntity extends Monster implements IAnimatable {

    /** What animal this Face Thief is currently disguised as. NONE = true form. */
    public enum Disguise { NONE, PIG, CHICKEN, COW, VILLAGER, SHEEP, HORSE }

    private static final EntityDataAccessor<Boolean> DATA_AGGRESSIVE =
            SynchedEntityData.defineId(FaceThiefEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_STALKING =
            SynchedEntityData.defineId(FaceThiefEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CLIMBING =
            SynchedEntityData.defineId(FaceThiefEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_DISGUISE =
            SynchedEntityData.defineId(FaceThiefEntity.class, EntityDataSerializers.INT);

    private static final double SPEED_WALK    = 0.20D;
    private static final double SPEED_CHASE   = 0.30D;
    private static final double SPEED_CLIMB   = 0.25D;
    private static final double SPEED_DISGUISED = 0.18D; // a slow walk while disguised - approaches the player

    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);

    private int aliveTicks = 0;
    /** Tick count of the last time the player noticed this Face Thief (looked at it, hit it,
     * or came within ~8 blocks). Used to aggro after 3 unnoticed minutes. */
    private int lastNoticedTicks = 0;
    /** Counts ticks only while the entity is NOT disguised. Lets us despawn after 1 minute
     *  of being uncovered/aggressive while still allowing the 3-min unnoticed disguise timer. */
    private int nonDisguiseTicks = 0;
    private int aggroSoundCooldown = 0;
    // Initial sound cooldowns are set in the constructor to random long delays so the
    // first ambient noise never feels tied to spawn time.
    private int ambientSoundCooldown;
    private int distanceMoanCooldown;
    private int helpmeCooldown;
    private int mimicHelpmeCooldown;
    private int screamCooldown;

    private boolean clientChaseSoundStarted = false; // client-only flag

    public FaceThiefEntity(EntityType<? extends FaceThiefEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
        this.maxUpStep = 1.0F;
        // Randomize all initial sound cooldowns. Minimums chosen so the first noise of
        // any kind is at least ~40 seconds away from spawn - prevents the "every spawn
        // immediately makes a noise" pattern.
        this.ambientSoundCooldown = 800  + this.random.nextInt(1600);  // 40-120s
        this.distanceMoanCooldown = 1200 + this.random.nextInt(2400);  // 60-180s
        this.helpmeCooldown       = 1600 + this.random.nextInt(2400);  // 80-200s
        this.mimicHelpmeCooldown  = 2400 + this.random.nextInt(3600);  // 120-300s
        this.screamCooldown       = 1200 + this.random.nextInt(1200);  // 60-120s
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, SPEED_WALK)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_AGGRESSIVE, false);
        this.entityData.define(DATA_STALKING,   false);
        this.entityData.define(DATA_CLIMBING,   false);
        this.entityData.define(DATA_DISGUISE,   0);
    }

    /** WallClimberNavigation lets pathfinding plot routes UP walls (same as vanilla Spider). */
    @Override
    protected PathNavigation createNavigation(Level level) {
        WallClimberNavigation nav = new WallClimberNavigation(this, level);
        // BreakDoorGoal only fires if the navigation reports it can open doors. Without this,
        // the goal is technically registered but never activates on wooden doors in the path.
        nav.setCanOpenDoors(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Door breaking: slow drain per the user's spec; vanilla door sound plays from the
        // block break, no extra wiring needed.
        this.goalSelector.addGoal(1, new BreakDoorGoal(this, d -> true));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 64.0F, 1.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1,
                new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // === State accessors ======================================================

    public boolean isFaceThiefAggressive() { return this.entityData.get(DATA_AGGRESSIVE); }
    public boolean isStalking()            { return this.entityData.get(DATA_STALKING); }
    public boolean isClimbing()            { return this.entityData.get(DATA_CLIMBING); }
    public Disguise getDisguise()          { return Disguise.values()[Math.max(0, Math.min(this.entityData.get(DATA_DISGUISE), Disguise.values().length - 1))]; }
    public boolean isDisguised()           { return getDisguise() != Disguise.NONE; }

    public void setFaceThiefAggressive(boolean aggressive) {
        boolean was = this.entityData.get(DATA_AGGRESSIVE);
        this.entityData.set(DATA_AGGRESSIVE, aggressive);
        if (aggressive) {
            // Aggro cancels stalking and disguise.
            this.entityData.set(DATA_STALKING, false);
            this.entityData.set(DATA_DISGUISE, 0);
            this.setNoAi(false);
            if (!was && !this.level.isClientSide && aggroSoundCooldown <= 0) {
                this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.SCREAM.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
                aggroSoundCooldown = 80;
            }
        }
    }

    public void setStalking(boolean stalking) {
        this.entityData.set(DATA_STALKING, stalking);
        if (stalking) {
            this.setNoAi(true);
        }
    }

    public void setClimbing(boolean climbing) {
        this.entityData.set(DATA_CLIMBING, climbing);
    }

    public void setDisguise(Disguise disguise) {
        this.entityData.set(DATA_DISGUISE, disguise.ordinal());
        // AI stays active in every mode now - disguised Face Thieves walk toward the player.
        this.setNoAi(false);
    }

    // === Climbing =============================================================

    @Override
    public boolean onClimbable() { return this.isClimbing(); }

    // === Tick =================================================================

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide) {
            // Climb when bumping a wall, and apply upward force if the target is above so
            // the entity actually scales the wall instead of just hugging it.
            boolean blocked = this.horizontalCollision;
            LivingEntity target = this.getTarget();
            boolean targetAbove = target != null && target.getY() > this.getY() + 1.5;
            if (blocked && targetAbove) {
                Vec3 d = this.getDeltaMovement();
                this.setDeltaMovement(d.x, Math.max(0.18D, d.y), d.z);
            }
            this.setClimbing(blocked);
        } else {
            // Start the looping chase sound on the client when we go aggressive.
            if (!clientChaseSoundStarted && isFaceThiefAggressive() && !isStalking() && !isDisguised()) {
                clientChaseSoundStarted = true;
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                        net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () -> com.redstonedev.nightoffalsefaces.client.sound.ClientChaseSoundStarter.start(this));
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level.isClientSide) return;

        aliveTicks++;
        if (!isDisguised()) {
            nonDisguiseTicks++;
        }
        // Despawn rules:
        //   - 1 minute of being NOT disguised (stalking, walking, aggressive) -> despawn
        //   - 6 minute hard cap regardless (safety net for stuck-disguise scenarios)
        if (nonDisguiseTicks >= 1200 || aliveTicks >= 7200) {
            this.discard();
            return;
        }

        // Track when the player last "noticed" us: looking at us, hitting us (handled in
        // hurt()), or coming within ~8 blocks. Used by the 3-min-unnoticed-aggro check below.
        Player noticer = this.level.getNearestPlayer(this, 64.0D);
        if (noticer != null) {
            if (this.distanceTo(noticer) < 8.0D || isPlayerStaringAt(noticer)) {
                lastNoticedTicks = aliveTicks;
            }
        }
        // 3 minutes (3600 ticks) of being ignored while disguised -> ditch the disguise and
        // come for the player anyway.
        if (isDisguised() && (aliveTicks - lastNoticedTicks) >= 3600) {
            setDisguise(Disguise.NONE);
            setFaceThiefAggressive(true);
        }

        if (aggroSoundCooldown > 0)    aggroSoundCooldown--;
        if (ambientSoundCooldown > 0)  ambientSoundCooldown--;
        if (distanceMoanCooldown > 0)  distanceMoanCooldown--;
        if (helpmeCooldown > 0)        helpmeCooldown--;
        if (mimicHelpmeCooldown > 0)   mimicHelpmeCooldown--;
        if (screamCooldown > 0)        screamCooldown--;

        // Adjust speed by current mode.
        double speed;
        if (isDisguised())             speed = SPEED_DISGUISED;
        else if (isClimbing())         speed = SPEED_CLIMB;
        else if (isFaceThiefAggressive()) speed = SPEED_CHASE;
        else                           speed = SPEED_WALK;
        net.minecraft.world.entity.ai.attributes.AttributeInstance attr =
                this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null && Math.abs(attr.getBaseValue() - speed) > 1e-6) {
            attr.setBaseValue(speed);
        }

        Player nearest = this.level.getNearestPlayer(this, 64.0D);

        // STALKING mode: stares at player, disappears when looked at.
        if (isStalking()) {
            if (nearest != null) {
                lockYawTo(nearest);
                if (isPlayerStaringAt(nearest)) {
                    this.discard();
                    return;
                }
            }
            // Random ambient noises play even while stalking.
            tickAmbientSounds(nearest);
            return;
        }

        // DISGUISED mode: walks toward the player, then stops when close enough and stares.
        // Gets aggressive on contact (< 3 blocks) or when hit (handled in hurt()).
        if (isDisguised()) {
            if (nearest != null) {
                double dist = this.distanceTo(nearest);
                if (dist < 3.0D) {
                    // Within striking range -> reveal and aggro.
                    setDisguise(Disguise.NONE);
                    setFaceThiefAggressive(true);
                } else if (dist < 4.5D) {
                    // Close enough - stop walking and just stare.
                    this.getNavigation().stop();
                    lockYawTo(nearest);
                } else {
                    // Walk toward the player at disguise speed.
                    this.getNavigation().moveTo(nearest, 1.0D);
                }
            } else {
                this.getNavigation().stop();
            }
            // Shape-shift every 240 ticks (12 seconds). Over a 5-minute life this happens ~25
            // times. Don't shift while the player is staring directly at us - it'd look glitchy.
            if (aliveTicks > 0 && aliveTicks % 240 == 0) {
                boolean staring = nearest != null && isPlayerStaringAt(nearest);
                if (!staring) {
                    Disguise[] forms = { Disguise.PIG, Disguise.CHICKEN, Disguise.COW,
                                         Disguise.VILLAGER, Disguise.SHEEP, Disguise.HORSE };
                    Disguise current = getDisguise();
                    Disguise next;
                    int tries = 0;
                    do {
                        next = forms[this.random.nextInt(forms.length)];
                        tries++;
                    } while (next == current && tries < 6);
                    setDisguise(next);
                }
            }
            tickAmbientSounds(nearest);
            return;
        }

        // NORMAL / AGGRESSIVE mode:

        if (nearest != null) {
            // If the player LOOKS at him while he's just walking around, that triggers chase
            // (per user spec).
            if (!isFaceThiefAggressive() && isPlayerStaringAt(nearest)) {
                setFaceThiefAggressive(true);
            }
            // Touch also triggers chase.
            if (!isFaceThiefAggressive() && this.distanceTo(nearest) < 1.6D) {
                setFaceThiefAggressive(true);
            }
        }

        tickAmbientSounds(nearest);
    }

    private void lockYawTo(Player p) {
        double dx = p.getX() - this.getX();
        double dz = p.getZ() - this.getZ();
        float yaw = (float) (Math.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
        // Hard-stop interpolation so vanilla goals can't drift the head during stopped states.
        // Without these, LookAtPlayerGoal lerps and the head wanders subtly.
        this.yHeadRotO = yaw;
        this.yBodyRotO = yaw;
        this.yRotO     = yaw;
        this.setYRot(yaw);
    }

    private void tickAmbientSounds(Player nearest) {
        // The user said "all other sounds plays randomly". These play on independent timers.
        if (ambientSoundCooldown <= 0) {
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.AMBIENT.get(), SoundSource.HOSTILE, 0.7F, 1.0F);
            ambientSoundCooldown = 400 + this.random.nextInt(600);
        }
        if (distanceMoanCooldown <= 0 && nearest != null) {
            this.level.playSound(null, nearest.getX(), nearest.getY(), nearest.getZ(),
                    ModSounds.DISTANCE_MOAN.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
            distanceMoanCooldown = 1200 + this.random.nextInt(2400);
        }
        if (helpmeCooldown <= 0 && nearest != null) {
            // "Help me" played at a position offset from the player so it sounds like another voice.
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double dist = 8 + this.random.nextInt(8);
            double px = nearest.getX() + Math.cos(angle) * dist;
            double pz = nearest.getZ() + Math.sin(angle) * dist;
            this.level.playSound(null, px, nearest.getY(), pz,
                    ModSounds.HELPME.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            helpmeCooldown = 1600 + this.random.nextInt(2400);
        }
        if (mimicHelpmeCooldown <= 0 && nearest != null) {
            this.level.playSound(null, nearest.getX(), nearest.getY(), nearest.getZ(),
                    ModSounds.MIMIC_HELPME.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
            mimicHelpmeCooldown = 2400 + this.random.nextInt(3600);
        }
        if (screamCooldown <= 0 && isFaceThiefAggressive()) {
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.SCREAM.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
            screamCooldown = 800 + this.random.nextInt(800);
        }
    }

    private boolean isPlayerStaringAt(Player p) {
        if (this.distanceTo(p) > 48.0D) return false;
        double dx = this.getX() - p.getX();
        double dy = this.getEyeY() - p.getEyeY();
        double dz = this.getZ() - p.getZ();
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001D) return false;
        dx /= len; dy /= len; dz /= len;
        Vec3 look = p.getViewVector(1.0F);
        double dot = look.x * dx + look.y * dy + look.z * dz;
        return dot > 0.9D && p.hasLineOfSight(this);
    }

    // === Animations ===========================================================

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 3, this::predicate));
        data.addAnimationController(new AnimationController<>(this, "attack_controller", 0, this::attackPredicate));
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        AnimationController<?> controller = event.getController();

        // When disguised, suppress all face_thief animations - the renderer draws a vanilla
        // animal model instead.
        if (isDisguised()) {
            return PlayState.STOP;
        }

        if (this.isClimbing()) {
            controller.setAnimation(new AnimationBuilder().loop("animation.face_thief.climb"));
            return PlayState.CONTINUE;
        }

        if (event.isMoving()) {
            if (isFaceThiefAggressive()) {
                controller.setAnimation(new AnimationBuilder().loop("animation.face_thief.run"));
            } else {
                controller.setAnimation(new AnimationBuilder().loop("animation.face_thief.walk"));
            }
            return PlayState.CONTINUE;
        }

        controller.setAnimation(new AnimationBuilder().loop("animation.face_thief.idle"));
        return PlayState.CONTINUE;
    }

    private <E extends IAnimatable> PlayState attackPredicate(AnimationEvent<E> event) {
        AnimationController<?> controller = event.getController();
        if (this.swinging && controller.getAnimationState() == AnimationState.Stopped) {
            controller.markNeedsReload();
            controller.setAnimation(new AnimationBuilder()
                    .addAnimation("animation.face_thief.attack", EDefaultLoopTypes.PLAY_ONCE));
            this.swinging = false;
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() { return factory; }

    // === Sounds ===============================================================

    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSounds.HURT.get(); }
    @Override protected SoundEvent getDeathSound()              { return ModSounds.SCREAM.get(); }
    @Override protected float getSoundVolume()                  { return 1.0F; }

    // === Damage / attack ======================================================

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && source.getEntity() instanceof LivingEntity) {
            // Hitting him - if disguised, reveal + aggro.
            if (isDisguised()) {
                setDisguise(Disguise.NONE);
            }
            setFaceThiefAggressive(true);
        }
        return result;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        this.swing(InteractionHand.MAIN_HAND);
        return super.doHurtTarget(target);
    }

    // === Removal hook =========================================================

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level.isClientSide) {
            // Aftermath only if this Face Thief was actively hunting (was aggressive).
            if (isFaceThiefAggressive()) {
                this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.AFTERMATH.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
                stopChaseThemeForNearbyPlayers();
            }
        }
        super.remove(reason);
    }

    private void stopChaseThemeForNearbyPlayers() {
        if (!(this.level instanceof ServerLevel)) return;
        ServerLevel serverLevel = (ServerLevel) this.level;
        ResourceLocation sound = ModSounds.CHASE_THEME.get().getLocation();
        ClientboundStopSoundPacket packet = new ClientboundStopSoundPacket(sound, SoundSource.HOSTILE);
        for (ServerPlayer p : serverLevel.players()) {
            if (p.distanceToSqr(this) < 96.0D * 96.0D) {
                p.connection.send(packet);
            }
        }
    }

    // === NBT ==================================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Aggressive", isFaceThiefAggressive());
        tag.putBoolean("Stalking",   isStalking());
        tag.putInt("Disguise",       this.entityData.get(DATA_DISGUISE));
        tag.putInt("AliveTicks",     aliveTicks);
        tag.putInt("LastNoticed",    lastNoticedTicks);
        tag.putInt("NonDisguiseTicks", nonDisguiseTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("Aggressive")) setFaceThiefAggressive(true);
        if (tag.getBoolean("Stalking"))   setStalking(true);
        setDisguise(Disguise.values()[Math.max(0, Math.min(tag.getInt("Disguise"), Disguise.values().length - 1))]);
        aliveTicks = tag.getInt("AliveTicks");
        lastNoticedTicks = tag.getInt("LastNoticed");
        nonDisguiseTicks = tag.getInt("NonDisguiseTicks");
    }

    @Override public boolean canBeAffected(MobEffectInstance effect) { return true; }
}
