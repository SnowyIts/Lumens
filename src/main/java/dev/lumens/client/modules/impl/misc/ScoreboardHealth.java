package dev.lumens.client.modules.impl.misc;

import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(
   name = "ScoreboardHealth",
   category = Category.MISC,
   description = "Фиксит хп цели если оно фейк"
)
public class ScoreboardHealth extends Module {
   public static final ScoreboardHealth INSTANCE = new ScoreboardHealth();
}
