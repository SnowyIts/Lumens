package dev.lumens.client.cosmetics;

import net.minecraft.util.Identifier;

/** Один аксессуар: слот, id папки, имя, модель, текстура. */
public final class Cosmetic {
    public enum Slot {
        HEAD("head", "Head", "Аксессуары на голову"),
        BACK("back", "Back", "Аксессуары на спину");

        private final String dir;
        private final String title;
        private final String subtitle;

        Slot(String dir, String title, String subtitle) {
            this.dir = dir;
            this.title = title;
            this.subtitle = subtitle;
        }

        public String getDir() {
            return dir;
        }

        public String getTitle() {
            return title;
        }

        public String getSubtitle() {
            return subtitle;
        }
    }

    private final Slot slot;
    private final String id;
    private final String name;
    private final CosmeticModel model;
    private final Identifier textureId;

    public Cosmetic(Slot slot, String id, String name, CosmeticModel model, Identifier textureId) {
        this.slot = slot;
        this.id = id;
        this.name = name;
        this.model = model;
        this.textureId = textureId;
    }

    public Slot getSlot() {
        return slot;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CosmeticModel getModel() {
        return model;
    }

    public Identifier getTextureId() {
        return textureId;
    }
}
