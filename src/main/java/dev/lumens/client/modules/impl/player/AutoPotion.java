package dev.lumens.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import dev.lumens.utility.component.RotationComponent;
import dev.lumens.utility.game.player.rotation.Rotation;
import dev.lumens.base.events.impl.input.EventKey;
import dev.lumens.base.events.impl.player.EventUpdate;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.BooleanSetting;
import dev.lumens.client.modules.api.setting.impl.KeySetting;
import dev.lumens.client.modules.api.setting.impl.MultiBooleanSetting;
import dev.lumens.client.modules.api.setting.impl.NumberSetting;
import dev.lumens.utility.game.player.PlayerInventoryUtil;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

@ModuleAnnotation(
        name = "AutoPotion",
        category = Category.PLAYER,
        description = "Автоматически кидает зелья себе под ноги когда эффект закончился"
)
public final class AutoPotion extends Module {
    public static final AutoPotion INSTANCE = new AutoPotion();

    private final MultiBooleanSetting potions = MultiBooleanSetting.create("Зелья",
            java.util.List.of("Speed", "Strength", "Fire Resistance", "Healing"));
    
    private final NumberSetting healHealth = new NumberSetting("Heal Health", 10.0F, 0.0F, 20.0F, 0.5F,
            () -> potions.isEnable("Healing"));
    private final KeySetting healKey = new KeySetting("Heal Key", -1,
            () -> potions.isEnable("Healing"));
    private final BooleanSetting single = new BooleanSetting("Single", "Кидать только одно зелье за раз", true);
    private final BooleanSetting multi = new BooleanSetting("Multi", "Кидать все подходящие", false);
    private final BooleanSetting disableAfterThrow = new BooleanSetting("Disable After Throw", "Выключаться после броска", false);
    private final BooleanSetting excludeDonate = new BooleanSetting("Exclude Donate Potions", "Не трогать донат-зелья (с лишними эффектами)", false);

    private int lastThrowTick = 0;
    private boolean thrown = false;
    private boolean blocked = false;
    private boolean manualHeal = false;

    private static final int POTION_COOLDOWN = 10;
    private static final float YAW_WOBBLE = 17.0F;
    private static final float THROW_PITCH = 90.0F;
    private static final float MIN_PITCH = 80.0F;

    private AutoPotion() {}

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;
        
        // Check if player is in inventory
        if (mc.currentScreen != null && !mc.currentScreen.getClass().getSimpleName().equals("InventoryScreen")) {
            return;
        }

        if (blocked) return;

        if (thrown) {
            thrown = false;
            return;
        }

        // Check if standing on solid block
        if (!mc.player.isOnGround()) return;

        boolean threwAnything = false;

        // Speed Potion
        if (potions.isEnable("Speed") && !hasPotionEffect("speed")) {
            if (tryThrowPotion(Items.SPLASH_POTION)) {
                threwAnything = true;
                if (single.isEnabled()) return;
            }
        }

        // Strength Potion
        if (potions.isEnable("Strength") && !hasPotionEffect("strength")) {
            if (tryThrowPotion(Items.SPLASH_POTION)) {
                threwAnything = true;
                if (single.isEnabled()) return;
            }
        }

        // Fire Resistance Potion
        if (potions.isEnable("Fire Resistance") && !hasPotionEffect("fire_resistance")) {
            if (tryThrowPotion(Items.SPLASH_POTION)) {
                threwAnything = true;
                if (single.isEnabled()) return;
            }
        }

        // Healing Potion
        if (potions.isEnable("Healing")) {
            boolean needHeal = manualHeal || mc.player.getHealth() < healHealth.getCurrent();
            if (needHeal) {
                if (tryThrowPotion(Items.SPLASH_POTION)) {
                    threwAnything = true;
                    manualHeal = false;
                    if (single.isEnabled()) return;
                }
            }
        }

        if (!threwAnything) {
            // Nothing to throw
        }
    }

    @EventTarget
    public void onKey(EventKey event) {
        if (healKey.getKeyCode() != -1 && event.getKeyCode() == healKey.getKeyCode() && potions.isEnable("Healing")) {
            manualHeal = true;
        }
    }

private boolean hasPotionEffect(String effectId) {
        return mc.player.getStatusEffects().stream()
                .anyMatch(e -> {
                    String translationKey = e.getEffectType().value().getTranslationKey();
                    return translationKey != null && translationKey.contains(effectId.toLowerCase());
                });
    }

    private boolean tryThrowPotion(Item potionItem) {
        int hotbarSlot = PlayerInventoryUtil.find(potionItem, 0, 8);
        if (hotbarSlot == -1) return false;

        int invSlot = PlayerInventoryUtil.find(potionItem, 9, 45);
        if (invSlot == -1) return false;

        // Check if we should exclude donate potions
        if (excludeDonate.isEnabled()) {
            // Simplified check - could be expanded
        }

        // Set rotation to look down with slight yaw wobble
        float yaw = mc.player.getYaw() + (float) (Math.sin(mc.player.age) * YAW_WOBBLE);
        float pitch = THROW_PITCH;

        // Use rotation component
        RotationComponent.update(
                new Rotation(yaw, pitch),
                360, 360, 360, 360, 0, 1, false
        );

        // Wait for pitch to be correct
        if (mc.player.getPitch() < MIN_PITCH) return false;

        // Throw the potion
        int originalSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = hotbarSlot;
        
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);
        
        mc.player.getInventory().selectedSlot = originalSlot;
        
        lastThrowTick = mc.player.age;
        thrown = true;
        
        if (disableAfterThrow.isEnabled()) {
            setEnabled(false);
        }
        
        return true;
    }
}