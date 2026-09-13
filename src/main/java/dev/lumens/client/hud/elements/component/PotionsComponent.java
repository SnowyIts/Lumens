package dev.lumens.client.hud.elements.component;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.nexusguard.protection.annotations.Native;
import dev.lumens.Lumens;
import dev.lumens.base.animations.base.Animation;
import dev.lumens.base.animations.base.Easing;
import dev.lumens.base.font.Fonts;
import dev.lumens.base.theme.Theme;
import dev.lumens.client.hud.elements.draggable.DraggableHudElement;
import dev.lumens.client.modules.api.setting.impl.BooleanSetting;
import dev.lumens.utility.render.display.base.BorderRadius;
import dev.lumens.utility.render.display.base.CustomDrawContext;
import dev.lumens.utility.render.display.base.Gradient;
import dev.lumens.utility.render.display.base.color.ColorRGBA;
import dev.lumens.utility.render.display.shader.DrawUtil;

public class PotionsComponent extends DraggableHudElement {
    private final BooleanSetting s1 = new BooleanSetting("Background", true);
    private final BooleanSetting s2 = new BooleanSetting("Icons", true);
    private final BooleanSetting s3 = new BooleanSetting("Amplifier", true);
    private final BooleanSetting s4 = new BooleanSetting("Duration", true);
    private final Animation widthAnimation;
    private final Animation xLine;
    private final Animation alpha;
    private final List<PotionsComponent.PotionItem> potionItems;

    public PotionsComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
        this.widthAnimation = new Animation(200L, Easing.CUBIC_OUT);
        this.xLine = new Animation(170L, Easing.SINE_OUT);
        this.alpha = new Animation(200L, Easing.CUBIC_OUT);
        this.potionItems = new CopyOnWriteArrayList();
    }

    @Native
    public void render(CustomDrawContext ctx) {
        if (mc.player != null) {
            this.updatePotions();
            float posX = this.getX();
            float posY = this.getY();
            float defaultWidth = 47.0F;
            float height = 22.0F;
            this.potionItems.sort(Comparator.comparing((pi) -> pi.name));
            boolean isFound = false;
            float durationWidth = 0.0F;
            Iterator var8 = this.potionItems.iterator();

            String duration;
            while(var8.hasNext()) {
                PotionsComponent.PotionItem item = (PotionsComponent.PotionItem)var8.next();
                item.animation.update(item.active);
                if (item.animation.getValue() != 0.0F) {
                    int seconds = item.durationTicks / 20;
                    int minutes = seconds / 60;
                    int sec = seconds % 60;
                    duration = String.format("%d:%02d", minutes, sec);
                    durationWidth = Fonts.REGULAR.getWidth(duration, 6.75F) + 4.0F;
                    if (this.s1.isEnabled()) height += 18.0F;
                    if (item.animation.getValue() != 0.0F) {
                        this.alpha.update(1.0F);
                        isFound = true;
                    }
                }
            }

            this.xLine.update(durationWidth);
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

            // Header background
            float ptIconW = Fonts.ICONS2.getWidth("\uf6e1", 7.0F);
            float headerWidth = 12 + Fonts.SEMIBOLD.getWidth("Potions", 7.0F) + 9 + 2 + 6 + ptIconW + 10;
            DrawUtil.drawHudBg(ctx.getMatrices(), posX, posY, headerWidth, 20, 6.0F, BorderRadius.all(8.0F), new ColorRGBA(17, 17, 22, 255).mix(accent, 0.12F).withAlpha(alphaVal * 255));
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), posX, posY, headerWidth, 20, 0.8F, BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255, 20 * alphaVal));
            
            float textX = posX + 12;
            // Potions text with gradient
            float wave = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 550.0);
            ColorRGBA gL = ColorRGBA.lerp(accent, second, wave * 0.6F);
            ColorRGBA gR = ColorRGBA.lerp(second, accent, wave * 0.6F);
            ctx.drawText(Fonts.SEMIBOLD.getFont(7.0F), "Potions", textX, posY + 6, Gradient.of(gL, gL, gR, gR).mulAlpha(alphaVal));
            textX += Fonts.SEMIBOLD.getWidth("Potions", 7.0F) + 9;
            
            // Divider
            DrawUtil.drawRoundedRect(ctx.getMatrices(), textX, posY + 8, 2, 2, BorderRadius.all(1.0F), accent.withAlpha(120 * alphaVal));
            textX += 2 + 6;
            
            // Icon
            ctx.drawText(Fonts.ICONS2.getFont(7.0F), "\uf6e1", textX, posY + 7, accent.withAlpha(alphaVal * 255));
            
            posY += 22;
            
            if (this.s1.isEnabled()) {
                Iterator var17 = this.potionItems.iterator();

                while(var17.hasNext()) {
                    PotionsComponent.PotionItem item = (PotionsComponent.PotionItem)var17.next();
                    if (item.animation.getValue() != 0.0F) {
                        String name = I18n.translate(item.name, new Object[0]);
                        String amp = this.getAmplifierText(item.amplifier);
                        duration = this.formatDuration(item.durationTicks);
                        Identifier icon = this.getEffectIcon((StatusEffect)item.effect.getEffectType().value());
                        float itemAnim = item.animation.getValue() * alphaVal;
                        float rowY = posY + (1.0F - itemAnim) * 6.0F;
                        
                        float elementsWidth = 10.0F;
                        if (this.s2.isEnabled()) elementsWidth += 12.0F;
                        elementsWidth += Fonts.REGULAR.getWidth(name, 6.75F) + 6.0F;
                        boolean showAmp = this.s3.isEnabled() && Integer.parseInt(amp) > 0;
                        if (showAmp) elementsWidth += Fonts.REGULAR.getWidth(amp, 6.75F) + 6.0F;
                        if (this.s4.isEnabled()) elementsWidth += Fonts.REGULAR.getWidth(duration, 6.75F);
                        elementsWidth += 10.0F;
                        
                        // Row background
                        if (itemAnim > 0.01F) {
                            DrawUtil.drawHudBg(ctx.getMatrices(), posX, rowY, this.widthAnimation.getValue(), 16, 6.0F, BorderRadius.all(6.0F), new ColorRGBA(26, 26, 32, 255).mix(accent, 0.08F).withAlpha(itemAnim * 255));
                            DrawUtil.drawRoundedBorder(ctx.getMatrices(), posX, rowY, this.widthAnimation.getValue(), 16, 0.6F, BorderRadius.all(6.0F), new ColorRGBA(255, 255, 255, 12 * itemAnim));
                        }
                        
                        float rowX = posX + 10;
                        
                        if (this.s2.isEnabled()) {
                            ctx.drawTexture(icon, rowX, rowY + 4, 10, 10, ColorRGBA.WHITE.withAlpha(itemAnim * 255));
                            rowX += 12;
                        }

                        if (this.s1.isEnabled()) {
                            ctx.drawText(Fonts.REGULAR.getFont(6.5F), name, rowX, rowY + 4.25F, new ColorRGBA(243, 243, 246, 255).withAlpha(itemAnim * 255));
                            rowX += Fonts.REGULAR.getWidth(name, 6.75F) + 6;
                        }

                        if (showAmp) {
                            ctx.drawText(Fonts.REGULAR.getFont(6.5F), amp, rowX, rowY + 4.25F, accent.withAlpha(itemAnim * 255));
                            rowX += Fonts.REGULAR.getWidth(amp, 6.75F) + 6;
                        }

                        if (this.s4.isEnabled()) {
                            ctx.drawText(Fonts.REGULAR.getFont(6.5F), duration, rowX, rowY + 4.25F, new ColorRGBA(243, 243, 246, 255).withAlpha(itemAnim * 255));
                        }

                        if (elementsWidth > defaultWidth) {
                            defaultWidth = elementsWidth;
                        }

                        posY += 18.0F;
                    }
                }
            }

            this.widthAnimation.update(defaultWidth);
            this.width = this.widthAnimation.getValue();
            this.height = height;
        }
    }

    private String getAmplifierText(int amplifier) {
        return String.valueOf(amplifier + 1);
    }

    private String formatDuration(int durationTicks) {
        int totalSeconds = durationTicks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private Identifier getEffectIcon(StatusEffect effect) {
        String id = effect.getTranslationKey().replace("effect.minecraft.", "").replace("effect.", "");
        return Identifier.of("minecraft", "textures/mob_effect/" + id + ".png");
    }

    @Native
    public void updatePotions() {
        if (mc.player != null) {
            Map<String, StatusEffectInstance> currentEffects = (Map)mc.player.getStatusEffects().stream().collect(Collectors.toMap((e) -> {
                String var10000 = Text.translatable(e.getTranslationKey()).getString();
                return var10000 + ":" + e.getAmplifier();
            }, (e) -> {
                return e;
            }, (e1, e2) -> {
                return e1;
            }));
            this.potionItems.forEach((item) -> {
                String key = item.name + ":" + item.amplifier;
                StatusEffectInstance effect = (StatusEffectInstance)currentEffects.get(key);
                if (effect != null) {
                    item.durationTicks = effect.getDuration();
                    if (!item.active) {
                        item.animation.setValue(1.0F);
                    }

                    item.active = true;
                    currentEffects.remove(key);
                } else {
                    item.active = false;
                }

            });
            currentEffects.forEach((key, effect) -> {
                this.potionItems.add(new PotionsComponent.PotionItem(Text.translatable(effect.getTranslationKey()).getString(), effect.getAmplifier(), effect.getDuration(), effect));
            });
            this.potionItems.removeIf((item) -> {
                return !item.active && item.animation.getValue() == 0.0F;
            });
        }
    }

    private static class PotionItem {
        String name;
        int amplifier;
        int durationTicks;
        boolean active;
        StatusEffectInstance effect;
        Animation animation;

        PotionItem(String name, int amplifier, int durationTicks, StatusEffectInstance effect) {
            this.animation = new Animation(250L, Easing.CUBIC_OUT);
            this.name = name;
            this.amplifier = amplifier;
            this.durationTicks = durationTicks;
            this.active = true;
            this.effect = effect;
        }
    }
}