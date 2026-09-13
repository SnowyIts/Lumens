package dev.lumens.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import dev.lumens.base.events.impl.other.EventTick;
import dev.lumens.base.events.impl.player.EventSprintUpdate;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.BooleanSetting;

@ModuleAnnotation(
    name = "Sprint",
    category = Category.MOVEMENT,
    description = "Постоянный спринт"
)
public final class Sprint extends Module {
    public static final Sprint INSTANCE = new Sprint();

    private final BooleanSetting ignoreHunger = new BooleanSetting("Ignore Hunger", "Игнорировать голод", true);

    private Sprint() {}

    @EventTarget
    public void onSprintUpdate(EventSprintUpdate event) {
        event.setCancelled(ignoreHunger.isEnabled());
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player != null && ignoreHunger.isEnabled()) {
            mc.player.setSprinting(mc.player.isWalking() && mc.player.canSprint() 
                    && !mc.player.isUsingItem() && !mc.player.isBlind() 
                    && (!mc.player.hasVehicle() || mc.player.getVehicle().canSprintAsVehicle() && mc.player.getVehicle().isLogicalSideForUpdatingMovement()) 
                    && !mc.player.isGliding() 
                    && (!mc.player.shouldSlowDown() || mc.player.isSubmergedInWater()) 
                    && (!mc.player.isTouchingWater() || mc.player.isSubmergedInWater()) 
                    && !mc.player.horizontalCollision && !mc.player.collidedSoftly);
        }
    }
}