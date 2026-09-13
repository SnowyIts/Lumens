package dev.lumens.client.screens.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.lumens.client.modules.impl.player.Cosmetics;
import dev.lumens.client.modules.impl.render.Menu;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import ru.nexusguard.protection.annotations.Native;
import dev.lumens.Lumens;
import dev.lumens.base.animations.base.Animation;
import dev.lumens.base.animations.base.Easing;
import dev.lumens.base.font.Font;
import dev.lumens.base.font.Fonts;
import dev.lumens.base.theme.Theme;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.setting.Setting;
import dev.lumens.client.modules.api.setting.impl.BooleanSetting;
import dev.lumens.client.modules.api.setting.impl.KeySetting;
import dev.lumens.client.modules.api.setting.impl.ModeSetting;
import dev.lumens.client.modules.api.setting.impl.MultiBooleanSetting;
import dev.lumens.client.modules.api.setting.impl.NumberSetting;
import dev.lumens.utility.interfaces.IClient;
import dev.lumens.utility.math.MathUtil;
import dev.lumens.utility.render.display.Keyboard;
import dev.lumens.utility.render.display.base.BorderRadius;
import dev.lumens.utility.render.display.base.CustomDrawContext;
import dev.lumens.utility.render.display.base.Gradient;
import dev.lumens.utility.render.display.base.color.ColorRGBA;
import dev.lumens.utility.render.display.shader.DrawUtil;

/**
 * ClickGUI в стиле Wedal со скриншота.
 * Слева сайдбар (Lumens / Version: 1.0.2, вкладки с иконками, поиск),
 * справа контент с карточками модулей. Настройки модуля открываются
 * ТОЛЬКО по ПКМ в отдельной панели справа от окна.
 */
public class MenuScreen extends Screen implements IClient {
    private static final Category[] TABS = {
            Category.COMBAT, Category.MOVEMENT, Category.RENDER,
            Category.PLAYER, Category.MISC, Category.CONFIG, Category.THEMES
    };

    private static final float SIDE_W = 150.0F;
    private static final float PANEL_W = 220.0F;
    private static final float PANEL_GAP = 8.0F;
    private static final float TAB_H = 29.0F;
    private static final float TAB_GAP = 3.0F;
    private static final float CONFIG_GAP = 12.0F;
    private static final float CARD_H = 33.0F;
    private static final float CARD_GAP = 7.0F;

    private ColorRGBA BG = new ColorRGBA(17, 17, 22, 255);
    private ColorRGBA CARD_BG = new ColorRGBA(24, 24, 29, 255);
    private ColorRGBA FIELD_BG = new ColorRGBA(30, 30, 36, 255);
    private ColorRGBA PILL_BG = new ColorRGBA(42, 42, 48, 255);
    private ColorRGBA TAB_ACTIVE_BG = new ColorRGBA(62, 56, 82, 255);
    private ColorRGBA ACCENT2 = new ColorRGBA(140, 120, 255, 255);
    private static final ColorRGBA TEXT_MAIN = new ColorRGBA(243, 243, 246, 255);
    private static final ColorRGBA TEXT_DIM = new ColorRGBA(152, 152, 160, 255);
    private static final ColorRGBA TEXT_FAINT = new ColorRGBA(112, 112, 120, 255);
    private static final ColorRGBA TEXT_OFF = new ColorRGBA(172, 170, 180, 255);

    // совместимость со старым кодом
    public boolean needToClose;
    public boolean search;
    public Runnable savedRunnable;
    public final Animation openAnimation = new Animation(200L, Easing.CUBIC_OUT);
    public final Animation openAnimationMetanoise = new Animation(200L, Easing.CUBIC_OUT);
    protected Category currentCategory = Category.COMBAT;

    private final Map<Category, Float> scrolls = new HashMap<>();
    private final Map<Category, Float> scrollTargets = new HashMap<>();
    private float panelScroll, panelTarget;
    private float extraScroll, extraTarget;

    private Module selectedModule;
    private Module bindModule;
    private KeySetting bindKey;
    private NumberSetting dragSetting;
    private float lastMouseX;

    private String searchText = "";
    private boolean searchFocused;
    private String configName = "";
    private boolean configFocused;

    private float winX, winY, winW, winH;
    private float panelX, panelY, panelH;
    private float contentX, contentY, contentW, contentH;

    private final List<Hit> hits = new ArrayList<>();
    private final Map<NumberSetting, float[]> sliderBounds = new HashMap<>();

    private static final class Hit {
        final float x, y, w, h;
        final int button; // -1 = любая
        final Runnable action;
        Hit(float x, float y, float w, float h, int button, Runnable action) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.button = button; this.action = action;
        }
        boolean matches(double mx, double my, int b) {
            return (button == -1 || button == b) && MathUtil.isHovered(mx, my, x, y, w, h);
        }
    }

    public MenuScreen() {
        super(Text.of("Lumens"));
        for (Category c : Category.values()) {
            scrolls.put(c, 0.0F);
            scrollTargets.put(c, 0.0F);
        }
    }

    // ---------------- helpers ----------------

    private List<Module> modulesOf(Category c) {
        List<Module> out = new ArrayList<>();
        try {
            for (Module m : Lumens.getInstance().getModuleManager().getModules()) {
                if (m.getCategory() == c) out.add(m);
            }
        } catch (Exception ignored) {
        }
        String q = searchText.trim().toLowerCase(Locale.ROOT);
        if (!q.isEmpty()) out.removeIf(m -> !m.getName().toLowerCase(Locale.ROOT).contains(q));
        out.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return out;
    }

    private static boolean isModuleTab(Category c) {
        return c == Category.COMBAT || c == Category.MOVEMENT || c == Category.RENDER
                || c == Category.PLAYER || c == Category.MISC;
    }

    private static String subtitleOf(Category c) {
        if (c == Category.COMBAT) return "Категория с модулями для PvP";
        if (c == Category.MOVEMENT) return "Категория с модулями для движения";
        if (c == Category.RENDER) return "Категория с визуальными модулями";
        if (c == Category.PLAYER) return "Категория с модулями для игрока";
        if (c == Category.MISC) return "Категория с прочими модулями";
        if (c == Category.CONFIG) return "Управление конфигами";
        return "Темы оформления клиента";
    }

    private static String fmtNumber(float v) {
        String s = String.format(Locale.US, "%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", ".0");
        if (s.equals("-0.0")) s = "0.0";
        return s;
    }

    private void layout(int sw, int sh) {
        winW = Math.min(590.0F, sw - 140.0F);
        winH = Math.min(360.0F, sh - 120.0F);
        winW = Math.max(440.0F, winW);
        winH = Math.max(280.0F, winH);
        boolean panel = selectedModule != null && isModuleTab(currentCategory);
        float totalW = winW + (panel ? PANEL_W + PANEL_GAP : 0.0F);
        if (totalW > sw - 8) {
            winW = Math.max(400.0F, sw - 8.0F - (panel ? PANEL_W + PANEL_GAP : 0.0F));
            totalW = winW + (panel ? PANEL_W + PANEL_GAP : 0.0F);
        }
        winX = Math.max(4.0F, sw / 2.0F - totalW / 2.0F);
        winY = Math.max(4.0F, sh / 2.0F - winH / 2.0F);
        panelX = winX + winW + PANEL_GAP;
        panelY = winY;
        panelH = winH;
        contentX = winX + SIDE_W;
        contentY = winY;
        contentW = winW - SIDE_W;
        contentH = winH;
    }

    private float tabsStartY() {
        return winY + 64.0F;
    }

    // ---------------- render ----------------

    @Override
    @Native
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (needToClose) return;
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        layout(sw, sh);
        lastMouseX = mouseX;
        openAnimation.update(true);
        openAnimationMetanoise.update(true);
        float openRaw = openAnimation.getValue();
        if (openRaw <= 0.01F) return;
        // текст всегда полностью непрозрачный, анимация влияет только на затемнение фона
        float open = 1.0F;

        Theme theme = Lumens.getInstance().getThemeManager().getCurrentTheme();
        try {
            theme.getAnimation().update(1.0F);
        } catch (Exception ignored) {
        }
        ColorRGBA accent = theme.getColor().withAlpha(255);
        ColorRGBA second = theme.getSecondColor().withAlpha(255);
        // сочная тонировка фонов под тему
        BG = new ColorRGBA(17, 17, 22, 255).mix(accent, 0.12F);
        CARD_BG = new ColorRGBA(26, 26, 32, 255).mix(accent, 0.08F);
        FIELD_BG = new ColorRGBA(32, 32, 39, 255).mix(accent, 0.07F);
        PILL_BG = new ColorRGBA(44, 44, 52, 255).mix(accent, 0.06F);
        TAB_ACTIVE_BG = accent.mix(new ColorRGBA(18, 18, 24, 255), 0.30F);
        ACCENT2 = second;
        CustomDrawContext ctx = CustomDrawContext.of(context);
        hits.clear();
        sliderBounds.clear();

        context.fill(0, 0, sw, sh, new ColorRGBA(4, 3, 8, (int) (openRaw * 150)).getRGB());

        // главное окно — блюр как в HUD
        DrawUtil.drawHudBg(ctx.getMatrices(), winX, winY, winW, winH, 8.0F,
                BorderRadius.all(12.0F), BG.withAlpha(open * 255.0F));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), winX, winY, winW, winH, 0.8F,
                BorderRadius.all(12.0F), new ColorRGBA(255, 255, 255, open * 20.0F));

        renderSidebar(ctx, theme, accent, mouseX, mouseY, open);
        renderHeader(ctx, accent, open);
        if (currentCategory == Category.CONFIG) renderConfigs(ctx, theme, accent, mouseX, mouseY, open);
        else if (currentCategory == Category.THEMES) renderThemes(ctx, theme, accent, mouseX, mouseY, open);
        else renderModules(ctx, theme, accent, mouseX, mouseY, open);

        if (selectedModule != null && isModuleTab(currentCategory)) {
            renderSidePanel(ctx, theme, accent, mouseX, mouseY, open);
        }

        // плавные скроллы
        for (Category c : TABS) {
            float t = scrollTargets.getOrDefault(c, 0.0F);
            float v = scrolls.getOrDefault(c, 0.0F);
            v += (t - v) * 0.2F;
            if (Math.abs(t - v) < 0.1F) v = t;
            scrolls.put(c, v);
        }
        panelScroll += (panelTarget - panelScroll) * 0.2F;
        if (Math.abs(panelTarget - panelScroll) < 0.1F) panelScroll = panelTarget;
        extraScroll += (extraTarget - extraScroll) * 0.2F;
        if (Math.abs(extraTarget - extraScroll) < 0.1F) extraScroll = extraTarget;
    }

    private void renderSidebar(CustomDrawContext ctx, Theme theme, ColorRGBA accent,
                               int mouseX, int mouseY, float open) {
        

        // лого: только текст, по центру шапки, без картинок.
        // LUMENS переливается градиентом двух цветов темы
        float headCX = winX + SIDE_W / 2.0F;
        String title = "LUMENS";
        String ver = "Version: 1.0.2";
        Font logoFont = Fonts.SEMIBOLD.getFont(15.0F);
        float tw = Fonts.SEMIBOLD.getWidth(title, 15.0F);
        float vw = Fonts.REGULAR.getWidth(ver, 8.0F);
        float wave = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 550.0);
        ColorRGBA gL = ColorRGBA.lerp(accent, ACCENT2, wave * 0.6F);
        ColorRGBA gR = ColorRGBA.lerp(ACCENT2, accent, wave * 0.6F);
        ctx.drawText(logoFont, title, headCX - tw / 2.0F, winY + 12,
                Gradient.of(gL, gL, gR, gR));
        ctx.drawText(Fonts.REGULAR.getFont(8.0F), ver, headCX - vw / 2.0F, winY + 31,
                TEXT_DIM.withAlpha(open * 255.0F));

        // вкладки
        float y = tabsStartY();
        for (Category c : TABS) {
            if (c == Category.CONFIG) y += CONFIG_GAP;
            float tx = winX + 10, twd = SIDE_W - 20;
            boolean active = c == currentCategory;
            boolean hov = MathUtil.isHovered(mouseX, mouseY, tx, y, twd, TAB_H);
            if (active) {
                DrawUtil.drawRoundedRect(ctx.getMatrices(), tx, y, twd, TAB_H,
                        BorderRadius.all(9.0F), TAB_ACTIVE_BG.withAlpha(open * 255.0F));
            } else if (hov) {
                DrawUtil.drawRoundedRect(ctx.getMatrices(), tx, y, twd, TAB_H,
                        BorderRadius.all(9.0F), new ColorRGBA(255, 255, 255, open * 14.0F));
            }
            ColorRGBA iconCol = active ? ColorRGBA.WHITE
                    : new ColorRGBA(205, 203, 213, 255);
            ColorRGBA nameCol = active ? ColorRGBA.WHITE
                    : new ColorRGBA(215, 213, 222, 255);
            try {
                ctx.drawText(Fonts.ICONS.getFont(8.5F), c.getIcon(), tx + 11, y + 10, iconCol);
            } catch (Exception ignored) {
            }
            ctx.drawText(Fonts.SEMIBOLD.getFont(8.5F), c.getName(), tx + 28, y + 9, nameCol);

            final Category tab = c;
            hits.add(new Hit(tx, y, twd, TAB_H, 0, () -> {
                currentCategory = tab;
                selectedModule = null;
                bindModule = null;
                bindKey = null;
                dragSetting = null;
                searchFocused = false;
                configFocused = false;
            }));
            y += TAB_H + TAB_GAP;
        }

        // поиск
        float sx = winX + 10, swd = SIDE_W - 20, sh2 = 27.0F;
        float sy = winY + winH - sh2 - 10;
        DrawUtil.drawRoundedRect(ctx.getMatrices(), sx, sy, swd, sh2,
                BorderRadius.all(8.0F), FIELD_BG.withAlpha(open * 255.0F));
        if (searchFocused) {
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), sx, sy, swd, sh2, 0.8F,
                    BorderRadius.all(8.0F), accent.withAlpha(open * 200.0F));
        } else {
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), sx, sy, swd, sh2, 0.6F,
                    BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255, open * 14.0F));
        }
        Font sf = Fonts.REGULAR.getFont(8.0F);
        if (searchText.isEmpty()) {
            ctx.drawText(sf, "Search", sx + 10, sy + 9, TEXT_FAINT.withAlpha(open * 255.0F));
        } else {
            ctx.drawText(sf, searchText, sx + 10, sy + 9, TEXT_MAIN.withAlpha(open * 255.0F));
            if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
                float cw = Fonts.REGULAR.getWidth(searchText, 8.0F);
                DrawUtil.drawRect(ctx.getMatrices(), sx + 11 + cw, sy + 7, 1, 13,
                        TEXT_MAIN.withAlpha(open * 255.0F));
            }
        }
        // лупа справа (кольцо + диагональная ручка)
        float lx = sx + swd - 21, ly = sy + 9;
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), lx, ly, 9, 9, 1.2F,
                BorderRadius.all(4.5F), TEXT_FAINT.withAlpha(open * 255.0F));
        DrawUtil.drawLine(ctx.getMatrices(),
                new Vec2f(lx + 7.5F, ly + 7.5F), new Vec2f(lx + 12.0F, ly + 12.0F),
                TEXT_FAINT.withAlpha(open * 255.0F));
        hits.add(new Hit(sx, sy, swd, sh2, 0, () -> {
            searchFocused = true;
            configFocused = false;
        }));
    }

    private void renderHeader(CustomDrawContext ctx, ColorRGBA accent, float open) {
        float hx = contentX + 16, hy = winY + 13;
        Category c = currentCategory;
        // чип-подложка под иконку категории - размер под иконку
        DrawUtil.drawRoundedRect(ctx.getMatrices(), hx - 3, hy - 1, 18, 18,
                BorderRadius.all(6.0F), accent.mulAlpha(0.16F));
        try {
            ctx.drawText(Fonts.ICONS.getFont(10.0F), c.getIcon(), hx, hy + 1, accent);
        } catch (Exception ignored) {
        }
        ctx.drawText(Fonts.SEMIBOLD.getFont(13.0F), c.getName(), hx + 22, hy,
                TEXT_MAIN.withAlpha(open * 255.0F));
        ctx.drawText(Fonts.REGULAR.getFont(7.0F), subtitleOf(c), hx, hy + 24,
                TEXT_DIM.withAlpha(open * 255.0F));
    }

    // ---------------- модули (только карточки, без настроек) ----------------

    private void renderModules(CustomDrawContext ctx, Theme theme, ColorRGBA accent,
                               int mouseX, int mouseY, float open) {
        List<Module> mods = modulesOf(currentCategory);
        float pad = 12, gap = 8;
        float top = winY + 56;
        float colW = (contentW - pad * 2 - gap) / 2.0F;
        float leftX = contentX + pad;
        float rightX = leftX + colW + gap;
        float listY = top;
        float listH = winY + winH - listY - 12;

        List<Module> left = new ArrayList<>(), right = new ArrayList<>();
        for (int i = 0; i < mods.size(); i++) {
            if (i % 2 == 0) left.add(mods.get(i));
            else right.add(mods.get(i));
        }
        float rows = Math.max(left.size(), right.size());
        float content = rows * (CARD_H + CARD_GAP);
        float max = Math.min(0.0F, -(content - listH));
        float t = MathHelper.clamp(scrollTargets.getOrDefault(currentCategory, 0.0F), max, 0.0F);
        scrollTargets.put(currentCategory, t);
        float sc = scrolls.getOrDefault(currentCategory, 0.0F);

        ctx.enableScissor((int) contentX, (int) listY, (int) (contentX + contentW), (int) (listY + listH));
        renderModuleColumn(ctx, theme, accent, left, leftX, listY + sc, colW, mouseX, mouseY, open);
        renderModuleColumn(ctx, theme, accent, right, rightX, listY + sc, colW, mouseX, mouseY, open);
        if (mods.isEmpty()) {
            String s = "Модулей не найдено";
            float w = Fonts.REGULAR.getWidth(s, 9.0F);
            ctx.drawText(Fonts.REGULAR.getFont(9.0F), s, contentX + contentW / 2.0F - w / 2.0F,
                    listY + 30, TEXT_FAINT.withAlpha(open * 255.0F));
        }
        ctx.disableScissor();
    }

    private void renderModuleColumn(CustomDrawContext ctx, Theme theme, ColorRGBA accent,
                                    List<Module> mods, float x, float y, float w,
                                    int mouseX, int mouseY, float open) {
        float my = y;
        for (Module m : mods) {
            try {
                m.getAnimation().update(m.isEnabled());
            } catch (Exception ignored) {
            }
            boolean hov = MathUtil.isHovered(mouseX, mouseY, x, my, w, CARD_H);
            boolean sel = m == selectedModule;
            ColorRGBA cardFill = CARD_BG.withAlpha(open * 255.0F);
            if (m.isEnabled()) cardFill = CARD_BG.mix(accent, 0.15F).withAlpha(open * 255.0F);
            if (sel) cardFill = CARD_BG.mix(accent, 0.30F).withAlpha(open * 255.0F);
            if (hov) cardFill = cardFill.mix(ColorRGBA.WHITE, 0.05F);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, my, w, CARD_H,
                    BorderRadius.all(10.0F), cardFill);
            ColorRGBA border = sel ? accent
                    : new ColorRGBA(255, 255, 255, open * (hov ? 30.0F : 12.0F));
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, my, w, CARD_H, 0.6F,
                    BorderRadius.all(10.0F), border);
            if (!m.isEnabled()) {
                ctx.drawText(Fonts.SEMIBOLD.getFont(8.5F), m.getName(), x + 12, my + 11, TEXT_OFF);
            } else {
                ctx.drawText(Fonts.SEMIBOLD.getFont(8.5F), m.getName(), x + 12, my + 11,
                        TEXT_MAIN.withAlpha(open * 255.0F));
            }
            float swAnim = 0.0F;
            try {
                swAnim = m.getAnimation().getValue();
            } catch (Exception ignored) {
            }
            drawSwitch(ctx, accent, x + w - 42, my + CARD_H / 2.0F - 8, 28, 16, swAnim, open);

            if (bindModule == m) {
                ctx.drawText(Fonts.REGULAR.getFont(7.0F), "...", x + w - 54, my + 13, accent);
            } else if (m.getKeyCode() != -1) {
                String bind = Keyboard.getKeyName(m.getKeyCode());
                float bw = Fonts.REGULAR.getWidth(bind, 7.0F);
                ctx.drawText(Fonts.REGULAR.getFont(7.0F), bind, x + w - 46 - bw, my + 13,
                        TEXT_FAINT.withAlpha(open * 220.0F));
            }

            final Module mod = m;
            final float cx = x, cy = my, cw = w;
            hits.add(new Hit(x, my, w, CARD_H, 0, mod::toggle));
            hits.add(new Hit(x, my, w, CARD_H, 1, () -> {
                if (selectedModule == mod) {
                    selectedModule = null;
                    dragSetting = null;
                } else {
                    // у функций без настроек панель не открываем
                    boolean hasSettings = false;
                    try {
                        for (Setting s : mod.getSettings()) {
                            if (s.isVisible()) {
                                hasSettings = true;
                                break;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    if (!hasSettings) return;
                    selectedModule = mod;
                    panelTarget = 0;
                    panelScroll = 0;
                    bindModule = null;
                    bindKey = null;
                }
            }));
            hits.add(new Hit(x, my, w, CARD_H, 2, () -> bindModule = mod));
            // свитч тоже тогглит на ЛКМ
            hits.add(new Hit(cx + cw - 46, cy + 4, 36, 28, 0, mod::toggle));
            my += CARD_H + CARD_GAP;
        }
    }

    // ---------------- правая панель настроек ----------------

    private void renderSidePanel(CustomDrawContext ctx, Theme theme, ColorRGBA accent,
                                 int mouseX, int mouseY, float open) {
        Module m = selectedModule;
        if (m == null) return;
        DrawUtil.drawHudBg(ctx.getMatrices(), panelX, panelY, PANEL_W, panelH, 8.0F,
                BorderRadius.all(12.0F), BG.withAlpha(open * 255.0F));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), panelX, panelY, PANEL_W, panelH, 0.8F,
                BorderRadius.all(12.0F), new ColorRGBA(255, 255, 255, open * 20.0F));

        // шапка панели
        float hx = panelX + 16, hy = panelY + 14;
        DrawUtil.drawRoundedRect(ctx.getMatrices(), hx - 4, hy - 3, 22, 22,
                BorderRadius.all(7.0F), accent.mulAlpha(0.16F));
        try {
            ctx.drawText(Fonts.ICONS.getFont(9.0F), m.getCategory().getIcon(), hx + 1, hy + 1, accent);
        } catch (Exception ignored) {
        }
        String name = m.getName();
        if (name.length() > 18) name = name.substring(0, 18);
        ctx.drawText(Fonts.SEMIBOLD.getFont(10.5F), name, hx + 24, hy, TEXT_MAIN.withAlpha(open * 255.0F));
        // точка статуса модуля
        float nameW = Fonts.SEMIBOLD.getWidth(name, 10.5F);
        ColorRGBA dot = m.isEnabled() ? new ColorRGBA(90, 255, 140, 255) : new ColorRGBA(115, 115, 125, 255);
        if (m.isEnabled()) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), hx + 24 + nameW + 4, hy, 11, 11,
                    BorderRadius.all(5.5F), accent.withAlpha(open * 45.0F));
        }
        DrawUtil.drawRoundedRect(ctx.getMatrices(), hx + 24 + nameW + 6, hy + 2, 7, 7,
                BorderRadius.all(3.5F), dot);
        String sub = m.isEnabled() ? "Включён" : "Настройки модуля";
        ctx.drawText(Fonts.REGULAR.getFont(7.0F), sub, hx, hy + 17, TEXT_DIM.withAlpha(open * 255.0F));
        // крестик
        float xx = panelX + PANEL_W - 28, xy = panelY + 12;
        ctx.drawText(Fonts.SEMIBOLD.getFont(9.0F), "x", xx + 6, xy + 3,
                TEXT_DIM.withAlpha(open * 255.0F));
        hits.add(new Hit(xx, xy, 18, 18, 0, () -> {
            selectedModule = null;
            dragSetting = null;
        }));

        float boxX = panelX + 12, boxW = PANEL_W - 24;
        float boxY = panelY + 52;
        float boxH = panelH - 64;
        DrawUtil.drawRoundedRect(ctx.getMatrices(), boxX, boxY, boxW, boxH,
                BorderRadius.all(10.0F), CARD_BG.withAlpha(open * 255.0F));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), boxX, boxY, boxW, boxH, 0.6F,
                BorderRadius.all(10.0F), new ColorRGBA(255, 255, 255, open * 8.0F));

        float innerX = boxX + 14, innerW = boxW - 28;
        float listY = boxY + 12;
        float listH = boxH - 24;

        // высота контента
        float total = settingsHeight(m, innerW);
        float max = Math.min(0.0F, -(total - listH));
        if (panelTarget > 0) panelTarget = 0;
        if (panelTarget < max) panelTarget = max;

        ctx.enableScissor((int) boxX, (int) listY, (int) (boxX + boxW), (int) (listY + listH));
        float sy = listY + 2 + panelScroll;
        sy = renderSettings(ctx, theme, accent, m, innerX, sy, innerW, mouseX, mouseY, open);
        ctx.disableScissor();

        // скроллбар
        if (total > listH + 4) {
            float barH = Math.max(24, listH * listH / total);
            float barY = listY + (-panelScroll / Math.max(1, total - listH)) * (listH - barH);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), boxX + boxW - 6, barY, 3, barH,
                    BorderRadius.all(1.5F), accent.withAlpha(open * 130.0F));
        }
    }

    private float settingsHeight(Module m, float innerW) {
        float h = 0;
        for (Setting s : m.getSettings()) {
            if (!s.isVisible()) continue;
            if (s instanceof BooleanSetting) h += 30;
            else if (s instanceof KeySetting) h += 30;
            else if (s instanceof NumberSetting) h += 50;
            else if (s instanceof ModeSetting md) h += measurePills(md.getValues(), (ModeSetting.Value v) -> v.getName(), innerW) + 26 + 10;
            else if (s instanceof MultiBooleanSetting mb) h += measurePills(mb.getBooleanSettings(), (MultiBooleanSetting.Value v) -> v.getName(), innerW) + 26 + 10;
            else h += 30;
        }
        return h + 4;
    }

    private <T> float measurePills(List<T> values, Namer<T> namer, float w) {
        float bx = 0, by = 0;
        for (T v : values) {
            float vw;
            try {
                vw = Fonts.REGULAR.getWidth(namer.name(v), 7.5F) + 16;
            } catch (Exception e) {
                vw = 60;
            }
            if (bx + vw > w + 1) {
                bx = 0;
                by += 25;
            }
            bx += vw + 6;
        }
        return by + 25;
    }

    private interface Namer<T> {
        String name(T v);
    }

    private float renderSettings(CustomDrawContext ctx, Theme theme, ColorRGBA accent, Module m,
                                 float innerX, float sy, float innerW,
                                 int mouseX, int mouseY, float open) {
        for (Setting s : m.getSettings()) {
            if (!s.isVisible()) continue;
            if (s instanceof BooleanSetting b) {
                float ban = 0.0F;
                try {
                    b.getAnimation().update(b.isEnabled());
                    ban = b.getAnimation().getValue();
                } catch (Exception ignored) {
                }
                ctx.drawText(Fonts.REGULAR.getFont(9.0F), b.getName(), innerX, sy + 4,
                        TEXT_MAIN.withAlpha(open * 255.0F));
                drawSwitch(ctx, accent, innerX + innerW - 36, sy + 1, 34, 19, ban, open);
                final BooleanSetting bb = b;
                hits.add(new Hit(innerX, sy - 2, innerW, 26, 0, bb::toggle));
                sy += 30;
            } else if (s instanceof KeySetting k) {
                String disp = bindKey == k ? "..." : k.getNameKey();
                if ((disp == null || disp.isEmpty()) && bindKey != k) disp = "None";
                ctx.drawText(Fonts.REGULAR.getFont(9.0F), k.getName(), innerX, sy + 4,
                        TEXT_MAIN.withAlpha(open * 255.0F));
                float pw = Fonts.REGULAR.getWidth(disp, 7.5F) + 16;
                float px = innerX + innerW - pw;
                DrawUtil.drawRoundedRect(ctx.getMatrices(), px, sy + 1, pw, 19,
                        BorderRadius.all(6.0F), PILL_BG.withAlpha(open * 255.0F));
                DrawUtil.drawRoundedBorder(ctx.getMatrices(), px, sy + 1, pw, 19, 0.5F,
                        BorderRadius.all(6.0F), new ColorRGBA(255, 255, 255, open * 10.0F));
                float dw = Fonts.REGULAR.getWidth(disp, 7.5F);
                ctx.drawText(Fonts.REGULAR.getFont(7.5F), disp, px + pw / 2.0F - dw / 2.0F, sy + 5,
                        TEXT_MAIN.withAlpha(open * 255.0F));
                final KeySetting kk = k;
                hits.add(new Hit(px, sy, pw + 4, 22, 0, () -> bindKey = kk));
                sy += 30;
            } else if (s instanceof NumberSetting n) {
                ctx.drawText(Fonts.REGULAR.getFont(9.0F), n.getName(), innerX, sy,
                        TEXT_MAIN.withAlpha(open * 255.0F));
                String val = fmtNumber(n.getCurrent());
                float vw = Fonts.SEMIBOLD.getWidth(val, 9.0F);
                ctx.drawText(Fonts.SEMIBOLD.getFont(9.0F), val, innerX + innerW - vw, sy,
                        TEXT_MAIN.withAlpha(open * 255.0F));
                float tx = innerX, ty = sy + 20, twd = innerW;
                DrawUtil.drawRoundedRect(ctx.getMatrices(), tx, ty, twd, 4,
                        BorderRadius.all(2.0F), new ColorRGBA(44, 44, 49, open * 255.0F));
                float frac = MathHelper.clamp((n.getCurrent() - n.getMin())
                        / Math.max(0.0001F, n.getMax() - n.getMin()), 0, 1);
                if (frac > 0.005F) {
                    DrawUtil.drawRoundedRect(ctx.getMatrices(), tx, ty, Math.max(6, twd * frac), 4,
                            BorderRadius.all(2.0F), accent);
                }
                float kx = tx + twd * frac;
                DrawUtil.drawRoundedRect(ctx.getMatrices(), kx - 7, ty - 6, 14, 16,
                        BorderRadius.all(7.0F), accent.withAlpha(open * 40.0F));
                DrawUtil.drawRoundedRect(ctx.getMatrices(), kx - 5, ty - 4, 10, 12,
                        BorderRadius.all(5.0F), new ColorRGBA(245, 245, 248, open * 255.0F));
                sliderBounds.put(n, new float[]{tx, ty - 5, twd, 14});
                if (dragSetting == n) {
                    float p = MathHelper.clamp((lastMouseX - tx) / twd, 0, 1);
                    float v = n.getMin() + p * (n.getMax() - n.getMin());
                    float inc = n.getIncrement();
                    if (inc > 0) v = Math.round(v / inc) * inc;
                    n.setCurrent(MathHelper.clamp(v, n.getMin(), n.getMax()));
                }
                final NumberSetting nn = n;
                hits.add(new Hit(tx, ty - 6, twd, 16, 0, () -> dragSetting = nn));
                sy += 50;
            } else if (s instanceof ModeSetting md) {
                ctx.drawText(Fonts.REGULAR.getFont(9.0F), md.getName(), innerX, sy,
                        TEXT_MAIN.withAlpha(open * 255.0F));
                sy = renderModePills(ctx, accent, innerX, sy + 18, innerW, open,
                        mouseX, mouseY,
                        md.getValues(), (ModeSetting.Value v) -> v.getName(),
                        (ModeSetting.Value v) -> md.getValue() == v,
                        (ModeSetting.Value v) -> md.setValue(v));
                sy += 10;
            } else if (s instanceof MultiBooleanSetting mb) {
                ctx.drawText(Fonts.REGULAR.getFont(9.0F), mb.getName(), innerX, sy,
                        TEXT_MAIN.withAlpha(open * 255.0F));
                int sel = mb.getSelectedValues().size();
                int tot = mb.getBooleanSettings().size();
                String cnt = sel + "/" + tot;
                float cw = Fonts.REGULAR.getWidth(cnt, 8.0F);
                ctx.drawText(Fonts.REGULAR.getFont(8.0F), cnt, innerX + innerW - cw, sy + 1,
                        TEXT_DIM.withAlpha(open * 255.0F));
                sy = renderModePills(ctx, accent, innerX, sy + 18, innerW, open,
                        mouseX, mouseY,
                        mb.getBooleanSettings(), (MultiBooleanSetting.Value v) -> v.getName(),
                        (MultiBooleanSetting.Value v) -> v.isEnabled(),
                        (MultiBooleanSetting.Value v) -> v.setEnabled(!v.isEnabled()));
                sy += 10;
            } else {
                sy += 30;
            }
        }
        return sy;
    }

    private interface IsActive<T> {
        boolean active(T v);
    }

    private interface OnPick<T> {
        void pick(T v);
    }

    private <T> float renderModePills(CustomDrawContext ctx, ColorRGBA accent,
                                      float x, float y, float w, float open,
                                      int mouseX, int mouseY,
                                      List<T> values, Namer<T> namer, IsActive<T> isActive, OnPick<T> onPick) {
        float bx = x, by = y;
        for (T v : values) {
            String n = namer.name(v);
            float vw = Fonts.REGULAR.getWidth(n, 7.5F) + 16;
            if (bx + vw > x + w + 1) {
                bx = x;
                by += 25;
            }
            boolean on = isActive.active(v);
            boolean hov = !on && MathUtil.isHovered(mouseX, mouseY, bx, by, vw, 20);
            if (on) {
                DrawUtil.drawRoundedRect(ctx.getMatrices(), bx - 1, by - 1, vw + 2, 22,
                        BorderRadius.all(7.0F), accent.withAlpha(open * 40.0F));
                DrawUtil.drawRoundedRect(ctx.getMatrices(), bx, by, vw, 20,
                        BorderRadius.all(6.0F), accent);
                float nw = Fonts.REGULAR.getWidth(n, 7.5F);
                ctx.drawText(Fonts.REGULAR.getFont(7.5F), n, bx + vw / 2.0F - nw / 2.0F, by + 5,
                        new ColorRGBA(255, 255, 255, open * 255.0F));
            } else {
                ColorRGBA pillFill = PILL_BG.withAlpha(open * 255.0F);
                if (hov) pillFill = pillFill.mix(ColorRGBA.WHITE, 0.07F);
                DrawUtil.drawRoundedRect(ctx.getMatrices(), bx, by, vw, 20,
                        BorderRadius.all(6.0F), pillFill);
                DrawUtil.drawRoundedBorder(ctx.getMatrices(), bx, by, vw, 20, 0.5F,
                        BorderRadius.all(6.0F),
                        new ColorRGBA(255, 255, 255, open * (hov ? 26.0F : 10.0F)));
                float nw = Fonts.REGULAR.getWidth(n, 7.5F);
                ctx.drawText(Fonts.REGULAR.getFont(7.5F), n, bx + vw / 2.0F - nw / 2.0F, by + 5,
                        (hov ? TEXT_MAIN : TEXT_DIM).withAlpha(open * 255.0F));
            }
            final float hx = bx, hy = by, hw = vw;
            final T vv = v;
            hits.add(new Hit(hx, hy, hw, 20, 0, () -> onPick.pick(vv)));
            bx += vw + 6;
        }
        return by + 25;
    }

    private void drawSwitch(CustomDrawContext ctx, ColorRGBA accent,
                            float x, float y, float w, float h, float anim, float open) {
        anim = MathHelper.clamp(anim, 0.0F, 1.0F);
        if (anim > 0.01F) {
            // свечение вокруг включённого свитча
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x - 3, y - 3, w + 6, h + 6,
                    BorderRadius.all(h / 2.0F + 3), accent.withAlpha(open * 45.0F * anim));
        }
        ColorRGBA off = new ColorRGBA(52, 52, 60, (int) (open * 255.0F));
        ColorRGBA track = off.mix(accent.withAlpha((int) (open * 255.0F)), anim);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, h,
                BorderRadius.all(h / 2.0F), track);
        float d = h - 5;
        float kx = x + 2.5F + (w - d - 5.0F) * anim;
        DrawUtil.drawRoundedRect(ctx.getMatrices(), kx, y + 2.5F, d, d,
                BorderRadius.all(d / 2.0F), new ColorRGBA(255, 255, 255, open * 255.0F));
    }

    // ---------------- конфиги ----------------

    private void renderConfigs(CustomDrawContext ctx, Theme theme, ColorRGBA accent,
                               int mouseX, int mouseY, float open) {
        float pad = 12;
        float listY = winY + 56;
        float listH = winY + winH - listY - 12;
        float boxX = contentX + pad, boxW = contentW - pad * 2;

        List<String> names = new ArrayList<>();
        try {
            for (String n : Lumens.getInstance().getConfigManager().configNames()) {
                int dot = n.lastIndexOf('.');
                names.add(dot > 0 ? n.substring(0, dot) : n);
            }
        } catch (Exception ignored) {
        }
        names.sort(String::compareToIgnoreCase);

        float total = 150 + names.size() * 42.0F;
        float max = Math.min(0.0F, -(total - listH));
        if (extraTarget > 0) extraTarget = 0;
        if (extraTarget < max) extraTarget = max;

        ctx.enableScissor((int) contentX, (int) listY, (int) (contentX + contentW), (int) (listY + listH));
        float y = listY + extraScroll;

        // карточка создания
        DrawUtil.drawRoundedRect(ctx.getMatrices(), boxX, y, boxW, 118,
                BorderRadius.all(10.0F), CARD_BG.withAlpha(open * 255.0F));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), boxX, y, boxW, 118, 0.6F,
                BorderRadius.all(10.0F), new ColorRGBA(255, 255, 255, open * 8.0F));
        ctx.drawText(Fonts.SEMIBOLD.getFont(9.5F), "Configs", boxX + 16, y + 12,
                TEXT_MAIN.withAlpha(open * 255.0F));
        ctx.drawText(Fonts.REGULAR.getFont(7.5F), "Введите имя и нажмите Create",
                boxX + 16, y + 30, TEXT_DIM.withAlpha(open * 255.0F));
        // поле ввода
        float fx = boxX + 16, fw = boxW - 32, fh = 28;
        float fy = y + 50;
        DrawUtil.drawRoundedRect(ctx.getMatrices(), fx, fy, fw, fh,
                BorderRadius.all(7.0F), FIELD_BG.withAlpha(open * 255.0F));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), fx, fy, fw, fh, 0.7F,
                BorderRadius.all(7.0F), configFocused ? accent.withAlpha(open * 200.0F)
                        : new ColorRGBA(255, 255, 255, open * 14.0F));
        Font cf = Fonts.REGULAR.getFont(8.5F);
        if (configName.isEmpty()) {
            ctx.drawText(cf, "Name...", fx + 10, fy + 9, TEXT_FAINT.withAlpha(open * 255.0F));
        } else {
            ctx.drawText(cf, configName, fx + 10, fy + 9, TEXT_MAIN.withAlpha(open * 255.0F));
        }
        hits.add(new Hit(fx, fy, fw, fh, 0, () -> {
            configFocused = true;
            searchFocused = false;
        }));
        // кнопки
        float bw = (fw - 8) / 2.0F, by = fy + fh + 8, bh = 24;
        drawButton(ctx, accent, fx, by, bw, bh, "Create", open);
        hits.add(new Hit(fx, by, bw, bh, 0, () -> {
            if (!configName.trim().isEmpty()) {
                try {
                    Lumens.getInstance().getConfigManager().saveConfig(configName.trim());
                } catch (Exception ignored) {
                }
            }
        }));
        drawButton(ctx, accent, fx + bw + 8, by, bw, bh, "Save current", open);
        hits.add(new Hit(fx + bw + 8, by, bw, bh, 0, () -> {
            try {
                String n = configName.trim().isEmpty() ? "current_config" : configName.trim();
                Lumens.getInstance().getConfigManager().saveConfig(n);
            } catch (Exception ignored) {
            }
        }));
        y += 128;

        for (String n : names) {
            boolean rowHov = MathUtil.isHovered(mouseX, mouseY, boxX, y, boxW, 34);
            ColorRGBA rowFill = CARD_BG.withAlpha(open * 255.0F);
            if (rowHov) rowFill = rowFill.mix(ColorRGBA.WHITE, 0.05F);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), boxX, y, boxW, 34,
                    BorderRadius.all(9.0F), rowFill);
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), boxX, y, boxW, 34, 0.6F,
                    BorderRadius.all(9.0F),
                    new ColorRGBA(255, 255, 255, open * (rowHov ? 22.0F : 10.0F)));
            String shortN = n.length() > 26 ? n.substring(0, 26) : n;
            ctx.drawText(Fonts.REGULAR.getFont(8.5F), shortN, boxX + 14, y + 11,
                    TEXT_MAIN.withAlpha(open * 255.0F));
            float abw = 62, abh = 20;
            float loadX = boxX + boxW - abw * 2 - 20;
            drawSmallButton(ctx, accent, loadX, y + 7, abw, abh, "Load", true, open);
            drawSmallButton(ctx, accent, loadX + abw + 8, y + 7, abw, abh, "Delete", false, open);
            final String cfg = n;
            hits.add(new Hit(loadX, y + 7, abw, abh, 0, () -> {
                try {
                    Lumens.getInstance().getConfigManager().loadConfig(cfg);
                } catch (Exception ignored) {
                }
            }));
            hits.add(new Hit(loadX + abw + 8, y + 7, abw, abh, 0, () -> {
                try {
                    Lumens.getInstance().getConfigManager().deleteConfig(cfg);
                } catch (Exception ignored) {
                }
            }));
            y += 42;
        }
        ctx.disableScissor();
    }

    private void drawButton(CustomDrawContext ctx, ColorRGBA accent,
                            float x, float y, float w, float h, String text, float open) {
        DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, h,
                BorderRadius.all(7.0F), accent);
        float tw = Fonts.SEMIBOLD.getWidth(text, 8.5F);
        ctx.drawText(Fonts.SEMIBOLD.getFont(8.5F), text, x + w / 2.0F - tw / 2.0F, y + 7,
                new ColorRGBA(255, 255, 255, open * 255.0F));
    }

    private void drawSmallButton(CustomDrawContext ctx, ColorRGBA accent,
                                 float x, float y, float w, float h, String text,
                                 boolean primary, float open) {
        if (primary) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, h,
                    BorderRadius.all(6.0F), accent);
            float tw = Fonts.SEMIBOLD.getWidth(text, 7.5F);
            ctx.drawText(Fonts.SEMIBOLD.getFont(7.5F), text, x + w / 2.0F - tw / 2.0F, y + 5,
                    new ColorRGBA(255, 255, 255, open * 255.0F));
        } else {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, h,
                    BorderRadius.all(6.0F), PILL_BG.withAlpha(open * 255.0F));
            float tw = Fonts.SEMIBOLD.getWidth(text, 7.5F);
            ctx.drawText(Fonts.SEMIBOLD.getFont(7.5F), text, x + w / 2.0F - tw / 2.0F, y + 5,
                    TEXT_DIM.withAlpha(open * 255.0F));
        }
    }

    // ---------------- темы ----------------

    private void renderThemes(CustomDrawContext ctx, Theme theme, ColorRGBA accent,
                              int mouseX, int mouseY, float open) {
        List<Theme> themes;
        try {
            themes = new ArrayList<>(Lumens.getInstance().getThemeManager().getThemes());
        } catch (Exception e) {
            return;
        }
        float pad = 12;
        float listY = winY + 56;
        float listH = winY + winH - listY - 12;
        float boxX = contentX + pad, boxW = contentW - pad * 2;
        float total = themes.size() * 52.0F + 8;
        float max = Math.min(0.0F, -(total - listH));
        if (extraTarget > 0) extraTarget = 0;
        if (extraTarget < max) extraTarget = max;

        ctx.enableScissor((int) contentX, (int) listY, (int) (contentX + contentW), (int) (listY + listH));
        float y = listY + extraScroll;
        Theme current = Lumens.getInstance().getThemeManager().getCurrentTheme();
        for (Theme t : themes) {
            boolean sel = t == current || (current != null && t.getName().equals(current.getName()));
            boolean hov = MathUtil.isHovered(mouseX, mouseY, boxX, y, boxW, 44);
            ColorRGBA themeFill = CARD_BG.withAlpha(open * 255.0F);
            if (!sel && hov) themeFill = themeFill.mix(ColorRGBA.WHITE, 0.05F);
            if (sel) {
                try {
                    themeFill = CARD_BG.mix(t.getColor(), 0.18F).withAlpha(open * 255.0F);
                } catch (Exception ignored) {
                }
            }
            DrawUtil.drawRoundedRect(ctx.getMatrices(), boxX, y, boxW, 44,
                    BorderRadius.all(10.0F), themeFill);
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), boxX, y, boxW, 44, 0.6F,
                    BorderRadius.all(10.0F), sel ? t.getColor().withAlpha(open * 255.0F)
                            : new ColorRGBA(255, 255, 255, open * (hov ? 20.0F : 8.0F)));
            // превью: градиент из двух цветов темы (левая половина + правая половина)
            ColorRGBA c1;
            ColorRGBA c2;
            try {
                c1 = new ColorRGBA(t.getColor1()).withAlpha(open * 255.0F);
                c2 = new ColorRGBA(t.getColor2()).withAlpha(open * 255.0F);
            } catch (Exception e) {
                c1 = TEXT_DIM;
                c2 = TEXT_DIM;
            }
            float pvx = boxX + 12, pvy = y + 12, pvw = 40, pvh = 20;
            DrawUtil.drawRoundedRect(ctx.getMatrices(), pvx, pvy, pvw / 2.0F + 0.5F, pvh,
                    BorderRadius.left(6.0F, 6.0F), c1);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), pvx + pvw / 2.0F - 0.5F, pvy, pvw / 2.0F + 0.5F, pvh,
                    BorderRadius.right(6.0F, 6.0F), c2);
            ctx.drawText(Fonts.SEMIBOLD.getFont(9.0F), t.getName(), boxX + 56, y + 15,
                    TEXT_MAIN.withAlpha(open * 255.0F));
            if (sel) {
                String s = "Active";
                float swd = Fonts.REGULAR.getWidth(s, 7.5F) + 16;
                float sx = boxX + boxW - swd - 12;
                DrawUtil.drawRoundedRect(ctx.getMatrices(), sx, y + 12, swd, 20,
                        BorderRadius.all(6.0F), t.getColor().withAlpha(open * 255.0F));
                float stw = Fonts.REGULAR.getWidth(s, 7.5F);
                ctx.drawText(Fonts.REGULAR.getFont(7.5F), s, sx + swd / 2.0F - stw / 2.0F, y + 17,
                        new ColorRGBA(255, 255, 255, open * 255.0F));
            }
            final Theme tt = t;
            hits.add(new Hit(boxX, y, boxW, 44, 0, () -> {
                try {
                    Theme cur = Lumens.getInstance().getThemeManager().getCurrentTheme();
                    int o1 = cur != null ? cur.getColor1() : tt.getColor1();
                    int o2 = cur != null ? cur.getColor2() : tt.getColor2();
                    tt.startAnimation(o1, o2);
                    Lumens.getInstance().getThemeManager().setCurrentTheme(tt);
                } catch (Exception ignored) {
                }
            }));
            y += 52;
        }
        ctx.disableScissor();
    }

    // ---------------- input ----------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (bindModule != null && button >= 0) {
            if (button == 0) {
                // ЛКМ отменяет ожидание, чтобы не ставить бинд кликом по GUI
                boolean onGui = mouseX >= winX && mouseX <= winX + winW
                        && mouseY >= winY && mouseY <= winY + winH;
                if (!onGui) {
                    bindModule.setKeyCode(button);
                    bindModule = null;
                } else {
                    bindModule = null;
                }
            } else {
                bindModule.setKeyCode(button);
                bindModule = null;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (bindKey != null && button >= 0) {
            if (button != 0) {
                bindKey.setKeyCode(button);
                bindKey = null;
            } else {
                bindKey = null;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        // слайдеры тоже через hits выставляют dragSetting
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.matches(mouseX, mouseY, button)) {
                h.action.run();
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }
        if (button == 0) {
            searchFocused = false;
            configFocused = false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragSetting = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        float amt = (float) (v * 20.0);
        if (selectedModule != null && isModuleTab(currentCategory)
                && mouseX >= panelX && mouseX <= panelX + PANEL_W
                && mouseY >= panelY && mouseY <= panelY + panelH) {
            panelTarget += amt;
            return true;
        }
        if (mouseX >= contentX && mouseX <= contentX + contentW) {
            if (currentCategory == Category.CONFIG || currentCategory == Category.THEMES) {
                extraTarget += amt;
            } else {
                scrollTargets.put(currentCategory,
                        scrollTargets.getOrDefault(currentCategory, 0.0F) + amt);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, h, v);
    }

    @Override
    @Native
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            needToClose = true;
            try {
                Menu.INSTANCE.setEnabled(false);
            } catch (Exception ignored) {
            }
            bindModule = null;
            bindKey = null;
            dragSetting = null;
            searchFocused = false;
            configFocused = false;
            search = false;
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        try {
            Cosmetics cosmetics =
                    Cosmetics.INSTANCE;
            if (keyCode == cosmetics.getKeyCode() && cosmetics.getKeyCode() != -1) {
                needToClose = true;
                try {
                    Menu.INSTANCE.setEnabled(false);
                } catch (Exception ignored) {
                }
                if (!cosmetics.isEnabled()) {
                    cosmetics.setToggled(true);
                } else {
                    try {
                        cosmetics.getScreen().open();
                        mc.setScreen(cosmetics.getScreen());
                    } catch (Exception ignored) {
                    }
                }
                return true;
            }
        } catch (Exception ignored) {
        }
        if (bindKey != null) {
            bindKey.setKeyCode(keyCode == 261 ? -1 : keyCode);
            bindKey = null;
            return true;
        }
        if (bindModule != null) {
            bindModule.setKeyCode(keyCode == 261 ? -1 : keyCode);
            bindModule = null;
            return true;
        }
        if (searchFocused) {
            if (keyCode == 259 && !searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
                return true;
            }
            if (keyCode == 261) {
                searchText = "";
                return true;
            }
        }
        if (configFocused) {
            if (keyCode == 259 && !configName.isEmpty()) {
                configName = configName.substring(0, configName.length() - 1);
                return true;
            }
            if (keyCode == 261) {
                configName = "";
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                configFocused = false;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchFocused) {
            if (codePoint >= 32 && codePoint != 127 && searchText.length() < 24) {
                searchText += codePoint;
                return true;
            }
        }
        if (configFocused) {
            if (codePoint >= 32 && codePoint != 127 && configName.length() < 24) {
                configName += codePoint;
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
