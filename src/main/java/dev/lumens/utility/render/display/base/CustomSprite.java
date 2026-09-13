package dev.lumens.utility.render.display.base;

import lombok.Generated;
import net.minecraft.util.Identifier;
import dev.lumens.Lumens;

public class CustomSprite {
   private final Identifier texture;

   public CustomSprite(String path) {
      if (path.contains(":")) {
         this.texture = Identifier.of(path);
      } else if (path.contains("/")) {
         this.texture = Lumens.id(path);
      } else {
         this.texture = Lumens.id("icons/category/" + path);
      }

   }

   @Generated
   public Identifier getTexture() {
      return this.texture;
   }
}
