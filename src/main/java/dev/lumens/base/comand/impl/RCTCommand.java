package dev.lumens.base.comand.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import dev.lumens.Lumens;
import dev.lumens.base.comand.api.CommandAbstract;
import dev.lumens.base.notify.NotifyManager;
import dev.lumens.base.repository.RCTRepository;
import dev.lumens.utility.game.server.ServerHandler;
import dev.lumens.utility.interfaces.IClient;

public class RCTCommand extends CommandAbstract implements IClient {
   private final RCTRepository repository = Lumens.getInstance().getRCTRepository();

   public RCTCommand() {
      super("rct");
   }

   @Native
   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes((context) -> {
         ServerHandler serverHandler = Lumens.getInstance().getServerHandler();
         if (!serverHandler.isHolyWorld()) {
            NotifyManager.getInstance().addNotification("[RCT]", Text.literal(" Не работает на этом " + String.valueOf(Formatting.RED) + "сервере"));
            return 1;
         } else if (serverHandler.isPvp()) {
            NotifyManager.getInstance().addNotification("️[RCT]", Text.literal(" Вы находитесь в режиме " + String.valueOf(Formatting.RED) + "пвп"));
            return 1;
         } else {
            this.repository.reconnect(serverHandler.getAnarchy());
            return 1;
         }
      });
      builder.then(CommandAbstract.arg("anarchy", IntegerArgumentType.integer(1, 63)).executes((context) -> {
         ServerHandler serverHandler = Lumens.getInstance().getServerHandler();
         if (!serverHandler.isHolyWorld()) {
            NotifyManager.getInstance().addNotification("[RCT]", Text.literal(" Не работает на этом " + String.valueOf(Formatting.RED) + "сервере"));
            return 1;
         } else if (serverHandler.isPvp()) {
            NotifyManager.getInstance().addNotification("[RCT]️", Text.literal(" Вы находитесь в режиме " + String.valueOf(Formatting.RED) + "пвп"));
            return 1;
         } else {
            int anarchy = (Integer)context.getArgument("anarchy", Integer.class);
            this.repository.reconnect(anarchy);
            return 1;
         }
      }));
   }
}
