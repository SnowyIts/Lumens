package dev.lumens.client.cosmetics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import dev.lumens.utility.interfaces.IMinecraft;

/**
 * Сканирует assets/javelin/cosmetics/&lt;slot&gt;/&lt;id&gt;/:
 * в каждой папке первый *.json — модель, одноименный *.png — текстура.
 */
public final class CosmeticsIO implements IMinecraft {
    private CosmeticsIO() {
    }

    public static List<Cosmetic> loadSlot(Cosmetic.Slot slot) {
        List<Cosmetic> out = new ArrayList<>();
        try {
            if (mc == null || mc.getResourceManager() == null) return out;
            String base = "cosmetics/" + slot.getDir();
            Map<Identifier, Resource> jsons;
            Map<Identifier, Resource> pngs;
            try {
                jsons = mc.getResourceManager().findResources(base,
                        id -> id != null && id.getNamespace().equals("javelin") && id.getPath().endsWith(".json"));
                pngs = mc.getResourceManager().findResources(base,
                        id -> id != null && id.getNamespace().equals("javelin") && id.getPath().endsWith(".png"));
            } catch (Exception e) {
                return out;
            }
            if (jsons == null || pngs == null) return out;
            // png: ключ "папка/имя"
            Map<String, Identifier> pngByKey = new HashMap<>();
            Map<String, Resource> pngResByKey = new HashMap<>();
            for (Map.Entry<Identifier, Resource> en : pngs.entrySet()) {
                if (en == null || en.getKey() == null || en.getValue() == null) continue;
                String folder = folderOf(en.getKey().getPath());
                if (folder == null) continue;
                String name = fileName(en.getKey().getPath());
                if (name.endsWith(".png")) name = name.substring(0, name.length() - 4);
                String key = folder + "/" + name.toLowerCase();
                if (!pngByKey.containsKey(key)) {
                    pngByKey.put(key, en.getKey());
                    pngResByKey.put(key, en.getValue());
                }
            }
            List<Map.Entry<Identifier, Resource>> entries = new ArrayList<>(jsons.entrySet());
            entries.removeIf(en -> en == null || en.getKey() == null || en.getValue() == null);
            entries.sort((a, b) -> a.getKey().getPath().compareToIgnoreCase(b.getKey().getPath()));
            for (Map.Entry<Identifier, Resource> en : entries) {
                try {
                    Identifier jsonId = en.getKey();
                    Resource res = en.getValue();
                    if (jsonId == null || res == null) continue;
                    String folder = folderOf(jsonId.getPath());
                    if (folder == null) continue;
                    String jsonName = fileName(jsonId.getPath());
                    if (!jsonName.endsWith(".json") || jsonName.length() <= 5) continue;
                    String baseName = jsonName.substring(0, jsonName.length() - 5).toLowerCase();
                    String key = folder + "/" + baseName;
                    Identifier pngId = pngByKey.get(key);
                    if (pngId == null) {
                        // запасной вариант: первый png из той же папки
                        for (Map.Entry<String, Identifier> pe : pngByKey.entrySet()) {
                            if (pe.getKey() != null && pe.getKey().startsWith(folder + "/")) {
                                pngId = pe.getValue();
                                break;
                            }
                        }
                    }
                    if (pngId == null) continue;
                    JsonObject root;
                    try (BufferedReader reader = res.getReader()) {
                        if (reader == null) continue;
                        root = JsonParser.parseReader(reader).getAsJsonObject();
                    }
                    if (root == null) continue;
                    CosmeticModel model = CosmeticModel.parse(root);
                    if (model.isEmpty()) continue;
                    String id = folder.substring(folder.lastIndexOf('/') + 1);
                    out.add(new Cosmetic(slot, id, prettyName(id), model, pngId));
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static String folderOf(String path) {
        int i = path.lastIndexOf('/');
        if (i <= 0) return null;
        return path.substring(0, i);
    }

    private static String fileName(String path) {
        int i = path.lastIndexOf('/');
        return i < 0 ? path : path.substring(i + 1);
    }

    private static String prettyName(String id) {
        if (id == null || id.isEmpty()) return "?";
        String s = id.replace('_', ' ').replace('-', ' ').trim();
        if (s.isEmpty()) return "?";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
