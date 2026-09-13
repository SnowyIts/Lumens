package dev.lumens.client.cosmetics;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Map;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

/**
 * Рисует CosmeticModel текстурированными кубоидами.
 * Вызывающий готовит матрицы (translate к якорю + scale).
 * size — размер 16 модельных единиц в текущих единицах (1.0 = блоки в мире).
 */
public final class CosmeticRenderer {
    private CosmeticRenderer() {
    }

    public static void render(MatrixStack matrices, Identifier texture, CosmeticModel model,
                              float size, float yawDeg, float pitchDeg) {
        if (model == null || model.isEmpty()) return;
        matrices.push();
        try {
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(yawDeg));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(pitchDeg));
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, texture);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            float tw = Math.max(1, model.getTextureWidth());
            float th = Math.max(1, model.getTextureHeight());
            for (CosmeticModel.Element e : model.getElements()) {
                float x0 = (Math.min(e.from[0], e.to[0]) - 8.0F) / 16.0F * size;
                float y0 = (e.from[1]) / 16.0F * size;
                float z0 = (Math.min(e.from[2], e.to[2]) - 8.0F) / 16.0F * size;
                float x1 = (Math.max(e.from[0], e.to[0]) - 8.0F) / 16.0F * size;
                float y1 = (e.to[1]) / 16.0F * size;
                float z1 = (Math.max(e.from[2], e.to[2]) - 8.0F) / 16.0F * size;
                float[] o = new float[]{
                        (e.rotOrigin[0] - 8.0F) / 16.0F * size,
                        (e.rotOrigin[1]) / 16.0F * size,
                        (e.rotOrigin[2] - 8.0F) / 16.0F * size};
                boolean hasRot = Math.abs(e.rotAngle) > 0.001F;
                for (Map.Entry<String, CosmeticModel.Face> fe : e.faces.entrySet()) {
                    CosmeticModel.Face f = fe.getValue();
                    float u1 = f.u1 / tw, v1 = f.v1 / th, u2 = f.u2 / tw, v2 = f.v2 / th;
                    float shade = shadeOf(fe.getKey());
                    int r = (int) (255 * shade), g = (int) (255 * shade), b = (int) (255 * shade);
                    float[][] corners = cornersOf(fe.getKey(), x0, y0, z0, x1, y1, z1);
                    float[][] uvs = {{u1, v1}, {u2, v1}, {u2, v2}, {u1, v2}};
                    for (int i = 0; i < 4; i++) {
                        float[] c = corners[i];
                        if (hasRot) c = rotate(c, o, e.rotAxis, e.rotAngle);
                        buffer.vertex(matrix, c[0], c[1], c[2]).texture(uvs[i][0], uvs[i][1]).color(r, g, b, 255);
                    }
                }
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } catch (Exception ignored) {
        } finally {
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            matrices.pop();
        }
    }

    private static float shadeOf(String dir) {
        switch (dir) {
            case "up": return 1.0F;
            case "down": return 0.5F;
            case "north":
            case "south": return 0.8F;
            case "west":
            case "east": return 0.65F;
            default: return 0.9F;
        }
    }

    private static float[][] cornersOf(String dir, float x0, float y0, float z0, float x1, float y1, float z1) {
        switch (dir) {
            case "down":
                return new float[][]{{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}};
            case "up":
                return new float[][]{{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}};
            case "north":
                return new float[][]{{x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}, {x1, y1, z0}};
            case "south":
                return new float[][]{{x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}};
            case "west":
                return new float[][]{{x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}, {x0, y1, z0}};
            case "east":
                return new float[][]{{x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}};
            default:
                return new float[][]{{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}};
        }
    }

    private static float[] rotate(float[] c, float[] o, String axis, float angleDeg) {
        float a = angleDeg * (float) (Math.PI / 180.0);
        float cos = MathHelper.cos(a);
        float sin = MathHelper.sin(a);
        float x = c[0] - o[0], y = c[1] - o[1], z = c[2] - o[2];
        float nx = x, ny = y, nz = z;
        if ("x".equalsIgnoreCase(axis)) {
            ny = y * cos - z * sin;
            nz = y * sin + z * cos;
        } else if ("z".equalsIgnoreCase(axis)) {
            nx = x * cos - y * sin;
            ny = x * sin + y * cos;
        } else {
            nx = x * cos + z * sin;
            nz = -x * sin + z * cos;
        }
        return new float[]{nx + o[0], ny + o[1], nz + o[2]};
    }
}
