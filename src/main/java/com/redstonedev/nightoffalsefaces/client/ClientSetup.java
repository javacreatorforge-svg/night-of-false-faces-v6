package com.redstonedev.nightoffalsefaces.client;

import com.redstonedev.nightoffalsefaces.client.renderer.FaceThiefRenderer;
import com.redstonedev.nightoffalsefaces.init.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientSetup {
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityRenderers.register(ModEntities.FACE_THIEF.get(), FaceThiefRenderer::new);
        });
    }
}
