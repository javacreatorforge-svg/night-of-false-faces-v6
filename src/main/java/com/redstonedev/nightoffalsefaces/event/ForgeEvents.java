package com.redstonedev.nightoffalsefaces.event;

import com.redstonedev.nightoffalsefaces.NightOfFalseFaces;
import com.redstonedev.nightoffalsefaces.entity.FaceThiefEntity;
import com.redstonedev.nightoffalsefaces.entity.FaceThiefEntity.Disguise;
import com.redstonedev.nightoffalsefaces.init.ModBlocks;
import com.redstonedev.nightoffalsefaces.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import com.redstonedev.nightoffalsefaces.command.SkinwalkerCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Random;

public class ForgeEvents {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        SkinwalkerCommand.register(event.getDispatcher());
    }

    private static final Random RNG = new Random();
    private int tickCounter = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCounter++;
        if (tickCounter % 100 != 0) return; // ~5s cadence

        if (event.getServer() == null) return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            tryAmbientSpawn(level);
            tryDropGutBlock(level);
        }
    }

    // --- Spawning -------------------------------------------------------------

    /** True if any Face Thief is currently loaded in this dimension. */
    private boolean hasFaceThief(ServerLevel level) {
        return !level.getEntities(ModEntities.FACE_THIEF.get(), n -> !n.isRemoved()).isEmpty();
    }

    private boolean isPlayerUnderground(ServerLevel level, ServerPlayer p) {
        BlockPos pos = p.blockPosition();
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        return pos.getY() < surfaceY - 5;
    }

    private void tryAmbientSpawn(ServerLevel level) {
        List<? extends ServerPlayer> players = level.players();
        if (players.isEmpty()) return;
        // One Face Thief per dimension at a time.
        if (hasFaceThief(level)) return;

        for (ServerPlayer player : players) {
            boolean underground = isPlayerUnderground(level, player);
            boolean night = !level.isDay() || underground;
            // Equal frequency day and night - the player wanted to actually see day shape-shifters.
            int chance = 300;
            if (RNG.nextInt(chance) != 0) continue;

            BlockPos spawnPos = underground
                    ? pickCaveSpawnPos(level, player, 8, 16)
                    : pickSurfaceSpawnPos(level, player, 14, 28);
            if (spawnPos == null) continue;

            FaceThiefEntity ent = ModEntities.FACE_THIEF.get().create(level);
            if (ent == null) return;
            ent.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    level.getRandom().nextFloat() * 360F, 0);

            // Per spec: "Randomly, he doesn't stalk and just shapeshifts into an Minecraft Animal
            // or gets aggressive and chase the player." So roll: stalk / disguise / walk.
            // Both day and night now lean HEAVILY toward shape-shifting - the player wanted way
            // less stalking and tons more disguises.
            //   night / cave -> still some stalk (25%), mostly disguise (65%)
            //   day surface  -> very little stalk (10%), almost entirely disguise (80%)
            int r = RNG.nextInt(100);
            int stalkCutoff    = night ? 25 : 10;
            int disguiseCutoff = stalkCutoff + (night ? 65 : 80);
            if (r < stalkCutoff) {
                ent.setStalking(true);
            } else if (r < disguiseCutoff) {
                Disguise[] forms = { Disguise.PIG, Disguise.CHICKEN, Disguise.COW,
                                     Disguise.VILLAGER, Disguise.SHEEP, Disguise.HORSE };
                ent.setDisguise(forms[RNG.nextInt(forms.length)]);
            } else {
                // WALK normally - will turn aggressive if the player looks at him.
            }

            ent.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                    MobSpawnType.EVENT, null, null);
            level.addFreshEntity(ent);

            NightOfFalseFaces.LOGGER.debug("Spawned Face Thief ({}, mode={}) for {}",
                    underground ? "CAVE" : "SURFACE",
                    ent.isStalking() ? "STALK" : (ent.isDisguised() ? ("DISGUISE:" + ent.getDisguise()) : "WALK"),
                    player.getName().getString());
            return; // one per dimension per tick
        }
    }

    /** Pick a position 14-28 blocks from the player on solid ground. */
    private BlockPos pickSurfaceSpawnPos(ServerLevel level, ServerPlayer player, int minDist, int maxDist) {
        BlockPos origin = player.blockPosition();
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = RNG.nextDouble() * Math.PI * 2.0;
            double dist = minDist + RNG.nextInt(Math.max(1, maxDist - minDist + 1));
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * dist);
            // MOTION_BLOCKING - biome-agnostic, lands on real solid ground (skips flowers/grass).
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos candidate = new BlockPos(x, y, z);
            if (level.getBlockState(candidate).isAir()
                    && level.getBlockState(candidate.above()).isAir()
                    && !level.getBlockState(candidate.below()).isAir()) {
                return candidate;
            }
        }
        return null;
    }

    /** Pick a position near the player at their Y level for cave spawns. */
    private BlockPos pickCaveSpawnPos(ServerLevel level, ServerPlayer player, int minDist, int maxDist) {
        BlockPos origin = player.blockPosition();
        for (int attempt = 0; attempt < 32; attempt++) {
            double angle = RNG.nextDouble() * Math.PI * 2.0;
            double dist = minDist + RNG.nextInt(Math.max(1, maxDist - minDist + 1));
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * dist);
            for (int dy = 0; dy <= 8; dy++) {
                for (int sign : new int[]{ 1, -1 }) {
                    if (dy == 0 && sign == -1) continue;
                    int y = origin.getY() + sign * dy;
                    BlockPos candidate = new BlockPos(x, y, z);
                    if (level.getBlockState(candidate).isAir()
                            && level.getBlockState(candidate.above()).isAir()
                            && level.getBlockState(candidate.below()).getMaterial().isSolid()) {
                        if (candidate.distSqr(origin) < (double)(minDist * minDist)) continue;
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    // --- Gut block random placement -------------------------------------------

    /**
     * Per spec: "Sometimes, Gut block appears on top of an block then stays there".
     * Very rare per 5-second tick - drops one Gut block near a random player on a valid
     * surface position.
     */
    private void tryDropGutBlock(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            // ~1 in 1500 every 5s per player = ~once every ~2 hours of play per player.
            if (RNG.nextInt(1500) != 0) continue;
            BlockPos origin = player.blockPosition();
            for (int attempt = 0; attempt < 16; attempt++) {
                int dx = RNG.nextInt(33) - 16;
                int dz = RNG.nextInt(33) - 16;
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                BlockPos candidate = new BlockPos(x, y, z);
                BlockState here = level.getBlockState(candidate);
                BlockState below = level.getBlockState(candidate.below());
                if (here.isAir() && below.getMaterial().isSolid()
                        && !below.is(ModBlocks.GUT.get())) {
                    level.setBlock(candidate, ModBlocks.GUT.get().defaultBlockState(), 3);
                    break;
                }
            }
        }
    }
}
