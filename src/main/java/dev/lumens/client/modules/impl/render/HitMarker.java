package dev.lumens.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import dev.lumens.Lumens;
import dev.lumens.base.events.impl.player.EventAttack;
import dev.lumens.base.events.impl.render.EventHudRender;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.BooleanSetting;
import dev.lumens.client.modules.api.setting.impl.ColorSetting;
import dev.lumens.client.modules.api.setting.impl.ModeSetting;
import dev.lumens.client.modules.api.setting.impl.NumberSetting;
import dev.lumens.utility.render.display.Render2DUtil;
import dev.lumens.utility.render.display.base.CustomDrawContext;
import dev.lumens.utility.render.display.base.color.ColorRGBA;

@ModuleAnnotation(
   name = "HitMarker",
   category = Category.RENDER,
   description = "Маркер попадания в прицел"
)
public final class HitMarker extends Module {
   public static final HitMarker INSTANCE = new HitMarker();
   private final ModeSetting colorMode = new ModeSetting("Цвет", new String[]{"Белый", "Тема", "Кастом", "Rainbow"});
   private final ColorSetting customColor = new ColorSetting("Кастом цвет", new ColorRGBA(255, 60, 60, 255), () -> this.colorMode.is("Кастом"));
   private final NumberSetting size = new NumberSetting("Размер", 5.0F, 2.0F, 12.0F, 0.5F);
   private final NumberSetting gap = new NumberSetting("Разрыв", 2.5F, 0.0F, 8.0F, 0.5F);
   private final NumberSetting thickness = new NumberSetting("Толщина", 1.2F, 0.5F, 3.0F, 0.1F);
   private final NumberSetting showTime = new NumberSetting("Время (мс)", 400.0F, 150.0F, 1200.0F, 50.0F);
   private final BooleanSetting outline = new BooleanSetting("Обводка", true);

   private long lastHit = 0L;

   private HitMarker() {
   }

   @EventTarget
   public void onAttack(EventAttack e) {
      this.lastHit = System.currentTimeMillis();
   }

   @EventTarget
   public void onRender(EventHudRender event) {
      if (mc.player == null || mc.world == null) return;
      if (mc.options.getPerspective() != Perspective.FIRST_PERSON) return;
      long elapsed = System.currentTimeMillis() - this.lastHit;
      if (elapsed > (long) this.showTime.getCurrent()) return;
      float progress = MathHelper.clamp((float) elapsed / this.showTime.getCurrent(), 0.0F, 1.0F);
      float alpha = 1.0F - progress;
      float expand = progress * 3.0F;
      CustomDrawContext ctx = event.getContext();
      float cx = (float) mc.getWindow().getScaledWidth() / 2.0F;
      float cy = (float) mc.getWindow().getScaledHeight() / 2.0F;
      float s = this.size.getCurrent();
      float g = this.gap.getCurrent() + expand;
      float t = this.thickness.getCurrent();
      ColorRGBA color = this.resolveColor().withAlpha((int) (alpha * 255.0F));
      // 4 диагональные черточки
      this.drawDiagonal(ctx, cx - g - s, cy - g - s, cx - g, cy - g, t, color);
      this.drawDiagonal(ctx, cx + g, cy - g, cx + g + s, cy - g - s, t, color);
      this.drawDiagonal(ctx, cx - g - s, cy + g + s, cx - g, cy + g, t, color);
      this.drawDiagonal(ctx, cx + g, cy + g, cx + g + s, cy + g + s, t, color);
   }

   private void drawDiagonal(CustomDrawContext ctx, float x1, float y1, float x2, float y2, float thickness, ColorRGBA color) {
      float dx = x2 - x1;
      float dy = y2 - y1;
      float len = (float) Math.sqrt(dx * dx + dy * dy);
      if (len < 0.001F) return;
      float nx = -dy / len * thickness / 2.0F;
      float ny = dx / len * thickness / 2.0F;
      if (this.outline.isEnabled()) {
         this.quad(ctx, x1 - nx * 1.6F, y1 - ny * 1.6F, x2 + dx / len * 0.6F - nx * 1.6F, y2 + dy / len * 0.6F - ny * 1.6F,
               x2 + dx / len * 0.6F + nx * 1.6F, y2 + dy / len * 0.6F + ny * 1.6F, x1 + nx * 1.6F, y1 + ny * 1.6F, new ColorRGBA(0, 0, 0, (int) (color.getAlpha() * 0.6F)));
      }
      this.quad(ctx, x1 - nx, y1 - ny, x2 - nx, y2 - ny, x2 + nx, y2 + ny, x1 + nx, y1 + ny, color);
   }

   private void quad(CustomDrawContext ctx, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, ColorRGBA color) {
      com.mojang.blaze3d.systems.RenderSystem.enableBlend();
      com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
      net.minecraft.client.render.BufferBuilder buffer = net.minecraft.client.render.Tessellator.getInstance()
            .begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_COLOR);
      org.joml.Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
      int c = color.getRGB();
      buffer.vertex(m, x1, y1, 0.0F).color(c);
      buffer.vertex(m, x2, y2, 0.0F).color(c);
      buffer.vertex(m, x3, y3, 0.0F).color(c);
      buffer.vertex(m, x4, y4, 0.0F).color(c);
      net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buffer.end());
      com.mojang.blaze3d.systems.RenderSystem.disableBlend();
   }

   private ColorRGBA resolveColor() {
      switch (this.colorMode.get()) {
         case "Тема": return Lumens.getInstance().getThemeManager().getCurrentTheme().getColor();
         case "Кастом": return this.customColor.getColor();
         case "Rainbow": return new ColorRGBA(Render2DUtil.rainbow(300, 0.6F, 1.0F));
         default: return new ColorRGBA(255, 255, 255, 255);
      }
   }
}
