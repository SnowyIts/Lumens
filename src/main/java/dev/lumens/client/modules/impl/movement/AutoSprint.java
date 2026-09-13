package dev.lumens.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import dev.lumens.base.events.impl.other.EventTick;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.BooleanSetting;

@ModuleAnnotation(
    name = "AutoSprint",
    category = Category.MOVEMENT,
    description = "Автоматический спринт при ходьбе"
)
public final class AutoSprint extends Module {
    public static final AutoSprint INSTANCE = new AutoSprint();

    private final BooleanSetting enabled = new BooleanSetting("Включено", true);
    private final BooleanSetting ignoreHunger = new BooleanSetting("Игнорировать голод", true);
    private final BooleanSetting onlyOnGround = new BooleanSetting("Только на земле", false);

    private AutoSprint() {}

    @EventTarget
    public void onTick(EventTick event) {
        if (!enabled.isEnabled() || mc.player == null) return;
        
        if (mc.player.isUsingItem() || mc.player.isSneaking() || mc.player.isGliding()) return;
        if (onlyOnGround.isEnabled() && !mc.player.isOnGround()) return;
        if (!ignoreHunger.isEnabled() && mc.player.getHungerManager().getFoodLevel() <= 6) return;
        
        if (mc.player.isWalking() && mc.player.canSprint()) {
            mc.player.setSprinting(true);
        }
    }

    public boolean isEnabled() {
        return enabled.isEnabled() && super.isEnabled();
    }
}