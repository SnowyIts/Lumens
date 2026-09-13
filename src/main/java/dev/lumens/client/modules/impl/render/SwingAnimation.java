package dev.lumens.client.modules.impl.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.ModeSetting;
import dev.lumens.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "SwingAnimation",
   category = Category.RENDER,
   description = "Кастомные анимации удара"
)
public final class SwingAnimation extends Module {
   public static final SwingAnimation INSTANCE = new SwingAnimation();
   public ModeSetting animationMode = new ModeSetting("Режим", new String[]{"Smooth", "Self", "Self2", "Down", "Forward", "Touch", "Pander", "Curt", "BlockHit", "Spin", "Slide", "Old", "Stab"});
   public NumberSetting swingPower = new NumberSetting("Сила", 5.0F, 1.0F, 10.0F, 1.0F, () -> {
      return !this.animationMode.is("BlockHit") && !this.animationMode.is("Pander") && !this.animationMode.is("Curt");
   });
   public NumberSetting speed = new NumberSetting("Скорость", 7.0F, 0.0F, 10.0F, 1.0F);
   public NumberSetting angle = new NumberSetting("Угол", 0.0F, 0.0F, 360.0F, 1.0F, () -> {
      return this.animationMode.is("Self") || this.animationMode.is("Self2");
   });

   private SwingAnimation() {
   }

   public void renderSwordAnimation(MatrixStack matrices, float swingProgress, float equipProgress, Arm arm) {
      float anim = (float)Math.sin((double)swingProgress * 1.5707963267948966D * 2.0D);
      float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
      String m = this.animationMode.get();

      float f;
      float g;
      float sinExtra;
      if (m.equals("Smooth")) {
         matrices.translate(0.56F, -0.52F, -0.72F);
         f = this.swingPower.getCurrent() * 10.0F;
         g = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F + g * (-f / 4.0F)));
         sinExtra = MathHelper.sin(MathHelper.sqrt(swingProgress * swingProgress) * 3.1415927F);
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sinExtra * -(f / 4.0F)));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sinExtra * -f));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45.0F));
         return;
      }
      if (m.equals("Self2")) {
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-30.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-this.angle.getCurrent() - this.swingPower.getCurrent() * 10.0F * anim));
         return;
      }
      if (m.equals("Forward")) {
         matrices.translate(0.56F, -0.52F, -0.72F);
         f = 35.0F;
         matrices.translate(0.0D, 0.0D, -0.3D * (double)sin2);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -f));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * f));
         return;
      }
      if (m.equals("Self")) {
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-60.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-this.angle.getCurrent() - this.swingPower.getCurrent() * 10.0F * anim));
         return;
      }
      if (m.equals("Down")) {
         matrices.translate(0.56F, -0.52F - anim * this.swingPower.getCurrent() / 24.0F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-30.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
         return;
      }
      if (m.equals("Touch")) {
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.scale(1.0F, 1.0F, 1.0F + anim * this.swingPower.getCurrent() / 4.0F);
         matrices.translate(0.0F, 0.0F, -0.265F);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0F));
         return;
      }
      if (m.equals("Curt")) {
         matrices.translate(0.56F, -0.52F, -0.72F);
         f = MathHelper.sqrt(swingProgress);
         g = MathHelper.sin(f * 3.1415927F);
         sinExtra = MathHelper.sin(swingProgress * 3.1415927F);
         matrices.translate(0.4F - g * 0.2F, -0.2F + g * 0.3F, -0.5F - sinExtra * 0.2F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(91.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F + g * -100.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F));
         return;
      }
      if (m.equals("Pander")) {
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.scale(0.8F, 0.8F, 0.8F);
         f = 1.0F - MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), mc.gameRenderer.firstPersonRenderer.prevEquipProgressMainHand, mc.gameRenderer.firstPersonRenderer.equipProgressMainHand);
         matrices.translate(0.3D - (double)(anim * 0.15F), (double)(0.2F - f * 0.12F), (double)(-0.15F - anim * 0.13F));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76.0F - 10.0F * anim));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-16.0F - 8.0F * anim));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-83.0F - 26.0F * anim));
         return;
      }
      if (m.equals("BlockHit")) {
         matrices.translate(0.56F, -0.52F, -0.72F);
         f = MathHelper.sin((float)((double)(swingProgress * swingProgress) * 3.141592653589793D));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F));
         g = MathHelper.sin((float)((double)MathHelper.sqrt(swingProgress) * 3.141592653589793D));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f * -20.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(g * -20.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
         matrices.translate(0.4F, 0.2F, 0.2F);
         matrices.translate(-0.5F, 0.08F, 0.0F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
         return;
      }
      if (m.equals("Spin")) {
         matrices.translate(0.56F, -0.52F, -0.72F);
         float spin = swingProgress * 360.0F * (this.swingPower.getCurrent() / 5.0F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F + spin * 0.4F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * -60.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -90.0F));
         matrices.translate(0.0D, -0.1D * sin2, -0.15D * sin2);
         return;
      }
      if (m.equals("Slide")) {
         matrices.translate(0.56F, -0.52F, -0.72F);
         float slide = MathHelper.sin(swingProgress * 3.1415927F);
         matrices.translate(-0.3D * slide * (this.swingPower.getCurrent() / 5.0F), 0.15D * slide, -0.4D * slide);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F - slide * 30.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(slide * -50.0F));
         return;
      }
      if (m.equals("Old")) {
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F));
         float punch = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
         matrices.translate(0.1D * punch, -0.1D * punch, -0.2D * punch);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(punch * -90.0F));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45.0F));
         return;
      }
      if (m.equals("Stab")) {
         matrices.translate(0.56F, -0.52F, -0.72F);
         float stab = MathHelper.sin(swingProgress * 3.1415927F);
         matrices.translate(0.0D, 0.0D, -0.55D * stab * (this.swingPower.getCurrent() / 5.0F));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-25.0F - stab * 25.0F));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45.0F));
         return;
      }

   }

   private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
      int i = arm == Arm.RIGHT ? 1 : -1;
      matrices.translate((float)i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
   }

   private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
      int i = arm == Arm.RIGHT ? 1 : -1;
      float f = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * (45.0F + f * -20.0F)));
      float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)i * g * -20.0F));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * -45.0F));
   }
}
