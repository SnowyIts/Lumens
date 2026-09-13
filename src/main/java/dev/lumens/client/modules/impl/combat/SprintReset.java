package dev.lumens.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import dev.lumens.base.events.impl.other.EventTick;
import dev.lumens.base.events.impl.player.EventAttack;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.BooleanSetting;
import dev.lumens.client.modules.api.setting.impl.ModeSetting;
import dev.lumens.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
        name = "SprintReset",
        category = Category.COMBAT,
        description = "Сброс спринта при ударе (w-tap и вариации) для полного нокбэка"
)
public final class SprintReset extends Module {
    public static final SprintReset INSTANCE = new SprintReset();

    private final ModeSetting mode = new ModeSetting("Режим", "W-Tap", "W-Tap", "S-Tap", "Shift-Tap", "No-Stop");
    private final NumberSetting chance = new NumberSetting("Chance", 100.0F, 1.0F, 100.0F, 1.0F);
    private final BooleanSetting groundOnly = new BooleanSetting("Ground Only", "Только стоя на земле", true);
    private final NumberSetting delay = new NumberSetting("Delay", 1.0F, 1.0F, 10.0F, 1.0F,
            () -> !mode.get().equals("W-Tap"));

    private int resetTicks = 0;
    private int startDelay = 0;
    private int cooldown = 0;

    private static final int COOLDOWN_TICKS = 10;

    private SprintReset() {}

    @EventTarget
    public void onTick(EventTick event) {
        cooldown = Math.max(0, cooldown - 1);

        if (startDelay > 0) {
            startDelay--;
        } else if (resetTicks > 0) {
            resetTicks--;
            executeReset();
        }
    }

    @EventTarget
    public void onAttack(EventAttack event) {
        if (cooldown > 0) return;
        if (groundOnly.isEnabled() && !mc.player.isOnGround()) return;
        if (Math.random() * 100 > chance.getCurrent()) return;

        startDelay = (int) (Math.random() * 3); // 0-2 ticks
        resetTicks = getResetDuration();
        cooldown = COOLDOWN_TICKS;
    }

    private int getResetDuration() {
        switch (mode.get()) {
            case "W-Tap": return 2;
            case "S-Tap": return 2;
            case "Shift-Tap": return 3;
            case "No-Stop": return 1;
            default: return 2;
        }
    }

    private void executeReset() {
        String selectedMode = mode.get();
        
        if (mc.player == null) return;

        switch (selectedMode) {
            case "W-Tap":
                mc.player.setSprinting(false);
                mc.options.forwardKey.setPressed(false);
                mc.options.forwardKey.setPressed(true);
                break;
            case "S-Tap":
                mc.player.setSprinting(false);
                mc.options.backKey.setPressed(true);
                break;
            case "Shift-Tap":
                mc.player.setSprinting(false);
                mc.options.sneakKey.setPressed(true);
                break;
            case "No-Stop":
                // Just stop sprinting briefly
                mc.player.setSprinting(false);
                break;
        }
    }
}