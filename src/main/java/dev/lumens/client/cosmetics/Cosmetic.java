package dev.lumens.client.cosmetics;

import net.minecraft.util.Identifier;

/** Один аксессуар: слот, id папки, имя, модель, текстура. */
public final class Cosmetic {
    /** Кость модели игрока, к которой крепится косметика. */
    public enum Anchor {
        /** ModelPart head: следует за поворотами головы. */
        HEAD,
        /** ModelPart body: следует за телом (sneak, плавание, полёт и т.д.). */
        BODY
    }

    public enum Slot {
        HEAD("head", "Head", "Аксессуары на голову", Anchor.HEAD),
        BACK("back", "Back", "Аксессуары на спину", Anchor.BODY);

        private final String dir;
        private final String title;
        private final String subtitle;
        private final Anchor anchor;

        Slot(String dir, String title, String subtitle, Anchor anchor) {
            this.dir = dir;
            this.title = title;
            this.subtitle = subtitle;
            this.anchor = anchor;
        }

        public String getDir() {
            return dir;
        }

        /** Кость модели, к которой привязаны все аксессуары этого слота. */
        public Anchor getAnchor() {
            return anchor;
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

    /** Кость модели, к которой привязан этот аксессуар (определяется слотом). */
    public Anchor getAnchor() {
        return slot.getAnchor();
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
