package dev.lumens.client.hud.elements.component;

import java.util.Locale;
import ru.nexusguard.protection.annotations.Native;
import dev.lumens.Lumens;
import dev.lumens.base.font.Fonts;
import dev.lumens.base.theme.Theme;
import dev.lumens.client.hud.elements.draggable.DraggableHudElement;
import dev.lumens.utility.render.display.base.BorderRadius;
import dev.lumens.utility.render.display.base.CustomDrawContext;
import dev.lumens.utility.render.display.base.color.ColorRGBA;
import dev.lumens.utility.render.display.shader.DrawUtil;

public class InformationComponent extends DraggableHudElement {

    public InformationComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
    }

    @Native
    public void render(CustomDrawContext ctx) {
        if (mc.player == null) return;
        Theme theme = Lumens.getInstance().getThemeManager().getCurrentTheme();
        ColorRGBA accent = theme.getColor().withAlpha(255);

        int px = (int)Math.floor(mc.player.getX());
        int py = (int)Math.floor(mc.player.getY());
        int pz = (int)Math.floor(mc.player.getZ());
        double speed = Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ);
        String coordsText = String.format(Locale.US, "%d %d %d", px, py, pz);
        String speedText = String.format("%.2f", speed * 20.0D).replace(",", ".");
        
        float coordsWidth = Fonts.REGULAR.getWidth(coordsText, 7.5F);
        float speedWidth = Fonts.REGULAR.getWidth(speedText + " Б/С", 7.5F);
        float width = 12 + 10 + coordsWidth + 8 + 2 + 6 + 10 + speedWidth + 12;
        
        float x = this.getX();
        float y = this.getY();
        
        // Background matching ClickGUI style
        DrawUtil.drawHudBg(ctx.getMatrices(), x, y, width, 22, 6.0F, BorderRadius.all(8.0F), new ColorRGBA(17, 17, 22, 255).mix(accent, 0.12F));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, width, 22, 0.8F, BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255, 20));
        
        float textX = x + 12;
        
        // Coords icon and text
        ctx.drawText(Fonts.ICONS2.getFont(7.5F), "\uf57d", textX, y + 6, accent);
        ctx.drawText(Fonts.REGULAR.getFont(7.5F), coordsText, textX + 10, y + 5.5F, new ColorRGBA(243, 243, 246, 255));
        textX += 10 + coordsWidth + 8;
        
        // Divider
        DrawUtil.drawRoundedRect(ctx.getMatrices(), textX, y + 8, 2, 2, BorderRadius.all(1.0F), accent.withAlpha(120));
        textX += 6;
        
        // Speed icon and text
        ctx.drawText(Fonts.ICONS2.getFont(7.5F), "\uf70c", textX, y + 6, accent);
        ctx.drawText(Fonts.REGULAR.getFont(7.5F), speedText + " Б/С", textX + 10, y + 5.5F, new ColorRGBA(243, 243, 246, 255));
        
        this.width = width;
        this.height = 22.0F;
    }
}