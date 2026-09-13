package dev.lumens.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import dev.lumens.base.events.impl.other.EventTick;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.BooleanSetting;
import dev.lumens.client.modules.api.setting.impl.ModeSetting;
import dev.lumens.utility.math.Timer;

@ModuleAnnotation(
        name = "AntiAFK",
        category = Category.PLAYER,
        description = "Имитация активности, чтобы сервер не кикнул за бездействие"
)
public final class AntiAFK extends Module {
    public static final AntiAFK INSTANCE = new AntiAFK();

    private final Timer timer = new Timer();
    private final ModeSetting mode = new ModeSetting("Режим", "Custom", "Custom", "FT");
    private final BooleanSetting cameraShake = new BooleanSetting("Camera Shake", "Подёргивание камеры", true);
    private final BooleanSetting click = new BooleanSetting("Click", "Клик", true);

    private boolean active = false;
    private static final int IDLE_DELAY = 30;

    private AntiAFK() {}

    @EventTarget
    public void onTick(EventTick event) {
        if (active) {
            executeAction();
        }
    }

    @EventTarget
    public void onRu(EventTick event) {
        active = false;

        if (mc.player == null || mc.world == null) return;

        // Check if player is in menu/pause
        if (mc.currentScreen != null) return;

        boolean playerBusy = mc.player.isUsingItem() || mc.player.isSleeping();
        if (playerBusy) {
            timer.reset();
            return;
        }

        if (timer.finished(IDLE_DELAY * 50)) { // Timer uses milliseconds
            active = true;
            timer.reset();
        }

        if (active) {
            executeAction();
        }
    }

    private void executeAction() {
        if (mode.get().equals("Custom")) {
            if (cameraShake.isEnabled()) {
                // Shake camera slightly
                float yaw = mc.player.getYaw() + (float) (Math.random() - 0.5) * 2;
                float pitch = mc.player.getPitch() + (float) (Math.random() - 0.5) * 2;
                mc.player.setYaw(yaw);
                mc.player.setPitch(pitch);
            }
            if (click.isEnabled()) {
                // Simulate click
                mc.options.attackKey.setPressed(true);
                mc.options.attackKey.setPressed(false);
            }
        } else if (mode.get().equals("FT")) {
            // FT mode - similar but with different pattern
            if (cameraShake.isEnabled()) {
                float yaw = mc.player.getYaw() + (float) (Math.random() - 0.5) * 1.5f;
                mc.player.setYaw(yaw);
            }
        }
    }
}