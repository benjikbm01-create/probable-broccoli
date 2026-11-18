package dev.forgottenisland.client.render;

import dev.forgottenisland.ForgottenIsland;
import dev.forgottenisland.game.WardenEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** The final adversary: player-shaped model with its own skin and glowing eyes. */
public class WardenRenderer extends LivingEntityRenderer<WardenEntity, PlayerEntityModel<WardenEntity>> {
    static final Identifier TEX = Identifier.of(ForgottenIsland.MOD_ID, "textures/entity/npc/warden.png");
    static final RenderLayer EYES = RenderLayer.getEyes(Identifier.of(ForgottenIsland.MOD_ID, "textures/entity/warden_eyes.png"));

    public WardenRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false) {
            @Override public void setAngles(WardenEntity e, float a, float b, float c, float d, float f) {
                rightArmPose = BipedEntityModel.ArmPose.ITEM;
                super.setAngles(e, a, b, c, d, f);
                if (e.isAttacking()) { rightArm.pitch = -1.6f + rightArm.pitch * 0.2f; leftArm.pitch = -1.4f; }
            }
        }, 0.6f);
        addFeature(new HeldItemFeatureRenderer<>(this, ctx.getHeldItemRenderer()));
        addFeature(new EyesFeatureRenderer<>(this) { @Override public RenderLayer getEyesTexture() { return EYES; } });
    }
    @Override protected void scale(WardenEntity e, MatrixStack m, float amount) { m.scale(1.12f, 1.12f, 1.12f); }
    @Override public Identifier getTexture(WardenEntity e) { return TEX; }
}
