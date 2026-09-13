package dev.lumens.client.screens.cosmetics;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import ru.nexusguard.protection.annotations.Native;
import dev.lumens.Lumens;
import dev.lumens.base.font.Fonts;
import dev.lumens.base.theme.Theme;
import dev.lumens.client.cosmetics.Cosmetic;
import dev.lumens.client.cosmetics.CosmeticRenderer;
import dev.lumens.client.modules.impl.player.Cosmetics;
import dev.lumens.client.modules.impl.render.Menu;
import dev.lumens.utility.interfaces.IClient;
import dev.lumens.utility.math.MathUtil;
import dev.lumens.utility.render.display.base.BorderRadius;
import dev.lumens.utility.render.display.base.CustomDrawContext;
import dev.lumens.utility.render.display.base.Gradient;
import dev.lumens.utility.render.display.base.color.ColorRGBA;
import dev.lumens.utility.render.display.shader.DrawUtil;

/**
 * Панель косметики в стиле ClickGUI: фон и секции слева как у MenuScreen,
 * заголовок Cosmetics, разделы Head / Back / Pets.
 */
public class CosmeticsScreen extends Screen implements IClient {
    public enum Section {
        HEAD("Head", "Аксессуары на голову"),
        BACK("Back", "Аксессуары на спину"),
        PETS("Pets", "Питомцы ходят за тобой");

        private final String title;
        private final String subtitle;

        Section(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    private static final float SIDE_W = 150.0F;
    private static final float TAB_H = 29.0F;
    private static final float TAB_GAP = 3.0F;
    private static final float CARD_GAP = 8.0F;
    private static final float PREVIEW_W = 176.0F;

    private static final ColorRGBA TEXT_MAIN = new ColorRGBA(243, 243, 246, 255);
    private static final ColorRGBA TEXT_DIM = new ColorRGBA(152, 152, 160, 255);
    private static final ColorRGBA TEXT_FAINT = new ColorRGBA(112, 112, 120, 255);

    private Section section = Section.HEAD;
    private float winX, winY, winW, winH;
    private float contentX, contentW;
    private float scroll, scrollTarget;

    private float dragYaw = 0.0F;
    private float dragPitch = 0.0F;
    private boolean draggingPreview;
    private float pvX, pvY, pvW, pvH;

    private CatEntity catPreview;
    private WolfEntity wolfPreview;
    private Object previewWorld;

    private static final class Hit {
        final float x, y, w, h;
        final Runnable action;

        Hit(float x, float y, float w, float h, Runnable action) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.action = action;
        }

        boolean matches(double mx, double my) {
            return MathUtil.isHovered(mx, my, x, y, w, h);
        }
    }

    private final List<Hit> hits = new ArrayList<>();

    public CosmeticsScreen() {
        super(Text.of("Cosmetics"));
    }

    public void open() {
        try {
            Cosmetics.INSTANCE.reload();
        } catch (Exception ignored) {
        }
        draggingPreview = false;
    }

    private void layout(int sw, int sh) {
        winW = Math.min(680.0F, sw - 60.0F);
        winH = Math.min(360.0F, sh - 120.0F);
        winW = Math.max(520.0F, winW);
        winH = Math.max(300.0F, winH);
        winX = Math.max(4.0F, sw / 2.0F - winW / 2.0F);
        winY = Math.max(4.0F, sh / 2.0F - winH / 2.0F);
        contentX = winX + SIDE_W;
        contentW = winW - SIDE_W;
    }

    @Override
    @Native
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        layout(sw, sh);
        Theme theme = Lumens.getInstance().getThemeManager().getCurrentTheme();
        try {
            theme.getAnimation().update(1.0F);
        } catch (Exception ignored) {
        }
        ColorRGBA accent = theme.getColor().withAlpha(255);
        ColorRGBA second = theme.getSecondColor().withAlpha(255);
        ColorRGBA bg = new ColorRGBA(17, 17, 22, 255).mix(accent, 0.12F);
        ColorRGBA cardBg = new ColorRGBA(26, 26, 32, 255).mix(accent, 0.08F);
        ColorRGBA tabActive = accent.mix(new ColorRGBA(18, 18, 24, 255), 0.30F);

        CustomDrawContext ctx = CustomDrawContext.of(context);
        hits.clear();

        context.fill(0, 0, sw, sh, new ColorRGBA(4, 3, 8, 150).getRGB());
        DrawUtil.drawHudBg(ctx.getMatrices(), winX, winY, winW, winH, 8.0F, BorderRadius.all(12.0F), bg);
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), winX, winY, winW, winH, 0.8F,
                BorderRadius.all(12.0F), new ColorRGBA(255, 255, 255, 20));

        renderSidebar(ctx, accent, second, mouseX, mouseY);
        renderHeader(ctx, accent, mouseX, mouseY);
        if (section == Section.PETS) {
            renderPets(ctx, accent, cardBg, tabActive, mouseX, mouseY, delta);
        } else {
            renderAccessories(ctx, accent, cardBg, tabActive, mouseX, mouseY, delta);
        }

        scroll += (scrollTarget - scroll) * 0.2F;
        if (Math.abs(scrollTarget - scroll) < 0.1F) scroll = scrollTarget;
    }

    private void renderSidebar(CustomDrawContext ctx, ColorRGBA accent, ColorRGBA second, int mouseX, int mouseY) {
        float headCX = winX + SIDE_W / 2.0F;
        String title = "COSMETICS";
        float tw = Fonts.SEMIBOLD.getWidth(title, 13.0F);
        float wave = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 550.0);
        ColorRGBA gL = ColorRGBA.lerp(accent, second, wave * 0.6F);
        ColorRGBA gR = ColorRGBA.lerp(second, accent, wave * 0.6F);
        ctx.drawText(Fonts.SEMIBOLD.getFont(13.0F), title, headCX - tw / 2.0F, winY + 12,
                Gradient.of(gL, gL, gR, gR));
        String hint = "Version: 1.0";
        float hw = Fonts.REGULAR.getWidth(hint, 8.0F);
        ctx.drawText(Fonts.REGULAR.getFont(8.0F), hint, headCX - hw / 2.0F, winY + 30, TEXT_DIM);

        float y = winY + 64.0F;
        for (Section s : Section.values()) {
            float tx = winX + 10, twd = SIDE_W - 20;
            boolean active = s == section;
            boolean hov = MathUtil.isHovered(mouseX, mouseY, tx, y, twd, TAB_H);
            if (active) {
                DrawUtil.drawRoundedRect(ctx.getMatrices(), tx, y, twd, TAB_H, BorderRadius.all(9.0F), tabBg());
            } else if (hov) {
                DrawUtil.drawRoundedRect(ctx.getMatrices(), tx, y, twd, TAB_H, BorderRadius.all(9.0F),
                        new ColorRGBA(255, 255, 255, 14));
            }
            // чип с буквой вместо иконки
            ColorRGBA chip = active ? new ColorRGBA(255, 255, 255, 255) : new ColorRGBA(205, 203, 213, 255);
            ctx.drawText(Fonts.SEMIBOLD.getFont(8.5F), s.title.substring(0, 1), tx + 12, y + 9, chip);
            ctx.drawText(Fonts.SEMIBOLD.getFont(8.5F), s.title, tx + 28, y + 9,
                    active ? ColorRGBA.WHITE : new ColorRGBA(215, 213, 222, 255));
            final Section tab = s;
            hits.add(new Hit(tx, y, twd, TAB_H, () -> {
                section = tab;
                scroll = 0.0F;
                scrollTarget = 0.0F;
                draggingPreview = false;
            }));
            y += TAB_H + TAB_GAP;
        }
    }

    private ColorRGBA tabBg() {
        try {
            ColorRGBA accent = Lumens.getInstance().getThemeManager().getCurrentTheme().getColor();
            return accent.mix(new ColorRGBA(18, 18, 24, 255), 0.30F);
        } catch (Exception e) {
            return new ColorRGBA(62, 56, 82, 255);
        }
    }

    private void renderHeader(CustomDrawContext ctx, ColorRGBA accent, int mouseX, int mouseY) {
        float hx = contentX + 16, hy = winY + 13;
        DrawUtil.drawRoundedRect(ctx.getMatrices(), hx - 3, hy - 1, 18, 18,
                BorderRadius.all(6.0F), accent.mulAlpha(0.16F));
        ctx.drawText(Fonts.SEMIBOLD.getFont(10.0F), section.title.substring(0, 1), hx + 1, hy + 1, accent);
        ctx.drawText(Fonts.SEMIBOLD.getFont(13.0F), section.title, hx + 22, hy, TEXT_MAIN);
        ctx.drawText(Fonts.REGULAR.getFont(7.0F), section.subtitle, hx, hy + 24, TEXT_DIM);
    }

    // ---------------- аксессуары ----------------

    private void renderAccessories(CustomDrawContext ctx, ColorRGBA accent, ColorRGBA cardBg,
                                   ColorRGBA tabActive, int mouseX, int mouseY, float delta) {
        Cosmetic.Slot slot = section == Section.HEAD ? Cosmetic.Slot.HEAD : Cosmetic.Slot.BACK;
        List<Cosmetic> list;
        String selected;
        try {
            list = Cosmetics.INSTANCE.listOf(slot);
            selected = slot == Cosmetic.Slot.HEAD ? Cosmetics.INSTANCE.getSelectedHead() : Cosmetics.INSTANCE.getSelectedBack();
        } catch (Exception e) {
            return;
        }
        float top = winY + 56;
        float listH = winY + winH - top - 12;
        float panelW = PREVIEW_W;
        float panelX = contentX + contentW - 12 - panelW;
        float panelTop = winY + 12;
        float panelH = winY + winH - panelTop - 12;
        float cardsX = contentX + 12;
        float cardsW = contentW - 24 - panelW - 8;
        float colW = (cardsW - CARD_GAP) / 2.0F;
        float cardH = 112.0F;

        float rows = (list.size() + 1) / 2.0F;
        float content = rows * (cardH + CARD_GAP);
        float max = Math.min(0.0F, -(content - listH));
        scrollTarget = MathHelper.clamp(scrollTarget, max, 0.0F);

        ctx.enableScissor((int) cardsX, (int) top, (int) (cardsX + cardsW), (int) (top + listH));
        if (list.isEmpty()) {
            String s = "Нет аксессуаров";
            String s2 = "Положи папку в assets/javelin/cosmetics/" + slot.getDir() + "/";
            float w = Fonts.REGULAR.getWidth(s, 9.0F);
            ctx.drawText(Fonts.REGULAR.getFont(9.0F), s, cardsX + cardsW / 2.0F - w / 2.0F, top + 30, TEXT_FAINT);
            float w2 = Fonts.REGULAR.getWidth(s2, 7.0F);
            ctx.drawText(Fonts.REGULAR.getFont(7.0F), s2, cardsX + cardsW / 2.0F - w2 / 2.0F, top + 44, TEXT_FAINT);
        }
        for (int i = 0; i < list.size(); i++) {
            Cosmetic c = list.get(i);
            float cx = cardsX + (i % 2) * (colW + CARD_GAP);
            float cy = top + (i / 2) * (cardH + CARD_GAP) + scroll;
            if (cy + cardH < top || cy > top + listH) continue;
            boolean sel = c.getId().equals(selected);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), cx, cy, colW, cardH, BorderRadius.all(10.0F),
                    sel ? cardBg.mix(accent, 0.22F) : cardBg);
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), cx, cy, colW, cardH, 0.6F, BorderRadius.all(10.0F),
                    sel ? accent : new ColorRGBA(255, 255, 255, 12));
            renderModelCard(ctx, c, cx + 6, cy + 4, colW - 12, cardH - 30);
            float nw = Fonts.REGULAR.getWidth(c.getName(), 8.0F);
            ctx.drawText(Fonts.REGULAR.getFont(8.0F), c.getName(), cx + colW / 2.0F - nw / 2.0F,
                    cy + cardH - 18, sel ? TEXT_MAIN : TEXT_DIM);
            final String id = c.getId();
            hits.add(new Hit(cx, cy, colW, cardH, () -> {
                try {
                    if (slot == Cosmetic.Slot.HEAD) Cosmetics.INSTANCE.selectHead(id);
                    else Cosmetics.INSTANCE.selectBack(id);
                } catch (Exception ignored) {
                }
            }));
        }
        ctx.disableScissor();

        renderPlayerPreview(ctx, accent, cardBg, panelX, panelTop, panelW, panelH, mouseX, mouseY, delta);
    }

    private void renderModelCard(CustomDrawContext ctx, Cosmetic c, float x, float y, float w, float h) {
        try {
            float[] b = boundsOf(c);
            float maxDim = Math.max(0.05F, Math.max(b[3] - b[0], Math.max(b[4] - b[1], b[5] - b[2])));
            float k = Math.min(w, h) * 0.70F / maxDim;
            float cx = x + w / 2.0F - ((b[0] + b[3]) / 2.0F) * k;
            float bottom = y + h - 6.0F - b[1] * k;
            MatrixStack matrices = ctx.getMatrices();
            matrices.push();
            matrices.translate(cx, bottom, 60.0F);
            matrices.scale(k, k, k);
            float yaw = (System.currentTimeMillis() / 40.0F) % 360.0F;
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            CosmeticRenderer.render(matrices, c.getTextureId(), c.getModel(), 1.0F, yaw, -12.0F);
            matrices.pop();
        } catch (Exception ignored) {
        }
    }

    /** Границы модели в блоках [minX,minY,minZ,maxX,maxY,maxZ]. */
    private static float[] boundsOf(Cosmetic c) {
        float minX = 99, minY = 99, minZ = 99, maxX = -99, maxY = -99, maxZ = -99;
        try {
            for (dev.lumens.client.cosmetics.CosmeticModel.Element e : c.getModel().getElements()) {
                minX = Math.min(minX, Math.min(e.from[0], e.to[0]));
                minY = Math.min(minY, e.from[1]);
                minZ = Math.min(minZ, Math.min(e.from[2], e.to[2]));
                maxX = Math.max(maxX, Math.max(e.from[0], e.to[0]));
                maxY = Math.max(maxY, e.to[1]);
                maxZ = Math.max(maxZ, Math.max(e.from[2], e.to[2]));
            }
        } catch (Exception ignored) {
        }
        if (minX > maxX) {
            minX = minY = minZ = 0;
            maxX = maxY = maxZ = 16;
        }
        float s = 1.0F / 16.0F;
        return new float[]{(minX - 8) * s, minY * s, (minZ - 8) * s, (maxX - 8) * s, maxY * s, (maxZ - 8) * s};
    }

    private void renderPlayerPreview(CustomDrawContext ctx, ColorRGBA accent, ColorRGBA cardBg,
                                     float px, float py, float pw, float ph,
                                     int mouseX, int mouseY, float delta) {
        DrawUtil.drawRoundedRect(ctx.getMatrices(), px, py, pw, ph, BorderRadius.all(10.0F), cardBg);
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), px, py, pw, ph, 0.6F, BorderRadius.all(10.0F),
                new ColorRGBA(255, 255, 255, 12));
        pvX = px;
        pvY = py;
        pvW = pw;
        pvH = ph;
        hits.add(new Hit(px, py, pw, ph, () -> {
        }));
        if (mc.player == null || mc.world == null) {
            String s = "Нет игрока";
            float w = Fonts.REGULAR.getWidth(s, 8.0F);
            ctx.drawText(Fonts.REGULAR.getFont(8.0F), s, px + pw / 2.0F - w / 2.0F, py + ph / 2.0F, TEXT_FAINT);
            return;
        }
        try {
            float boxH = (float) (mc.player.getBoundingBox().maxY - mc.player.getBoundingBox().minY);
            if (boxH < 0.5F) boxH = 1.8F;
            float k = (ph - 26.0F) / (boxH + 0.35F);
            float feetX = px + pw / 2.0F;
            float feetY = py + ph - 13.0F;
            // сохраняем повороты
            float oYaw = mc.player.getYaw(), oBody = mc.player.bodyYaw, oHead = mc.player.headYaw, oPitch = mc.player.getPitch();
            mc.player.setYaw(0.0F);
            mc.player.bodyYaw = 0.0F;
            mc.player.setHeadYaw(0.0F);
            mc.player.setPitch(dragPitch);
            MatrixStack matrices = ctx.getMatrices();
            matrices.push();
            matrices.translate(feetX, feetY, 60.0F);
            matrices.scale(k, k, k);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(dragYaw));
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            mc.getEntityRenderDispatcher().setRenderShadows(false);
            net.minecraft.client.render.VertexConsumerProvider.Immediate vcp =
                    mc.getBufferBuilders().getEntityVertexConsumers();
            mc.getEntityRenderDispatcher().render(mc.player, 0.0D, 0.0D, 0.0D, 0.0F, matrices,
                    vcp, 15728880);
            vcp.draw();
            mc.getEntityRenderDispatcher().setRenderShadows(true);
            // выбранный аксессуар на модели
            Cosmetic.Slot slot = section == Section.HEAD ? Cosmetic.Slot.HEAD : Cosmetic.Slot.BACK;
            Cosmetic sel = slot == Cosmetic.Slot.HEAD ? Cosmetics.INSTANCE.headCosmetic() : Cosmetics.INSTANCE.backCosmetic();
            if (sel != null) {
                if (slot == Cosmetic.Slot.HEAD) {
                    matrices.translate(0.0F, boxH, 0.0F);
                    CosmeticRenderer.render(matrices, sel.getTextureId(), sel.getModel(), 1.0F, 0.0F, dragPitch);
                } else {
                    matrices.translate(0.0F, 1.15F, -0.32F);
                    CosmeticRenderer.render(matrices, sel.getTextureId(), sel.getModel(), 1.0F, 180.0F, 0.0F);
                }
            }
            matrices.pop();
            mc.player.setYaw(oYaw);
            mc.player.bodyYaw = oBody;
            mc.player.setHeadYaw(oHead);
            mc.player.setPitch(oPitch);
            String s = "Тяни чтобы крутить";
            float w = Fonts.REGULAR.getWidth(s, 7.0F);
            ctx.drawText(Fonts.REGULAR.getFont(7.0F), s, px + pw / 2.0F - w / 2.0F, py + ph - 10, TEXT_FAINT);
        } catch (Exception ignored) {
        }
        final float hx = px, hy = py, hw = pw, hh = ph;
        hits.add(new Hit(hx, hy, hw, hh, () -> {
        }));
    }

    // ---------------- питомцы ----------------

    private void renderPets(CustomDrawContext ctx, ColorRGBA accent, ColorRGBA cardBg,
                            ColorRGBA tabActive, int mouseX, int mouseY, float delta) {
        ensurePetPreviews();
        String selected;
        try {
            selected = Cosmetics.INSTANCE.getSelectedPet();
        } catch (Exception e) {
            return;
        }
        float top = winY + 56;
        float listH = winY + winH - top - 12;
        float panelW = PREVIEW_W;
        float panelX = contentX + contentW - 12 - panelW;
        float panelTop = winY + 12;
        float panelH = winY + winH - panelTop - 12;
        float cardsX = contentX + 12;
        float cardsW = contentW - 24 - panelW - 8;
        float colW = (cardsW - CARD_GAP) / 2.0F;
        float cardH = 132.0F;

        Cosmetics.Pet[] pets = Cosmetics.Pet.values();
        ctx.enableScissor((int) cardsX, (int) top, (int) (cardsX + cardsW), (int) (top + listH));
        for (int i = 0; i < pets.length; i++) {
            Cosmetics.Pet p = pets[i];
            float cx = cardsX + (i % 2) * (colW + CARD_GAP);
            float cy = top + (i / 2) * (cardH + CARD_GAP) + scroll;
            boolean sel = p.getId().equals(selected);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), cx, cy, colW, cardH, BorderRadius.all(10.0F),
                    sel ? cardBg.mix(accent, 0.22F) : cardBg);
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), cx, cy, colW, cardH, 0.6F, BorderRadius.all(10.0F),
                    sel ? accent : new ColorRGBA(255, 255, 255, 12));
            renderPetCard(ctx, p, cx + 6, cy + 4, colW - 12, cardH - 30, delta);
            float nw = Fonts.REGULAR.getWidth(p.getName(), 8.0F);
            ctx.drawText(Fonts.REGULAR.getFont(8.0F), p.getName(), cx + colW / 2.0F - nw / 2.0F,
                    cy + cardH - 18, sel ? TEXT_MAIN : TEXT_DIM);
            final String id = p.getId();
            hits.add(new Hit(cx, cy, colW, cardH, () -> {
                try {
                    Cosmetics.INSTANCE.selectPet(id);
                } catch (Exception ignored) {
                }
            }));
        }
        ctx.disableScissor();

        // правая панель: большой питомец
        DrawUtil.drawRoundedRect(ctx.getMatrices(), panelX, panelTop, panelW, panelH, BorderRadius.all(10.0F), cardBg);
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), panelX, panelTop, panelW, panelH, 0.6F, BorderRadius.all(10.0F),
                new ColorRGBA(255, 255, 255, 12));
        pvX = panelX;
        pvY = panelTop;
        pvW = panelW;
        pvH = panelH;
        Cosmetics.Pet sel = Cosmetics.Pet.byId(selected);
        LivingEntity entity = sel == Cosmetics.Pet.DOG ? wolfPreview : catPreview;
        if (entity != null) {
            try {
                float yaw = dragYaw + (System.currentTimeMillis() / 60.0F) % 360.0F;
                float k = (panelH - 52.0F) / 1.15F;
                MatrixStack matrices = ctx.getMatrices();
                matrices.push();
                matrices.translate(panelX + panelW / 2.0F, panelTop + panelH - 30.0F, 60.0F);
                matrices.scale(k, k, k);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
                com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
                mc.getEntityRenderDispatcher().setRenderShadows(false);
                net.minecraft.client.render.VertexConsumerProvider.Immediate vcp2 =
                        mc.getBufferBuilders().getEntityVertexConsumers();
                mc.getEntityRenderDispatcher().render(entity, 0.0D, 0.0D, 0.0D, 0.0F, matrices,
                        vcp2, 15728880);
                vcp2.draw();
                mc.getEntityRenderDispatcher().setRenderShadows(true);
                matrices.pop();
            } catch (Exception ignored) {
            }
        }
        String name = sel == null ? "Не выбран" : sel.getName();
        float nw = Fonts.SEMIBOLD.getWidth(name, 10.0F);
        ctx.drawText(Fonts.SEMIBOLD.getFont(10.0F), name, panelX + panelW / 2.0F - nw / 2.0F, panelTop + 10, TEXT_MAIN);
        String st = sel == null ? "Выбери карточку" : "Следует за тобой";
        float sw = Fonts.REGULAR.getWidth(st, 7.0F);
        ctx.drawText(Fonts.REGULAR.getFont(7.0F), st, panelX + panelW / 2.0F - sw / 2.0F, panelTop + panelH - 14, TEXT_FAINT);
        hits.add(new Hit(panelX, panelTop, panelW, panelH, () -> {
        }));
    }

    private void renderPetCard(CustomDrawContext ctx, Cosmetics.Pet p, float x, float y, float w, float h, float delta) {
        LivingEntity entity = p == Cosmetics.Pet.DOG ? wolfPreview : catPreview;
        if (entity == null) return;
        try {
            float yaw = (System.currentTimeMillis() / 60.0F) % 360.0F;
            float k = (h - 10.0F) / 1.2F;
            MatrixStack matrices = ctx.getMatrices();
            matrices.push();
            matrices.translate(x + w / 2.0F, y + h - 4.0F, 60.0F);
            matrices.scale(k, k, k);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            mc.getEntityRenderDispatcher().setRenderShadows(false);
            net.minecraft.client.render.VertexConsumerProvider.Immediate vcp3 =
                    mc.getBufferBuilders().getEntityVertexConsumers();
            mc.getEntityRenderDispatcher().render(entity, 0.0D, 0.0D, 0.0D, 0.0F, matrices,
                    vcp3, 15728880);
            vcp3.draw();
            mc.getEntityRenderDispatcher().setRenderShadows(true);
            matrices.pop();
        } catch (Exception ignored) {
        }
    }

    private void ensurePetPreviews() {
        try {
            if (mc.world == null) return;
            if (previewWorld != mc.world || catPreview == null || wolfPreview == null) {
                catPreview = new CatEntity(EntityType.CAT, mc.world);
                wolfPreview = new WolfEntity(EntityType.WOLF, mc.world);
                previewWorld = mc.world;
            }
        } catch (Exception ignored) {
        }
    }

    // ---------------- ввод ----------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (draggingPreview) draggingPreview = false;
            // начали тянуть превью?
            if (isOverPreview(mouseX, mouseY)) {
                draggingPreview = true;
                return true;
            }
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (h.matches(mouseX, mouseY)) {
                    try {
                        h.action.run();
                    } catch (Exception ignored) {
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && draggingPreview) {
            dragYaw += (float) (deltaX * 0.6F);
            dragPitch = MathHelper.clamp(dragPitch + (float) (deltaY * 0.4F), -60.0F, 60.0F);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) draggingPreview = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isOverPreview(double mx, double my) {
        return pvW > 0 && MathUtil.isHovered(mx, my, pvX, pvY, pvW, pvH);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        float amt = (float) (v * 20.0);
        float top = winY + 56;
        float listH = winY + winH - top - 12;
        if (mouseX >= contentX && mouseX <= contentX + contentW && mouseY >= top && mouseY <= top + listH) {
            scrollTarget += amt;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, h, v);
    }

    @Override
    @Native
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            // закрыть только панель — модуль и питомцы остаются включены
            try {
                mc.setScreen(null);
            } catch (Exception ignored) {
            }
            return true;
        }
        try {
            if (keyCode == Menu.INSTANCE.getKeyCode()) {
                if (!Menu.INSTANCE.isEnabled()) Menu.INSTANCE.setToggled(true);
                else mc.setScreen(Lumens.getInstance().getMenuScreen());
                return true;
            }
        } catch (Exception ignored) {
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
