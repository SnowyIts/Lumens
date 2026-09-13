package dev.lumens.base.discord.callbacks;

import com.sun.jna.Callback;
import dev.lumens.base.discord.utils.DiscordUser;

public interface ReadyCallback extends Callback {
   void apply(DiscordUser var1);
}
