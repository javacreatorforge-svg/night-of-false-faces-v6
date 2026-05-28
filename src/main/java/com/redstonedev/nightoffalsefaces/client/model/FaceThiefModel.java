package com.redstonedev.nightoffalsefaces.client.model;

import com.redstonedev.nightoffalsefaces.NightOfFalseFaces;
import com.redstonedev.nightoffalsefaces.entity.FaceThiefEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class FaceThiefModel extends AnimatedGeoModel<FaceThiefEntity> {
    private static final ResourceLocation MODEL =
            new ResourceLocation(NightOfFalseFaces.MODID, "geo/face_thief.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NightOfFalseFaces.MODID, "textures/entity/face_thief.png");
    private static final ResourceLocation ANIMATIONS =
            new ResourceLocation(NightOfFalseFaces.MODID, "animations/face_thief.animation.json");

    @Override public ResourceLocation getModelResource(FaceThiefEntity e)     { return MODEL; }
    @Override public ResourceLocation getTextureResource(FaceThiefEntity e)   { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(FaceThiefEntity e) { return ANIMATIONS; }
}
