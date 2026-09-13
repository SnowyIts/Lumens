package dev.lumens.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import dev.lumens.base.events.impl.render.EventFov;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.BooleanSetting;
import dev.lumens.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "FOV",
   category = Category.RENDER,
   description = "Свой FOV и зум"
)
public final class FOV extends Module {
   public static final FOV INSTANCE = new FOV();
   private final NumberSetting fov = new NumberSetting("FOV", 110.0F, 30.0F, 170.0F, 1.0F);
   private final BooleanSetting sprintLock = new BooleanSetting("Фикс спринта", true);

   private FOV() {
   }

   @EventTarget
   public void onFov(EventFov e) {
      if (!this.sprintLock.isEnabled() && mc.player != null && mc.player.isSprinting()) {
         return;
      }
      e.setFov((int) this.fov.getCurrent());
      e.cancel();
   }
}
