package dev.lumens.base.discord.callbacks;

import com.sun.jna.Callback;
import dev.lumens.base.discord.utils.DiscordUser;

public interface JoinRequestCallback extends Callback {
   void apply(DiscordUser var1);
}
