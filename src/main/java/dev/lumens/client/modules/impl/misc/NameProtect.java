package dev.lumens.client.modules.impl.misc;

import java.util.Collection;
import java.util.Iterator;
import ru.nexusguard.protection.annotations.Native;
import dev.lumens.Lumens;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.BooleanSetting;

@ModuleAnnotation(
   name = "NameProtect",
   category = Category.MISC,
   description = "Защищает имена игроков"
)
public final class NameProtect extends Module {
   public static final NameProtect INSTANCE = new NameProtect();
   private final BooleanSetting hideFriends = new BooleanSetting("Скрыть друзей", false);

   private NameProtect() {
   }

   @Native
   public static String getCustomName() {
      Module module = INSTANCE;
      return module != null && module.isEnabled() ? "t.me/lumensdevblogs" : mc.player.getNameForScoreboard();
   }

   @Native
   public static String getCustomName(String originalName) {
      Module module = INSTANCE;
      if (module != null && module.isEnabled() && mc.player != null) {
         String me = mc.player.getNameForScoreboard();
         if (originalName.contains(me)) {
            return originalName.replace(me, "t.me/lumensdevblogs");
         } else {
            if (module instanceof NameProtect) {
               NameProtect nameProtect = (NameProtect)module;
               if (nameProtect.hideFriends.isEnabled()) {
                  Collection<String> friends = Lumens.getInstance().getFriendManager().getItems();
                  Iterator var5 = friends.iterator();

                  while(var5.hasNext()) {
                     String friend = (String)var5.next();
                     if (originalName.contains(friend)) {
                        return originalName.replace(friend, "t.me/lumensdevblogs");
                     }
                  }
               }
            }

            return originalName;
         }
      } else {
         return originalName;
      }
   }
}
