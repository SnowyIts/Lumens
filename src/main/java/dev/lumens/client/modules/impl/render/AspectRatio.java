package dev.lumens.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import dev.lumens.base.events.impl.render.EventAspectRatio;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.ModeSetting;
import dev.lumens.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
        name = "AspectRatio",
        category = Category.RENDER,
        description = "Принудительное соотношение сторон рендера"
)
public final class AspectRatio extends Module {
    public static final AspectRatio INSTANCE = new AspectRatio();

    private final ModeSetting preset = new ModeSetting("Пресет", "16:9", "16:9", "16:10", "21:9", "4:3", "Custom");
    private final NumberSetting customRatio = new NumberSetting("Custom Ratio", 1.0F, 0.5F, 2.0F, 0.01F,
            () -> preset.get().equals("Custom"));

    private AspectRatio() {}

    @EventTarget
    public void onAspectRatio(EventAspectRatio event) {
        float ratio = 1.0f;
        String selected = preset.get();
        
        switch (selected) {
            case "16:9":
                ratio = 1.7777778f;
                break;
            case "16:10":
                ratio = 1.6f;
                break;
            case "21:9":
                ratio = 2.3888888f;
                break;
            case "4:3":
                ratio = 1.3333333f;
                break;
            case "Custom":
                ratio = customRatio.getCurrent();
                break;
        }
        
        event.setRatio(ratio);
    }
}