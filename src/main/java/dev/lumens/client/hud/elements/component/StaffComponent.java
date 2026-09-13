package dev.lumens.client.hud.elements.component;

import com.mojang.authlib.GameProfile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import ru.nexusguard.protection.annotations.Native;
import dev.lumens.Lumens;
import dev.lumens.base.animations.base.Animation;
import dev.lumens.base.animations.base.Easing;
import dev.lumens.base.font.Fonts;
import dev.lumens.base.theme.Theme;
import dev.lumens.client.hud.elements.draggable.DraggableHudElement;
import dev.lumens.utility.render.display.base.BorderRadius;
import dev.lumens.utility.render.display.base.CustomDrawContext;
import dev.lumens.utility.render.display.base.Gradient;
import dev.lumens.utility.render.display.base.color.ColorRGBA;
import dev.lumens.utility.render.display.shader.DrawUtil;

public class StaffComponent extends DraggableHudElement {
    private final Map<String, StaffComponent.StaffModule> modules = new LinkedHashMap();
    private final Set<String> staffPrefix = Set.of(new String[]{"helper", "ᴀдмин", "moder", "staff", "admin", "curator", "стажёр", "сотрудник", "помощник", "админ", "модер", "ꔗ", "ꔥ", "ꔡ", "ꔳ"});
    private final Map<String, Identifier> skinTextureCache = new HashMap();
    private long lastStaffUpdate = 0L;
    private long lastSkinCacheClear = 0L;
    private final Set<String> currentStaffKeys = new HashSet();
    private final Animation widthAnimation;
    private final Animation alpha;

    public StaffComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
        this.widthAnimation = new Animation(200L, Easing.CUBIC_OUT);
        this.alpha = new Animation(200L, Easing.CUBIC_OUT);
    }

    @Native
    public void render(CustomDrawContext ctx) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastStaffUpdate > 50L && mc.getNetworkHandler() != null) {
            this.updateStaffList();
            this.lastStaffUpdate = currentTime;
        }

        if (currentTime - this.lastSkinCacheClear > 30000L) {
            this.skinTextureCache.clear();
            this.lastSkinCacheClear = currentTime;
        }

        this.modules.entrySet().removeIf((entry) -> {
            return ((StaffComponent.StaffModule)entry.getValue()).isDelete();
        });
        float posX = this.getX();
        float posY = this.getY();
        float defaultWidth = 51.0F;
        float height = 22.0F;
        boolean isFound = false;
        Iterator var9 = this.modules.entrySet().iterator();

        while(var9.hasNext()) {
            Entry<String, StaffComponent.StaffModule> module = (Entry)var9.next();
            ((StaffComponent.StaffModule)module.getValue()).animation.update(this.currentStaffKeys.contains(module.getKey()));
            if (((StaffComponent.StaffModule)module.getValue()).animation.getValue() != 0.0F) {
                this.alpha.update(1.0F);
                isFound = true;
            }
        }

        if (!isFound && !(mc.currentScreen instanceof ChatScreen)) {
            this.alpha.update(0.0F);
        }

        if (mc.currentScreen instanceof ChatScreen) {
            this.alpha.update(1.0F);
        }

        Theme theme = Lumens.getInstance().getThemeManager().getCurrentTheme();
        ColorRGBA accent = theme.getColor().withAlpha(255);
        ColorRGBA second = theme.getSecondColor().withAlpha(255);
        
        float alphaVal = this.alpha.getValue();
        if (alphaVal <= 0.01F) {
            this.width = 0;
            this.height = 0;
            return;
        }

        // Header background
        float stIconW = Fonts.ICONS2.getWidth("\uf728", 7.0F);
        float headerWidth = 12 + Fonts.SEMIBOLD.getWidth("StaffList", 7.0F) + 9 + 2 + 6 + stIconW + 10;
        DrawUtil.drawHudBg(ctx.getMatrices(), posX, posY, headerWidth, 20, 6.0F, BorderRadius.all(8.0F), new ColorRGBA(17, 17, 22, 255).mix(accent, 0.12F).withAlpha(alphaVal * 255));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), posX, posY, headerWidth, 20, 0.8F, BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255, 20 * alphaVal));
        
        float textX = posX + 12;
        // StaffList text with gradient
        float wave = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 550.0);
        ColorRGBA gL = ColorRGBA.lerp(accent, second, wave * 0.6F);
        ColorRGBA gR = ColorRGBA.lerp(second, accent, wave * 0.6F);
        ctx.drawText(Fonts.SEMIBOLD.getFont(7.0F), "StaffList", textX, posY + 6, Gradient.of(gL, gL, gR, gR).mulAlpha(alphaVal));
        textX += Fonts.SEMIBOLD.getWidth("StaffList", 7.0F) + 9;
        
        // Divider
        DrawUtil.drawRoundedRect(ctx.getMatrices(), textX, posY + 8, 2, 2, BorderRadius.all(1.0F), accent.withAlpha(120 * alphaVal));
        textX += 2 + 6;
        
        // Icon
        ctx.drawText(Fonts.ICONS2.getFont(7.0F), "\uf728", textX, posY + 7, accent.withAlpha(alphaVal * 255));
        
        posY += 22;
        
        Iterator var16 = this.modules.entrySet().iterator();

        while(var16.hasNext()) {
            Entry<String, StaffComponent.StaffModule> module = (Entry)var16.next();
            StaffComponent.StaffModule sm = (StaffComponent.StaffModule)module.getValue();
            if (sm.animation.getValue() != 0.0F) {
                height += 18.0F;
                Identifier skinTexture = (Identifier)this.skinTextureCache.get(sm.name);
                if (skinTexture == null && mc.getNetworkHandler() != null) {
                    PlayerListEntry player = (PlayerListEntry)mc.getNetworkHandler().getPlayerList().stream().filter((p) -> {
                        return p.getProfile() != null && sm.name.equals(p.getProfile().getName());
                    }).findFirst().orElse(null);
                    if (player != null && player.getSkinTextures() != null) {
                        skinTexture = player.getSkinTextures().texture();
                        this.skinTextureCache.put(sm.name, skinTexture);
                    }
                }

                if (skinTexture == null) {
                    skinTexture = DefaultSkinHelper.getSteve().texture();
                }

                Text prefix = sm.displayNameText;
                float elementsWidth = Fonts.REGULAR.getWidth(prefix.getString(), 6.75F) + 36.0F;
                float itemAnim = sm.animation.getValue() * alphaVal;
                float rowY = posY + (1.0F - itemAnim) * 6.0F;
                
                // Row background
                if (itemAnim > 0.01F) {
                    DrawUtil.drawHudBg(ctx.getMatrices(), posX, rowY, this.widthAnimation.getValue(), 16, 6.0F, BorderRadius.all(6.0F), new ColorRGBA(26, 26, 32, 255).mix(accent, 0.08F).withAlpha(itemAnim * 255));
                    DrawUtil.drawRoundedBorder(ctx.getMatrices(), posX, rowY, this.widthAnimation.getValue(), 16, 0.6F, BorderRadius.all(6.0F), new ColorRGBA(255, 255, 255, 12 * itemAnim));
                }
                
                float rowX = posX + 10;
                
                // Player head
                DrawUtil.drawPlayerHeadWithRoundedShader(ctx.getMatrices(), skinTexture, rowX, rowY + 3, 10.0F, BorderRadius.all(2.0F), ColorRGBA.WHITE.withAlpha(itemAnim * 255.0F));
                rowX += 12;
                
                // Name with prefix
                ctx.drawText(Fonts.REGULAR.getFont(6.5F), prefix.getString(), rowX, rowY + 4.5F, new ColorRGBA(243, 243, 246, 255).withAlpha(itemAnim * 255));
                
                // Status indicator
                float statusX = posX + this.widthAnimation.getValue() - 10;
                float statusY = rowY + 6;
                ColorRGBA statusColor = sm.status == StaffComponent.Status.NONE ? new ColorRGBA(90, 255, 140, 255) : new ColorRGBA(255, 90, 90, 255);
                DrawUtil.drawRoundedRect(ctx.getMatrices(), statusX, statusY, 4.0F, 4.0F, BorderRadius.all(2.0F), statusColor.withAlpha(itemAnim * 255.0F));
                
                if (elementsWidth > defaultWidth) {
                    defaultWidth = elementsWidth;
                }

                posY += 18.0F;
            }
        }

        this.widthAnimation.update(defaultWidth);
        this.width = this.widthAnimation.getValue();
        this.height = height;
    }

    private void updateStaffList() {
        if (mc.getNetworkHandler() != null) {
            this.currentStaffKeys.clear();
            Iterator var1 = mc.getNetworkHandler().getPlayerList().iterator();

            while(true) {
                PlayerListEntry entry;
                Text displayName;
                String display;
                String name;
                String prefix;
                do {
                    do {
                        do {
                            GameProfile profile;
                            do {
                                do {
                                    if (!var1.hasNext()) {
                                        return;
                                    }

                                    entry = (PlayerListEntry)var1.next();
                                    profile = entry.getProfile();
                                    displayName = entry.getDisplayName();
                                } while(displayName == null);
                            } while(profile == null);

                            display = displayName.getString();
                            name = profile.getName();
                            prefix = display.replace(name, "").trim();
                            String var10000 = prefix.replaceAll("ꔗ", String.valueOf(Formatting.BLUE) + "MODER").replaceAll("ꔥ", String.valueOf(Formatting.BLUE) + "ST.MODER").replaceAll("ꔡ", String.valueOf(Formatting.LIGHT_PURPLE) + "MODER+").replaceAll("ꔀ", String.valueOf(Formatting.GRAY) + "PLAYER").replaceAll("ꔉ", String.valueOf(Formatting.YELLOW) + "HELPER").replaceAll("◆", "@").replaceAll("┃", "|").replaceAll("ꔳ", String.valueOf(Formatting.AQUA) + "ML.ADMIN");
                            String var10002 = String.valueOf(Formatting.RED);
                            prefix = var10000.replaceAll("ꔅ", var10002 + "Y" + String.valueOf(Formatting.WHITE) + "T").replaceAll("ꔂ", String.valueOf(Formatting.BLUE) + "D.MODER").replaceAll("ꕠ", String.valueOf(Formatting.YELLOW) + "D.HELPER").replaceAll("ꕄ", String.valueOf(Formatting.RED) + "DRACULA").replaceAll("ꔖ", String.valueOf(Formatting.AQUA) + "OVERLORD").replaceAll("ꕈ", String.valueOf(Formatting.GREEN) + "COBRA").replaceAll("ꔨ", String.valueOf(Formatting.LIGHT_PURPLE) + "DRAGON").replaceAll("ꔤ", String.valueOf(Formatting.RED) + "IMPERATOR").replaceAll("ꔠ", String.valueOf(Formatting.GOLD) + "MAGISTER").replaceAll("ꔄ", String.valueOf(Formatting.BLUE) + "HERO").replaceAll("ꔒ", String.valueOf(Formatting.GREEN) + "AVENGER").replaceAll("ꕒ", String.valueOf(Formatting.WHITE) + "RABBIT").replaceAll("ꔈ", String.valueOf(Formatting.YELLOW) + "TITAN").replaceAll("ꕀ", String.valueOf(Formatting.DARK_GREEN) + "HYDRA").replaceAll("ꔶ", String.valueOf(Formatting.GOLD) + "TIGER").replaceAll("ꔲ", String.valueOf(Formatting.DARK_PURPLE) + "BULL").replaceAll("ꕖ", String.valueOf(Formatting.BLACK) + "BUNNY").replaceAll("ꕗꕘ", String.valueOf(Formatting.YELLOW) + "SPONSOR").replaceAll("\ud83d\udd25", "@").replaceAll("ᴀ", "A").replaceAll("ʙ", "B").replaceAll("ᴄ", "C").replaceAll("ᴅ", "D").replaceAll("ᴇ", "E").replaceAll("ғ", "F").replaceAll("ɢ", "G").replaceAll("ʜ", "H").replaceAll("ɪ", "I").replaceAll("ᴊ", "J").replaceAll("ᴋ", "K").replaceAll("ʟ", "L").replaceAll("ᴍ", "M").replaceAll("ɴ", "N").replaceAll("ꜱ", "S").replaceAll("ᴏ", "O").replaceAll("ᴘ", "P").replaceAll("ǫ", "Q").replaceAll("ʀ", "R").replaceAll("ᴛ", "T").replaceAll("ᴜ", "U").replaceAll("ᴠ", "V").replaceAll("ᴡ", "W").replaceAll("ꜰ", "F").replaceAll("ʏ", "Y").replaceAll("ᴢ", "Z");
                        } while(prefix.length() < 2);
                    } while(!this.containsAnyKeyword(prefix));
                } while(Lumens.getInstance().getServerHandler().getServer().equals("LonyGrief") && (prefix.contains("D.ADMIN") || prefix.contains("sTAFF")));

                StaffComponent.Status status = entry.getGameMode() == GameMode.SPECTATOR ? StaffComponent.Status.VANISHED : StaffComponent.Status.NONE;
                final Text finalDisplayName = displayName;
                final String finalDisplay = display;
                final String finalName = name;
                final StaffComponent.Status finalStatus = status;
                this.modules.computeIfAbsent(display, (k) -> {
                    return new StaffComponent.StaffModule(this, finalDisplayName, finalDisplay, finalName, finalStatus);
                });
                this.currentStaffKeys.add(display);
            }
        }
    }

    public boolean containsAnyKeyword(String text) {
        String lower = text.toLowerCase(Locale.US);
        Iterator var3 = this.staffPrefix.iterator();

        String keyword;
        do {
            if (!var3.hasNext()) {
                return false;
            }

            keyword = (String)var3.next();
        } while(!lower.contains(keyword));

        return true;
    }

    private class StaffModule {
        private final Animation animation;
        private final Animation animationColor;
        private final Text displayNameText;
        private final String key;
        private final String name;
        private final StaffComponent.Status status;
        private final long appearTime;

        public StaffModule(final StaffComponent param1, Text displayNameText, String key, String name, StaffComponent.Status status) {
            this.animation = new Animation(250L, 0.01F, Easing.CUBIC_OUT);
            this.animationColor = new Animation(200L, Easing.QUAD_IN_OUT);
            this.displayNameText = displayNameText;
            this.key = key;
            this.name = name;
            this.status = status;
            this.appearTime = System.currentTimeMillis();
        }

        public boolean isDelete() {
            return this.animation.getValue() == 0.0F;
        }
    }

    public static enum Status {
        NONE,
        VANISHED;

    
        private static StaffComponent.Status[] $values() {
            return new StaffComponent.Status[]{NONE, VANISHED};
        }
    }
}