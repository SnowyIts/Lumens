package dev.lumens.client.hud.elements.component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;
import ru.nexusguard.protection.annotations.Native;
import dev.lumens.Lumens;
import dev.lumens.base.animations.base.Animation;
import dev.lumens.base.animations.base.Easing;
import dev.lumens.base.font.Fonts;
import dev.lumens.base.theme.Theme;
import dev.lumens.client.hud.elements.draggable.DraggableHudElement;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.impl.render.Interface;
import dev.lumens.client.modules.impl.render.Menu;
import dev.lumens.utility.render.display.Keyboard;
import dev.lumens.utility.render.display.base.BorderRadius;
import dev.lumens.utility.render.display.base.CustomDrawContext;
import dev.lumens.utility.render.display.base.Gradient;
import dev.lumens.utility.render.display.base.color.ColorRGBA;
import dev.lumens.utility.render.display.shader.DrawUtil;

public class KeybindsComponent extends DraggableHudElement {
    private final Animation alpha;

    public KeybindsComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
        this.alpha = new Animation(200L, Easing.CUBIC_OUT);
    }

    @Native
    public void render(CustomDrawContext ctx) {
        float posX = this.getX();
        float posY = this.getY();
        boolean isFound = false;
        Iterator var7 = Lumens.getInstance().getModuleManager().getModules().iterator();

        while(var7.hasNext()) {
            Module module = (Module)var7.next();
            if (module.isEnabled() && module.getKeyCode() != -1 && !(module instanceof Menu)) {
                this.alpha.update(1.0F);
                isFound = true;
            }
        }

        if (!isFound && !(mc.currentScreen instanceof ChatScreen)) {
            this.alpha.update(0.0F);
        }

        if (mc.currentScreen instanceof ChatScreen) {
            this.alpha.update(1.0F);
        }

        Theme theme = Lumens.getInstance().getThemeManager().getCurrentTheme();
        ColorRGBA accent = theme.getColor().withAlpha(255);
        ColorRGBA second = theme.getSecondColor().withAlpha(255);
        
        float alphaVal = this.alpha.getValue();
        if (alphaVal <= 0.01F) {
            this.width = 0;
            this.height = 0;
            return;
        }

        // ---- замеры шапки и строк ----
        float titleW = Fonts.SEMIBOLD.getWidth("KeyBinds", 7.0F);
        float kbIconW = Fonts.ICONS2.getWidth("", 7.0F);
        float headerWidth = 12 + titleW + 9 + 2 + 6 + kbIconW + 10;

        List<Module> visible = new ArrayList<>();
        List<Float> rowWidths = new ArrayList<>();
        float maxRowW = 0.0F;
        for (Module m : Lumens.getInstance().getModuleManager().getModules()) {
            if (m.getAnimation().getValue() != 0.0F && m.getKeyCode() != -1 && !(m instanceof Menu)) {
                float rw = 10 + 9 + Fonts.REGULAR.getWidth(m.getName(), 6.75F) + 12 + 2 + 6
                        + Fonts.REGULAR.getWidth(Keyboard.getKeyName(m.getKeyCode()), 6.75F) + 10;
                visible.add(m);
                rowWidths.add(rw);
                if (rw > maxRowW) maxRowW = rw;
            }
        }
        float groupW = Math.max(headerWidth, maxRowW);

        // ---- позиция колонки: общий центр шапки и строк ----
        boolean rightAlign = this.getAlign() == DraggableHudElement.Align.TOP_RIGHT
                || this.getAlign() == DraggableHudElement.Align.CENTER_RIGHT
                || this.getAlign() == DraggableHudElement.Align.BOTTOM_RIGHT;
        float scrW = (float) mc.getWindow().getWidth() / Interface.INSTANCE.getCustomScale();
        float groupLeft = posX;
        if (rightAlign) {
            // правый край колонки стоит на месте, колонка растёт влево
            float anchor = this.width > 0.5F ? posX + this.width : posX + groupW;
            if (anchor > scrW) anchor = scrW;
            groupLeft = anchor - groupW;
            if (groupLeft < 0) groupLeft = 0;
            this.x = groupLeft;
        } else if (groupLeft + groupW > scrW) {
            groupLeft = Math.max(0, scrW - groupW);
        }
        this.width = groupW;

        // Header background (по центру колонки)
        float headerX = groupLeft + (groupW - headerWidth) / 2.0F;
        DrawUtil.drawHudBg(ctx.getMatrices(), headerX, posY, headerWidth, 20, 6.0F, BorderRadius.all(8.0F), new ColorRGBA(17, 17, 22, 255).mix(accent, 0.12F).withAlpha(alphaVal * 255));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), headerX, posY, headerWidth, 20, 0.8F, BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255, 20 * alphaVal));
        
        float textX = headerX + 12;
        // KeyBinds text with gradient
        float wave = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 550.0);
        ColorRGBA gL = ColorRGBA.lerp(accent, second, wave * 0.6F);
        ColorRGBA gR = ColorRGBA.lerp(second, accent, wave * 0.6F);
        ctx.drawText(Fonts.SEMIBOLD.getFont(7.0F), "KeyBinds", textX, posY + 6, Gradient.of(gL, gL, gR, gR).mulAlpha(alphaVal));
        textX += titleW + 9;
        
        // Divider
        DrawUtil.drawRoundedRect(ctx.getMatrices(), textX, posY + 8, 2, 2, BorderRadius.all(1.0F), accent.withAlpha(120 * alphaVal));
        textX += 2 + 6;
        
        // Icon
        ctx.drawText(Fonts.ICONS2.getFont(7.0F), "", textX, posY + 7, accent.withAlpha(alphaVal * 255));
        
        float rowTop = posY + 22;

        for (int i = 0; i < visible.size(); i++) {
            Module module = visible.get(i);
            float rowW = rowWidths.get(i);
            float rx = groupLeft + (groupW - rowW) / 2.0F;
            String bind = Keyboard.getKeyName(module.getKeyCode());
            String moduleName = module.getName();
            float moduleAnim = module.getAnimation().getValue() * alphaVal;
            float rowY = rowTop + (1.0F - moduleAnim) * 6.0F;

            // Row background — ровно под контент строки
            DrawUtil.drawHudBg(ctx.getMatrices(), rx, rowY, rowW, 16, 6.0F, BorderRadius.all(6.0F), new ColorRGBA(26, 26, 32, 255).mix(accent, 0.08F).withAlpha(moduleAnim * 255));
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), rx, rowY, rowW, 16, 0.6F, BorderRadius.all(6.0F), new ColorRGBA(255, 255, 255, 12 * moduleAnim));

            float rowX = rx + 10;
                // Category icon
                ctx.drawText(Fonts.ICONS.getFont(5.0F), module.getCategory().getIcon(), rowX, rowY + 4.5F, accent.withAlpha(moduleAnim * 255));
                rowX += 9;
                
                // Module name
                ctx.drawText(Fonts.REGULAR.getFont(6.5F), moduleName, rowX, rowY + 4.25F, new ColorRGBA(243, 243, 246, 255).withAlpha(moduleAnim * 255));
                rowX += Fonts.REGULAR.getWidth(moduleName, 6.75F) + 12;
                
                // Divider
                DrawUtil.drawRoundedRect(ctx.getMatrices(), rowX, rowY + 6, 2, 2, BorderRadius.all(1.0F), accent.withAlpha(120 * moduleAnim));
                rowX += 2 + 6;
                
                // Bind
                ctx.drawText(Fonts.REGULAR.getFont(6.5F), bind, rowX, rowY + 4.25F, new ColorRGBA(243, 243, 246, 255).withAlpha(moduleAnim * 255));

            rowTop += 18.0F;
        }

        this.height = 22.0F + 18.0F * visible.size();
    }
}