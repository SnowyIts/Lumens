package dev.lumens.base.discord.utils;

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
import dev.lumens.base.discord.callbacks.DisconnectedCallback;
import dev.lumens.base.discord.callbacks.ErroredCallback;
import dev.lumens.base.discord.callbacks.JoinGameCallback;
import dev.lumens.base.discord.callbacks.JoinRequestCallback;
import dev.lumens.base.discord.callbacks.ReadyCallback;
import dev.lumens.base.discord.callbacks.SpectateGameCallback;

public class DiscordEventHandlers extends Structure {
   public DisconnectedCallback disconnected;
   public JoinRequestCallback joinRequest;
   public SpectateGameCallback spectateGame;
   public ReadyCallback ready;
   public ErroredCallback errored;
   public JoinGameCallback joinGame;

   protected List<String> getFieldOrder() {
      return Arrays.asList("ready", "disconnected", "errored", "joinGame", "spectateGame", "joinRequest");
   }

   public static class Builder {
      private final DiscordEventHandlers handlers = new DiscordEventHandlers();

      public DiscordEventHandlers build() {
         return this.handlers;
      }

      public DiscordEventHandlers.Builder disconnected(DisconnectedCallback callback) {
         this.handlers.disconnected = callback;
         return this;
      }

      public DiscordEventHandlers.Builder errored(ErroredCallback callback) {
         this.handlers.errored = callback;
         return this;
      }

      public DiscordEventHandlers.Builder ready(ReadyCallback callback) {
         this.handlers.ready = callback;
         return this;
      }

      public DiscordEventHandlers.Builder joinRequest(JoinRequestCallback callback) {
         this.handlers.joinRequest = callback;
         return this;
      }

      public DiscordEventHandlers.Builder joinGame(JoinGameCallback callback) {
         this.handlers.joinGame = callback;
         return this;
      }

      public DiscordEventHandlers.Builder spectateGame(SpectateGameCallback callback) {
         this.handlers.spectateGame = callback;
         return this;
      }
   }
}
