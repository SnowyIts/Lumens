package dev.lumens.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import dev.lumens.utility.render.display.base.color.ColorRGBA;
import dev.lumens.Lumens;
import dev.lumens.base.events.impl.render.EventFog;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.NumberSetting;

import dev.lumens.client.modules.api.setting.impl.ColorSetting;
import dev.lumens.client.modules.api.setting.impl.ModeSetting;

@ModuleAnnotation(
   name = "CustomFog",
   category = Category.RENDER,
   description = "Меняет туман"
)
public class CustomFog extends Module {
   public static final CustomFog INSTANCE = new CustomFog();
   public final NumberSetting distanceSetting = new NumberSetting("Дистанция тумана", 80.0F, 10.0F, 255.0F, 5.0F);
   private final ModeSetting colorMode = new ModeSetting("Цвет тумана", new String[]{"Тема", "Кастом", "Мир"});
   private final ColorSetting customColor = new ColorSetting("Кастом цвет", new ColorRGBA(120, 160, 255, 255), () -> this.colorMode.is("Кастом"));

   @EventTarget
   public void onFog(EventFog e) {
      e.setDistance(this.distanceSetting.getCurrent());
      if (this.colorMode.is("Кастом")) {
         e.setColor(this.customColor.getColor().getRGB());
      } else if (this.colorMode.is("Тема")) {
         e.setColor(Lumens.getInstance().getThemeManager().getCurrentTheme().getColor().getRGB());
      }
      // "Мир" — не трогаем цвет, только дистанцию
      e.setCancelled(true);
   }
}
