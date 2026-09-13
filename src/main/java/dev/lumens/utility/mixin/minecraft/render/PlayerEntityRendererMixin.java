package dev.lumens.utility.mixin.minecraft.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.lumens.client.cosmetics.CosmeticSelfMarker;
import dev.lumens.client.modules.impl.render.EntityESP;

@Mixin({PlayerEntityRenderer.class})
public class PlayerEntityRendererMixin {
   @Inject(
      method = {"renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void render(PlayerEntityRenderState playerEntityRenderState, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
      if (EntityESP.INSTANCE.isEnabled()) {
         ci.cancel();
      }

   }

   /** Помечаем состояние локального игрока, чтобы косметика рисовалась только на себе. */
   @Inject(
      method = {"updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V"},
      at = {@At("TAIL")}
   )
   private void javelin$markSelf(AbstractClientPlayerEntity entity, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
      try {
         boolean self = false;
         try {
            self = entity != null && entity == MinecraftClient.getInstance().player;
         } catch (Exception ignored) {
         }
         ((CosmeticSelfMarker) state).javelin$setSelf(self);
      } catch (Exception ignored) {
      }
   }
}
