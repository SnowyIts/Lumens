package dev.lumens.base.discord;

import java.io.IOException;
import lombok.Generated;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.IPCUser;
import meteordevelopment.discordipc.RichPresence;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.util.Identifier;
import ru.nexusguard.protection.annotations.Native;
import dev.lumens.Lumens;
import dev.lumens.utility.render.display.BufferUtil;

public class DiscordManager {
   public static final String APPLICATION_ID = "1546229273835208744";
   public static final String VERSION = "Free";
   private static final long APP_ID = 1546229273835208744L;

   private final DiscordManager.DiscordDaemonThread discordDaemonThread = new DiscordManager.DiscordDaemonThread();
   private volatile boolean running = true;
   private volatile boolean ready;
   private DiscordManager.DiscordInfo info = new DiscordManager.DiscordInfo("Unknown", "", "");
   private Identifier avatarId;
   private long startTime = System.currentTimeMillis() / 1000L;

   public DiscordManager() {
      this.initRPC();
   }

   @Native
   private void initRPC() {
      try {
         DiscordIPC.setOnError((code, message) -> this.setReady(false));
         this.tryConnect();
         this.discordDaemonThread.start();
      } catch (Throwable t) {
         this.running = false;
      }
   }

   /** Подключение к локальному Discord через IPC-пайп. Чистый Java, без DLL. Можно вызывать повторно. */
   private boolean tryConnect() {
      try {
         if (DiscordIPC.isConnected()) {
            return true;
         }
         return DiscordIPC.start(APP_ID, () -> {
            try {
               IPCUser user = DiscordIPC.getUser();
               if (user != null) {
                  String avatar = user.avatar != null && !user.avatar.isEmpty()
                          ? "https://cdn.discordapp.com/avatars/" + user.id + "/" + user.avatar + ".png" : "";
                  this.setInfo(new DiscordManager.DiscordInfo(user.username, avatar, user.id));
               }
            } catch (Throwable ignored) {
            }
            this.markReady();
            this.updatePresence();
         });
      } catch (Throwable t) {
         return false;
      }
   }

   public void markReady() {
      this.ready = true;
   }

   public void setReady(boolean ready) {
      this.ready = ready;
   }

   /** Обновление присутствия, вызывается из фонового потока. */
   public void updatePresence() {
      try {
         if (!ready || !DiscordIPC.isConnected()) return;
         String nick;
         try {
            nick = MinecraftClient.getInstance().getSession().getUsername();
         } catch (Exception e) {
            nick = "Player";
         }
         if (nick == null || nick.isEmpty()) nick = "Player";
         RichPresence presence = new RichPresence();
         presence.setDetails(resolveLocation());
         presence.setLargeImage("logo", nick + " | Version: " + VERSION);
         presence.setStart(startTime);
         DiscordIPC.setActivity(presence);
      } catch (Exception ignored) {
      }
   }

   /** Где сейчас игрок: Main Menu / MultiPlayer / SinglePlayer / IP сервера. */
   private String resolveLocation() {
      try {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.world != null || mc.player != null) {
            if (mc.isInSingleplayer()) {
               return "SinglePlayer";
            }
            if (mc.getCurrentServerEntry() != null
                    && mc.getCurrentServerEntry().address != null
                    && !mc.getCurrentServerEntry().address.isEmpty()) {
               return mc.getCurrentServerEntry().address;
            }
            if (Lumens.getInstance().getServerHandler() != null
                    && Lumens.getInstance().getServerHandler().getServer() != null) {
               return Lumens.getInstance().getServerHandler().getServer();
            }
            return "MultiPlayer";
         }
         if (mc.currentScreen instanceof MultiplayerScreen) {
            return "MultiPlayer";
         }
         return "Main Menu";
      } catch (Exception e) {
         return "Main Menu";
      }
   }

   @Native
   public void stopRPC() {
      this.running = false;
      try {
         DiscordIPC.stop();
      } catch (Throwable ignored) {
      }
   }

   @Native
   public void load() throws IOException {
      if (this.avatarId == null && !this.info.avatarUrl.isEmpty()) {
         this.avatarId = BufferUtil.registerDynamicTexture("avatar-", BufferUtil.getHeadFromURL(this.info.avatarUrl));
      }
   }

   @Generated
   public void setRunning(boolean running) {
      this.running = running;
   }

   @Generated
   public void setInfo(DiscordManager.DiscordInfo info) {
      this.info = info;
   }

   @Generated
   public void setAvatarId(Identifier avatarId) {
      this.avatarId = avatarId;
   }

   @Generated
   public DiscordManager.DiscordDaemonThread getDiscordDaemonThread() {
      return this.discordDaemonThread;
   }

   @Generated
   public boolean isRunning() {
      return this.running;
   }

   @Generated
   public DiscordManager.DiscordInfo getInfo() {
      return this.info;
   }

   @Generated
   public Identifier getAvatarId() {
      return this.avatarId;
   }

   private class DiscordDaemonThread extends Thread {
      @Native
      public void run() {
         this.setName("Discord-RPC");
         int ticks = 0;
         try {
            while (DiscordManager.this.running) {
               try {
                  if (!DiscordIPC.isConnected()) {
                     // Discord могли запустить уже после игры — пробуем переподключиться
                     DiscordManager.this.setReady(false);
                     DiscordManager.this.tryConnect();
                  } else {
                     try {
                        DiscordManager.this.load();
                     } catch (Exception ignored) {
                     }
                     // Presence обновляем каждые ~16с + сразу после ready
                     if (++ticks % 2 == 0) {
                        try {
                           DiscordManager.this.updatePresence();
                        } catch (Exception ignored) {
                        }
                     }
                  }
               } catch (Throwable ignored) {
               }
               Thread.sleep(8000L);
            }
         } catch (Throwable t) {
            DiscordManager.this.stopRPC();
         }
         super.run();
      }
   }

   public static record DiscordInfo(String userName, String avatarUrl, String userId) {
      public DiscordInfo(String userName, String avatarUrl, String userId) {
         this.userName = userName;
         this.avatarUrl = avatarUrl;
         this.userId = userId;
      }

      public String userName() {
         return this.userName;
      }

      public String avatarUrl() {
         return this.avatarUrl;
      }

      public String userId() {
         return this.userId;
      }
   }
}
