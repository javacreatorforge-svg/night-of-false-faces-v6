package com.redstonedev.nightoffalsefaces.client.sound;

import com.redstonedev.nightoffalsefaces.entity.FaceThiefEntity;
import com.redstonedev.nightoffalsefaces.init.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FaceThiefChaseSoundInstance extends AbstractTickableSoundInstance {

    private final FaceThiefEntity faceThief;

    public FaceThiefChaseSoundInstance(FaceThiefEntity entity) {
        super(ModSounds.CHASE_THEME.get(), SoundSource.HOSTILE, RandomSource.create());
        this.faceThief = entity;
        this.looping = true;     // gapless loop, sound engine handles repetition
        this.delay   = 0;
        this.volume  = 1.0F;
        this.pitch   = 1.0F;
        this.attenuation = Attenuation.LINEAR;
        this.x = entity.getX();
        this.y = entity.getY();
        this.z = entity.getZ();
    }

    @Override
    public void tick() {
        if (faceThief.isRemoved()
                || !faceThief.isAlive()
                || !faceThief.isFaceThiefAggressive()
                || faceThief.isStalking()
                || faceThief.isDisguised()) {
            this.stop();
            return;
        }
        this.x = faceThief.getX();
        this.y = faceThief.getY();
        this.z = faceThief.getZ();
    }
}
