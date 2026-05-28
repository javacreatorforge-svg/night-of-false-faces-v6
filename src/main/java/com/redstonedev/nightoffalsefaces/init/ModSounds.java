package com.redstonedev.nightoffalsefaces.init;

import com.redstonedev.nightoffalsefaces.NightOfFalseFaces;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, NightOfFalseFaces.MODID);

    public static final RegistryObject<SoundEvent> HURT          = register("face_thief_hurt");
    public static final RegistryObject<SoundEvent> AMBIENT       = register("face_thief_ambient");
    public static final RegistryObject<SoundEvent> SCREAM        = register("face_thief_scream");
    public static final RegistryObject<SoundEvent> AFTERMATH     = register("face_thief_aftermath");
    public static final RegistryObject<SoundEvent> CHASE_THEME   = register("face_thief_chase_theme");
    public static final RegistryObject<SoundEvent> DISTANCE_MOAN = register("face_thief_distance_moan");
    public static final RegistryObject<SoundEvent> HELPME        = register("face_thief_helpme");
    public static final RegistryObject<SoundEvent> MIMIC_HELPME  = register("face_thief_mimicking_helpme_echo");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> new SoundEvent(new ResourceLocation(NightOfFalseFaces.MODID, name)));
    }
}
