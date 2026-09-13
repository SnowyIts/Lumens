package dev.lumens.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.util.math.MathHelper;
import dev.lumens.base.events.impl.other.EventTick;
import dev.lumens.client.cosmetics.Cosmetic;
import dev.lumens.client.cosmetics.CosmeticsIO;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.impl.render.Menu;
import dev.lumens.client.screens.cosmetics.CosmeticsScreen;

@ModuleAnnotation(
        name = "Cosmetics",
        category = Category.PLAYER,
        description = "Косметика: аксессуары на голову/спину и питомцы"
)
public final class Cosmetics extends Module {
    public static final Cosmetics INSTANCE = new Cosmetics();
    public static final String PET_TAG = "javelin_pet";

    public enum Pet {
        CAT("cat", "Кот"),
        DOG("dog", "Собака");

        private final String id;
        private final String name;

        Pet(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public static Pet byId(String id) {
            for (Pet p : values()) {
                if (p.id.equals(id)) return p;
            }
            return null;
        }
    }

    private String selectedHead = "";
    private String selectedBack = "";
    private String selectedPet = "";

    private List<Cosmetic> headList = new ArrayList<>();
    private List<Cosmetic> backList = new ArrayList<>();
    private CosmeticsScreen screen;

    private Entity pet;
    private int petIdSeq = 1000;

    private Cosmetics() {
    }

    public static boolean isPet(Entity e) {
        return e != null && e.getCommandTags().contains(PET_TAG);
    }

    // ---------------- выбор (1 аксессуар на слот) ----------------

    public void selectHead(String id) {
        selectedHead = selectedHead.equals(id) ? "" : id;
    }

    public void selectBack(String id) {
        selectedBack = selectedBack.equals(id) ? "" : id;
    }

    public void selectPet(String id) {
        if (selectedPet.equals(id)) {
            selectedPet = "";
        } else {
            selectedPet = id;
        }
        removePet();
    }

    public String getSelectedHead() {
        return selectedHead;
    }

    public String getSelectedBack() {
        return selectedBack;
    }

    public String getSelectedPet() {
        return selectedPet;
    }

    public void reload() {
        headList = CosmeticsIO.loadSlot(Cosmetic.Slot.HEAD);
        backList = CosmeticsIO.loadSlot(Cosmetic.Slot.BACK);
        if (!selectedHead.isEmpty() && headCosmetic() == null) selectedHead = "";
        if (!selectedBack.isEmpty() && backCosmetic() == null) selectedBack = "";
        if (!selectedPet.isEmpty() && Pet.byId(selectedPet) == null) {
            selectedPet = "";
            removePet();
        }
    }

    public List<Cosmetic> getHeadList() {
        return headList;
    }

    public List<Cosmetic> getBackList() {
        return backList;
    }

    public List<Cosmetic> listOf(Cosmetic.Slot slot) {
        return slot == Cosmetic.Slot.HEAD ? headList : backList;
    }

    public Cosmetic headCosmetic() {
        return byId(headList, selectedHead);
    }

    public Cosmetic backCosmetic() {
        return byId(backList, selectedBack);
    }

    private static Cosmetic byId(List<Cosmetic> list, String id) {
        if (id == null || id.isEmpty()) return null;
        for (Cosmetic c : list) {
            if (c.getId().equals(id)) return c;
        }
        return null;
    }

    public CosmeticsScreen getScreen() {
        if (screen == null) screen = new CosmeticsScreen();
        return screen;
    }

    // ---------------- вкл/выкл ----------------

    public void toggle() {
        try {
            // модуль уже включен, экрана нет — просто открыть панель, не выключать
            if (isEnabled() && mc.currentScreen == null) {
                reload();
                getScreen().open();
                mc.setScreen(getScreen());
                return;
            }
        } catch (Exception ignored) {
        }
        super.toggle();
    }

    @Override
    public void onEnable() {
        if (mc.world == null || mc.player == null) {
            this.setEnabled(false);
            return;
        }
        try {
            if (Menu.INSTANCE.isEnabled()) Menu.INSTANCE.setToggled(false);
        } catch (Exception ignored) {
        }
        reload();
        getScreen().open();
        mc.setScreen(getScreen());
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        removePet();
        try {
            if (screen != null && mc.currentScreen == screen) mc.setScreen(null);
        } catch (Exception ignored) {
        }
    }

    @Override
    public JsonObject save() {
        JsonObject o = super.save();
        o.addProperty("selHead", selectedHead);
        o.addProperty("selBack", selectedBack);
        o.addProperty("selPet", selectedPet);
        return o;
    }

    @Override
    public void load(JsonObject o) {
        super.load(o);
        try {
            if (o.has("selHead")) selectedHead = o.get("selHead").getAsString();
            if (o.has("selBack")) selectedBack = o.get("selBack").getAsString();
            if (o.has("selPet")) selectedPet = o.get("selPet").getAsString();
        } catch (Exception ignored) {
        }
    }

    // ---------------- питомцы ----------------

    private void removePet() {
        if (pet != null) {
            try {
                if (mc.world != null) {
                    mc.world.removeEntity(pet.getId(), Entity.RemovalReason.DISCARDED);
                }
            } catch (Exception ignored) {
            }
            pet = null;
        }
    }

    private void spawnPet() {
        removePet();
        if (mc.world == null || mc.player == null || selectedPet.isEmpty()) return;
        try {
            Entity e;
            if (selectedPet.equals(Pet.DOG.getId())) {
                e = new WolfEntity(EntityType.WOLF, mc.world);
            } else {
                e = new CatEntity(EntityType.CAT, mc.world);
            }
            e.setId(-(++petIdSeq));
            e.addCommandTag(PET_TAG);
            e.setSilent(true);
            e.setPos(mc.player.getX() + 1.0D, mc.player.getY(), mc.player.getZ() + 1.0D);
            e.setYaw(mc.player.getYaw());
            e.setHeadYaw(mc.player.getHeadYaw());
            e.setBodyYaw(mc.player.getYaw());
            mc.world.addEntity(e);
            pet = e;
        } catch (Exception ignored) {
            pet = null;
        }
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) {
            removePet();
            return;
        }
        if (selectedPet.isEmpty()) {
            removePet();
            return;
        }
        if (pet == null || pet.isRemoved() || pet.getWorld() != mc.world) {
            spawnPet();
        }
        if (pet == null) return;
        // следует за игроком: точка позади-справа
        double yawR = Math.toRadians(mc.player.getYaw());
        double s = Math.sin(yawR), c = Math.cos(yawR);
        double tx = mc.player.getX() + s * 1.4D - c * 0.7D;
        double tz = mc.player.getZ() - c * 1.4D - s * 0.7D;
        double ty = mc.player.getY();
        double dx = tx - pet.getX(), dz = tz - pet.getZ();
        double hd = Math.sqrt(dx * dx + dz * dz);
        if (hd > 0.4D) {
            double step = Math.min(hd, 0.5D);
            double nx = pet.getX() + dx / hd * step;
            double nz = pet.getZ() + dz / hd * step;
            double ny = pet.getY() + MathHelper.clamp((float) (ty - pet.getY()), -0.4F, 0.4F) * 0.5F
                    + Math.sin(System.currentTimeMillis() / 150.0D) * 0.01D;
            pet.setPos(nx, ny, nz);
            float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx))) - 90.0F;
            pet.setYaw(yaw);
            pet.setHeadYaw(yaw);
            pet.setBodyYaw(yaw);
        } else {
            double lx = mc.player.getX() - pet.getX(), lz = mc.player.getZ() - pet.getZ();
            if (lx * lx + lz * lz > 0.01D) {
                float yaw = (float) (Math.toDegrees(Math.atan2(lz, lx))) - 90.0F;
                pet.setYaw(yaw);
                pet.setHeadYaw(yaw);
            }
        }
    }

    // ---------------- аксессуары в мире ----------------
    // Рендерятся через CosmeticFeatureRenderer прямо на костях модели
    // (HEAD — ModelPart head, BACK — ModelPart body), отдельный хук тут не нужен.
}
