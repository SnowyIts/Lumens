package dev.lumens.base.license;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.MinecraftClient;
import dev.lumens.Lumens;

public final class LumensAuth {
    public static final String BASE_URL = "http://node.ravenhost.space:19238";

    private static final LumensAuth INSTANCE = new LumensAuth();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final AtomicInteger uid = new AtomicInteger(-1);
    private final AtomicReference<String> role = new AtomicReference<>("user");
    private final AtomicReference<String> gameToken = new AtomicReference<>(null);
    private final AtomicReference<String> linkCode = new AtomicReference<>(null);
    private volatile long linkCodeExpiresAt = 0L;
    private volatile boolean linked = false;
    private volatile boolean loading = true;

    private LumensAuth() {
    }

    public static LumensAuth get() {
        return INSTANCE;
    }

    public static String getHwid() {
        try {
            String raw = System.getProperty("os.name", "win")
                    + "|" + System.getProperty("os.arch", "x64")
                    + "|" + System.getProperty("user.name", "u")
                    + "|" + System.getenv().getOrDefault("COMPUTERNAME",
                            System.getenv().getOrDefault("HOSTNAME", "pc"));
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "unknown-hwid";
        }
    }

    private File licenseFile() {
        File dir = Lumens.DIRECTORY != null ? Lumens.DIRECTORY
                : new File(MinecraftClient.getInstance().runDirectory, "Javelin");
        return new File(dir, "lumens_license.json");
    }

    public void initAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                loadFromDisk();
                String token = gameToken.get();
                if (token != null && !token.isEmpty()) {
                    if (verifyToken(token)) {
                        loading = false;
                        return;
                    }
                }
                // Нет валидного токена — тихая проверка без кодов
                linked = false;
            } catch (Exception ignored) {
            } finally {
                loading = false;
            }
        });
    }

    private void loadFromDisk() {
        try {
            File f = licenseFile();
            if (!f.exists()) return;
            String json = Files.readString(f.toPath());
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            if (o.has("gameToken")) gameToken.set(o.get("gameToken").getAsString());
            if (o.has("uid")) uid.set(o.get("uid").getAsInt());
            if (o.has("role")) role.set(o.get("role").getAsString());
        } catch (Exception ignored) {
        }
    }

    private void saveToDisk() {
        try {
            File f = licenseFile();
            if (f.getParentFile() != null) f.getParentFile().mkdirs();
            JsonObject o = new JsonObject();
            o.addProperty("gameToken", gameToken.get() == null ? "" : gameToken.get());
            o.addProperty("uid", uid.get());
            o.addProperty("role", role.get());
            Files.writeString(f.toPath(), o.toString());
        } catch (Exception ignored) {
        }
    }

    private JsonObject post(String path, JsonObject body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(6))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        return JsonParser.parseString(res.body()).getAsJsonObject();
    }

    public boolean verifyToken(String token) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("token", token);
            body.addProperty("hwid", getHwid());
            JsonObject res = post("/api/link/verify", body);
            String status = res.has("status") ? res.get("status").getAsString() : "revoked";
            if ("ok".equals(status)) {
                uid.set(res.has("uid") ? res.get("uid").getAsInt() : -1);
                if (res.has("role")) role.set(res.get("role").getAsString());
                linked = true;
                saveToDisk();
                return true;
            }
        } catch (Exception ignored) {
        }
        linked = false;
        return false;
    }

    public synchronized void requestLinkCode() {
        // Не спамим сервер — код живёт 10 минут
        if (linkCode.get() != null && System.currentTimeMillis() < linkCodeExpiresAt) return;
        try {
            JsonObject body = new JsonObject();
            body.addProperty("hwid", getHwid());
            JsonObject res = post("/api/link/start", body);
            if (res.has("ok") && res.get("ok").getAsBoolean() && res.has("code")) {
                linkCode.set(res.get("code").getAsString());
                linkCodeExpiresAt = System.currentTimeMillis() + 10 * 60 * 1000L;
            }
        } catch (Exception ignored) {
        }
    }

    public boolean pollLink() {
        try {
            String code = linkCode.get();
            if (code == null) {
                requestLinkCode();
                return false;
            }
            JsonObject body = new JsonObject();
            body.addProperty("code", code);
            body.addProperty("hwid", getHwid());
            JsonObject res = post("/api/link/poll", body);
            String status = res.has("status") ? res.get("status").getAsString() : "pending";
            if ("ok".equals(status)) {
                if (res.has("uid")) uid.set(res.get("uid").getAsInt());
                if (res.has("token")) gameToken.set(res.get("token").getAsString());
                linked = true;
                saveToDisk();
                return true;
            }
            if ("expired".equals(status)) {
                linkCode.set(null);
                requestLinkCode();
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public boolean ping() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/ping"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonObject o = JsonParser.parseString(res.body()).getAsJsonObject();
            return o.has("ok") && o.get("ok").getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean tryVerifySaved() {
        try {
            String token = gameToken.get();
            if (token == null || token.isEmpty()) return false;
            return verifyToken(token);
        } catch (Exception e) {
            return false;
        }
    }

    public int getUid() {
        return uid.get();
    }

    public String getUidText() {
        int id = uid.get();
        return id > 0 ? String.valueOf(id) : "----";
    }

    public String getRole() {
        return role.get();
    }

    public boolean isLinked() {
        return linked && uid.get() > 0;
    }

    public boolean isLoading() {
        return loading;
    }

    public String getLinkCode() {
        if (linkCode.get() == null) requestLinkCode();
        return linkCode.get();
    }

    public String getPlayerName() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.getSession() != null) {
                String name = mc.getSession().getUsername();
                if (name != null && !name.isEmpty()) return name;
            }
        } catch (Exception ignored) {
        }
        return "Player";
    }
}
