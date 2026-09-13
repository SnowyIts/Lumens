package dev.lumens.client.modules.impl.render;

import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "WorldTime",
   description = "Меняет время суток",
   category = Category.RENDER
)
public class WorldTime extends Module {
   public static final WorldTime INSTANCE = new WorldTime();
   public final NumberSetting timeSetting = new NumberSetting("Время", 12.0F, 0.0F, 24.0F, 1.0F);
}
