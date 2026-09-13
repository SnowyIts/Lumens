package dev.lumens.utility.render.display;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public class BufferUtil {
   public static Identifier registerDynamicTexture(String prefix, NativeImage image) {
      if (image == null) {
         return null;
      } else {
         Identifier id = Identifier.of("javelin", prefix + System.currentTimeMillis());
         MinecraftClient mc = MinecraftClient.getInstance();
         mc.execute(() -> {
            try {
               NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
               mc.getTextureManager().registerTexture(id, texture);
            } catch (Exception var4) {
               var4.printStackTrace();
            }

         });
         return id;
      }
   }

    public static NativeImage getHeadFromURL(String urlString) {
       try {
          URLConnection conn = new URL(urlString).openConnection();
          conn.setConnectTimeout(5000);
          conn.setReadTimeout(5000);
          conn.setRequestProperty("User-Agent", "Mozilla/5.0");
          try (InputStream stream = conn.getInputStream()) {
             return NativeImage.read(stream);
          }
       } catch (Exception var4) {
          return null;
       }
    }
}
