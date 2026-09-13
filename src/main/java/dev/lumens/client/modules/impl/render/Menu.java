package dev.lumens.client.modules.impl.render;

import dev.lumens.Lumens;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(
   name = "Menu",
   category = Category.RENDER,
   description = "Меню чита"
)
public final class Menu extends Module {
   public static final Menu INSTANCE = new Menu();

   private Menu() {
      this.setKeyCode(344);
   }

   public void onEnable() {
      if (mc.world == null) {
         this.setEnabled(false);
      } else {
         Lumens.getInstance().getMenuScreen().needToClose = false;
         if (mc.currentScreen != Lumens.getInstance().getMenuScreen()) {
            mc.setScreen(Lumens.getInstance().getMenuScreen());
            super.onEnable();
         }
      }
   }

   public void onDisable() {
      super.onDisable();
   }

   public void setKeyCode(int keyCode) {
      if (keyCode != -1) {
         super.setKeyCode(keyCode);
      }
   }
}
