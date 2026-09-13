package dev.lumens.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import dev.lumens.Lumens;
import dev.lumens.base.animations.base.Animation;
import dev.lumens.base.animations.base.Easing;
import dev.lumens.base.events.impl.render.EventRender3D;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.BooleanSetting;
import dev.lumens.client.modules.api.setting.impl.ColorSetting;
import dev.lumens.client.modules.api.setting.impl.ModeSetting;
import dev.lumens.client.modules.api.setting.impl.NumberSetting;
import dev.lumens.client.modules.impl.combat.Aura;
import dev.lumens.utility.render.display.Render2DUtil;
import dev.lumens.utility.render.display.base.color.ColorRGBA;
import dev.lumens.utility.render.display.shader.DrawUtil;
import dev.lumens.utility.render.level.Render3DUtil;

@ModuleAnnotation(
   name = "TargetESP",
   category = Category.RENDER,
   description = "Выделяет цель"
)
public class TargetESP extends Module {
   public static final TargetESP INSTANCE = new TargetESP();
   private final ModeSetting mode = new ModeSetting("Мод", new String[]{"Маркер", "Призраки", "Круг", "Луч"});
   private final ModeSetting colorMode = new ModeSetting("Цвет", new String[]{"Тема", "Кастом", "Rainbow"});
   private final ColorSetting customColor = new ColorSetting("Кастом цвет", new ColorRGBA(0, 200, 255, 255), () -> this.colorMode.is("Кастом"));
   private final NumberSetting size = new NumberSetting("Размер", 1.0F, 0.5F, 2.0F, 0.1F, () -> !this.mode.is("Призраки"));
   private final NumberSetting speed = new NumberSetting("Скорость", 1.0F, 0.2F, 3.0F, 0.1F, () -> this.mode.is("Круг") || this.mode.is("Луч") || this.mode.is("Маркер"));
   private final NumberSetting lineWidth = new NumberSetting("Толщина", 2.0F, 1.0F, 5.0F, 0.5F, () -> this.mode.is("Круг") || this.mode.is("Луч"));
   private final BooleanSetting beam = new BooleanSetting("Вертикальный луч", true, () -> this.mode.is("Круг"));
   private final BooleanSetting fadeOut = new BooleanSetting("Плавное исчезновение", true);
   private final Animation animation;
   private final Animation animation2;
   private Entity lastTarget;
   private boolean textureLoaded;
   private float rotationAngle;
   private float rotationSpeed;
   private boolean isReversing;
   private float animationNurik;
   private long currentTime;

   public TargetESP() {
      this.animation = new Animation(400L, Easing.CUBIC_OUT);
      this.animation2 = new Animation(250L, Easing.CUBIC_OUT);
      this.lastTarget = null;
      this.textureLoaded = false;
      this.rotationAngle = 0.0F;
      this.rotationSpeed = 0.0F;
      this.isReversing = false;
      this.animationNurik = 0.0F;
   }

   public void onEnable() {
      super.onEnable();
   }

   @EventTarget
   private void onRenderWorldLast(EventRender3D e) {
      if (this.mode.is("Призраки")) {
         this.drawSpiritsTrack(e);
         return;
      }
      if (this.mode.is("Круг")) {
         this.drawCircle(e);
         return;
      }
      if (this.mode.is("Луч")) {
         this.drawBeam(e);
         return;
      }

      if (!this.textureLoaded) {
            MinecraftClient.getInstance().getTextureManager().registerTexture(Identifier.of("javelin", "hud/marker.png"), new ResourceTexture(Identifier.of("javelin", "hud/marker.png")));
            this.textureLoaded = true;
         }

         Vec3d camPos = mc.gameRenderer.getCamera().getPos();
         Entity target = Aura.INSTANCE.getTarget();
         if (target != null) {
            this.lastTarget = target;
            this.animation.update(true);
         } else {
            this.animation.update(false);
            if (this.animation.getValue() == 0.0F) {
               this.lastTarget = null;
            }
         }

         if (this.lastTarget != null) {
            double tickDelta = (double)e.getPartialTicks();
            MatrixStack matrices = e.getMatrix();
            double x = MathHelper.lerp(tickDelta, this.lastTarget.lastRenderX, this.lastTarget.getX());
            double y = MathHelper.lerp(tickDelta, this.lastTarget.lastRenderY, this.lastTarget.getY()) + (double)this.lastTarget.getHeight() / 2.0D;
            double z = MathHelper.lerp(tickDelta, this.lastTarget.lastRenderZ, this.lastTarget.getZ());
            matrices.push();
            matrices.translate(x - camPos.x, y - camPos.y, z - camPos.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
            float scale = 0.15F * this.animation.getValue();
            matrices.scale(-scale, -scale, scale);
            this.updateRotation();
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotationAngle));
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            Identifier textureId = Identifier.of("javelin", "icons/marker.png");
            AbstractTexture abstractTexture = MinecraftClient.getInstance().getTextureManager().getTexture(textureId);
            int texId = abstractTexture.getGlId();
            float alpha = this.fadeOut.isEnabled() ? this.animation.getValue() : 1.0F;
            float size = 12.0F * this.size.getCurrent();
            ColorRGBA color = this.resolveColor(0).withAlpha((int)(alpha * 255.0F));
            DrawUtil.drawTexture(matrices, textureId, 0.0F - size / 2.0F, 0.0F - size / 2.0F, size, size, color);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            matrices.pop();
         }
   }

   @Native
   private void updateRotation() {
      float s = this.speed.getCurrent();
      if (!this.isReversing) {
         this.rotationSpeed += 0.01F * s;
         if ((double)this.rotationSpeed > 2.3D) {
            this.rotationSpeed = 2.3F;
            this.isReversing = true;
         }
      } else {
         this.rotationSpeed -= 0.01F * s;
         if ((double)this.rotationSpeed < -2.3D) {
            this.rotationSpeed = -2.3F;
            this.isReversing = false;
         }
      }

      this.rotationAngle += this.rotationSpeed;
      this.rotationAngle %= 360.0F;
   }

   private ColorRGBA resolveColor(int index) {
      String cm = this.colorMode.get();
      switch (cm) {
         case "Кастом":
            return this.customColor.getColor();
         case "Rainbow":
            return new ColorRGBA(Render2DUtil.rainbow(300, 0.6F, 1.0F));
         default:
            return Lumens.getInstance().getThemeManager().getClientColor(index);
      }
   }

   private Entity resolveTarget() {
      Entity target = Aura.INSTANCE.getTarget();
      if (target != null) {
         this.lastTarget = target;
         this.animation.update(true);
      } else {
         this.animation.update(!this.fadeOut.isEnabled());
         if (!this.fadeOut.isEnabled() || this.animation.getValue() <= 0.01F) {
            if (this.fadeOut.isEnabled() && this.animation.getValue() <= 0.01F) {
               this.lastTarget = null;
            }
            if (!this.fadeOut.isEnabled()) {
               this.lastTarget = null;
            }
         }
      }
      if (!this.fadeOut.isEnabled()) {
         return target;
      }
      return this.lastTarget;
   }

   private void drawCircle(EventRender3D e) {
      Entity target = this.resolveTarget();
      if (target == null) return;
      float alpha = this.fadeOut.isEnabled() ? this.animation.getValue() : 1.0F;
      if (alpha <= 0.01F) return;
      double tickDelta = e.getPartialTicks();
      double x = MathHelper.lerp(tickDelta, target.lastRenderX, target.getX());
      double y = MathHelper.lerp(tickDelta, target.lastRenderY, target.getY()) + 0.05D;
      double z = MathHelper.lerp(tickDelta, target.lastRenderZ, target.getZ());
      float radius = (target.getWidth() + 0.4F) * this.size.getCurrent();
      float width = this.lineWidth.getCurrent();
      double time = System.currentTimeMillis() / 1000.0D * this.speed.getCurrent();
      int segments = 48;
      for (int layer = 0; layer < 2; layer++) {
         float r = radius + layer * 0.12F;
         float aOffset = (float)(time * (layer == 0 ? 1.6D : -1.1D));
         for (int i = 0; i < segments; i++) {
            double a1 = (Math.PI * 2.0D * i) / segments + aOffset;
            double a2 = (Math.PI * 2.0D * (i + 1)) / segments + aOffset;
            int skip = segments / 4;
            int mod = i % (skip * 2);
            if (mod >= skip && layer == 0) continue;
            if (mod < skip && layer == 1) continue;
            Vec3d p1 = new Vec3d(x + Math.cos(a1) * r, y + Math.sin(time * 2.0D + i * 0.3D) * 0.03D, z + Math.sin(a1) * r);
            Vec3d p2 = new Vec3d(x + Math.cos(a2) * r, y + Math.sin(time * 2.0D + (i + 1) * 0.3D) * 0.03D, z + Math.sin(a2) * r);
            ColorRGBA c1 = this.resolveColor(i * 4).withAlpha((int)(alpha * 255.0F));
            ColorRGBA c2 = this.resolveColor((i + 1) * 4).withAlpha((int)(alpha * 255.0F));
            Render3DUtil.drawLine(p1, p2, c1.getRGB(), c2.getRGB(), width, false);
         }
      }
      if (this.beam.isEnabled()) {
         ColorRGBA c = this.resolveColor(0).withAlpha((int)(alpha * 120.0F));
         Vec3d bottom = new Vec3d(x, y, z);
         Vec3d top = new Vec3d(x, y + target.getHeight() + 0.6D, z);
         Render3DUtil.drawLine(bottom, top, c.getRGB(), c.withAlpha(0).getRGB(), width * 0.6F, false);
      }
   }

   private void drawBeam(EventRender3D e) {
      Entity target = this.resolveTarget();
      if (target == null) return;
      float alpha = this.fadeOut.isEnabled() ? this.animation.getValue() : 1.0F;
      if (alpha <= 0.01F) return;
      double tickDelta = e.getPartialTicks();
      double x = MathHelper.lerp(tickDelta, target.lastRenderX, target.getX());
      double y = MathHelper.lerp(tickDelta, target.lastRenderY, target.getY());
      double z = MathHelper.lerp(tickDelta, target.lastRenderZ, target.getZ());
      float width = this.lineWidth.getCurrent();
      double h = target.getHeight() + 0.5D;
      double time = System.currentTimeMillis() / 700.0D * this.speed.getCurrent();
      float radius = (target.getWidth() + 0.35F) * this.size.getCurrent();
      int segments = 32;
      for (int ring = 0; ring < 3; ring++) {
         double ry = y + 0.1D + (h - 0.2D) * ring / 2.0D;
         for (int i = 0; i < segments; i++) {
            double a1 = (Math.PI * 2.0D * i) / segments + time;
            double a2 = (Math.PI * 2.0D * (i + 1)) / segments + time;
            Vec3d p1 = new Vec3d(x + Math.cos(a1) * radius, ry, z + Math.sin(a1) * radius);
            Vec3d p2 = new Vec3d(x + Math.cos(a2) * radius, ry, z + Math.sin(a2) * radius);
            Render3DUtil.drawLine(p1, p2, this.resolveColor(i * 6 + ring * 40).withAlpha((int)(alpha * 255.0F)).getRGB(), width, false);
         }
      }
      Vec3d bottom = new Vec3d(x, y, z);
      Vec3d top = new Vec3d(x, y + h + 0.4D, z);
      ColorRGBA cTop = this.resolveColor(0).withAlpha(0);
      ColorRGBA cBottom = this.resolveColor(0).withAlpha((int)(alpha * 255.0F));
      Render3DUtil.drawLine(bottom, top, cBottom.getRGB(), cTop.getRGB(), width, false);
      Render3DUtil.drawLine(new Vec3d(x - radius, y + h + 0.4D, z), new Vec3d(x + radius, y + h + 0.4D, z), cBottom.withAlpha((int)(alpha * 120)).getRGB(), width * 0.5F, false);
   }

   public static double interpolate(double current, double old, double scale) {
      return old + (current - old) * scale;
   }

   private void drawSpiritsTrack(EventRender3D event3D) {
      Aura aura = Aura.INSTANCE;
      this.animation2.update(aura.getTarget() != null && aura.isEnabled());
      if ((double)this.animation2.getValue() != 0.0D) {
         MatrixStack e = event3D.getMatrix();
         if (aura.getTarget() != null) {
            if (this.lastTarget == null) {
               this.currentTime = System.currentTimeMillis();
            }

            this.lastTarget = aura.getTarget();
         }

         if (this.lastTarget != null) {
            this.animationNurik += (float)(5L * (System.currentTimeMillis() - this.currentTime)) / 600.0F;
            this.currentTime = System.currentTimeMillis();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, Lumens.id("icons/glow.png"));
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(770, 1, 0, 1);
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            double x = interpolate(this.lastTarget.getX(), this.lastTarget.lastRenderX, (double)event3D.getPartialTicks()) - mc.gameRenderer.getCamera().getPos().getX();
            double y = interpolate(this.lastTarget.getY(), this.lastTarget.lastRenderY, (double)event3D.getPartialTicks()) - mc.gameRenderer.getCamera().getPos().getY();
            double z = interpolate(this.lastTarget.getZ(), this.lastTarget.lastRenderZ, (double)event3D.getPartialTicks()) - mc.gameRenderer.getCamera().getPos().getZ();
            int n2 = 3;
            int n3 = 12;
            int n4 = 3 * n2;
            e.push();
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for(int i = 0; i < n4; i += n2) {
               for(int j = 0; j < n3; ++j) {
                  ColorRGBA color = this.resolveColor(j * 12 + i * 20);
                  float f2 = this.animationNurik + (float)j * 0.1F;
                  float f3 = 0.8F;
                  float f4 = 0.5F;
                  int n5 = (int)Math.pow((double)i, 2.0D);
                  e.push();
                  e.translate(x + (double)(f3 * MathHelper.sin(f2 + (float)n5)), y + (double)f4 + (double)(0.3F * MathHelper.sin(this.animationNurik + (float)j * 0.2F)) + (double)(0.2F * (float)i), z + (double)(f3 * MathHelper.cos(f2 - (float)n5)));
                  e.scale(this.animation2.getValue() * (0.005F + (float)j / 2000.0F), this.animation2.getValue() * (0.005F + (float)j / 2000.0F), this.animation2.getValue() * (0.005F + (float)j / 2000.0F));
                  e.multiply(mc.gameRenderer.getCamera().getRotation());
                  int n7 = -25;
                  int n8 = 50;
                  buffer.vertex(e.peek().getPositionMatrix(), (float)n7, (float)(n7 + n8), 0.0F).texture(0.0F, 1.0F).color(color.withAlpha((int)(this.animation2.getValue() * 255.0F)).getRGB());
                  buffer.vertex(e.peek().getPositionMatrix(), (float)(n7 + n8), (float)(n7 + n8), 0.0F).texture(1.0F, 1.0F).color(color.withAlpha((int)(this.animation2.getValue() * 255.0F)).getRGB());
                  buffer.vertex(e.peek().getPositionMatrix(), (float)(n7 + n8), (float)n7, 0.0F).texture(1.0F, 0.0F).color(color.withAlpha((int)(this.animation2.getValue() * 255.0F)).getRGB());
                  buffer.vertex(e.peek().getPositionMatrix(), (float)n7, (float)n7, 0.0F).texture(0.0F, 0.0F).color(color.withAlpha((int)(this.animation2.getValue() * 255.0F)).getRGB());
                  e.pop();
               }
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            e.pop();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.blendFunc(770, 771);
            RenderSystem.enableCull();
         }

      }
   }
}
