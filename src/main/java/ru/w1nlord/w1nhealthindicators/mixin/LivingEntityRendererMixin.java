
package ru.w1nlord.w1nhealthindicators.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.AtlasManager;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.w1nlord.w1nhealthindicators.config.Config;
import ru.w1nlord.w1nhealthindicators.util.ServerUtil;

import java.util.Map;
import java.util.WeakHashMap;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends EntityRenderer<T, S> implements FeatureRendererContext<S, M> {
    @Unique private static final Identifier ARMOR_EMPTY_TEXTURE = Identifier.ofVanilla("hud/armor_empty");
    @Unique private static final Identifier ARMOR_HALF_TEXTURE = Identifier.ofVanilla("hud/armor_half");
    @Unique private static final Identifier ARMOR_FULL_TEXTURE = Identifier.ofVanilla("hud/armor_full");
    @Unique private static final Identifier GUI_ATLAS_TEXTURE = Identifier.ofVanilla("textures/atlas/gui.png");
    @Unique private static final MinecraftClient mc = MinecraftClient.getInstance();
    @Unique private static final Map<LivingEntityRenderState, LivingEntity> entitiesByState = new WeakHashMap<>();

    protected LivingEntityRendererMixin(EntityRendererFactory.Context context) {
        super(context);
    }

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void saveEntity(T entity, S state, float tickDelta, CallbackInfo ci) {
        if (Config.shouldRenderHearts() || Config.shouldRenderArmor()) entitiesByState.put(state, entity);
        else entitiesByState.remove(state);
    }

    @Inject(method = "render*", at = @At("HEAD"))
    private void renderHealth(S state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
        LivingEntity entity = entitiesByState.get(state);
        boolean renderHearts = Config.shouldRenderHearts();
        boolean renderArmor = Config.shouldRenderArmor();

        if (entity == null || (!renderHearts && !renderArmor) || entity.isInvisible() || entity == mc.player || entity instanceof ArmorStandEntity || entity instanceof PlayerEntity player && ServerUtil.isNPC(player)) return;

        int armor = renderArmor ? entity.getArmor() : 0;
        int health = renderHearts ? MathHelper.ceil(ServerUtil.resolveHealth(entity, Config.getHealthSource())) : 0;
        int absorption = renderHearts ? MathHelper.ceil(entity.getAbsorptionAmount()) : 0;
        int maxHealth = MathHelper.ceil((float) entity.getAttributeValue(EntityAttributes.MAX_HEALTH));
        int heartCount = renderHearts ? MathHelper.ceil(Math.max(maxHealth, health) / 2.0F) : MathHelper.ceil(maxHealth / 2.0F);
        int totalHeartCount = renderHearts ? heartCount + MathHelper.ceil(absorption / 2.0F) : heartCount;

        if ((!renderHearts || totalHeartCount <= 0) && armor <= 0) return;
        float scale = Config.getScale();

        matrices.push();
        matrices.translate(0.0D, entity.getHeight() + 0.5F * scale, 0.0F);

        if ((entity.hasCustomName() || entity instanceof PlayerEntity) && state.squaredDistanceToCamera <= 4096.0D) matrices.translate(0.0D, 9.0F * 1.15F * 0.025F, 0.0F);

        matrices.multiply(cameraState.orientation);
        matrices.scale(-0.025F * scale, 0.025F * scale, 0.025F);

        boolean hardcore = entity.getEntityWorld().getLevelProperties().isHardcore();
        int rows = MathHelper.ceil(totalHeartCount / 10.0F);
        int lines = Math.max(10 - (rows - 2), 3);
        float maxX = (Math.min(totalHeartCount, 10) * 8.0F + 1.0F) / 2.0F;
        float armorY = renderHearts ? (rows - 1) * lines + 10.0F : 0.0F;

        AtlasManager atlas = mc.getAtlasManager();
        RenderLayer layer = RenderLayers.entityTranslucent(GUI_ATLAS_TEXTURE);

        queue.submitCustom(matrices, layer, (entry, vertices) -> {
            Matrix4f model = entry.getPositionMatrix();
            if (renderHearts && totalHeartCount > 0) drawHearts(model, vertices, entity, health, absorption, heartCount, totalHeartCount, rows, lines, maxX, hardcore, atlas);
            if (armor > 0) drawArmor(model, vertices, armor, maxX, armorY, atlas);
        });

        matrices.pop();
    }

    @Unique
    private static void drawHearts(Matrix4f model, VertexConsumer vertices, LivingEntity entity, int health, int absorption, int heartCount, int totalHeartCount, int rows, int lines, float maxX, boolean hardcore, AtlasManager atlas) {
        InGameHud.HeartType type = getHeartType(entity);
        InGameHud.HeartType absorptionType = type == InGameHud.HeartType.WITHERED ? type : InGameHud.HeartType.ABSORBING;

        for (int i = totalHeartCount - 1; i >= 0; i--) {
            int row = i / 10;
            float x = maxX - i % 10 * 8.0F;
            float y = row * lines;
            float z = row * 0.01F;

            drawSprite(model, vertices, atlas, InGameHud.HeartType.CONTAINER.getTexture(hardcore, false, false), x, y, z);

            int value = i * 2;

            if (i >= heartCount) {
                int remaining = value - heartCount * 2;
                if (remaining < absorption) drawSprite(model, vertices, atlas, absorptionType.getTexture(hardcore, remaining + 1 == absorption, false), x, y, z);
            } else if (value < health) {
                drawSprite(model, vertices, atlas, type.getTexture(hardcore, value + 1 == health, false), x, y, z);
            }
        }
    }

    @Unique
    private static void drawArmor(Matrix4f model, VertexConsumer vertices, int armor, float maxX, float y, AtlasManager atlas) {
        for (int i = 0; i < 10; i++) {
            Identifier texture = i * 2 + 1 < armor ? ARMOR_FULL_TEXTURE : i * 2 + 1 == armor ? ARMOR_HALF_TEXTURE : ARMOR_EMPTY_TEXTURE;
            drawSprite(model, vertices, atlas, texture, maxX - i * 8.0F, y, -0.01F);
        }
    }

    @Unique
    private static InGameHud.HeartType getHeartType(LivingEntity entity) {
        if (entity.hasStatusEffect(StatusEffects.POISON)) return InGameHud.HeartType.POISONED;
        if (entity.hasStatusEffect(StatusEffects.WITHER)) return InGameHud.HeartType.WITHERED;
        return entity.isFrozen() ? InGameHud.HeartType.FROZEN : InGameHud.HeartType.NORMAL;
    }

    @Unique
    private static void drawSprite(Matrix4f model, VertexConsumer vertices, AtlasManager atlas, Identifier texture, float x, float y, float z) {
        Sprite sprite = atlas.getSprite(new SpriteIdentifier(GUI_ATLAS_TEXTURE, texture));
        drawVertex(model, vertices, x, y - 9.0F, z, sprite.getMinU(), sprite.getMaxV());
        drawVertex(model, vertices, x - 9.0F, y - 9.0F, z, sprite.getMaxU(), sprite.getMaxV());
        drawVertex(model, vertices, x - 9.0F, y, z, sprite.getMaxU(), sprite.getMinV());
        drawVertex(model, vertices, x, y, z, sprite.getMinU(), sprite.getMinV());
    }

    @Unique
    private static void drawVertex(Matrix4f model, VertexConsumer vertices, float x, float y, float z, float u, float v) {
        vertices.vertex(model, x, y, z).color(255, 255, 255, 255).texture(u, v).overlay(OverlayTexture.packUv(0, 10)).light(15728880).normal(0.0F, 1.0F, 0.0F);
    }
}
