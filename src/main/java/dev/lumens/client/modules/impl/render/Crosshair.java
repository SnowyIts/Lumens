package dev.lumens.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.hit.HitResult.Type;
import dev.lumens.Lumens;
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
import dev.lumens.utility.render.display.shader.DrawUtil;

@ModuleAnnotation(
   name = "Crosshair",
   category = Category.RENDER,
   description = "Кастомный прицел"
)
public final class Crosshair extends Module {
   public static final Crosshair INSTANCE = new Crosshair();
   private final ModeSetting mode = new ModeSetting("Режим", new String[]{"Классика", "Круг", "Точка", "Крест+Точка"});
   private final ModeSetting colorMode = new ModeSetting("Цвет", new String[]{"Белый", "Тема", "Кастом", "Rainbow"});
   private final ColorSetting customColor = new ColorSetting("Кастом цвет", new ColorRGBA(0, 200, 255, 255), () -> this.colorMode.is("Кастом"));
   private final NumberSetting thickness = new NumberSetting("Толщина", 1.0F, 0.5F, 3.0F, 0.1F, () -> !this.mode.is("Точка") && !this.mode.is("Круг"));
   private final NumberSetting length = new NumberSetting("Длина", 3.0F, 1.0F, 8.0F, 0.5F, () -> !this.mode.is("Точка") && !this.mode.is("Круг"));
   private final NumberSetting gap = new NumberSetting("Разрыв", 2.0F, 0.0F, 5.0F, 0.5F, () -> !this.mode.is("Точка"));
   private final NumberSetting dotSize = new NumberSetting("Размер точки", 1.5F, 0.5F, 4.0F, 0.5F, () -> this.mode.is("Точка") || this.mode.is("Крест+Точка"));
   private final NumberSetting circleRadius = new NumberSetting("Радиус круга", 3.0F, 1.0F, 10.0F, 0.5F, () -> this.mode.is("Круг"));
   private final BooleanSetting dynamicGap = new BooleanSetting("Динамический разрыв", false, () -> !this.mode.is("Точка"));
   private final BooleanSetting outline = new BooleanSetting("Обводка", true);
   private final BooleanSetting shadow = new BooleanSetting("Тень", false);
   private final BooleanSetting useEntityColor = new BooleanSetting("Цвет при наведении", false);
   private final ColorRGBA entityColor = new ColorRGBA(255, 0, 0, 255);

   private Crosshair() {
   }

   @EventTarget
   public void onRender(EventHudRender event) {
      if (mc.player != null && mc.world != null) {
         if (mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            CustomDrawContext ctx = event.getContext();
            float x = (float)mc.getWindow().getScaledWidth() / 2.0F;
            float y = (float)mc.getWindow().getScaledHeight() / 2.0F;
            float currentGap = this.gap.getCurrent();
            float currentThickness;
            if (this.dynamicGap.isEnabled() && !this.mode.is("Точка")) {
               currentThickness = 1.0F - mc.player.getAttackCooldownProgress(0.0F);
               currentGap += 8.0F * currentThickness;
            }

            currentThickness = this.thickness.getCurrent();
            float currentLength = this.length.getCurrent();
            ColorRGBA color = this.resolveColor();
            String m = this.mode.get();
            if (m.equals("Точка")) {
               float s = this.dotSize.getCurrent();
               this.drawLine(ctx, x - s / 2.0F, y - s / 2.0F, s, s, color);
            } else if (m.equals("Круг")) {
               this.drawCircle(ctx, x, y, this.circleRadius.getCurrent() + currentGap, color);
               float s = this.dotSize.getCurrent();
               if (s >= 0.5F) {
                  this.drawLine(ctx, x - s / 2.0F, y - s / 2.0F, s, s, color);
               }
            } else {
               this.drawLine(ctx, x - currentThickness / 2.0F, y - currentGap - currentLength, currentThickness, currentLength, color);
               this.drawLine(ctx, x - currentThickness / 2.0F, y + currentGap, currentThickness, currentLength, color);
               this.drawLine(ctx, x - currentGap - currentLength, y - currentThickness / 2.0F, currentLength, currentThickness, color);
               this.drawLine(ctx, x + currentGap, y - currentThickness / 2.0F, currentLength, currentThickness, color);
               if (m.equals("Крест+Точка")) {
                  float s = this.dotSize.getCurrent();
                  this.drawLine(ctx, x - s / 2.0F, y - s / 2.0F, s, s, color);
               }
            }
         }
      }
   }

   private ColorRGBA resolveColor() {
      if (this.useEntityColor.isEnabled() && mc.crosshairTarget != null && mc.crosshairTarget.getType() == Type.ENTITY) {
         return this.entityColor;
      }
      String cm = this.colorMode.get();
      switch (cm) {
         case "Тема":
            return Lumens.getInstance().getThemeManager().getCurrentTheme().getColor();
         case "Кастом":
            return this.customColor.getColor();
         case "Rainbow":
            return new ColorRGBA(Render2DUtil.rainbow(300, 0.6F, 1.0F));
         default:
            return new ColorRGBA(255, 255, 255, 255);
      }
   }

   private void drawCircle(CustomDrawContext ctx, float cx, float cy, float radius, ColorRGBA color) {
      int segments = 32;
      float prevX = cx + radius;
      float prevY = cy;
      for (int i = 1; i <= segments; i++) {
         double a = (Math.PI * 2.0D * i) / segments;
         float nx = cx + (float)(Math.cos(a) * radius);
         float ny = cy + (float)(Math.sin(a) * radius);
         DrawUtil.drawLine(ctx.getMatrices(), new net.minecraft.util.math.Vec2f(prevX, prevY), new net.minecraft.util.math.Vec2f(nx, ny), color);
         prevX = nx;
         prevY = ny;
      }
      if (this.outline.isEnabled()) {
         ColorRGBA black = new ColorRGBA(0, 0, 0, 120);
         float r2 = radius + 0.6F;
         float px2 = cx + r2;
         float py2 = cy;
         for (int i = 1; i <= segments; i++) {
            double a = (Math.PI * 2.0D * i) / segments;
            float nx = cx + (float)(Math.cos(a) * r2);
            float ny = cy + (float)(Math.sin(a) * r2);
            DrawUtil.drawLine(ctx.getMatrices(), new net.minecraft.util.math.Vec2f(px2, py2), new net.minecraft.util.math.Vec2f(nx, ny), black);
            px2 = nx;
            py2 = ny;
         }
      }
   }

   private void drawLine(CustomDrawContext ctx, float x, float y, float width, float height, ColorRGBA color) {
      if (this.shadow.isEnabled()) {
         ctx.drawRect(x + 0.5F, y + 0.5F, width, height, new ColorRGBA(0, 0, 0, 120));
      }
      if (this.outline.isEnabled()) {
         ctx.drawRect(x - 0.5F, y - 0.5F, width + 1.0F, height + 1.0F, new ColorRGBA(0, 0, 0, 160));
      }
      ctx.drawRect(x, y, width, height, color);
   }
}
