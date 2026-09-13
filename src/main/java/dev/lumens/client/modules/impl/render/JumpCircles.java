package dev.lumens.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import dev.lumens.Lumens;
import dev.lumens.base.events.impl.player.EventUpdate;
import dev.lumens.base.events.impl.render.EventRender3D;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.ColorSetting;
import dev.lumens.client.modules.api.setting.impl.ModeSetting;
import dev.lumens.client.modules.api.setting.impl.MultiBooleanSetting;
import dev.lumens.client.modules.api.setting.impl.NumberSetting;
import dev.lumens.utility.game.player.PlayerIntersectionUtil;
import dev.lumens.utility.render.display.Render2DUtil;
import dev.lumens.utility.render.display.base.color.ColorRGBA;
import dev.lumens.utility.render.level.Render3DUtil;

@ModuleAnnotation(
   name = "JumpCircles",
   category = Category.RENDER,
   description = "Круги при прыжке и приземлении"
)
public final class JumpCircles extends Module {
   public static final JumpCircles INSTANCE = new JumpCircles();
   private final ModeSetting colorMode = new ModeSetting("Цвет", new String[]{"Тема", "Кастом", "Rainbow"});
   private final ColorSetting customColor = new ColorSetting("Кастом цвет", new ColorRGBA(0, 200, 255, 255), () -> this.colorMode.is("Кастом"));
   private final MultiBooleanSetting triggers = MultiBooleanSetting.create("Триггеры", List.of("Прыжок", "Приземление", "Чужие"));
   private final NumberSetting maxRadius = new NumberSetting("Макс. радиус", 1.2F, 0.5F, 3.0F, 0.1F);
   private final NumberSetting lifeTime = new NumberSetting("Время жизни", 800.0F, 300.0F, 2000.0F, 50.0F);
   private final NumberSetting lineWidth = new NumberSetting("Толщина", 2.0F, 1.0F, 5.0F, 0.5F);
   private final NumberSetting circles = new NumberSetting("Колец", 2.0F, 1.0F, 4.0F, 1.0F);

   private final List<Circle> active = new CopyOnWriteArrayList<>();
   private final java.util.Map<Integer, Boolean> groundMap = new java.util.HashMap<>();
   private boolean wasOnGround = true;

   private JumpCircles() {
   }

   @EventTarget
   public void onUpdate(EventUpdate e) {
      if (mc.player == null || mc.world == null) return;
      boolean onGround = mc.player.isOnGround();
      if (!this.wasOnGround && onGround && this.triggers.isEnable("Приземление")) {
         this.spawn(mc.player.getPos());
      }
      if (this.wasOnGround && !onGround && mc.player.getVelocity().y > 0.0D && this.triggers.isEnable("Прыжок")) {
         this.spawn(mc.player.getPos());
      }
      this.wasOnGround = onGround;
      if (this.triggers.isEnable("Чужие")) {
         PlayerIntersectionUtil.streamEntities()
               .filter(ent -> ent instanceof PlayerEntity && ent != mc.player && mc.player.distanceTo(ent) < 32.0F)
               .forEach(ent -> {
                  boolean prev = this.groundMap.getOrDefault(ent.getId(), true);
                  boolean g = ent.isOnGround();
                  if (!prev && g) {
                     this.spawn(ent.getPos());
                  }
                  this.groundMap.put(ent.getId(), g);
               });
      }
      long now = System.currentTimeMillis();
      this.active.removeIf(c -> now - c.born() > (long) this.lifeTime.getCurrent());
   }

   private void spawn(Vec3d pos) {
      this.active.add(new Circle(new Vec3d(pos.x, pos.y + 0.05D, pos.z), System.currentTimeMillis()));
      if (this.active.size() > 24) this.active.remove(0);
   }

   @EventTarget
   public void onRender3D(EventRender3D e) {
      if (mc.player == null || mc.world == null || this.active.isEmpty()) return;
      long now = System.currentTimeMillis();
      float life = this.lifeTime.getCurrent();
      for (Circle c : this.active) {
         float progress = MathHelper.clamp((now - c.born()) / life, 0.0F, 1.0F);
         float radius = this.maxRadius.getCurrent() * progress;
         if (radius < 0.05F) continue;
         int alpha = (int) (255.0F * (1.0F - progress));
         int count = (int) this.circles.getCurrent();
         for (int ring = 0; ring < count; ring++) {
            float r = radius * (1.0F - ring * 0.18F);
            if (r <= 0.05F) continue;
            int segments = 40;
            for (int i = 0; i < segments; i++) {
               double a1 = (Math.PI * 2.0D * i) / segments;
               double a2 = (Math.PI * 2.0D * (i + 1)) / segments;
               Vec3d p1 = new Vec3d(c.pos().x + Math.cos(a1) * r, c.pos().y, c.pos().z + Math.sin(a1) * r);
               Vec3d p2 = new Vec3d(c.pos().x + Math.cos(a2) * r, c.pos().y, c.pos().z + Math.sin(a2) * r);
               ColorRGBA col = this.colorFor(i * 9).withAlpha((int) (alpha * (1.0F - ring * 0.25F)));
               Render3DUtil.drawLine(p1, p2, col.getRGB(), this.lineWidth.getCurrent(), false);
            }
         }
      }
   }

   private ColorRGBA colorFor(int index) {
      switch (this.colorMode.get()) {
         case "Кастом": return this.customColor.getColor();
         case "Rainbow": return new ColorRGBA(Render2DUtil.rainbow(300, 0.6F, 1.0F));
         default: return Lumens.getInstance().getThemeManager().getClientColor(index);
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.active.clear();
   }

   private record Circle(Vec3d pos, long born) {
   }
}
