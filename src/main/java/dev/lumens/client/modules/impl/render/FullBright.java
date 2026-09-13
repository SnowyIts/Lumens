package dev.lumens.client.modules.impl.render;

import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;

import dev.lumens.client.modules.api.setting.impl.ModeSetting;
import dev.lumens.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "FullBright",
   category = Category.RENDER,
   description = "Максимальное освещение"
)
public class FullBright extends Module {
   public static final FullBright INSTANCE = new FullBright();
   public final ModeSetting mode = new ModeSetting("Режим", new String[]{"Гамма", "Зелье"});
   public final NumberSetting brightness = new NumberSetting("Яркость", 10.0F, 2.0F, 12.0F, 0.5F, () -> this.mode.is("Гамма"));
}
