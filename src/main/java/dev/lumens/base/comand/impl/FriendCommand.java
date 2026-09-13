package dev.lumens.base.comand.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import dev.lumens.Lumens;
import dev.lumens.base.comand.api.CommandAbstract;
import dev.lumens.base.comand.impl.args.FriendArgumentType;
import dev.lumens.base.comand.impl.args.PlayerArgumentType;
import dev.lumens.utility.game.other.MessageUtil;

public class FriendCommand extends CommandAbstract {
   public FriendCommand() {
      super("friend");
   }

   @Native
   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(literal("add").then(arg("player", PlayerArgumentType.create()).executes((context) -> {
         String name = (String)context.getArgument("player", String.class);
         String var10000;
         if (Lumens.getInstance().getFriendManager().getItems().contains(name)) {
            var10000 = String.valueOf(Formatting.GRAY);
            MessageUtil.displayInfo(var10000 + "Игрок с ником " + String.valueOf(Formatting.WHITE) + name + String.valueOf(Formatting.GRAY) + " уже добавлен в список друзей");
            return 1;
         } else {
            Lumens.getInstance().getFriendManager().add(name);
            var10000 = String.valueOf(Formatting.GRAY);
            MessageUtil.displayInfo(var10000 + "Игрок с ником " + String.valueOf(Formatting.WHITE) + name + String.valueOf(Formatting.GRAY) + " добавлен в список друзей");
            return 1;
         }
      })));
      builder.then(literal("remove").then(arg("player", FriendArgumentType.create()).executes((context) -> {
         String nickname = (String)context.getArgument("player", String.class);
         Lumens.getInstance().getFriendManager().removeFriend(nickname);
         String var10000 = String.valueOf(Formatting.GRAY);
         MessageUtil.displayInfo(var10000 + "Игрок с ником " + String.valueOf(Formatting.WHITE) + nickname + String.valueOf(Formatting.GRAY) + " удален из списка друзей");
         return 1;
      })));
      builder.then(literal("list").executes((commandContext) -> {
         MessageUtil.displayInfo(Lumens.getInstance().getFriendManager().getItems().toString());
         return 1;
      }));
   }
}
