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

    private CosmeticModel(int textureWidth, int textureHeight, List<Element> elements) {
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.elements = elements;
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
        return new CosmeticModel(texW, texH, elements);
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
