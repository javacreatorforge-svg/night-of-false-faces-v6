package com.redstonedev.nightoffalsefaces.init;

import com.redstonedev.nightoffalsefaces.NightOfFalseFaces;
import com.redstonedev.nightoffalsefaces.block.GutBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, NightOfFalseFaces.MODID);

    public static final RegistryObject<Block> GUT = BLOCKS.register("gut",
            () -> new GutBlock(BlockBehaviour.Properties
                    .of(Material.PLANT)
                    .strength(1.0F, 1.0F)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));   // only drops with proper tool (stone+ pickaxe via loot table)
}
