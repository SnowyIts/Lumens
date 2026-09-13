package dev.lumens.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4d;
import org.joml.Vector4f;
import dev.lumens.Lumens;
import dev.lumens.base.events.impl.render.EventRender2D;
import dev.lumens.base.font.Fonts;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.impl.misc.NameProtect;
import dev.lumens.client.modules.impl.misc.ScoreboardHealth;
import dev.lumens.utility.game.other.ReplaceUtil;
import dev.lumens.utility.game.player.PlayerIntersectionUtil;
import dev.lumens.utility.math.ProjectionUtil;
import dev.lumens.utility.render.display.base.BorderRadius;
import dev.lumens.utility.render.display.base.color.ColorRGBA;
import dev.lumens.utility.render.display.shader.DrawUtil;

import dev.lumens.client.modules.api.setting.impl.BooleanSetting;
import dev.lumens.client.modules.api.setting.impl.MultiBooleanSetting;
import dev.lumens.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "NameTags",
   category = Category.RENDER,
   description = "Показывает информацию о игроке"
)
public final class EntityESP extends Module {
   public static final EntityESP INSTANCE = new EntityESP();
   private final MultiBooleanSetting elements = MultiBooleanSetting.create("Элементы", java.util.List.of("Игроки", "Предметы", "HP-бар", "Броня", "Энчанты", "Дистанция", "Фон"));
   private final NumberSetting maxDistance = new NumberSetting("Дистанция", 64.0F, 8.0F, 256.0F, 4.0F);
   private final BooleanSetting showSelf = new BooleanSetting("Себя", false);
   private final BooleanSetting healthColor = new BooleanSetting("Цвет HP по здоровью", true);
   private final HashMap<Entity, Vector4f> positions = new HashMap();

   @EventTarget
   private void onRender(EventRender2D e) {
      if (mc.world != null && mc.player != null) {
         float tickDelta = e.getTickDelta();
         if (this.elements.isEnable("Игроки")) this.renderPlayerTags(tickDelta, e);
         if (this.elements.isEnable("Предметы")) this.renderItemTags(tickDelta, e);
      }
   }

   private ColorRGBA hpColor(float hp, float max) {
      if (!this.healthColor.isEnabled()) return new ColorRGBA(255, 80, 80, 255);
      float f = MathHelper.clamp(hp / max, 0.0F, 1.0F);
      int r = (int)(255.0F * (1.0F - f));
      int g = (int)(255.0F * f);
      return new ColorRGBA(r, g, 80, 255);
   }

   private void renderPlayerTags(float tickDelta, EventRender2D e) {
      boolean thirdPerson = mc.getEntityRenderDispatcher().camera.isThirdPerson();
      for (PlayerEntity entity : mc.world.getPlayers()) {
         if (entity == mc.player && (!thirdPerson || !this.showSelf.isEnabled())) continue;
         if (mc.player.distanceTo(entity) > this.maxDistance.getCurrent()) continue;
         if (!ProjectionUtil.canSee(entity.getBoundingBox().getCenter())) continue;

         double x = MathHelper.lerp((double)tickDelta, entity.lastRenderX, entity.getX());
         double y = MathHelper.lerp((double)tickDelta, entity.lastRenderY, entity.getY()) + (double)entity.getHeight() + 0.2D;
         double z = MathHelper.lerp((double)tickDelta, entity.lastRenderZ, entity.getZ());
         Vec3d pos = ProjectionUtil.worldSpaceToScreenSpace(new Vec3d(x, y, z));
         if (pos.z <= 0.0D || pos.z >= 1.0D) continue;

         Vector4d position = ProjectionUtil.getVector4D(entity);
         if (position == null) continue;
         float posY = (float)(position.y - 11.0D);
         float hp = ScoreboardHealth.INSTANCE.isEnabled() && entity != mc.player ? PlayerIntersectionUtil.getHealth(entity) : entity.getHealth();
         float maxHp = Math.max(entity.getMaxHealth(), 1.0F);
         Text name = entity == mc.player && NameProtect.INSTANCE.isEnabled() ? Text.literal(NameProtect.getCustomName()) : ReplaceUtil.replaceSymbols(entity.getDisplayName());
         String hpStr = String.valueOf((int)Math.ceil(hp));
         int dist = (int)mc.player.distanceTo(entity);
         String suffix = this.elements.isEnable("Дистанция") ? " [" + dist + "m]" : "";
         float hf = MathHelper.clamp(hp / maxHp, 0.0F, 1.0F);
         Formatting hpFmt = hf > 0.66F ? Formatting.GREEN : (hf > 0.33F ? Formatting.YELLOW : Formatting.RED);
         Text nameWithHp = ((Text)name).copy().append(Text.literal(" " + hpStr).setStyle(Style.EMPTY.withColor(hpFmt))).append(Text.literal(suffix).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
         String plain = nameWithHp.getString();
         float textWidth = Fonts.REGULAR.getWidth(plain, 6.5F);
         boolean isFriend = Lumens.getInstance().getFriendManager().isFriend(entity.getNameForScoreboard());
         ColorRGBA bg = isFriend ? new ColorRGBA(0, 166, 0, 123) : new ColorRGBA(0, 0, 0, 123);
         float bgX = (float)(position.x + (position.z - position.x) / 2.0D - (double)(textWidth / 2.0F) - 3.0D);
         if (this.elements.isEnable("Фон")) {
            DrawUtil.drawRoundedRect(e.getContext().getMatrices(), bgX, posY - 2.5F, textWidth + 5.0F, 10.0F, BorderRadius.all(3.0F), bg);
         }
         e.getContext().drawText(Fonts.REGULAR.getFont(6.5F), nameWithHp, (float)(position.x + (position.z - position.x) / 2.0D - (double)(textWidth / 2.0F)), posY, 255.0F);
         if (this.elements.isEnable("HP-бар")) {
            float barW = textWidth + 5.0F;
            float barX = bgX;
            float barY = posY + 8.0F;
            DrawUtil.drawRoundedRect(e.getContext().getMatrices(), barX, barY, barW, 1.8F, BorderRadius.ZERO, new ColorRGBA(0, 0, 0, 140));
            float fill = barW * MathHelper.clamp(hp / maxHp, 0.0F, 1.0F);
            if (fill > 0.5F) {
               DrawUtil.drawRoundedRect(e.getContext().getMatrices(), barX, barY, fill, 1.8F, BorderRadius.ZERO, this.hpColor(hp, maxHp));
            }
         }
         if (!this.elements.isEnable("Броня")) {
            continue;
         }
         ItemStack[] itemArray = new ItemStack[6];
         int itemCount = 0;
         EquipmentSlot[] slots = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

         for (EquipmentSlot slot : slots) {
            ItemStack stack = entity.getEquippedStack(slot);
            if (!stack.isEmpty()) {
               itemArray[itemCount++] = stack;
            }
         }

         ItemStack mainHand = entity.getMainHandStack();
         if (!mainHand.isEmpty()) {
            itemArray[itemCount++] = mainHand;
         }

         ItemStack offHand = entity.getOffHandStack();
         if (!offHand.isEmpty()) {
            itemArray[itemCount++] = offHand;
         }
         if (itemCount == 0) continue;

         float iconSize = 16.0F;
         float spacing = 0.0F;
         float totalWidth = (float)itemCount * iconSize + (float)(itemCount - 1) * spacing;
         float startX = (float)(position.x + (position.z - position.x) / 2.0D - (double)(totalWidth / 2.0F) + 7.5D);
         float iconY = posY - 12.0F;
         MatrixStack matrices = e.getContext().getMatrices();

         for(int i = 0; i < itemCount; ++i) {
            ItemStack stack = itemArray[i];
            if (stack != null && !stack.isEmpty()) {
               float x2 = startX + (float)i * (iconSize + spacing);
               ItemEnchantmentsComponent enchComp = EnchantmentHelper.getEnchantments(stack);
               float enchantmentY;
               if (!enchComp.isEmpty() && this.elements.isEnable("Энчанты")) {
                  Map<RegistryEntry<Enchantment>, Integer> enchMap = (Map)enchComp.getEnchantmentEntries().stream().collect(Collectors.toMap(Entry::getKey, it.unimi.dsi.fastutil.objects.Object2IntMap.Entry::getIntValue));
                  enchantmentY = iconY - 16.0F;
                  Iterator var33 = enchMap.entrySet().iterator();

                  label134:
                  while(true) {
                     Entry enchEntry;
                     int lvl;
                     do {
                        if (!var33.hasNext()) {
                           break label134;
                        }

                        enchEntry = (Entry)var33.next();
                        lvl = (Integer)enchEntry.getValue();
                     } while(lvl <= 0);

                     String fullName = Enchantment.getName((RegistryEntry)enchEntry.getKey(), lvl).getString();
                     String shortName = fullName.length() > 2 ? fullName.substring(0, 2) : fullName;
                     String enchantmentText = shortName + lvl;
                     float enchantmentTextWidth = Fonts.REGULAR.getWidth(enchantmentText, 6.0F);
                     int color = -1;
                     if (shortName.equalsIgnoreCase("Sh") && lvl > 5 || shortName.equalsIgnoreCase("Pr") && lvl > 4) {
                        color = (new ColorRGBA(212, 45, 43, 255)).getRGB();
                     }

                     e.getContext().drawText(Fonts.REGULAR.getFont(6.0F), enchantmentText, x2 - enchantmentTextWidth / 2.0F, enchantmentY, new ColorRGBA(color));
                     enchantmentY -= 8.0F;
                  }
               }

               DrawUtil.drawRoundedRect(matrices, x2 - 7.0F, iconY - 7.0F, 14.0F, 14.0F, BorderRadius.all(3.0F), new ColorRGBA(0, 0, 0, 123));
               float scale = 0.7F;
               enchantmentY = -18.0F;
               matrices.push();
               matrices.translate(x2 + enchantmentY, iconY + enchantmentY, 0.0F);
               matrices.scale(scale, scale, 1.0F);
               int drawX = (int)(-enchantmentY);
               int drawY = (int)(-enchantmentY);
               e.getContext().drawItem(stack, drawX, drawY);
               e.getContext().drawStackOverlay(mc.textRenderer, stack, drawX, drawY);
               matrices.pop();
            }
         }
      }
   }

   private void renderItemTags(float tickDelta, EventRender2D e) {
      Iterator var3 = mc.world.getEntities().iterator();

      while(var3.hasNext()) {
         Entity entity = (Entity)var3.next();
         if (entity instanceof ItemEntity) {
            ItemEntity itemEntity = (ItemEntity)entity;
            if (mc.player.distanceTo(entity) > this.maxDistance.getCurrent()) continue;
            if (ProjectionUtil.canSee(itemEntity.getBoundingBox().getCenter())) {
               double x = MathHelper.lerp((double)tickDelta, entity.lastRenderX, entity.getX());
               double y = MathHelper.lerp((double)tickDelta, entity.lastRenderY, entity.getY()) + (double)entity.getHeight() + 0.1D;
               double z = MathHelper.lerp((double)tickDelta, entity.lastRenderZ, entity.getZ());
               Vec3d pos = ProjectionUtil.worldSpaceToScreenSpace(new Vec3d(x, y, z));
               if (!(pos.z <= 0.0D) && !(pos.z >= 1.0D)) {
                  Vector4d position = ProjectionUtil.getVector4D(entity);
                  float posY = (float)(position.y - 11.0D);
                  ItemStack stack = itemEntity.getStack();
                  if (!stack.isEmpty()) {
                     int rarityOrdinal = stack.getRarity().ordinal();
                     Formatting var10000;
                     switch(rarityOrdinal) {
                     case 1:
                        var10000 = Formatting.YELLOW;
                        break;
                     case 2:
                        var10000 = Formatting.AQUA;
                        break;
                     case 3:
                        var10000 = Formatting.LIGHT_PURPLE;
                        break;
                     default:
                        var10000 = Formatting.WHITE;
                     }

                     Formatting rarityColor = var10000;
                     String itemName = stack.getName().getString();
                     Text nameText = Text.literal(itemName).setStyle(Style.EMPTY.withColor(rarityColor));
                     if (!stack.getName().getSiblings().isEmpty()) {
                        nameText = stack.getName();
                     }

                     Text countComponent = stack.getCount() > 1 ? Text.literal(" х" + stack.getCount()).setStyle(Style.EMPTY.withColor(Formatting.GRAY)) : Text.empty();
                     Text textComponent = ((Text)nameText).copy().append(countComponent);
                     float textWidth = Fonts.REGULAR.getFont(6.5F).width((Text)textComponent);
                     if (this.elements.isEnable("Фон")) {
                        DrawUtil.drawRoundedRect(e.getContext().getMatrices(), (float)(position.x + (position.z - position.x) / 2.0D - (double)(textWidth / 2.0F) - 3.0D), (float)(position.y - 13.5D), textWidth + 4.0F, 10.0F, BorderRadius.all(3.0F), new ColorRGBA(0, 0, 0, 123));
                     }
                     e.getContext().drawText(Fonts.REGULAR.getFont(6.5F), textComponent, (float)(position.x + (position.z - position.x) / 2.0D - (double)(textWidth / 2.0F)), (float)position.y - 11.0F, 255.0F);
                  }
               }
            }
         }
      }

   }

   public static void drawBox(double x, double y, double width, double height, double size, int color, BufferBuilder bufferbuilder) {
      drawRectBuilding(x + size, y, width - size, y + size, color, bufferbuilder);
      drawRectBuilding(x, y, x + size, height, color, bufferbuilder);
      drawRectBuilding(width - size, y, width, height, color, bufferbuilder);
      drawRectBuilding(x + size, height - size, width - size, height, color, bufferbuilder);
   }

   public static void drawBoxTest(double x, double y, double width, double height, double size, Vector4f colors, BufferBuilder bufferbuilder) {
      drawMCHorizontalBuilding(x + size, y, width - size, y + size, (int)colors.x(), (int)colors.y(), bufferbuilder);
      drawMCVerticalBuilding(width - size, y + size, width, height - size, (int)colors.y(), (int)colors.z(), bufferbuilder);
      drawMCHorizontalBuilding(x + size, height - size, width - size, height, (int)colors.w(), (int)colors.z(), bufferbuilder);
      drawMCVerticalBuilding(x, y + size, x + size, height - size, (int)colors.x(), (int)colors.w(), bufferbuilder);
   }

   public static void drawRectBuilding(double left, double top, double right, double bottom, int color, BufferBuilder bufferbuilder) {
      double j;
      if (left < right) {
         j = left;
         left = right;
         right = j;
      }

      if (top < bottom) {
         j = top;
         top = bottom;
         bottom = j;
      }

      float f3 = (float)(color >> 24 & 255) / 255.0F;
      float f = (float)(color >> 16 & 255) / 255.0F;
      float f1 = (float)(color >> 8 & 255) / 255.0F;
      float f2 = (float)(color & 255) / 255.0F;
      bufferbuilder.vertex((float)left, (float)bottom, 0.0F).color(f, f1, f2, f3);
      bufferbuilder.vertex((float)right, (float)bottom, 0.0F).color(f, f1, f2, f3);
      bufferbuilder.vertex((float)right, (float)top, 0.0F).color(f, f1, f2, f3);
      bufferbuilder.vertex((float)left, (float)top, 0.0F).color(f, f1, f2, f3);
   }

   public static void drawMCHorizontalBuilding(double x1, double y1, double x2, double y2, int start, int end, BufferBuilder bufferbuilder) {
      float a1 = (float)(start >> 24 & 255) / 255.0F;
      float r1 = (float)(start >> 16 & 255) / 255.0F;
      float g1 = (float)(start >> 8 & 255) / 255.0F;
      float b1 = (float)(start & 255) / 255.0F;
      float a2 = (float)(end >> 24 & 255) / 255.0F;
      float r2 = (float)(end >> 16 & 255) / 255.0F;
      float g2 = (float)(end >> 8 & 255) / 255.0F;
      float b2 = (float)(end & 255) / 255.0F;
      bufferbuilder.vertex((float)x1, (float)y2, 0.0F).color(r1, g1, b1, a1);
      bufferbuilder.vertex((float)x2, (float)y2, 0.0F).color(r2, g2, b2, a2);
      bufferbuilder.vertex((float)x2, (float)y1, 0.0F).color(r2, g2, b2, a2);
      bufferbuilder.vertex((float)x1, (float)y1, 0.0F).color(r1, g1, b1, a1);
   }

   public static void drawMCVerticalBuilding(double x1, double y1, double x2, double y2, int start, int end, BufferBuilder bufferbuilder) {
      float a1 = (float)(start >> 24 & 255) / 255.0F;
      float r1 = (float)(start >> 16 & 255) / 255.0F;
      float g1 = (float)(start >> 8 & 255) / 255.0F;
      float b1 = (float)(start & 255) / 255.0F;
      float a2 = (float)(end >> 24 & 255) / 255.0F;
      float r2 = (float)(end >> 16 & 255) / 255.0F;
      float g2 = (float)(end >> 8 & 255) / 255.0F;
      float b2 = (float)(end & 255) / 255.0F;
      bufferbuilder.vertex((float)x1, (float)y2, 0.0F).color(r2, g2, b2, a2);
      bufferbuilder.vertex((float)x2, (float)y2, 0.0F).color(r2, g2, b2, a2);
      bufferbuilder.vertex((float)x2, (float)y1, 0.0F).color(r1, g1, b1, a1);
      bufferbuilder.vertex((float)x1, (float)y1, 0.0F).color(r1, g1, b1, a1);
   }
}
