package com.redstonedev.nightoffalsefaces.init;

import com.redstonedev.nightoffalsefaces.NightOfFalseFaces;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, NightOfFalseFaces.MODID);

    // Spawn egg - white tints so the custom texture renders as-drawn.
    public static final RegistryObject<ForgeSpawnEggItem> FACE_THIEF_SPAWN_EGG =
            ITEMS.register("face_thief_spawn_egg",
                    () -> new ForgeSpawnEggItem(
                            ModEntities.FACE_THIEF,
                            0xFFFFFF, 0xFFFFFF,
                            new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    // BlockItem for the gut block - uses its own flat item model + texture.
    public static final RegistryObject<Item> GUT = ITEMS.register("gut",
            () -> new BlockItem(ModBlocks.GUT.get(), new Item.Properties().tab(CreativeModeTab.TAB_MISC)));
}
