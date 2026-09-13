package dev.lumens.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.util.List;

import dev.lumens.client.modules.impl.player.Cosmetics;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
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
import dev.lumens.client.modules.api.setting.impl.MultiBooleanSetting;
import dev.lumens.client.modules.api.setting.impl.NumberSetting;
import dev.lumens.utility.game.player.PlayerIntersectionUtil;
import dev.lumens.utility.render.display.Render2DUtil;
import dev.lumens.utility.render.display.base.color.ColorRGBA;
import dev.lumens.utility.render.level.Render3DUtil;

@ModuleAnnotation(
   name = "Tracers",
   category = Category.RENDER,
   description = "Линии до сущностей"
)
public final class Tracers extends Module {
   public static final Tracers INSTANCE = new Tracers();
   private final MultiBooleanSetting targets = MultiBooleanSetting.create("Цели", List.of("Игроки", "Мобы", "Предметы", "Проектайлы"));
   private final ModeSetting colorMode = new ModeSetting("Цвет", new String[]{"Тема", "Дистанция", "Кастом", "Rainbow"});
   private final ColorSetting customColor = new ColorSetting("Кастом цвет", new ColorRGBA(0, 200, 255, 255), () -> this.colorMode.is("Кастом"));
   private final ModeSetting origin = new ModeSetting("Позиция", new String[]{"Центр", "Низ"});
   private final NumberSetting distance = new NumberSetting("Дистанция", 64.0F, 8.0F, 256.0F, 4.0F);
   private final NumberSetting width = new NumberSetting("Толщина", 1.5F, 0.5F, 5.0F, 0.5F);
   private final BooleanSetting friends = new BooleanSetting("Друзья зеленым", true);

   private Tracers() {
   }

   @EventTarget
   public void onRender3D(EventRender3D e) {
      if (mc.player == null || mc.world == null) return;
      Vec3d camera = mc.gameRenderer.getCamera().getPos();
      float tickDelta = e.getPartialTicks();
      PlayerIntersectionUtil.streamEntities().forEach(entity -> {
         if (!this.shouldRender(entity)) return;
         if (mc.player.distanceTo(entity) > this.distance.getCurrent()) return;
         double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
         double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()) + entity.getHeight() / 2.0D;
         double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
         Vec3d start = this.origin.is("Низ") ? camera.add(0.0D, -0.4D, 0.0D) : camera;
         Vec3d end = new Vec3d(x, y, z);
         int color = this.colorFor(entity).getRGB();
         Render3DUtil.drawLine(start, end, color, this.width.getCurrent(), false);
      });
   }

   private boolean shouldRender(Entity entity) {
      if (entity == mc.player || !entity.isAlive()) return false;
      if (entity.getCommandTags().contains(Cosmetics.PET_TAG)) return false;
      if (entity instanceof PlayerEntity) return this.targets.isEnable("Игроки");
      if (entity instanceof HostileEntity || entity instanceof PassiveEntity) return this.targets.isEnable("Мобы");
      if (entity instanceof LivingEntity) return this.targets.isEnable("Мобы");
      if (entity instanceof ItemEntity) return this.targets.isEnable("Предметы");
      if (entity instanceof ProjectileEntity) return this.targets.isEnable("Проектайлы");
      return false;
   }

   private ColorRGBA colorFor(Entity entity) {
      if (this.friends.isEnabled() && entity instanceof PlayerEntity
            && Lumens.getInstance().getFriendManager().isFriend(entity.getNameForScoreboard())) {
         return new ColorRGBA(60, 255, 60, 200);
      }
      switch (this.colorMode.get()) {
         case "Кастом":
            return this.customColor.getColor().withAlpha(200);
         case "Rainbow":
            return new ColorRGBA(Render2DUtil.rainbow(300, 0.6F, 1.0F)).withAlpha(200);
         case "Дистанция": {
            float d = (float) mc.player.distanceTo(entity);
            float f = MathHelper.clamp(d / this.distance.getCurrent(), 0.0F, 1.0F);
            return new ColorRGBA((int) (255 * f), (int) (255 * (1.0F - f)), 80, 200);
         }
         default: {
            int idx = (int) (mc.player.distanceTo(entity) * 4.0F);
            return Lumens.getInstance().getThemeManager().getClientColor(idx).withAlpha(200);
         }
      }
   }
}
