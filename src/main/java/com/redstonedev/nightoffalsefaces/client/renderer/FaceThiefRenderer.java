package com.redstonedev.nightoffalsefaces.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.redstonedev.nightoffalsefaces.client.model.FaceThiefModel;
import com.redstonedev.nightoffalsefaces.entity.FaceThiefEntity;
import com.redstonedev.nightoffalsefaces.entity.FaceThiefEntity.Disguise;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

import java.util.EnumMap;

/**
 * Renderer for the Face Thief.
 *
 * Normally renders the GeckoLib model. When the entity reports a non-NONE Disguise,
 * the renderer instead delegates to the vanilla renderer for that animal type
 * (Pig, Chicken, Cow, Villager, Sheep, Horse) using a cached fake entity instance
 * whose position is synced to the Face Thief just-in-time.
 */
@OnlyIn(Dist.CLIENT)
public class FaceThiefRenderer extends GeoEntityRenderer<FaceThiefEntity> {

    private final EnumMap<Disguise, Entity> fakeCache = new EnumMap<>(Disguise.class);

    public FaceThiefRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new FaceThiefModel());
        this.shadowRadius = 0.5F;
        // The geo model was authored at the original 0.9 x 2.4 hitbox. Hitbox is now
        // 1.0 x 3.2 (~1.33x taller), so scale the visual the same amount so what the
        // player sees matches what they can hit.
        this.widthScale  = 1.33F;
        this.heightScale = 1.33F;
    }

    @Override
    public void render(FaceThiefEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        Disguise disguise = entity.getDisguise();
        if (disguise == Disguise.NONE) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            return;
        }

        Entity fake = fakeCache.get(disguise);
        if (fake == null) {
            fake = createFake(disguise);
            if (fake == null) {
                // Fallback - couldn't create the fake; just render true form.
                super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
                return;
            }
            fakeCache.put(disguise, fake);
        }

        // Sync the fake's transform with the Face Thief just before we render it.
        fake.setPos(entity.getX(), entity.getY(), entity.getZ());
        fake.setYRot(entityYaw);
        fake.setXRot(0);
        fake.yRotO = entityYaw;
        fake.xRotO = 0;
        if (fake instanceof LivingEntity) {
            LivingEntity le = (LivingEntity) fake;
            le.yBodyRot = entityYaw;
            le.yBodyRotO = entityYaw;
            le.yHeadRot = entityYaw;
            le.yHeadRotO = entityYaw;
            // Show as a docile, idle animal - no walking animation, no attack.
            le.animationSpeed = 0F;
            le.animationPosition = 0F;
        }
        fake.tickCount = entity.tickCount;

        // Use MC's own renderer for the disguise animal.
        @SuppressWarnings("unchecked")
        EntityRenderer<Entity> r = (EntityRenderer<Entity>)
                Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(fake);
        if (r != null) {
            r.render(fake, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        } else {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        }
    }

    private Entity createFake(Disguise disguise) {
        if (Minecraft.getInstance().level == null) return null;
        EntityType<?> type = null;
        if (disguise == Disguise.PIG)      type = EntityType.PIG;
        else if (disguise == Disguise.CHICKEN)  type = EntityType.CHICKEN;
        else if (disguise == Disguise.COW)      type = EntityType.COW;
        else if (disguise == Disguise.VILLAGER) type = EntityType.VILLAGER;
        else if (disguise == Disguise.SHEEP)    type = EntityType.SHEEP;
        else if (disguise == Disguise.HORSE)    type = EntityType.HORSE;
        return type != null ? type.create(Minecraft.getInstance().level) : null;
    }
}
