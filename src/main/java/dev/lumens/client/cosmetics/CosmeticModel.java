package dev.lumens.client.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Распарсенная Blockbench-модель (подмножество block-model формата). */
public final class CosmeticModel {
    public static final class Face {
        public final float u1, v1, u2, v2;

        public Face(float u1, float v1, float u2, float v2) {
            this.u1 = u1;
            this.v1 = v1;
            this.u2 = u2;
            this.v2 = v2;
        }
    }

    public static final class Element {
        public final float[] from = new float[3];
        public final float[] to = new float[3];
        public final float rotAngle;
        public final String rotAxis;
        public final float[] rotOrigin = new float[]{8.0F, 8.0F, 8.0F};
        public final Map<String, Face> faces = new LinkedHashMap<>();

        public Element(float[] from, float[] to, float rotAngle, String rotAxis, float[] rotOrigin) {
            System.arraycopy(from, 0, this.from, 0, 3);
            System.arraycopy(to, 0, this.to, 0, 3);
            this.rotAngle = rotAngle;
            this.rotAxis = rotAxis == null ? "y" : rotAxis;
            if (rotOrigin != null) System.arraycopy(rotOrigin, 0, this.rotOrigin, 0, 3);
        }
    }

    private final int textureWidth;
    private final int textureHeight;
    private final List<Element> elements;
    /** Сдвиг якоря в блоках (из "anchor_offset" в пикселях модели): +Y — вверх. */
    private final float[] anchorOffset = new float[3];
    /** Масштаб модели относительно якоря (из "scale", по умолчанию 1). */
    private float visualScale = 1.0F;

    public float getVisualScale() {
        return visualScale;
    }

    private CosmeticModel(int textureWidth, int textureHeight, List<Element> elements) {
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.elements = elements;
    }

    public float getAnchorOffsetX() {
        return anchorOffset[0];
    }

    public float getAnchorOffsetY() {
        return anchorOffset[1];
    }

    public float getAnchorOffsetZ() {
        return anchorOffset[2];
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public int getTextureHeight() {
        return textureHeight;
    }

    public List<Element> getElements() {
        return elements;
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public static CosmeticModel parse(JsonObject root) {
        int texW = 64, texH = 64;
        List<Element> elements = new ArrayList<>();
        try {
            if (root.has("texture_size") && root.get("texture_size").isJsonArray()
                    && root.getAsJsonArray("texture_size").size() >= 2) {
                texW = Math.max(1, root.getAsJsonArray("texture_size").get(0).getAsInt());
                texH = Math.max(1, root.getAsJsonArray("texture_size").get(1).getAsInt());
            }
            if (root.has("elements") && root.get("elements").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("elements")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    float[] from = new float[3];
                    float[] to = new float[3];
                    readVec(o, "from", from);
                    readVec(o, "to", to);
                    float angle = 0.0F;
                    String axis = "y";
                    float[] origin = new float[]{8.0F, 8.0F, 8.0F};
                    if (o.has("rotation") && o.get("rotation").isJsonObject()) {
                        JsonObject r = o.getAsJsonObject("rotation");
                        if (r.has("angle")) angle = r.get("angle").getAsFloat();
                        if (r.has("axis")) axis = r.get("axis").getAsString();
                        if (r.has("origin") && r.get("origin").isJsonArray()) {
                            readArr(r.getAsJsonArray("origin"), origin);
                        }
                    }
                    Element e = new Element(from, to, angle, axis, origin);
                    if (o.has("faces") && o.get("faces").isJsonObject()) {
                        JsonObject faces = o.getAsJsonObject("faces");
                        for (Map.Entry<String, JsonElement> fe : faces.entrySet()) {
                            if (!fe.getValue().isJsonObject()) continue;
                            JsonObject fo = fe.getValue().getAsJsonObject();
                            if (!fo.has("uv") || !fo.get("uv").isJsonArray()
                                    || fo.getAsJsonArray("uv").size() < 4) continue;
                            float[] uv = new float[4];
                            readArr(fo.getAsJsonArray("uv"), uv);
                            e.faces.put(fe.getKey().toLowerCase(), new Face(uv[0], uv[1], uv[2], uv[3]));
                        }
                    }
                    if (!e.faces.isEmpty()) elements.add(e);
                }
            }
        } catch (Exception ignored) {
        }
        CosmeticModel model = new CosmeticModel(texW, texH, elements);
        // Автоцентровка по X/Z на 8 (модели, построенные от 0). Отключается
        // полем "center_xz": false — если автор заложил крепление в координаты
        // (например крылья: плоскость крепления z=7 должна остаться на месте).
        boolean center = true;
        try {
            if (root.has("center_xz")) center = root.get("center_xz").getAsBoolean();
        } catch (Exception ignored) {
        }
        if (center) model.centerXZ();
        // Необязательный сдвиг якоря: "anchor_offset": [x, y, z] в пикселях модели.
        // Например шлем, обхватывающий голову: [0, -8, 0] (опустить на высоту головы).
        try {
            if (root.has("anchor_offset") && root.get("anchor_offset").isJsonArray()) {
                JsonArray arr = root.getAsJsonArray("anchor_offset");
                for (int i = 0; i < 3 && i < arr.size(); i++) {
                    model.anchorOffset[i] = arr.get(i).getAsFloat() / 16.0F;
                }
            }
        } catch (Exception ignored) {
        }
        // Необязательный масштаб: "scale": 1.1 (растянет модель от якоря).
        try {
            if (root.has("scale")) {
                float s = root.get("scale").getAsFloat();
                if (s > 0.01F && s < 10.0F) model.visualScale = s;
            }
        } catch (Exception ignored) {
        }
        return model;
    }

    /**
     * Центрирует модель по X/Z на 8 (центр головы).
     * Модели из Blockbench часто строят от 0, а рендер считает центром 8 —
     * без этого аксессуар сидит со сдвигом вбок. Y не трогаем (y=0 — основание).
     * Сдвигаем и точки вращения, чтобы изгибы (например кончик шапки) не сломались.
     */
    private void centerXZ() {
        if (elements.isEmpty()) return;
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (Element e : elements) {
            minX = Math.min(minX, Math.min(e.from[0], e.to[0]));
            maxX = Math.max(maxX, Math.max(e.from[0], e.to[0]));
            minZ = Math.min(minZ, Math.min(e.from[2], e.to[2]));
            maxZ = Math.max(maxZ, Math.max(e.from[2], e.to[2]));
        }
        float dx = 8.0F - (minX + maxX) * 0.5F;
        float dz = 8.0F - (minZ + maxZ) * 0.5F;
        if (Math.abs(dx) < 0.001F && Math.abs(dz) < 0.001F) return;
        for (Element e : elements) {
            e.from[0] += dx;
            e.to[0] += dx;
            e.from[2] += dz;
            e.to[2] += dz;
            e.rotOrigin[0] += dx;
            e.rotOrigin[2] += dz;
        }
    }

    private static void readVec(JsonObject o, String key, float[] out) {
        try {
            if (o.has(key) && o.get(key).isJsonArray()) {
                readArr(o.getAsJsonArray(key), out);
            }
        } catch (Exception ignored) {
        }
    }

    private static void readArr(JsonArray arr, float[] out) {
        for (int i = 0; i < out.length && i < arr.size(); i++) {
            try {
                out[i] = arr.get(i).getAsFloat();
            } catch (Exception ignored) {
            }
        }
    }
}
