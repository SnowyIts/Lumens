package dev.lumens.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.lumens.Lumens;
import dev.lumens.base.events.impl.input.EventSetScreen;
import dev.lumens.base.events.impl.other.EventGameUpdate;
import dev.lumens.base.events.impl.other.EventTick;

@Mixin({MinecraftClient.class})
public abstract class MinecraftClientMixin {
   @Unique
   private long lastHookTime = Util.getMeasuringTimeNano();
   @Unique
   private int accumulatedCalls = 0;
   @Unique
   private String javelin$lastTitle = null;
   @Unique
   private int javelin$titleCooldown = 0;
   @Shadow
   @Final
   private Window window;

   @Shadow
   public abstract Window getWindow();

   @Shadow
   @Nullable
   public ClientWorld world;

   @Shadow
   public abstract boolean isInSingleplayer();

   @Shadow
   public abstract ServerInfo getCurrentServerEntry();

   @Unique
   private static String javelin$cachedVersion;

   /** Версия мода из fabric.mod.json (без хардкода). */
   @Unique
   private static String javelin$version() {
      if (javelin$cachedVersion != null) return javelin$cachedVersion;
      String v = "1.0";
      try {
         v = FabricLoader.getInstance().getModContainer("javelin")
               .map(c -> c.getMetadata().getVersion().getFriendlyString())
               .orElse("1.0");
      } catch (Exception ignored) {
      }
      javelin$cachedVersion = v;
      return v;
   }

   /** "Lumens <версия> | SinglePlayer / Main Menu / <ip сервера>". */
   @Unique
   private String javelin$buildTitle() {
      String where;
      try {
         if (this.world == null) {
            where = "Main Menu";
         } else if (this.isInSingleplayer()) {
            where = "SinglePlayer";
         } else {
            String addr = null;
            try {
               ServerInfo info = this.getCurrentServerEntry();
               if (info != null) addr = info.address;
            } catch (Exception ignored) {
            }
            where = (addr == null || addr.isEmpty()) ? "Multiplayer" : addr;
         }
      } catch (Exception e) {
         where = "Main Menu";
      }
      return "Lumens " + javelin$version() + " | " + where;
   }

   @Unique
   private void javelin$refreshTitle() {
      try {
         String title = javelin$buildTitle();
         if (!title.equals(javelin$lastTitle)) {
            getWindow().setTitle(title);
            javelin$lastTitle = title;
         }
      } catch (Exception ignored) {
      }
   }

   @Inject(method = {"updateWindowTitle"}, at = {@At("HEAD")}, cancellable = true)
   private void javelin$customWindowTitle(CallbackInfo ci) {
      javelin$refreshTitle();
      ci.cancel();
   }

   @Inject(
      method = {"<init>"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/MinecraftClient$1;<init>(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/RunArgs;)V"
)}
   )
   public void init(RunArgs args, CallbackInfo ci) {
      Lumens.getInstance().init();
   }

   @Inject(
      method = {"onResolutionChanged"},
      at = {@At("TAIL")}
   )
   private void captureResize(CallbackInfo ci) {
   }

   @ModifyVariable(
      method = {"setScreen(Lnet/minecraft/client/gui/screen/Screen;)V"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private Screen mixin$modifySetScreenArg(Screen original) {
      EventSetScreen event = new EventSetScreen(original);
      EventManager.call(event);
      return event.getScreen();
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void render(boolean tick, CallbackInfo ci) {
      long now = Util.getMeasuringTimeNano();
      long delta = now - this.lastHookTime;
      this.accumulatedCalls += (int)(delta / 4166666L);
      this.lastHookTime += (long)this.accumulatedCalls * 4166666L;

      for(this.accumulatedCalls = Math.min(this.accumulatedCalls, 240); this.accumulatedCalls > 0; --this.accumulatedCalls) {
         EventManager.call(new EventGameUpdate());
      }

   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   public void tick(CallbackInfo ci) {
      EventTick event = new EventTick();
      EventManager.call(event);
      // заголовок мог устареть (заход/выход с сервера) — обновляем раз в секунду
      if (++javelin$titleCooldown >= 20) {
         javelin$titleCooldown = 0;
         javelin$refreshTitle();
      }
   }
}
