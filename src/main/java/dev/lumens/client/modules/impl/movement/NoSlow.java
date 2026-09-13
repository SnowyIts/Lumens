package dev.lumens.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.util.Hand;
import ru.nexusguard.protection.annotations.Native;
import dev.lumens.base.events.impl.player.EventSlowWalking;
import dev.lumens.base.events.impl.player.EventUpdate;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.BooleanSetting;
import dev.lumens.client.modules.api.setting.impl.ModeSetting;

@ModuleAnnotation(
    name = "NoSlow",
    category = Category.MOVEMENT,
    description = "Убирает замедление при еде / использовании предмета / щите"
)
public final class NoSlow extends Module {
    public static final NoSlow INSTANCE = new NoSlow();
    
    private final ModeSetting mode = new ModeSetting("Режим", "Vanilla", "Vanilla", "SpookyTime-Duels");
    private final BooleanSetting sprint = new BooleanSetting("Спринт", true, 
            () -> mode.get().equals("SpookyTime-Duels"));
    private int ticks = 0;

    private NoSlow() {}

    @EventTarget
    @Native
    public void onItemUse(EventSlowWalking e) {
        if (mode.get().equals("Vanilla")) {
            if (mc.player.getItemUseTime() % 2 == 0) {
                e.setCancelled(true);
            }
        }

        if (mode.get().equals("SpookyTime-Duels")) {
            Hand hand = mc.player.getActiveHand();
            if (sprint.isEnabled()) {
                mc.player.setSprinting(mc.player.canSprint() && mc.player.isWalking() 
                        && !mc.player.isBlind() && !mc.player.isGliding() 
                        && (!mc.player.shouldSlowDown() || mc.player.isSubmergedInWater()));
            }
            // Use off-hand to cancel slow
            mc.interactionManager.interactItem(mc.player, hand.equals(Hand.MAIN_HAND) ? Hand.OFF_HAND : Hand.MAIN_HAND);
            e.setCancelled(true);
        }
    }

    @EventTarget
    public void update(EventUpdate tickEvent) {
        if (!mc.player.isUsingItem() || !mc.player.isOnGround()) {
            this.ticks = 0;
        }
    }
}