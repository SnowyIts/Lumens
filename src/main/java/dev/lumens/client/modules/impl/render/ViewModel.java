package dev.lumens.client.modules.impl.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.BooleanSetting;
import dev.lumens.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "ViewModel",
   category = Category.RENDER,
   description = "Настройка позиции"
)
public final class ViewModel extends Module {
   public static final ViewModel INSTANCE = new ViewModel();
   public final NumberSetting leftX = new NumberSetting("Левая рука X", 0.0F, -1.0F, 1.0F, 0.1F);
   public final NumberSetting leftY = new NumberSetting("Левая рука Y", 0.0F, -1.0F, 1.0F, 0.1F);
   public final NumberSetting leftZ = new NumberSetting("Левая рука Z", 0.0F, -1.0F, 1.0F, 0.1F);
   public final NumberSetting leftScale = new NumberSetting("Левая рука размер", 1.0F, 0.5F, 1.5F, 0.05F);
   public final NumberSetting rightX = new NumberSetting("Правая рука X", 0.0F, -1.0F, 1.0F, 0.1F);
   public final NumberSetting rightY = new NumberSetting("Правая рука Y", 0.0F, -1.0F, 1.0F, 0.1F);
   public final NumberSetting rightZ = new NumberSetting("Правая рука Z", 0.0F, -1.0F, 1.0F, 0.1F);
   public final NumberSetting rightScale = new NumberSetting("Правая рука размер", 1.0F, 0.5F, 1.5F, 0.05F);
   public final BooleanSetting sway = new BooleanSetting("Покачивание", false);
   public final NumberSetting swayAmount = new NumberSetting("Сила покачивания", 0.5F, 0.0F, 2.0F, 0.1F, () -> this.sway.isEnabled());
   public final BooleanSetting swingTilt = new BooleanSetting("Наклон при ударе", false);
   public final NumberSetting tiltStrength = new NumberSetting("Сила наклона", 12.0F, 0.0F, 45.0F, 1.0F, () -> this.swingTilt.isEnabled());

   private ViewModel() {
   }

   public void applyHandScale(MatrixStack matrices, Arm arm) {
      if (this.isEnabled()) {
         if (arm == Arm.RIGHT) {
            matrices.scale(this.rightScale.getCurrent(), this.rightScale.getCurrent(), this.rightScale.getCurrent());
         } else {
            matrices.scale(this.leftScale.getCurrent(), this.leftScale.getCurrent(), this.leftScale.getCurrent());
         }
      } else {
         matrices.scale(1.0F, 1.0F, 1.0F);
      }

   }

   public void applyHandPosition(MatrixStack matrices, Arm arm) {
      if (this.isEnabled()) {
         if (arm == Arm.RIGHT) {
            matrices.translate(this.rightX.getCurrent(), this.rightY.getCurrent(), this.rightZ.getCurrent());
         } else {
            matrices.translate(-this.leftX.getCurrent(), this.leftY.getCurrent(), this.leftZ.getCurrent());
         }
         if (this.sway.isEnabled() && mc.player != null) {
            float amount = this.swayAmount.getCurrent() * 0.02F;
            double t = System.currentTimeMillis() / 900.0D;
            float strafe = mc.player.sidewaysSpeed;
            float forward = mc.player.forwardSpeed;
            float moveFactor = Math.min(1.0F, Math.abs(strafe) + Math.abs(forward));
            if (mc.player.isSprinting()) moveFactor *= 1.4F;
            matrices.translate(Math.cos(t) * amount * moveFactor, Math.sin(t * 2.0D) * amount * 0.6F * moveFactor, 0.0F);
         }
         if (this.swingTilt.isEnabled() && mc.player != null) {
            float swing = mc.player.getHandSwingProgress(mc.getRenderTickCounter().getTickDelta(false));
            if (swing > 0.0F) {
               float tilt = (float)Math.sin(swing * Math.PI) * this.tiltStrength.getCurrent();
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(arm == Arm.RIGHT ? -tilt * 0.4F : tilt * 0.4F));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-tilt * 0.3F));
            }
         }
      } else {
         matrices.translate(0.0F, 0.0F, 0.0F);
      }

   }
}
