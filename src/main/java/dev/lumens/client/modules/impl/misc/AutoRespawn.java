package dev.lumens.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import ru.nexusguard.protection.annotations.Native;
import dev.lumens.base.events.impl.player.EventUpdate;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(
   name = "AutoRespawn",
   category = Category.MISC,
   description = "Автовозраждение после смерти"
)
public final class AutoRespawn extends Module {
   public static final AutoRespawn INSTANCE = new AutoRespawn();

   private AutoRespawn() {
   }

   @EventTarget
   @Native
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.world != null) {
         if (mc.currentScreen instanceof DeathScreen && mc.player.deathTime > 5) {
            mc.player.requestRespawn();
            mc.setScreen((Screen)null);
         }

      }
   }
}
