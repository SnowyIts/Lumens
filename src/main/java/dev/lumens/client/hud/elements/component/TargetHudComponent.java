package dev.lumens.client.hud.elements.component;

import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import ru.nexusguard.protection.annotations.Native;
import dev.lumens.Lumens;
import dev.lumens.base.animations.base.Animation;
import dev.lumens.base.animations.base.Easing;
import dev.lumens.base.font.Fonts;
import dev.lumens.base.font.MsdfRenderer;
import dev.lumens.base.theme.Theme;
import dev.lumens.client.hud.elements.draggable.DraggableHudElement;
import dev.lumens.client.modules.impl.combat.Aura;
import dev.lumens.client.modules.impl.misc.NameProtect;
import dev.lumens.client.modules.impl.misc.ScoreboardHealth;
import dev.lumens.utility.game.player.PlayerIntersectionUtil;
import dev.lumens.utility.mixin.accessors.DrawContextAccessor;
import dev.lumens.utility.render.display.base.BorderRadius;
import dev.lumens.utility.render.display.base.CustomDrawContext;
import dev.lumens.utility.render.display.base.color.ColorRGBA;
import dev.lumens.utility.render.display.shader.DrawUtil;

public class TargetHudComponent extends DraggableHudElement {
    private final Animation healthAnimation;
    private final Animation outdatedHealthAnimation;
    private final Animation gappleAnimation;
    private final Animation toggleAnimation;
    private final Animation toggleAnimationMetanoise;
    private LivingEntity target;

    public TargetHudComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
        this.healthAnimation = new Animation(250L, Easing.CUBIC_OUT);
        this.outdatedHealthAnimation = new Animation(650L, Easing.CUBIC_OUT);
        this.gappleAnimation = new Animation(250L, Easing.CUBIC_OUT);
        this.toggleAnimation = new Animation(250L, Easing.CUBIC_OUT);
        this.toggleAnimationMetanoise = new Animation(1850L, Easing.CIRC_OUT);
    }

    @Native
    public void render(CustomDrawContext ctx) {
        Aura aura = Aura.INSTANCE;
        LivingEntity target = mc.currentScreen instanceof ChatScreen ? mc.player : aura.getTarget();
        this.setTarget(target);
        if (this.toggleAnimationMetanoise.getValue() != 0.0F && this.target != null) {
            this.renderTargetHud(ctx, this.target, this.toggleAnimation.getValue());
        }
    }

    @Native
    private void renderTargetHud(CustomDrawContext ctx, LivingEntity target, float animation) {
        float posX = this.x;
        float posY = this.y;
        float width = 94.0F;
        float height = 36.0F;
        Theme theme = Lumens.getInstance().getThemeManager().getCurrentTheme();
        ColorRGBA accent = theme.getColor().withAlpha(255);
        ColorRGBA second = theme.getSecondColor().withAlpha(255);
        
        float hp = ScoreboardHealth.INSTANCE.isEnabled() ? PlayerIntersectionUtil.getHealth(target) : target.getHealth();
        this.healthAnimation.update(hp / target.getMaxHealth());
        if (this.outdatedHealthAnimation.getValue() < this.healthAnimation.getValue()) {
            this.outdatedHealthAnimation.setValue(this.healthAnimation.getValue());
            this.outdatedHealthAnimation.setStartValue(this.healthAnimation.getValue());
        } else {
            this.outdatedHealthAnimation.update(hp / target.getMaxHealth());
        }

        this.gappleAnimation.update(target.getAbsorptionAmount() / target.getMaxHealth());
        
        float anim = this.toggleAnimationMetanoise.getValue() * animation;
        if (anim <= 0.01F) return;

        // Background matching ClickGUI style
        DrawUtil.drawHudBg(ctx.getMatrices(), posX, posY, width, height, 6.0F, BorderRadius.all(8.0F), new ColorRGBA(17, 17, 22, 255).mix(accent, 0.12F).withAlpha(anim * 255));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), posX, posY, width, height, 0.8F, BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255, 20 * anim));

        Identifier skinTextures = null;
        Iterator var11 = mc.getNetworkHandler().getPlayerList().iterator();

        while(var11.hasNext()) {
            PlayerListEntry playerListEntry = (PlayerListEntry)var11.next();
            if (playerListEntry.getProfile().getName().equals(target.getNameForScoreboard())) {
                skinTextures = playerListEntry.getSkinTextures().texture();
            }
        }

        if (skinTextures == null) {
            skinTextures = DefaultSkinHelper.getSteve().texture();
        }

        // Player head with rounded corners
        DrawUtil.drawPlayerHeadWithRoundedShader(ctx.getMatrices(), skinTextures, posX + 8, posY + 7, 22.0F, BorderRadius.all(4.0F), ColorRGBA.WHITE.withAlpha(anim * 255.0F));
        
        float textX = posX + 35;
        float textY = posY + 6;
        
        // Target name
        String targetName = target == mc.player ? NameProtect.getCustomName() : target.getNameForScoreboard();
        MsdfRenderer.renderText(Fonts.REGULAR, targetName, 7.25F, ColorRGBA.WHITE.withAlpha(anim * 255.0F).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), textX, textY, 0.0F, true, 0.7F, 1.0F, 56.0F);
        
        textY += 12;
        // HP text
        String hpText = "HP: " + String.format("%.0f", hp) + (target.getAbsorptionAmount() > 0.0F ? String.format(" (%.1f)", target.getAbsorptionAmount()) : "").replace(",", ".");
        ctx.drawText(Fonts.REGULAR.getFont(6.5F), hpText, textX, textY, new ColorRGBA(243, 243, 246, 255).withAlpha(anim * 255));
        
        textY += 10;
        // Health bar
        float barWidth = width - 43;
        float barX = posX + 35;
        float barY = posY + height - 10;
        
        // Background
        DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, barWidth, 4, BorderRadius.all(2.0F), new ColorRGBA(44, 44, 52, 255).withAlpha(anim * 255));
        
        // Outdated health
        float outdatedWidth = MathHelper.clamp(barWidth * this.outdatedHealthAnimation.getValue(), 0.0F, barWidth);
        if (outdatedWidth > 0) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, outdatedWidth, 4, BorderRadius.all(2.0F), second.darker(0.5F).withAlpha(anim * 255));
        }
        
        // Current health
        float currentWidth = MathHelper.clamp(barWidth * this.healthAnimation.getValue(), 0.0F, barWidth);
        if (currentWidth > 0) {
            ColorRGBA hpColor = second.darker(0.35F).mix(accent, 0.5F);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, currentWidth, 4, BorderRadius.all(2.0F), hpColor.withAlpha(anim * 255));
        }
        
        // Absorption — желтый отрезок поверх конца полоски здоровья
        if (target.getAbsorptionAmount() > 0.0F) {
            float absW = MathHelper.clamp(barWidth * (target.getAbsorptionAmount() / target.getMaxHealth()), 0.0F, Math.max(0.0F, barWidth - currentWidth));
            if (absW > 0.5F) {
                DrawUtil.drawRoundedRect(ctx.getMatrices(), barX + currentWidth, barY, absW, 4, BorderRadius.all(2.0F), new ColorRGBA(255, 209, 0, 255).withAlpha(anim * 255));
            }
        }
        
        if (target instanceof PlayerEntity) {
            this.drawArmor(ctx, (PlayerEntity)target, posX + 8, posY - 14, 0.0F, 0.0F, 0.0F);
        }

        this.width = width;
        this.height = height;
    }

    private void drawArmor(CustomDrawContext ctx, PlayerEntity player, float posX, float posY, float headSize, float padding, float fontSize) {
        float boxSizeItem = 10.0F;
        float paddingItem = 0.0F;
        float iconX = posX;
        float iconY = posY;
        List<ItemStack> armor = player.getInventory().armor;
        ItemStack[] items = new ItemStack[]{player.getMainHandStack(), player.getOffHandStack(), armor.get(3), armor.get(2), armor.get(1), armor.get(0)};
        
        float anim = this.toggleAnimation.getValue();
        ItemStack[] var15 = items;
        int var16 = items.length;

        for(int var17 = 0; var17 < var16; ++var17) {
            ItemStack stack = var15[var17];
            if (!stack.isEmpty()) {
                ctx.getMatrices().push();
                ctx.getMatrices().translate((double)iconX + ((double)boxSizeItem - 9.6D) / 2.0D, (double)iconY + ((double)boxSizeItem - 9.6D) / 2.0D, 0.0D);
                ctx.getMatrices().scale(0.6F * anim, 0.6F * anim, 0.6F * anim);
                ctx.drawItem(stack, 0, 0);
                ((DrawContextAccessor)ctx).callDrawItemBar(stack, 0, 0);
                ((DrawContextAccessor)ctx).callDrawCooldownProgress(stack, 0, 0);
                ctx.getMatrices().pop();
                iconX += boxSizeItem + paddingItem;
            }
        }

    }

    public void setTarget(LivingEntity target) {
        if (target == null) {
            this.toggleAnimation.update(0.0F);
            this.toggleAnimationMetanoise.update(0.0F);
            this.toggleAnimationMetanoise.setDuration(2200L);
            this.toggleAnimationMetanoise.setEasing(Easing.CIRC_OUT);
            if (this.toggleAnimationMetanoise.getValue() == 0.0F) {
                this.target = null;
            }
        } else {
            this.target = target;
            this.toggleAnimationMetanoise.update(1.0F);
            this.toggleAnimationMetanoise.setDuration(1300L);
            this.toggleAnimationMetanoise.setEasing(Easing.CIRC_OUT);
            this.toggleAnimation.update(1.0F);
        }

    }
}