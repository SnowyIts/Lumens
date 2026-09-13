package dev.lumens.utility.mixin.client;

import com.google.common.base.MoreObjects;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.HeldItemRenderer.HandRenderType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.lumens.client.modules.impl.render.SwingAnimation;
import dev.lumens.client.modules.impl.render.ViewModel;

@Mixin({HeldItemRenderer.class})
public abstract class HeldItemRendererMixin {
   @Shadow
   private ItemStack mainHand;
   @Shadow
   private float equipProgressMainHand;
   @Shadow
   private float prevEquipProgressMainHand;
   @Shadow
   private float prevEquipProgressOffHand;
   @Shadow
   private float equipProgressOffHand;
   @Shadow
   private ItemStack offHand;

   @Shadow
   private void renderFirstPersonItem(AbstractClientPlayerEntity var1, float var2, float var3, Hand var4, float var5, ItemStack var6, float var7, MatrixStack var8, VertexConsumerProvider var9, int var10) {
      throw new AssertionError();
   }

   @Shadow
   private void swingArm(float var1, float var2, MatrixStack var3, int var4, Arm var5) {
      throw new AssertionError();
   }

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
   ordinal = 0
)}
   )
   public void injectBeforeRenderCrossBowItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
      ViewModel viewModel = ViewModel.INSTANCE;
      if (viewModel.isEnabled()) {
         boolean isMainHand = hand == Hand.MAIN_HAND;
         Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
         viewModel.applyHandScale(matrices, arm);
      }

   }

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
   ordinal = 1
)}
   )
   public void injectBeforeRenderItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
      ViewModel viewModel = ViewModel.INSTANCE;
      if (viewModel.isEnabled()) {
         boolean isMainHand = hand == Hand.MAIN_HAND;
         Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
         viewModel.applyHandScale(matrices, arm);
      }

   }

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
   shift = Shift.AFTER,
   ordinal = 0
)}
   )
   public void injectAfterMatrixPushHandPosition(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
      ViewModel viewModel = ViewModel.INSTANCE;
      if (viewModel.isEnabled() && !item.isEmpty() && !item.contains(DataComponentTypes.MAP_ID)) {
         boolean isMainHand = hand == Hand.MAIN_HAND;
         Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
         viewModel.applyHandPosition(matrices, arm);
      }

   }

   @Redirect(
      method = {"renderFirstPersonItem"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FFLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V",
   ordinal = 2
)
   )
   public void redirectSwingArmForCustomAnim(HeldItemRenderer instance, float swingProgress, float equipProgress, MatrixStack matrices, int armX, Arm arm) {
      SwingAnimation swingAnimation = SwingAnimation.INSTANCE;
      if (swingAnimation.isEnabled()) {
         if (arm == Arm.RIGHT) {
            swingAnimation.renderSwordAnimation(matrices, swingProgress, equipProgress, arm);
         } else {
            this.swingArm(swingProgress, equipProgress, matrices, armX, arm);
         }
      } else {
         this.swingArm(swingProgress, equipProgress, matrices, armX, arm);
      }

   }

   /**
    * @author javelin
    * @reason render hook
    */
   @Overwrite
   public void renderItem(float tickDelta, MatrixStack matrices, Immediate vertexConsumers, ClientPlayerEntity player, int light) {
      float f = player.getHandSwingProgress(tickDelta);
      Hand hand = (Hand)MoreObjects.firstNonNull(player.preferredHand, Hand.MAIN_HAND);
      float g = player.getLerpedPitch(tickDelta);
      HandRenderType handRenderType = HeldItemRenderer.getHandRenderType(player);
      float j;
      float k;
      if (handRenderType.renderMainHand) {
         j = hand == Hand.MAIN_HAND ? f : 0.0F;
         k = 1.0F - MathHelper.lerp(tickDelta, this.prevEquipProgressMainHand, this.equipProgressMainHand);
         this.renderFirstPersonItem(player, tickDelta, g, Hand.MAIN_HAND, j, this.mainHand, k, matrices, vertexConsumers, light);
      }

      if (handRenderType.renderOffHand) {
         j = hand == Hand.OFF_HAND ? f : 0.0F;
         k = 1.0F - MathHelper.lerp(tickDelta, this.prevEquipProgressOffHand, this.equipProgressOffHand);
         this.renderFirstPersonItem(player, tickDelta, g, Hand.OFF_HAND, j, this.offHand, k, matrices, vertexConsumers, light);
      }

      vertexConsumers.draw();
   }
}
