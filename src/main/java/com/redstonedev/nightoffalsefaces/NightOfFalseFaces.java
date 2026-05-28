package com.redstonedev.nightoffalsefaces;

import com.mojang.logging.LogUtils;
import com.redstonedev.nightoffalsefaces.client.ClientSetup;
import com.redstonedev.nightoffalsefaces.entity.FaceThiefEntity;
import com.redstonedev.nightoffalsefaces.event.ForgeEvents;
import com.redstonedev.nightoffalsefaces.init.ModBlocks;
import com.redstonedev.nightoffalsefaces.init.ModEntities;
import com.redstonedev.nightoffalsefaces.init.ModItems;
import com.redstonedev.nightoffalsefaces.init.ModSounds;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import software.bernie.geckolib3.GeckoLib;

@Mod(NightOfFalseFaces.MODID)
public class NightOfFalseFaces {
    public static final String MODID = "night_of_false_faces";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NightOfFalseFaces() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        GeckoLib.initialize();

        ModEntities.ENTITIES.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModSounds.SOUND_EVENTS.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::entityAttributes);

        MinecraftForge.EVENT_BUS.register(new ForgeEvents());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Night of False Faces - common setup. He's wearing a different face tonight.");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        ClientSetup.onClientSetup(event);
    }

    private void entityAttributes(final EntityAttributeCreationEvent event) {
        event.put(ModEntities.FACE_THIEF.get(), FaceThiefEntity.createAttributes().build());
    }
}
