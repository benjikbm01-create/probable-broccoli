package dev.forgottenisland.client.render;

import dev.forgottenisland.ForgottenIsland;
import dev.forgottenisland.npc.IslandNpcEntity;
import dev.forgottenisland.npc.NpcDefs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/** Renders island residents with the full player model (both skin layers), slim or wide arms, per-character skin. */
public class NpcRenderer extends LivingEntityRenderer<IslandNpcEntity, PlayerEntityModel<IslandNpcEntity>> {
    private final PlayerEntityModel<IslandNpcEntity> wide, slim;
    private static final Map<String, Identifier> TEX = new HashMap<>();

    /** Player model that can be forced into the seated pose. */
    static class NpcModel extends PlayerEntityModel<IslandNpcEntity> {
        NpcModel(net.minecraft.client.model.ModelPart root, boolean thin) { super(root, thin); }
        @Override public void setAngles(IslandNpcEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
            if (e.mode() == 1) { riding = true; limbDistance = 0; }
            boolean holding = !e.getMainHandStack().isEmpty();
            rightArmPose = holding ? BipedEntityModel.ArmPose.ITEM : BipedEntityModel.ArmPose.EMPTY;
            leftArmPose = !e.getOffHandStack().isEmpty() ? BipedEntityModel.ArmPose.ITEM : BipedEntityModel.ArmPose.EMPTY;
            super.setAngles(e, limbAngle, limbDistance, age, headYaw, headPitch);
            if (e.mode() == 2 && e.handSwingProgress == 0) {   // working: slight forward lean of the arms
                rightArm.pitch -= 0.35f; leftArm.pitch -= 0.2f;
            }
        }
    }

    public NpcRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new NpcModel(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5f);
        wide = this.model;
        slim = new NpcModel(ctx.getPart(EntityModelLayers.PLAYER_SLIM), true);
        addFeature(new HeldItemFeatureRenderer<>(this, ctx.getHeldItemRenderer()));
    }

    @Override public void render(IslandNpcEntity e, float yaw, float tickDelta, MatrixStack m, VertexConsumerProvider vcp, int light) {
        NpcDefs.Def d = NpcDefs.get(e.npcId());
        this.model = d != null && d.slim() ? slim : wide;
        this.model.setVisible(true);
        m.push();
        if (e.mode() == 1) m.translate(0, -0.62, 0);
        super.render(e, yaw, tickDelta, m, vcp, light);
        m.pop();
        double dist = dispatcher.getSquaredDistanceToCamera(e);
        if (dist < 12 * 12 && d != null && !e.isInvisible()) {
            float base = e.getHeight() + (e.mode() == 1 ? -0.1f : 0.35f) - (e.getPose() == EntityPose.SLEEPING ? 1.2f : 0f);
            label(e, m, vcp, Text.literal(d.name()).formatted(Formatting.WHITE), base, light, 0.022f);
            String bark = e.bark();
            if (bark != null && !bark.isEmpty() && dist < 20 * 20) label(e, m, vcp, Text.literal(bark).formatted(Formatting.YELLOW, Formatting.ITALIC), base + 0.3f, light, 0.02f);
        }
    }

    private void label(IslandNpcEntity e, MatrixStack m, VertexConsumerProvider vcp, Text text, float y, int light, float scale) {
        TextRenderer tr = getTextRenderer();
        m.push();
        m.translate(0, y, 0);
        m.multiply(dispatcher.getRotation());
        m.scale(scale, -scale, scale);
        Matrix4f mat = m.peek().getPositionMatrix();
        int bg = (int) (MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25f) * 255f) << 24;
        // wrap long barks
        var lines = tr.wrapLines(text, 220);
        for (int i = 0; i < lines.size(); i++) {
            float x = -tr.getWidth(lines.get(i)) / 2f;
            float ly = (i - lines.size() + 1) * 10f;
            tr.draw(lines.get(i), x, ly, 0x20FFFFFF, false, mat, vcp, TextRenderer.TextLayerType.SEE_THROUGH, bg, light);
            tr.draw(lines.get(i), x, ly, 0xFFFFFFFF, false, mat, vcp, TextRenderer.TextLayerType.NORMAL, 0, light);
        }
        m.pop();
    }

    @Override protected boolean hasLabel(IslandNpcEntity e) { return false; }   // drawn manually above

    @Override protected void scale(IslandNpcEntity e, MatrixStack m, float amount) {
        NpcDefs.Def d = NpcDefs.get(e.npcId());
        float s = d == null ? 0.9375f : d.scale();
        m.scale(s, s, s);
    }

    @Override public Identifier getTexture(IslandNpcEntity e) {
        return TEX.computeIfAbsent(e.npcId(), id -> Identifier.of(ForgottenIsland.MOD_ID, "textures/entity/npc/" + (id.isEmpty() ? "hester" : id) + ".png"));
    }
}
