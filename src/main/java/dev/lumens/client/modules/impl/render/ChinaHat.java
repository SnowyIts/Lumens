package dev.lumens.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import dev.lumens.Lumens;
import dev.lumens.base.events.impl.render.EventRender3D;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.BooleanSetting;
import dev.lumens.client.modules.api.setting.impl.ColorSetting;
import dev.lumens.client.modules.api.setting.impl.ModeSetting;
import dev.lumens.client.modules.api.setting.impl.NumberSetting;
import dev.lumens.utility.render.display.Render2DUtil;
import dev.lumens.utility.render.display.base.color.ColorRGBA;
import dev.lumens.utility.render.level.Render3DUtil;

@ModuleAnnotation(
   name = "ChinaHat",
   category = Category.RENDER,
   description = "Шляпа над головой"
)
public final class ChinaHat extends Module {
   public static final ChinaHat INSTANCE = new ChinaHat();
   private final ModeSetting colorMode = new ModeSetting("Цвет", new String[]{"Тема", "Кастом", "Rainbow"});
   private final ColorSetting customColor = new ColorSetting("Кастом цвет", new ColorRGBA(0, 200, 255, 255), () -> this.colorMode.is("Кастом"));
   private final NumberSetting radius = new NumberSetting("Радиус", 0.6F, 0.3F, 1.2F, 0.05F);
   private final NumberSetting height = new NumberSetting("Высота", 0.35F, 0.1F, 0.8F, 0.05F);
   private final NumberSetting segments = new NumberSetting("Сегменты", 24.0F, 8.0F, 48.0F, 1.0F);
   private final NumberSetting lineWidth = new NumberSetting("Толщина", 1.5F, 0.5F, 4.0F, 0.5F);
   private final BooleanSetting spin = new BooleanSetting("Вращение", true);
   private final BooleanSetting fill = new BooleanSetting("Спицы", true);

   private ChinaHat() {
   }

   @EventTarget
   public void onRender3D(EventRender3D e) {
      if (mc.player == null || mc.world == null) return;
      float tickDelta = e.getPartialTicks();
      double x = MathHelper.lerp(tickDelta, mc.player.lastRenderX, mc.player.getX());
      double y = MathHelper.lerp(tickDelta, mc.player.lastRenderY, mc.player.getY());
      double z = MathHelper.lerp(tickDelta, mc.player.lastRenderZ, mc.player.getZ());
      boolean sneaking = mc.player.isSneaking();
      double baseY = y + mc.player.getHeight() + 0.35D + (sneaking ? -0.15D : 0.0D);
      float r = this.radius.getCurrent();
      float h = this.height.getCurrent();
      int seg = (int) this.segments.getCurrent();
      double rot = this.spin.isEnabled() ? (System.currentTimeMillis() / 900.0D) : 0.0D;
      Vec3d top = new Vec3d(x, baseY + h, z);
      for (int i = 0; i < seg; i++) {
         double a1 = (Math.PI * 2.0D * i) / seg + rot;
         double a2 = (Math.PI * 2.0D * (i + 1)) / seg + rot;
         Vec3d p1 = new Vec3d(x + Math.cos(a1) * r, baseY, z + Math.sin(a1) * r);
         Vec3d p2 = new Vec3d(x + Math.cos(a2) * r, baseY, z + Math.sin(a2) * r);
         ColorRGBA c1 = this.colorFor(i * 10);
         ColorRGBA c2 = this.colorFor((i + 1) * 10);
         Render3DUtil.drawLine(p1, p2, c1.getRGB(), c2.getRGB(), this.lineWidth.getCurrent(), false);
         if (this.fill.isEnabled()) {
            Render3DUtil.drawLine(top, p1, c1.withAlpha(180).getRGB(), this.lineWidth.getCurrent() * 0.7F, false);
         }
      }
      // средний ободок для объема
      int midSeg = seg / 2;
      float midR = r * 0.55F;
      double midY = baseY + h * 0.45D;
      for (int i = 0; i < midSeg; i++) {
         double a1 = (Math.PI * 2.0D * i) / midSeg - rot;
         double a2 = (Math.PI * 2.0D * (i + 1)) / midSeg - rot;
         Vec3d p1 = new Vec3d(x + Math.cos(a1) * midR, midY, z + Math.sin(a1) * midR);
         Vec3d p2 = new Vec3d(x + Math.cos(a2) * midR, midY, z + Math.sin(a2) * midR);
         Render3DUtil.drawLine(p1, p2, this.colorFor(i * 14).withAlpha(160).getRGB(), this.lineWidth.getCurrent() * 0.7F, false);
      }
   }

   private ColorRGBA colorFor(int index) {
      switch (this.colorMode.get()) {
         case "Кастом": return this.customColor.getColor();
         case "Rainbow": return new ColorRGBA(Render2DUtil.rainbow(300, 0.6F, 1.0F));
         default: return Lumens.getInstance().getThemeManager().getClientColor(index);
      }
   }
}
