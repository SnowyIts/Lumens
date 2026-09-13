package dev.lumens.client.modules.impl.render;

import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.particle.ParticlesMode;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.BooleanSetting;
import dev.lumens.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "FPSBoost",
   category = Category.RENDER,
   description = "Режим производительности: режет графику ради FPS"
)
public final class FPSBoost extends Module {
   public static final FPSBoost INSTANCE = new FPSBoost();

   private final BooleanSetting graphics = new BooleanSetting("Графика", "Быстрая графика, без облаков, минимум частиц", true);
   private final NumberSetting renderDistance = new NumberSetting("Дистанция", 6.0F, 2.0F, 12.0F, 1.0F);
   private final NumberSetting simulationDistance = new NumberSetting("Сим-дистанция", 5.0F, 2.0F, 12.0F, 1.0F);
   private final BooleanSetting entityDistance = new BooleanSetting("Дальность сущностей", "Ужать дистанцию отрисовки сущностей до 0.5", false);

   private GraphicsMode prevGraphics;
   private ParticlesMode prevParticles;
   private CloudRenderMode prevClouds;
   private int prevBiomeBlend;
   private int prevView;
   private int prevSim;
   private double prevEntityScale;
   private boolean applied;

   private FPSBoost() {
   }

   @Override
   public void onEnable() {
      super.onEnable();
      if (mc.options == null) return;
      try {
         prevGraphics = mc.options.getGraphicsMode().getValue();
         prevParticles = mc.options.getParticles().getValue();
         prevClouds = mc.options.getCloudRenderMode().getValue();
         prevBiomeBlend = mc.options.getBiomeBlendRadius().getValue();
         prevView = mc.options.getViewDistance().getValue();
         prevSim = mc.options.getSimulationDistance().getValue();
         prevEntityScale = mc.options.getEntityDistanceScaling().getValue();
         applied = true;

         if (graphics.isEnabled()) {
            mc.options.getGraphicsMode().setValue(GraphicsMode.FAST);
            mc.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
            mc.options.getParticles().setValue(ParticlesMode.MINIMAL);
            mc.options.getBiomeBlendRadius().setValue(0);
         }
         int rd = Math.round(renderDistance.getCurrent());
         if (prevView > rd) mc.options.getViewDistance().setValue(rd);
         int sd = Math.round(simulationDistance.getCurrent());
         if (prevSim > sd) mc.options.getSimulationDistance().setValue(sd);
         if (entityDistance.isEnabled()) mc.options.getEntityDistanceScaling().setValue(0.5D);
      } catch (Exception ignored) {
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      if (!applied || mc.options == null) return;
      try {
         mc.options.getGraphicsMode().setValue(prevGraphics);
         mc.options.getParticles().setValue(prevParticles);
         mc.options.getCloudRenderMode().setValue(prevClouds);
         mc.options.getBiomeBlendRadius().setValue(prevBiomeBlend);
         mc.options.getViewDistance().setValue(prevView);
         mc.options.getSimulationDistance().setValue(prevSim);
         mc.options.getEntityDistanceScaling().setValue(prevEntityScale);
      } catch (Exception ignored) {
      }
      applied = false;
   }
}
