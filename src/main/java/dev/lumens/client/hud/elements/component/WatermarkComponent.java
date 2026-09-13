package dev.lumens.client.hud.elements.component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.network.PlayerListEntry;
import ru.nexusguard.protection.annotations.Native;
import dev.lumens.Lumens;
import dev.lumens.base.font.Fonts;
import dev.lumens.base.theme.Theme;
import dev.lumens.client.hud.elements.draggable.DraggableHudElement;
import dev.lumens.client.modules.impl.misc.NameProtect;
import dev.lumens.utility.render.display.base.BorderRadius;
import dev.lumens.utility.render.display.base.CustomDrawContext;
import dev.lumens.utility.render.display.base.Gradient;
import dev.lumens.utility.render.display.base.color.ColorRGBA;
import dev.lumens.utility.render.display.shader.DrawUtil;

public class WatermarkComponent extends DraggableHudElement {
    public WatermarkComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
    }

    @Native
    public void render(CustomDrawContext ctx) {
        if (mc.player == null) return;
        float x = this.getX();
        float y = this.getY();
        Theme theme = Lumens.getInstance().getThemeManager().getCurrentTheme();
        ColorRGBA accent = theme.getColor().withAlpha(255);
        ColorRGBA second = theme.getSecondColor().withAlpha(255);
        
        String name = "Lumens";
        String fps = mc.getCurrentFps() + "fps";
        PlayerListEntry list = mc.getNetworkHandler() != null
                ? mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) : null;
        
        String playerName = NameProtect.INSTANCE.isEnabled() ? NameProtect.getCustomName() : mc.player.getNameForScoreboard();
        float nameWidth = Fonts.SEMIBOLD.getWidth(name, 8.0F);
        float playerWidth = Fonts.REGULAR.getWidth(playerName, 7.25F);
        String pingText = list != null ? list.getLatency() + "ms" : "0ms";
        float pingWidth = Fonts.REGULAR.getWidth(pingText, 7.25F);
        float fpsWidth = Fonts.REGULAR.getWidth(fps, 7.25F);

        // ширина по курсору: паддинг + сегменты (зазор 6, точка 2, зазор 6, слот иконки 9)
        float width = 8 + nameWidth + 6 + 2 + 6 + 9 + playerWidth + 8 + 2 + 6 + 9 + pingWidth + 8 + 2 + 6 + 9 + fpsWidth + 8;
        
        // Background blur - matching ClickGUI style
        DrawUtil.drawHudBg(ctx.getMatrices(), x, y, width, 18, 6.0F, BorderRadius.all(8.0F), new ColorRGBA(17, 17, 22, 255).mix(accent, 0.12F));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, width, 18, 0.8F, BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255, 20));
        
        // Lumens text with gradient like in ClickGUI
        float textX = x + 8;
        float wave = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 550.0);
        ColorRGBA gL = ColorRGBA.lerp(accent, second, wave * 0.6F);
        ColorRGBA gR = ColorRGBA.lerp(second, accent, wave * 0.6F);
        ctx.drawText(Fonts.SEMIBOLD.getFont(8.0F), name, textX, y + 5, Gradient.of(gL, gL, gR, gR));
        
        textX += nameWidth + 6;
        
        // Divider dot
        DrawUtil.drawRoundedRect(ctx.getMatrices(), textX, y + 8, 2, 2, BorderRadius.all(1.0F), accent.withAlpha(120));
        textX += 2 + 6;
        
        // Player icon and name
        ctx.drawText(Fonts.ICONS2.getFont(6.0F), "\uf007", textX, y + 6, accent);
        ctx.drawText(Fonts.REGULAR.getFont(7.25F), NameProtect.INSTANCE.isEnabled() ? NameProtect.getCustomName() : mc.player.getNameForScoreboard(), textX + 9, y + 5, new ColorRGBA(243, 243, 246, 255));
        textX += 9 + playerWidth + 8;
        
        // Divider dot
        DrawUtil.drawRoundedRect(ctx.getMatrices(), textX, y + 8, 2, 2, BorderRadius.all(1.0F), accent.withAlpha(120));
        textX += 2 + 6;
        
        // Ping icon and value
        ctx.drawText(Fonts.ICONS2.getFont(6.0F), "\uf1eb", textX, y + 5.5F, accent);
        ctx.drawText(Fonts.REGULAR.getFont(7.25F), list != null ? list.getLatency() + "ms" : "0ms", textX + 9, y + 5, new ColorRGBA(243, 243, 246, 255));
        textX += 9 + pingWidth + 8;
        
        // Divider dot
        DrawUtil.drawRoundedRect(ctx.getMatrices(), textX, y + 8, 2, 2, BorderRadius.all(1.0F), accent.withAlpha(120));
        textX += 2 + 6;
        
        // FPS icon and value
        ctx.drawText(Fonts.ICONS2.getFont(6.0F), "\uf624", textX, y + 5.5F, accent);
        ctx.drawText(Fonts.REGULAR.getFont(7.25F), fps, textX + 9, y + 5, new ColorRGBA(243, 243, 246, 255));
        
        // Second line - server info
        float x2 = this.getX();
        float y2 = this.getY() + 20;
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String server = mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null ? mc.getCurrentServerEntry().address : "Неизвестно";
        String tps = String.format("%.1f", Lumens.getInstance().getServerHandler().getTPS()).replace(",", ".") + "tps";
        
        float serverWidth = Fonts.REGULAR.getWidth(server, 7.25F);
        float timeWidth = Fonts.REGULAR.getWidth(time, 7.25F);
        float tpsWidth = Fonts.REGULAR.getWidth(tps, 7.25F);
        
        float width2 = 8 + 9 + serverWidth + 8 + 2 + 6 + 9 + timeWidth + 8 + 2 + 6 + 9 + tpsWidth + 8;
        
        DrawUtil.drawHudBg(ctx.getMatrices(), x2, y2, width2, 18, 6.0F, BorderRadius.all(8.0F), new ColorRGBA(26, 26, 32, 255).mix(accent, 0.08F));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), x2, y2, width2, 18, 0.8F, BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255, 12));
        
        // Server icon and address
        ctx.drawText(Fonts.ICONS2.getFont(6.0F), "\uf0ac", x2 + 8, y2 + 6, accent);
        ctx.drawText(Fonts.REGULAR.getFont(7.25F), server, x2 + 19, y2 + 5, new ColorRGBA(243, 243, 246, 255));
        x2 += 19 + serverWidth + 8;
        
        // Divider
        DrawUtil.drawRoundedRect(ctx.getMatrices(), x2, y2 + 8, 2, 2, BorderRadius.all(1.0F), accent.withAlpha(120));
        x2 += 2 + 6;
        
        // Time icon and value
        ctx.drawText(Fonts.ICONS2.getFont(6.0F), "\uf017", x2, y2 + 5.5F, accent);
        ctx.drawText(Fonts.REGULAR.getFont(7.25F), time, x2 + 9, y2 + 5, new ColorRGBA(243, 243, 246, 255));
        x2 += 9 + timeWidth + 8;
        
        // Divider
        DrawUtil.drawRoundedRect(ctx.getMatrices(), x2, y2 + 8, 2, 2, BorderRadius.all(1.0F), accent.withAlpha(120));
        x2 += 2 + 6;
        
        // TPS icon and value
        ctx.drawText(Fonts.ICONS2.getFont(6.0F), "\uf68f", x2, y2 + 5.5F, accent);
        ctx.drawText(Fonts.REGULAR.getFont(7.25F), tps, x2 + 9, y2 + 5, new ColorRGBA(243, 243, 246, 255));
        
        this.width = Math.max(width, width2);
        this.height = 40.0F;
    }
}