package dev.lumens.client.modules.api;

import lombok.Generated;

public enum Category {
   COMBAT("Combat", "0"),
   MOVEMENT("Movement", "1"),
   RENDER("Render", "3"),
    PLAYER("Player", "P"),
    MISC("Other", "4"),
    CONFIG("Configs", "F"),
    THEMES("Themes", "T");

   private final String name;
   private final String icon;

   private Category(String name, String icon) {
      this.name = name;
      this.icon = icon;
   }

   @Generated
   public String getIcon() {
      return this.icon;
   }

   @Generated
   public String getName() {
      return this.name;
   }


   private static Category[] $values() {
      return new Category[]{COMBAT, MOVEMENT, RENDER, PLAYER, MISC, CONFIG, THEMES};
   }
}
