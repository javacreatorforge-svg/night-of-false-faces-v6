package com.redstonedev.nightoffalsefaces.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The "Gut" block - a small flat slab of meat that the Face Thief leaves behind.
 * The voxel shape matches the model's slab cube exactly (from [5,0,4] to [11,0.5,10]).
 * Breaks with stone or better pickaxe; drops nothing otherwise (handled in loot table).
 */
public class GutBlock extends Block {

    private static final VoxelShape SHAPE = Shapes.box(
            5.0 / 16.0, 0.0,         4.0 / 16.0,
            11.0 / 16.0, 0.5 / 16.0, 10.0 / 16.0);

    public GutBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Thin enough that the player can walk over it; no real collision.
        return Shapes.empty();
    }
}
