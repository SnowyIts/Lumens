package dev.lumens.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import dev.lumens.utility.game.player.rotation.Rotation;
import org.joml.Vector2f;
import dev.lumens.base.events.impl.render.EventCamera;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.ModeSetting;

@ModuleAnnotation(
        name = "FreeLook",
        category = Category.RENDER,
        description = "Свободная камера: мышь крутит вид, игрок продолжает смотреть в сохранённом направлении"
)
public final class FreeLook extends Module {
    public static final FreeLook INSTANCE = new FreeLook();

    private final ModeSetting mode = new ModeSetting("Режим", "Back", "Back", "Front", "Nothing");
    private Vector2f savedRotation;

    private FreeLook() {}

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player != null) {
            savedRotation = new Vector2f(mc.player.getYaw(), mc.player.getPitch());
        }
    }

    @EventTarget
    public void onCamera(EventCamera event) {
        if (savedRotation == null) return;
        
        String selectedMode = mode.get();
        float yaw = savedRotation.x;
        float pitch = savedRotation.y;
        
        if (selectedMode.equals("Front")) {
            yaw += 180.0f;
        } else if (selectedMode.equals("Nothing")) {
            // Do nothing - keep current camera rotation
            return;
        }
        
        event.setAngle(new Rotation(yaw, pitch));
    }
}