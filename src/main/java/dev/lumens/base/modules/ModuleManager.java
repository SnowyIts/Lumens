package dev.lumens.base.modules;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.option.Perspective;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.util.math.MathHelper;
import dev.lumens.Lumens;
import dev.lumens.base.events.impl.input.EventKey;
import dev.lumens.base.events.impl.other.EventGameUpdate;
import dev.lumens.base.events.impl.render.EventHudRender;
import dev.lumens.base.events.impl.server.EventPacket;
import dev.lumens.base.macro.Macro;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.setting.Setting;
import dev.lumens.client.modules.api.setting.impl.BooleanSetting;
import dev.lumens.client.modules.api.setting.impl.ModeSetting;
import dev.lumens.client.modules.api.setting.impl.MultiBooleanSetting;
import dev.lumens.client.modules.impl.combat.AntiBot;
import dev.lumens.client.modules.impl.combat.Aura;
import dev.lumens.client.modules.impl.combat.AutoSwap;
import dev.lumens.client.modules.impl.combat.AutoTotem;
import dev.lumens.client.modules.impl.combat.TriggerBot;
import dev.lumens.client.modules.impl.combat.ClickPearl;
import dev.lumens.client.modules.impl.combat.SprintReset;
import dev.lumens.client.modules.impl.misc.AHHelper;
import dev.lumens.client.modules.impl.misc.AutoAccept;
import dev.lumens.client.modules.impl.misc.AutoRespawn;
import dev.lumens.client.modules.impl.misc.ClickAction;
import dev.lumens.client.modules.impl.misc.ElytraHelper;
import dev.lumens.client.modules.impl.misc.FreeCam;
import dev.lumens.client.modules.impl.misc.ItemScroller;
import dev.lumens.client.modules.impl.misc.NameProtect;
import dev.lumens.client.modules.impl.misc.NoInteract;
import dev.lumens.client.modules.impl.misc.ScoreboardHealth;
import dev.lumens.client.modules.impl.misc.ServerHelper;
import dev.lumens.client.modules.impl.movement.AirStuck;
import dev.lumens.client.modules.impl.movement.Sprint;
import dev.lumens.client.modules.impl.movement.ElytraAccelerate;
import dev.lumens.client.modules.impl.movement.ElytraBooster;
import dev.lumens.client.modules.impl.movement.ElytraMotion;
import dev.lumens.client.modules.impl.movement.ElytraRecast;
import dev.lumens.client.modules.impl.movement.GuiWalk;
import dev.lumens.client.modules.impl.movement.NoSlow;
import dev.lumens.client.modules.impl.movement.NoWeb;
import dev.lumens.client.modules.impl.movement.Speed;
import dev.lumens.client.modules.impl.player.AntiAFK;
import dev.lumens.client.modules.impl.player.AutoArmor;
import dev.lumens.client.modules.impl.player.AutoPotion;
import dev.lumens.client.modules.impl.player.AutoTool;
import dev.lumens.client.modules.impl.player.Blink;
import dev.lumens.client.modules.impl.player.Cosmetics;
import dev.lumens.client.modules.impl.player.FastBreak;
import dev.lumens.client.modules.impl.player.NoDelay;
import dev.lumens.client.modules.impl.player.NoPush;
import dev.lumens.client.modules.impl.render.AntiInvisible;
import dev.lumens.client.modules.impl.render.AspectRatio;
import dev.lumens.client.modules.impl.render.ChinaHat;
import dev.lumens.client.modules.impl.render.Crosshair;
import dev.lumens.client.modules.impl.render.CustomFog;
import dev.lumens.client.modules.impl.render.EntityESP;
import dev.lumens.client.modules.impl.render.FOV;
import dev.lumens.client.modules.impl.render.FreeLook;
import dev.lumens.client.modules.impl.render.FPSBoost;
import dev.lumens.client.modules.impl.render.FullBright;
import dev.lumens.client.modules.impl.render.HitMarker;
import dev.lumens.client.modules.impl.render.Interface;
import dev.lumens.client.modules.impl.render.JumpCircles;
import dev.lumens.client.modules.impl.render.Menu;
import dev.lumens.client.modules.impl.render.NoRender;
import dev.lumens.client.modules.impl.render.Predictions;
import dev.lumens.client.modules.impl.render.StorageESP;
import dev.lumens.client.modules.impl.render.SwingAnimation;
import dev.lumens.client.modules.impl.render.TargetESP;
import dev.lumens.client.modules.impl.render.Tracers;
import dev.lumens.client.modules.impl.render.ViewModel;
import dev.lumens.client.modules.impl.render.WorldTime;
import dev.lumens.client.screens.menu.MenuScreen;
import dev.lumens.utility.component.RotationComponent;
import dev.lumens.utility.game.player.rotation.Rotation;
import dev.lumens.utility.interfaces.IMinecraft;

public final class ModuleManager implements IMinecraft {
   private final List<Module> modules = new ArrayList<>();
   private boolean isBack;
   private boolean isRotated;
   private float acceleration;

   private long lastKeyPressTime = 0;
   private int lastKeyCode = -1;
   private static final long DEBOUNCE_THRESHOLD_MS = 200;

   public ModuleManager() {
      init();
      EventManager.register(this);
   }

   private void init() {
      registerCombat();
      registerMovement();
      registerRender();
      registerPlayer();
      registerMisc();
   }

private void registerCombat() {
       registerModule(AntiBot.INSTANCE);
       registerModule(Aura.INSTANCE);
       registerModule(AutoSwap.INSTANCE);
       registerModule(AutoTotem.INSTANCE);
       registerModule(ClickPearl.INSTANCE);
       registerModule(TriggerBot.INSTANCE);
       registerModule(SprintReset.INSTANCE);
    }

private void registerMovement() {
       registerModule(Sprint.INSTANCE);
       registerModule(ElytraBooster.INSTANCE);
       registerModule(ElytraRecast.INSTANCE);
       registerModule(GuiWalk.INSTANCE);
       registerModule(NoSlow.INSTANCE);
       registerModule(Speed.INSTANCE);
       registerModule(AirStuck.INSTANCE);
       registerModule(ElytraMotion.INSTANCE);
       registerModule(NoWeb.INSTANCE);
    }

private void registerRender() {
       registerModule(Interface.INSTANCE);
       registerModule(AntiInvisible.INSTANCE);
       registerModule(Menu.INSTANCE);
       registerModule(NoRender.INSTANCE);
       registerModule(Predictions.INSTANCE);
       registerModule(SwingAnimation.INSTANCE);
       registerModule(Crosshair.INSTANCE);
       registerModule(ViewModel.INSTANCE);
       registerModule(CustomFog.INSTANCE);
       registerModule(FullBright.INSTANCE);
       registerModule(FPSBoost.INSTANCE);
       registerModule(WorldTime.INSTANCE);
       registerModule(EntityESP.INSTANCE);
       registerModule(TargetESP.INSTANCE);
       registerModule(FreeLook.INSTANCE);
       registerModule(AspectRatio.INSTANCE);
       registerModule(Tracers.INSTANCE);
       registerModule(StorageESP.INSTANCE);
       registerModule(JumpCircles.INSTANCE);
       registerModule(ChinaHat.INSTANCE);
       registerModule(HitMarker.INSTANCE);
       registerModule(FOV.INSTANCE);
    }

private void registerPlayer() {
       registerModule(AutoTool.INSTANCE);
       registerModule(AutoArmor.INSTANCE);
       registerModule(Blink.INSTANCE);
       registerModule(NoDelay.INSTANCE);
       registerModule(FastBreak.INSTANCE);
       registerModule(NoPush.INSTANCE);
       registerModule(AutoPotion.INSTANCE);
       registerModule(AntiAFK.INSTANCE);
       registerModule(Cosmetics.INSTANCE);
    }

   private void registerMisc() {
      registerModule(ServerHelper.INSTANCE);
      registerModule(ElytraHelper.INSTANCE);
      registerModule(ItemScroller.INSTANCE);
      registerModule(ClickAction.INSTANCE);
      registerModule(FreeCam.INSTANCE);
      registerModule(AHHelper.INSTANCE);
      registerModule(NoInteract.INSTANCE);
      registerModule(AutoAccept.INSTANCE);
      registerModule(AutoRespawn.INSTANCE);
      registerModule(NameProtect.INSTANCE);
      registerModule(ScoreboardHealth.INSTANCE);
      registerModule(ElytraAccelerate.INSTANCE);
   }

   private void registerModule(Module module) {
      modules.add(module);
   }

   public Module getModule(String name) {
      return modules.stream()
              .filter(module -> module.getName().equalsIgnoreCase(name))
              .findFirst()
              .orElse(null);
   }

   public Set<Module> getActiveModules() {
      Set<Module> active = new HashSet<>();
      for (Module module : modules) {
         if (module.isEnabled()) {
            active.add(module);
         }
      }
      return active;
   }

   @EventTarget
   public void onKey(EventKey event) {
      if (mc.currentScreen == null && event.getAction() == 1) {
         int keyCode = event.getKeyCode();
         long currentTime = System.currentTimeMillis();
         
         if (keyCode == lastKeyCode && (currentTime - lastKeyPressTime) < DEBOUNCE_THRESHOLD_MS) {
            return;
         }
         
         lastKeyCode = keyCode;
         lastKeyPressTime = currentTime;
         
         for (Module module : modules) {
            if (module.getKeyCode() == keyCode && module.getKeyCode() != -1) {
               module.toggle();
            }
         }

         for (Macro macro : Lumens.getInstance().getMacroManager().getItems()) {
            if (keyCode == macro.getBind()) {
               mc.getNetworkHandler().sendChatMessage(macro.getText());
            }
         }
      }
   }

   @EventTarget
   public void onRender(EventHudRender e) {
      Lumens.getInstance().getThemeManager().getCurrentTheme().getAnimation().update(1.0F);

      for (Module module : modules) {
         module.getAnimation().update(module.isEnabled());

         for (Setting setting : module.getSettings()) {
            if (setting instanceof BooleanSetting booleanSetting) {
               booleanSetting.getAnimation().update(booleanSetting.isEnabled());
            } else if (setting instanceof ModeSetting modeSetting) {
               for (ModeSetting.Value value : modeSetting.getValues()) {
                  value.getAnimation().update(value.isSelected());
               }
            } else if (setting instanceof MultiBooleanSetting multiBooleanSetting) {
               for (MultiBooleanSetting.Value value : multiBooleanSetting.getBooleanSettings()) {
                  value.getAnimation().update(value.isEnabled());
               }
            }
         }
      }

      MenuScreen menuScreen = Lumens.getInstance().getMenuScreen();
      if (menuScreen.needToClose) {
         if (menuScreen.savedRunnable != null) {
            menuScreen.savedRunnable.run();
         }

         if (menuScreen.openAnimationMetanoise.getValue() <= 0.27F) {
            menuScreen.savedRunnable = null;
            menuScreen.needToClose = false;
            menuScreen.openAnimationMetanoise.setValue(0.0F);
            menuScreen.openAnimationMetanoise.setStartValue(0.0F);
         }
      }
   }

   @EventTarget
   private void onPacket(EventPacket e) {
      if (e.getPacket() instanceof CloseScreenS2CPacket && mc.currentScreen instanceof MenuScreen) {
         e.cancel();
      }
   }

   @EventTarget
   private void onGameUpdate(EventGameUpdate e) {
      if (mc.player == null) return;

      if (!Aura.INSTANCE.isEnabled() || Aura.INSTANCE.getTarget() == null) {
         float cameraYaw = mc.gameRenderer.getCamera().getYaw();
         float cameraPitch = mc.gameRenderer.getCamera().getPitch();

         if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
            Aura.INSTANCE.lastYaw = cameraYaw - 180.0F;
            Aura.INSTANCE.lastPitch = -cameraPitch;
         } else {
            Aura.INSTANCE.lastYaw = cameraYaw;
            Aura.INSTANCE.lastPitch = cameraPitch;
         }

         if (Aura.INSTANCE.rotationMode.is("Vanilla")) {
            return;
         }

         Rotation current = new Rotation(mc.player.getYaw(), mc.player.getPitch());
         float deltaYaw = MathHelper.wrapDegrees(cameraYaw - current.getYaw());
         float deltaPitch = cameraPitch - current.getPitch();

         if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
            deltaYaw = MathHelper.wrapDegrees(cameraYaw - 180.0F - current.getYaw());
            deltaPitch = -cameraPitch - current.getPitch();
         }

         acceleration += 0.0024F;
         float smooth = MathHelper.clamp(acceleration, 0.0F, 1.0F);
         float newYaw = current.getYaw() + deltaYaw * smooth;
         float newPitch = current.getPitch() + deltaPitch * (smooth / 2.0F);

         Rotation smoothRot = new Rotation(newYaw, newPitch);
         RotationComponent.update(smoothRot, 360.0F, 360.0F, 360.0F, 360.0F, 0, 2, false);
      }
   }


   public List<Module> getModules() {
      return modules;
   }

   public boolean isBack() {
      return isBack;
   }

   public boolean isRotated() {
      return isRotated;
   }

   public float getAcceleration() {
      return acceleration;
   }

   public void setBack(boolean isBack) {
      this.isBack = isBack;
   }

   public void setRotated(boolean isRotated) {
      this.isRotated = isRotated;
   }

   public void setAcceleration(float acceleration) {
      this.acceleration = acceleration;
   }
}