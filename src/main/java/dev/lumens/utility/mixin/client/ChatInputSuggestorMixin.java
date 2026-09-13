package dev.lumens.utility.mixin.client;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.ChatInputSuggestor.SuggestionWindow;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import dev.lumens.Lumens;

@Mixin({ChatInputSuggestor.class})
public abstract class ChatInputSuggestorMixin {
   @Final
   @Shadow
   TextFieldWidget textField;
   @Shadow
   boolean completingSuggestions;
   @Shadow
   private ParseResults<CommandSource> parse;
   @Shadow
   private CompletableFuture<Suggestions> pendingSuggestions;
   @Shadow
   private SuggestionWindow window;

   @Shadow
   private void showCommandSuggestions() {
      throw new AssertionError();
   }

   @Inject(
      method = {"refresh"},
      at = {@At(
   value = "INVOKE",
   target = "Lcom/mojang/brigadier/StringReader;canRead()Z",
   remap = false
)},
      cancellable = true,
      locals = LocalCapture.CAPTURE_FAILHARD
   )
   public void refreshHook(CallbackInfo ci, String string, StringReader reader) {
      if (reader.canRead(Lumens.getInstance().getCommandManager().getPrefix().length()) && reader.getString().startsWith(Lumens.getInstance().getCommandManager().getPrefix(), reader.getCursor())) {
         reader.setCursor(reader.getCursor() + 1);
         if (this.parse == null) {
            this.parse = Lumens.getInstance().getCommandManager().getDispatcher().parse(reader, Lumens.getInstance().getCommandManager().getSource());
         }

         int cursor = this.textField.getCursor();
         if (cursor >= 1 && (this.window == null || !this.completingSuggestions)) {
            this.pendingSuggestions = Lumens.getInstance().getCommandManager().getDispatcher().getCompletionSuggestions(this.parse, cursor);
            this.pendingSuggestions.thenRun(() -> {
               if (this.pendingSuggestions.isDone()) {
                  this.showCommandSuggestions();
               }

            });
         }

         ci.cancel();
      }

   }
}
