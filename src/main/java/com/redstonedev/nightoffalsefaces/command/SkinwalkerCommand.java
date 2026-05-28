package com.redstonedev.nightoffalsefaces.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.redstonedev.nightoffalsefaces.entity.FaceThiefEntity;
import com.redstonedev.nightoffalsefaces.init.ModEntities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

/**
 * Adds:
 *   /skinwalker run shapeshifting  - spawns a Face Thief already disguised (shape-shifting)
 *   /skinwalker run stalk          - spawns a Face Thief in stalking mode
 */
public class SkinwalkerCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("skinwalker")
                        .requires(src -> src.hasPermission(0)) // anyone can use
                        .then(Commands.literal("run")
                                .then(Commands.literal("shapeshifting")
                                        .executes(ctx -> spawn(ctx, true)))
                                .then(Commands.literal("stalk")
                                        .executes(ctx -> spawn(ctx, false)))));
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx, boolean shapeshifting) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        ServerLevel level = source.getLevel();

        // Spawn ~12 blocks in front of the player.
        Vec3 look = player.getLookAngle();
        double x = player.getX() + look.x * 12.0;
        double z = player.getZ() + look.z * 12.0;
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                (int) Math.floor(x), (int) Math.floor(z));

        FaceThiefEntity thief = ModEntities.FACE_THIEF.get().create(level);
        if (thief == null) {
            source.sendFailure(Component.literal("Failed to create the skinwalker."));
            return 0;
        }
        thief.moveTo(x + 0.5, y, z + 0.5, player.getYRot() + 180.0F, 0);
        thief.finalizeSpawn(level, level.getCurrentDifficultyAt(thief.blockPosition()),
                MobSpawnType.COMMAND, null, null);

        if (shapeshifting) {
            // Start disguised - it picks a random animal form and shape-shifts as it does normally.
            FaceThiefEntity.Disguise[] forms = {
                    FaceThiefEntity.Disguise.PIG, FaceThiefEntity.Disguise.CHICKEN,
                    FaceThiefEntity.Disguise.COW, FaceThiefEntity.Disguise.VILLAGER,
                    FaceThiefEntity.Disguise.SHEEP, FaceThiefEntity.Disguise.HORSE };
            thief.setDisguise(forms[level.getRandom().nextInt(forms.length)]);
        } else {
            thief.setStalking(true);
        }

        level.addFreshEntity(thief);

        final boolean ss = shapeshifting;
        source.sendSuccess(Component.literal(
                "Spawned a skinwalker in " + (ss ? "shape-shifting" : "stalking") + " mode."), false);
        return 1;
    }
}
