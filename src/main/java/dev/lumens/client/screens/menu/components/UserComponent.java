package dev.lumens.client.screens.menu.components;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;
import dev.lumens.base.font.Fonts;
import dev.lumens.base.font.MsdfRenderer;
import dev.lumens.base.license.LumensAuth;
import dev.lumens.base.theme.Theme;
import dev.lumens.Lumens;
import dev.lumens.utility.render.display.base.BorderRadius;
import dev.lumens.utility.render.display.base.CustomDrawContext;
import dev.lumens.utility.render.display.base.color.ColorRGBA;
import dev.lumens.utility.render.display.shader.DrawUtil;

public class UserComponent {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public void render(DrawContext context, float x, float y, float alpha) {
      render(context, x, y, 132.0F, alpha);
   }

   public void render(DrawContext context, float x, float y, float width, float alpha) {
      CustomDrawContext drawContext = CustomDrawContext.of(context);
      Theme theme = Lumens.getInstance().getThemeManager().getCurrentTheme();

      float height = 24.0F;
      int a = (int) (alpha * 255.0F);

      // Карточка пользователя — тёмный пилюля-блок как на макете LUMENS
      DrawUtil.drawRoundedRect(context.getMatrices(), x, y, width, height,
              BorderRadius.all(5.0F), new ColorRGBA(24, 12, 46, a));
      DrawUtil.drawRoundedBorder(context.getMatrices(), x, y, width, height,
              0.6F, BorderRadius.all(5.0F),
              new ColorRGBA(255, 255, 255, (int) (a * 0.08F)));

      Identifier skin = resolveSkin();
      float headSize = 15.0F;
      float headX = x + 4.0F;
      float headY = y + 4.5F;

      // Белый квадрат с макета заменён на реальное лицо игрока
      DrawUtil.drawRoundedRect(context.getMatrices(), headX - 1.0F, headY - 1.0F,
              headSize + 2.0F, headSize + 2.0F, BorderRadius.all(4.0F),
              new ColorRGBA(255, 255, 255, a));
      DrawUtil.drawPlayerHeadWithRoundedShader(context.getMatrices(), skin,
              headX, headY, headSize, BorderRadius.all(3.0F),
              new ColorRGBA(255, 255, 255, a));

      String playerName = LumensAuth.get().getPlayerName();
      try {
         String sessionName = mc.getSession().getUsername();
         if (sessionName != null && !sessionName.isEmpty()) playerName = sessionName;
      } catch (Exception ignored) {
      }

      String uidText = "UID: " + LumensAuth.get().getUidText();

      float textX = headX + headSize + 5.0F;
      float maxNameWidth = width - (textX - x) - 4.0F;

      MsdfRenderer.renderText(Fonts.SEMIBOLD.getFont(7.0F).getFont(), playerName, 7.0F,
              new ColorRGBA(255, 255, 255, a).getRGB(),
              context.getMatrices().peek().getPositionMatrix(),
              textX, y + 4.5F, 0.0F, true, 0.7F, 1.0F, maxNameWidth);
      drawContext.drawText(Fonts.REGULAR.getFont(5.5F), uidText,
              textX, y + 14.0F, new ColorRGBA(160, 150, 190, a));

      // Если не привязан — тихо поллим сервер, код покажем в MenuScreen
      if (!LumensAuth.get().isLinked() && !LumensAuth.get().isLoading()) {
         // poll раз в ~5 сек чтобы не спамить; MenuScreen дергает чаще при открытой GUI
      }
   }

   private Identifier resolveSkin() {
      try {
         if (mc.getNetworkHandler() != null && mc.player != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null && entry.getSkinTextures() != null) {
               return entry.getSkinTextures().texture();
            }
         }
      } catch (Exception ignored) {
      }
      return DefaultSkinHelper.getSteve().texture();
   }
}
