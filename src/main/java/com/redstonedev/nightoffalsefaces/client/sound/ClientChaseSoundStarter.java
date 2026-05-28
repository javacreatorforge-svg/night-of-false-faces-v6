package com.redstonedev.nightoffalsefaces.client.sound;

import com.redstonedev.nightoffalsefaces.entity.FaceThiefEntity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientChaseSoundStarter {
    private ClientChaseSoundStarter() {}
    public static void start(FaceThiefEntity entity) {
        Minecraft.getInstance().getSoundManager().play(new FaceThiefChaseSoundInstance(entity));
    }
}
