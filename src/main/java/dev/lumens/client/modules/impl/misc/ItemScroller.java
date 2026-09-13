package dev.lumens.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import dev.lumens.base.events.impl.input.EventMouse;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(
   name = "ItemScroller",
   description = "Перемещение преметов без задержки",
   category = Category.MISC
)
public final class ItemScroller extends Module {
   public static final ItemScroller INSTANCE = new ItemScroller();
   public boolean mouseHold;

   @EventTarget
   private void onMouse(EventMouse e) {
      if (e.getButton() == 0 && e.getAction() == 1) {
         this.mouseHold = true;
      }

      if (this.mouseHold && e.getAction() == 0) {
         this.mouseHold = false;
      }

   }
}
