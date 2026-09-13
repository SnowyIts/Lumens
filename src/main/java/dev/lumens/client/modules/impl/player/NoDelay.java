package dev.lumens.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import dev.lumens.base.events.impl.player.EventUpdate;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(
   name = "NoJumpDelay",
   category = Category.PLAYER,
   description = "Убирает задержку на прыжок"
)
public final class NoDelay extends Module {
   public static final NoDelay INSTANCE = new NoDelay();

   @EventTarget
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.world != null) {
         mc.player.jumpingCooldown = 0;
      }
   }
}
