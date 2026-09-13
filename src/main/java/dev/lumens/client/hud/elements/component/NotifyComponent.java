package dev.lumens.client.hud.elements.component;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.nexusguard.protection.annotations.Native;
import dev.lumens.Lumens;
import dev.lumens.base.animations.base.Animation;
import dev.lumens.base.animations.base.Easing;
import dev.lumens.base.font.Font;
import dev.lumens.base.font.Fonts;
import dev.lumens.base.theme.Theme;
import dev.lumens.client.hud.elements.draggable.DraggableHudElement;
import dev.lumens.client.modules.api.Module;
import dev.lumens.utility.render.display.base.BorderRadius;
import dev.lumens.utility.render.display.base.CustomDrawContext;
import dev.lumens.utility.render.display.base.Gradient;
import dev.lumens.utility.render.display.base.color.ColorRGBA;
import dev.lumens.utility.render.display.shader.DrawUtil;

public class NotifyComponent extends DraggableHudElement {
    private final Animation toggleAnimation;
    private final List<NotifyComponent.BaseNotification> notifications;

    public NotifyComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
        this.toggleAnimation = new Animation(200L, Easing.CUBIC_OUT);
        this.notifications = new ArrayList();
    }

    public void addNotification(Module module, boolean enabled) {
        this.notifications.addLast(new NotifyComponent.ModuleNotification(module, enabled));
    }

    public void addTextNotification(String icon, Text text) {
        this.notifications.addLast(new NotifyComponent.TextNotification(icon, text));
    }

    public void addTotemNotification(String name, boolean enchanted) {
        this.notifications.addLast(new NotifyComponent.TotemNotification(name, enchanted));
    }

    @Native
    public void render(CustomDrawContext ctx) {
        Iterator<NotifyComponent.BaseNotification> iterator = this.notifications.iterator();
        this.toggleAnimation.update(mc.currentScreen instanceof ChatScreen && this.notifications.isEmpty());
        Theme theme = Lumens.getInstance().getThemeManager().getCurrentTheme();
        ColorRGBA accent = theme.getColor().withAlpha(255);
        ColorRGBA second = theme.getSecondColor().withAlpha(255);
        
        Font textFont = Fonts.REGULAR.getFont(6.75F);
        Font iconFont = Fonts.ICONS2.getFont(6.75F);
        float notificationHeight = 18.0F;
        
        float x = (float)mc.getWindow().getScaledWidth() / 2.0F;
        float y = (float)mc.getWindow().getScaledHeight() / 2.0F + 16.0F;
        
        // Demo notification when in ChatScreen
        if (mc.currentScreen instanceof ChatScreen && this.notifications.isEmpty()) {
            float demoWidth = 8 + Fonts.SEMIBOLD.getWidth("Lumens", 7.0F) + 9 + 12 + 20;
            float alphaVal = this.toggleAnimation.getValue();
            DrawUtil.drawHudBg(ctx.getMatrices(), x - demoWidth / 2, y, demoWidth, notificationHeight, 6.0F, BorderRadius.all(8.0F), new ColorRGBA(17, 17, 22, 255).mix(accent, 0.12F).withAlpha(alphaVal * 255));
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), x - demoWidth / 2, y, demoWidth, notificationHeight, 0.8F, BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255, 20 * alphaVal));
            
            float textX = x - demoWidth / 2 + 12;
            float wave = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 550.0);
            ColorRGBA gL = ColorRGBA.lerp(accent, second, wave * 0.6F);
            ColorRGBA gR = ColorRGBA.lerp(second, accent, wave * 0.6F);
            ctx.drawText(Fonts.SEMIBOLD.getFont(7.0F), "Lumens", textX, y + 5, Gradient.of(gL, gL, gR, gR).mulAlpha(alphaVal));
            textX += Fonts.SEMIBOLD.getWidth("Lumens", 7.0F) + 9;
            
            DrawUtil.drawRoundedRect(ctx.getMatrices(), textX, y + 7, 2, 2, BorderRadius.all(1.0F), accent.withAlpha(120 * this.toggleAnimation.getValue()));
            textX += 6;
            
            ctx.drawText(Fonts.ICONS2.getFont(7.0F), "\uf058", textX, y + 6, accent.withAlpha(this.toggleAnimation.getValue() * 255));
        }
        
        NotifyComponent.BaseNotification n;
        for(Iterator var11 = Lists.reverse(this.notifications).iterator(); var11.hasNext(); y += 22.0F * n.alphaAnimation.getValue()) {
            n = (NotifyComponent.BaseNotification)var11.next();
            y += 4.0F * n.alphaAnimation.getValue();
            n.render(ctx, x, y - 4.0F, textFont, theme, notificationHeight, this, accent, second);
        }

        while(true) {
            while(iterator.hasNext()) {
                NotifyComponent.BaseNotification notification = (NotifyComponent.BaseNotification)iterator.next();
                if (!notification.fadingOut && System.currentTimeMillis() - notification.timestamp > 3000L) {
                    notification.fadingOut = true;
                    notification.alphaAnimation.update(0.0F);
                }

                if (notification.fadingOut && notification.alphaAnimation.getValue() < 0.01F) {
                    iterator.remove();
                } else {
                    notification.alphaAnimation.update(notification.fadingOut ? 0.0F : 1.0F);
                }
            }

            return;
        }
    }

    private static class ModuleNotification extends NotifyComponent.BaseNotification {
        final Module module;
        final boolean enabled;

        ModuleNotification(Module module, boolean enabled) {
            this.module = module;
            this.enabled = enabled;
        }

        @Native
        void render(CustomDrawContext ctx, float x, float y, Font textFont, Theme theme, float notificationHeight, NotifyComponent parent, ColorRGBA accent, ColorRGBA second) {
            if (this.timestamp == 0L) {
                this.timestamp = System.currentTimeMillis();
            }

            ColorRGBA textColor = this.enabled ? new ColorRGBA(90, 255, 140, 255) : new ColorRGBA(255, 90, 90, 255);
            String moduleName = this.module.getName();
            String statusText = this.enabled ? " включена" : " выключена";
            float moduleNameWidth = Fonts.REGULAR.getWidth(moduleName, 7.25F);
            float statusTextWidth = textFont.width(statusText);
            float width = 12 + 9 + moduleNameWidth + 6 + statusTextWidth + 12;
            
            String icon = this.enabled ? "\uf058" : "\uf057";
            
            x -= width / 2.0F;
            float anim = this.alphaAnimation.getValue();
            
            DrawUtil.drawHudBg(ctx.getMatrices(), x, y, width, notificationHeight, 6.0F, BorderRadius.all(8.0F), new ColorRGBA(17, 17, 22, 255).mix(accent, 0.12F).withAlpha(anim * 255));
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, width, notificationHeight, 0.8F, BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255, 20 * anim));
            
            float textX = x + 12;
            ctx.drawText(Fonts.ICONS2.getFont(6.75F), icon, textX, y + 6, textColor.withAlpha(anim * 255));
            textX += 9 + 6;
            
            ctx.drawText(textFont, moduleName, textX, y + 5.5F, new ColorRGBA(243, 243, 246, 255).withAlpha(anim * 255));
            textX += moduleNameWidth + 6;
            
            ctx.drawText(textFont, statusText, textX, y + 5.5F, textColor.withAlpha(anim * 255));
        }
    }

    private static class TextNotification extends NotifyComponent.BaseNotification {
        final String icon;
        final Text text;

        TextNotification(String icon, Text text) {
            this.icon = icon;
            this.text = text;
        }

        void render(CustomDrawContext ctx, float x, float y, Font textFont, Theme theme, float notificationHeight, NotifyComponent parent, ColorRGBA accent, ColorRGBA second) {
            if (this.timestamp == 0L) {
                this.timestamp = System.currentTimeMillis();
            }

            float iconBgWidth = this.text.getString().contains("Игрок ") && this.text.getString().contains("просит о наблюдении") ? 9.0F : 14.0F;
            ColorRGBA textColor = this.icon.equals("\uf06a") ? new ColorRGBA(255, 234, 13, 255) : ColorRGBA.WHITE;
            String moduleNameStr = this.text.getString();
            float moduleNameWidth = textFont.width(moduleNameStr);
            float width = 12 + 9 + moduleNameWidth + 12;
            
            String iconStr = this.icon;
            x -= width / 2.0F;
            float anim = this.alphaAnimation.getValue();
            
            DrawUtil.drawHudBg(ctx.getMatrices(), x, y, width, notificationHeight, 6.0F, BorderRadius.all(8.0F), new ColorRGBA(17, 17, 22, 255).mix(accent, 0.12F).withAlpha(anim * 255));
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, width, notificationHeight, 0.8F, BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255, 20 * anim));
            
            float textX = x + 12;
            ctx.drawText(Fonts.ICONS2.getFont(6.75F), iconStr, textX, y + 6, textColor.withAlpha(anim * 255));
            textX += 9 + 6;
            
            ctx.drawText(textFont, moduleNameStr, textX, y + 5.5F, new ColorRGBA(243, 243, 246, 255).withAlpha(anim * 255));
        }
    }

    private static class TotemNotification extends NotifyComponent.BaseNotification {
        final String name;
        final boolean enchanted;

        TotemNotification(String icon, boolean enchanted) {
            this.name = icon;
            this.enchanted = enchanted;
        }

        void render(CustomDrawContext ctx, float x, float y, Font textFont, Theme theme, float notificationHeight, NotifyComponent parent, ColorRGBA accent, ColorRGBA second) {
            if (this.timestamp == 0L) {
                this.timestamp = System.currentTimeMillis();
            }

            String moduleName = "Игрок %s потерял тотем, зачарован: ".formatted(this.name);
            float moduleNameWidth = textFont.width(moduleName);
            float width = 12 + 12 + moduleNameWidth + 12;
            
            x -= width / 2.0F;
            float anim = this.alphaAnimation.getValue();
            
            DrawUtil.drawHudBg(ctx.getMatrices(), x, y, width, notificationHeight, 6.0F, BorderRadius.all(8.0F), new ColorRGBA(17, 17, 22, 255).mix(accent, 0.12F).withAlpha(anim * 255));
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, width, notificationHeight, 0.8F, BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255, 20 * anim));
            
            float textX = x + 12;
            ctx.drawTexture(Identifier.of("minecraft", "textures/item/totem_of_undying.png"), textX, y + 5, 8, 8, ColorRGBA.WHITE.withAlpha(anim * 255));
            textX += 12 + 6;
            
            ctx.drawText(textFont, moduleName, textX, y + 5.5F, new ColorRGBA(243, 243, 246, 255).withAlpha(anim * 255));
            textX += moduleNameWidth + 6;
            
            ColorRGBA enchantedColor = this.enchanted ? new ColorRGBA(90, 255, 140, 255) : new ColorRGBA(255, 90, 90, 255);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), textX, y + 7, 4.0F, 4.0F, BorderRadius.all(2.0F), enchantedColor.withAlpha(anim * 255));
        }
    }

    private abstract static class BaseNotification {
        long timestamp;
        boolean fadingOut = false;
        final Animation alphaAnimation;

        private BaseNotification() {
            this.alphaAnimation = new Animation(300L, Easing.CUBIC_OUT);
        }

        abstract void render(CustomDrawContext var1, float var2, float var3, Font var4, Theme var5, float var6, NotifyComponent var7, ColorRGBA var8, ColorRGBA var9);
    }
}