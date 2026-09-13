package dev.lumens.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import dev.lumens.base.events.impl.player.EventUpdate;
import dev.lumens.base.events.impl.render.EventRender2D;
import dev.lumens.base.events.impl.render.EventRender3D;
import dev.lumens.base.font.Fonts;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.BooleanSetting;
import dev.lumens.client.modules.api.setting.impl.ColorSetting;
import dev.lumens.client.modules.api.setting.impl.MultiBooleanSetting;
import dev.lumens.client.modules.api.setting.impl.NumberSetting;
import dev.lumens.utility.game.player.PlayerIntersectionUtil;
import dev.lumens.utility.math.ProjectionUtil;
import dev.lumens.utility.render.display.base.BorderRadius;
import dev.lumens.utility.render.display.base.color.ColorRGBA;
import dev.lumens.utility.render.display.shader.DrawUtil;
import dev.lumens.utility.render.level.Render3DUtil;

@ModuleAnnotation(
   name = "StorageESP",
   category = Category.RENDER,
   description = "Подсветка хранилищ и руд"
)
public final class StorageESP extends Module {
   public static final StorageESP INSTANCE = new StorageESP();
   private final MultiBooleanSetting storages = MultiBooleanSetting.create("Хранилища",
         List.of("Сундуки", "Эндер-сундук", "Шалкеры", "Печки", "Воронки", "Бочки"));
   private final MultiBooleanSetting ores = MultiBooleanSetting.create("Руды",
         List.of("Алмазы", "Золото", "Железо", "Уголь", "Лазурит", "Редстоун", "Изумруд", "Незерит"));
   private final NumberSetting radius = new NumberSetting("Радиус", 48.0F, 16.0F, 128.0F, 8.0F);
   private final NumberSetting maxCount = new NumberSetting("Макс. блоков", 128.0F, 16.0F, 512.0F, 16.0F);
   private final NumberSetting lineWidth = new NumberSetting("Толщина", 1.5F, 0.5F, 5.0F, 0.5F);
   private final BooleanSetting box = new BooleanSetting("Бокс", true);
   private final BooleanSetting fill = new BooleanSetting("Заливка", true);
   private final BooleanSetting tracer = new BooleanSetting("Линии", false);
   private final BooleanSetting nametag = new BooleanSetting("Названия", true);
   private final ColorSetting chestColor = new ColorSetting("Сундуки", new ColorRGBA(255, 170, 0, 255));
   private final ColorSetting oreColor = new ColorSetting("Руды", new ColorRGBA(0, 220, 255, 255));

   private final List<Entry> cache = new ArrayList<>();
   private int tickCounter = 0;

   private StorageESP() {
   }

   @EventTarget
   public void onUpdate(EventUpdate e) {
      if (mc.player == null || mc.world == null) return;
      if (++this.tickCounter % 20 != 0) return;
      this.tickCounter = 0;
      this.cache.clear();
      BlockPos center = mc.player.getBlockPos();
      int r = (int) this.radius.getCurrent();
      int max = (int) this.maxCount.getCurrent();
      for (BlockPos pos : PlayerIntersectionUtil.getCube(center, r, Math.min(r, 48), true)) {
         if (this.cache.size() >= max) break;
         if (!mc.world.isChunkLoaded(pos)) continue;
         Block block = mc.world.getBlockState(pos).getBlock();
         BlockEntity be = null;
         try {
            be = mc.world.getBlockEntity(pos);
         } catch (Exception ignored) {
         }
         ColorRGBA color = null;
         String name = null;
         if (be instanceof ChestBlockEntity && this.storages.isEnable("Сундуки")) {
            color = this.chestColor.getColor(); name = "Сундук";
         } else if (be instanceof EnderChestBlockEntity && this.storages.isEnable("Эндер-сундук")) {
            color = new ColorRGBA(180, 0, 255, 255); name = "Эндер-сундук";
         } else if (be instanceof ShulkerBoxBlockEntity && this.storages.isEnable("Шалкеры")) {
            color = new ColorRGBA(200, 80, 220, 255); name = "Шалкер";
         } else if (be instanceof FurnaceBlockEntity && this.storages.isEnable("Печки")) {
            color = new ColorRGBA(150, 150, 150, 255); name = "Печка";
         } else if (be instanceof HopperBlockEntity && this.storages.isEnable("Воронки")) {
            color = new ColorRGBA(120, 120, 130, 255); name = "Воронка";
         } else if (be instanceof BarrelBlockEntity && this.storages.isEnable("Бочки")) {
            color = new ColorRGBA(150, 100, 50, 255); name = "Бочка";
         } else {
            OreInfo ore = this.matchOre(block);
            if (ore != null) {
               color = ore.color(); name = ore.name();
            }
         }
         if (color != null) {
            this.cache.add(new Entry(pos.toImmutable(), color, name));
         }
      }
   }

   private OreInfo matchOre(Block block) {
      if ((block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) && this.ores.isEnable("Алмазы"))
         return new OreInfo("Алмаз", new ColorRGBA(0, 230, 255, 255));
      if ((block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE) && this.ores.isEnable("Золото"))
         return new OreInfo("Золото", new ColorRGBA(255, 210, 0, 255));
      if ((block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) && this.ores.isEnable("Железо"))
         return new OreInfo("Железо", new ColorRGBA(220, 170, 150, 255));
      if ((block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) && this.ores.isEnable("Уголь"))
         return new OreInfo("Уголь", new ColorRGBA(60, 60, 60, 255));
      if ((block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) && this.ores.isEnable("Лазурит"))
         return new OreInfo("Лазурит", new ColorRGBA(40, 80, 255, 255));
      if ((block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) && this.ores.isEnable("Редстоун"))
         return new OreInfo("Редстоун", new ColorRGBA(255, 40, 40, 255));
      if ((block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) && this.ores.isEnable("Изумруд"))
         return new OreInfo("Изумруд", new ColorRGBA(0, 255, 100, 255));
      if (block == Blocks.ANCIENT_DEBRIS && this.ores.isEnable("Незерит"))
         return new OreInfo("Незерит", new ColorRGBA(120, 70, 50, 255));
      return null;
   }

   @EventTarget
   public void onRender3D(EventRender3D e) {
      if (mc.player == null || mc.world == null) return;
      for (Entry entry : this.cache) {
         Box box = new Box(entry.pos());
         if (!ProjectionUtil.canSee(box)) continue;
         if (this.box.isEnabled()) {
            Render3DUtil.drawBox(box, entry.color().getRGB(), this.lineWidth.getCurrent(), true, this.fill.isEnabled(), false);
         }
         if (this.tracer.isEnabled()) {
            Vec3d camera = mc.gameRenderer.getCamera().getPos();
            Vec3d center = box.getCenter();
            Render3DUtil.drawLine(camera, center, entry.color().withAlpha(160).getRGB(), 1.0F, false);
         }
      }
   }

   @EventTarget
   public void onRender2D(EventRender2D e) {
      if (!this.nametag.isEnabled() || mc.player == null || mc.world == null) return;
      for (Entry entry : this.cache) {
         Box box = new Box(entry.pos());
         if (!ProjectionUtil.canSee(box)) continue;
         Vec3d center = box.getCenter();
         Vec3d screen = ProjectionUtil.worldSpaceToScreenSpace(center);
         if (screen.z <= 0.0D || screen.z >= 1.0D) continue;
         int dist = (int) mc.player.getPos().distanceTo(center);
         String text = entry.name() + " [" + dist + "m]";
         float w = Fonts.REGULAR.getWidth(text, 6.0F);
         float x = (float) screen.x - w / 2.0F;
         float y = (float) screen.y - 4.0F;
         DrawUtil.drawRoundedRect(e.getContext().getMatrices(), x - 2.0F, y - 1.5F, w + 4.0F, 9.0F, BorderRadius.all(3.0F), new ColorRGBA(0, 0, 0, 120));
         e.getContext().drawText(Fonts.REGULAR.getFont(6.0F), text, x, y, entry.color());
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.cache.clear();
   }

   private record Entry(BlockPos pos, ColorRGBA color, String name) {
   }

   private record OreInfo(String name, ColorRGBA color) {
   }
}
