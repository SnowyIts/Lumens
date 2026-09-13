package dev.lumens.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import dev.lumens.Lumens;
import dev.lumens.base.events.impl.other.EventGameUpdate;
import dev.lumens.base.events.impl.other.EventTick;
import dev.lumens.base.events.impl.other.EventTickMovement;
import dev.lumens.base.events.impl.player.EventMoveInput;
import dev.lumens.base.events.impl.player.EventUpdate;
import dev.lumens.base.player.AttackUtil;
import dev.lumens.base.request.ScriptManager;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.BooleanSetting;
import dev.lumens.client.modules.api.setting.impl.ModeSetting;
import dev.lumens.client.modules.api.setting.impl.MultiBooleanSetting;
import dev.lumens.client.modules.api.setting.impl.NumberSetting;
import dev.lumens.client.modules.impl.movement.Sprint;
import dev.lumens.utility.component.RotationComponent;
import dev.lumens.utility.game.player.MovingUtil;
import dev.lumens.utility.game.player.PlayerInventoryUtil;
import dev.lumens.utility.game.player.RaytracingUtil;
import dev.lumens.utility.game.player.rotation.Rotation;
import dev.lumens.utility.game.player.rotation.RotationUtil;
import dev.lumens.utility.math.MultipointUtils;
import dev.lumens.utility.math.Timer;
import dev.lumens.utility.predict.PredictUtils;

/**
 * Aura — переписана по мотивам Marlboro AttackAura + улучшения:
 * - цели: Игроки/Друзья/Голые/Животные/Мобы + приоритет + сортировка (дистанция/хп/броня/комплекс)
 * - ротации: Vanilla / LonyGrief / CakeWorld / LegendsGrief / FunTime / Matrix
 * - криты: СТРОГО только криты (серверные условия), smart-режим ждёт падения,
 *   TPS-sync + кулдаун 450мс + рандом пауз в SpookyTime
 * - щит: legit/packet свап топора, отжим щита
 * - стены: raycast + multipoint, обход для элитр
 * - коррекция движения: Фокус/Свободная/Нет + предикт на элитре
 */
@ModuleAnnotation(
        name = "Aura",
        category = Category.COMBAT,
        description = "Автоматически бьет цель"
)
public final class Aura extends Module {
   public static final Aura INSTANCE = new Aura();

   // ---- цели (Marlboro targetOptions + sortMode) ----
   private final MultiBooleanSetting targetTypeSetting = MultiBooleanSetting.create("Атаковать",
           List.of("Игроков", "Друзья", "Голые", "Мобов", "Животных"));
   private final ModeSetting sortMode = new ModeSetting("Сортировка", "Всему сразу", "Дистанции", "Здоровью", "Броне", "Всему сразу");
   private final BooleanSetting targetPriority = new BooleanSetting("Приоритет цели", false);
   private String priorityName;

// ---- ротация (Marlboro rotationMode) ----
    public final ModeSetting rotationMode = new ModeSetting("Ротация", "Vanilla", "LonyGrief", "CakeWorld", "LegendsGrief", "FunTime", "Matrix", "SpookyTime", "SlothAC");
   private final ModeSetting correction;
   private final NumberSetting distance;
   private final NumberSetting distanceRotation;
   private final NumberSetting predictRange;

   // ---- бой ----
   private final BooleanSetting shieldBreak;
   private final BooleanSetting legitSwap;
   private final BooleanSetting raycastCheck;
   private final BooleanSetting predictOnElytra;
   public final NumberSetting predict;
private final BooleanSetting onlyCrit;
    private final BooleanSetting smartCrits;
    private final BooleanSetting tpsSync;
    private final BooleanSetting noWalls;
    private final BooleanSetting autoDisableOnDeath;
    private final BooleanSetting onlyWeapon;
    private final BooleanSetting noEatAttack;
    public final BooleanSetting critsOnlyWithSpace;

    // ---- sprint reset (Marlboro/SpookyAC) ----
    private final BooleanSetting smartSprintReset = new BooleanSetting("Умный сброс спринта", true);
    private final NumberSetting sprintResetTicks;

    private LivingEntity target;
    private float acceleration;
    private boolean isBack;
    private final Timer hurtTimer = new Timer();
    private final ScriptManager.ScriptTask script = new ScriptManager.ScriptTask();
    private int lastSlot = -1;
    private long critCooldownUntil;
    private float attackBonus;
    private int lastRand = 999;
    public float lastYaw;
    public float lastPitch;
    // Sprint reset state (Marlboro/SpookyAC)
    private boolean sprintResetArmed;
    private boolean sprintResetShouldRestore;
    private boolean sprintKeyForcedRelease;
    private int sprintResetReadyTick = Integer.MIN_VALUE;
    private int sprintResetExpireTick = Integer.MIN_VALUE;
    private long nextAttackAtMs;
    // SpookyTime STRICT: кап скорости, доводка раз в тик, живая точка аима
    public final NumberSetting spookySpeed = new NumberSetting("Скор. Spooky", 28.0F, 10.0F, 60.0F, 1.0F,
            () -> rotationMode.is("SpookyTime"));
    public final BooleanSetting spookyLegitHit = new BooleanSetting("Бить в прицел", true,
            () -> rotationMode.is("SpookyTime"));
    private boolean spookyActive;
    private float spookyYawErr = 180.0F;
    private float spookyPitchErr = 90.0F;
    private boolean spookyJumped;
    private LivingEntity spookyLastTarget;
    private int spookyLastAge = -1;
    private long spookyNextHit;
    private long spookyLastJump;
    // SPAngle-порт: мультипоинт прицела + живая скорость
    private final SecureRandom spSecure = new SecureRandom();
    private Vec3d spPoint;
    private long spPointAt;
    private int spBodyPart;
    private long spBodyAt;
    private LivingEntity spookyTickLast;
    private long spookyTargetAt;
    private long spookyReactionMs = 200L;
    // Marlboro: история скоростей для капа + потеря прицела
    private long lostAimAt = -1L;
    private final float[] speedHistory = new float[5];
    private int speedHistoryIndex = 0;
    private long lastHistoryTick = 0L;
    
    // SlothAC: HelixWave rotation state
    private boolean slothActive;
    private LivingEntity slothLastTarget;
    private final SecureRandom slothSecure = new SecureRandom();

private Aura() {
       this.correction = new ModeSetting("Коррекция", "Свободная", "Фокус", "Свободная", "Нет");
       this.distance = new NumberSetting("Дистанция", 3.0F, 0.5F, 6.0F, 0.1F, "Дистанция атаки");
       this.distanceRotation = new NumberSetting("Дистанция аима", 0.1F, 0.0F, 6.0F, 0.1F);
       this.predictRange = new NumberSetting("Предикт дистанция", 8.0F, 3.0F, 12.0F, 0.5F);
       this.shieldBreak = new BooleanSetting("Ломать щит", true);
       BooleanSetting sb = this.shieldBreak;
       Objects.requireNonNull(sb);
       this.legitSwap = new BooleanSetting("Легитно ломать", true, sb::isEnabled);
       this.raycastCheck = new BooleanSetting("Проверка на наведение", false);
       this.predictOnElytra = new BooleanSetting("Перегонять противника", true);
       this.predict = new NumberSetting("Насколько перегонять", 2.0F, 1.0F, 4.0F, 0.1F);
       this.onlyCrit = new BooleanSetting("Только криты", true);
       this.smartCrits = new BooleanSetting("Умные криты", false);
this.tpsSync = new BooleanSetting("TPS-Sync", false);
        this.noWalls = new BooleanSetting("Не бить через стены", true);
        this.autoDisableOnDeath = new BooleanSetting("Выкл после смерти", true);
        this.onlyWeapon = new BooleanSetting("Только с оружием", false);
        this.noEatAttack = new BooleanSetting("Не бить когда ешь", false);
        this.critsOnlyWithSpace = new BooleanSetting("Только с пробелом", true);
        this.sprintResetTicks = new NumberSetting("Тиков сброса спринта", 2.0F, 1.0F, 5.0F, 1.0F,
                () -> smartSprintReset.isEnabled() && onlyCrit.isEnabled());
        this.smartSprintReset.setEnabled(true);
        // Дефолт как в Marlboro: LonyGrief-подобное поведение вторым пресетом
        this.rotationMode.set("LonyGrief");
    }

    // Sprint reset constants (Marlboro/SpookyAC)
    private static final int SPRINT_RESET_MIN_TICKS = 1;
    private static final int SMART_SPRINT_RESET_MAX_ARM_TICKS = 5;
    private static final int SPRINT_RESET_ARM_TICKS = 2;
    private static final int SPRINT_RESET_MAX_WAIT_TICKS = 8;
    private static final float FULL_ATTACK_COOLDOWN = 0.91F;
    private static final float CRITICAL_ATTACK_COOLDOWN = 0.95F;
    private static final int COOLDOWN_LOOKAHEAD_TICKS = 20;

    // ---------------- tick: выбор цели + атака ----------------

    @EventTarget
    public void onTick(EventTick e) {
       if (autoDisableOnDeath.isEnabled() && mc.player != null && !mc.player.isAlive()) {
          setEnabled(false);
          return;
       }
       if (target == null || !isValid(target)) target = updateTarget();
       if (target == null) return;
       // Фиксируем момент появления новой цели — реакция как у живого (120-240мс)
       if (target != spookyTickLast) {
          spookyTickLast = target;
          spookyTargetAt = System.currentTimeMillis();
          spookyReactionMs = 120L + (long) (Math.random() * 120);
          lostAimAt = -1L; // Marlboro: сброс потери прицела при смене цели
       }

       // Sprint reset logic (Marlboro/SpookyAC)
       tickSprintResetTimeout(mc);

       // SpookyTime: сам подпрыгивает для крита, чтобы бить без помощи игрока
       spookyAutoJump();

       if (isCanAttack() && spookyAimReady() && spookyHitReady() && hurtTimer.finished(458L) && !target.isBlocking()) {
          // Use new sprint reset system instead of stopSprintForCrit
          if (!isMode("SpookyTime")) {
              handleSprintReset(mc);
          }
          mc.interactionManager.attackEntity(mc.player, target);
          mc.player.swingHand(Hand.MAIN_HAND);
          hurtTimer.reset();
          afterHit();
          nextAttackAtMs = System.currentTimeMillis() + 500L; // base for next attack window
          // SpookyTime: следующий удар со случайной паузой, не метроном
          if (isMode("SpookyTime")) {
             spookyNextHit = System.currentTimeMillis() + 500 + (long) (Math.random() * 250);
          }
       }
    }

   @EventTarget
   @Native
   public void onTickMovement(EventTickMovement e) {
if (target != null && target.isBlocking() && hurtTimer.finished(50L)) {
          if (!isMode("SpookyTime") && smartSprintReset.isEnabled() && onlyCrit.isEnabled()) {
              handleSprintReset(mc);
          } else if (!isMode("SpookyTime")) {
              stopSprintForCrit();
          }
          breakShieldAndAttack();
          hurtTimer.reset();
       }

    }

    /**
     * SpookyTime STRICT: рандомная пауза между ударами вместо метронома 458мс.
     * Для остальных режимов всегда true.
     */
   private boolean spookyHitReady() {
      if (!isMode("SpookyTime")) return true;
      return System.currentTimeMillis() >= spookyNextHit;
   }

   /**
    * SpookyTime: бьёт только когда наведение сошлось, иначе ждёт доводку.
    * Для остальных режимов всегда true.
    */
   private boolean spookyAimReady() {
      if (!isMode("SpookyTime")) return true;
      if (!spookyLegitHit.isEnabled()) return true;
      if (target == null) return true;
      if (spookyYawErr < 12.0F && spookyPitchErr < 10.0F) return true;
      try {
         return RaytracingUtil.rayTrace(mc.player.getRotationVector(),
                 distance.getCurrent() + distanceRotation.getCurrent() + 0.1, target.getBoundingBox());
      } catch (Exception e) {
         return true;
      }
   }

   /**
    * SpookyTime: сам подпрыгивает для крита когда кд готов и цель в радиусе,
    * чтобы аура била полностью сама без пробела от игрока.
    * Живой паттерн: паузы и редкие пропуски вместо баннихопа по таймеру.
    */
   private void spookyAutoJump() {
      if (!isMode("SpookyTime") || !onlyCrit.isEnabled() || mc.player == null || target == null) return;
      try {
         long now = System.currentTimeMillis();
         if (spookyJumped) {
            if (!mc.player.isOnGround() || mc.player.isUsingItem()) {
               mc.options.jumpKey.setPressed(false);
               spookyJumped = false;
            }
            return;
         }
         if (now - spookyLastJump < 650) return;
         if (Math.random() < 0.15) return; // иногда бьём с земли как легит
         if (mc.player.isOnGround() && !mc.options.jumpKey.isPressed() && !mc.player.isUsingItem()
                 && !mc.player.isSneaking() && hurtTimer.finished(400L)
                 && mc.player.getAttackCooldownProgress(0.5F) > 0.8F
                 && mc.player.distanceTo(target) <= distance.getCurrent() + 0.5F) {
            mc.options.jumpKey.setPressed(true);
            spookyJumped = true;
            spookyLastJump = now;
         }
      } catch (Exception ignored) {
      }
   }

private void stopSprintForCrit() {
       if (mc.player.isSprinting() && !mc.player.isOnGround() && !mc.player.isSwimming()) {
          mc.player.setSprinting(false);
          mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
          if (!Sprint.INSTANCE.isEnabled()) mc.options.sprintKey.setPressed(false);
       }
    }

    // New sprint reset system (Marlboro/SpookyAC)
    private void handleSprintReset(MinecraftClient client) {
        if (!smartSprintReset.isEnabled() || !onlyCrit.isEnabled() || client.player.isGliding()) {
            // Legacy behavior for non-smart or non-crit modes
            stopSprintForCrit();
            return;
        }

        int configuredLeadTicks = getConfiguredSprintResetTicks();
        int ticksUntilAttackWindow = getTicksUntilAttackWindow(client, System.currentTimeMillis());

        if (!sprintResetArmed) {
            if (ticksUntilAttackWindow <= configuredLeadTicks) {
                int waitTicks = Math.max(SPRINT_RESET_MIN_TICKS, ticksUntilAttackWindow);
                armSprintReset(client, waitTicks);
                if (waitTicks > 0) return;
            } else {
                finishSprintReset(client, true);
                return;
            }
        } else if (client.player.age < sprintResetReadyTick) {
            return;
        }

        boolean readyToAttack = ticksUntilAttackWindow <= 0 && canAttackNow(client);
        if (!readyToAttack) {
            maybeRestoreArmedSprint(client);
            return;
        }

        if (!prepareAttackTarget(client, target)) {
            finishSprintReset(client, true);
            return;
        }

        if (!sprintResetArmed && client.player.isSprinting()) {
            int waitTicks = configuredLeadTicks;
            armSprintReset(client, waitTicks);
            if (waitTicks > 0) return;
        }

        if (sprintResetArmed && client.player.age < sprintResetReadyTick) return;

        if (!client.player.isGliding()) {
            // AutoSprint suppression handled by manual sprint control
            client.player.setSprinting(false);
        }

        // Attack is performed by caller
        onAttackPerformed(client, System.currentTimeMillis());
        finishSprintReset(client, true);
    }

    private void armSprintReset(MinecraftClient client, int leadTicks) {
        if (client == null || client.player == null || client.player.isGliding()) return;
        leadTicks = MathHelper.clamp(leadTicks, SPRINT_RESET_MIN_TICKS, SMART_SPRINT_RESET_MAX_ARM_TICKS);

        sprintResetArmed = true;
        sprintResetShouldRestore = true;
        sprintResetReadyTick = client.player.age + leadTicks;
        sprintResetExpireTick = sprintResetReadyTick + SPRINT_RESET_MAX_WAIT_TICKS;

        // AutoSprint suppression handled by manual sprint control
        client.player.setSprinting(false);
        if (client.options != null) {
            client.options.sprintKey.setPressed(false);
            sprintKeyForcedRelease = true;
        }
    }

    private void tickSprintResetTimeout(MinecraftClient client) {
        if (!sprintResetArmed || client == null || client.player == null) return;
        if (client.player.age > sprintResetExpireTick) {
            finishSprintReset(client, true);
        }
    }

    private void maybeRestoreArmedSprint(MinecraftClient client) {
        if (!sprintResetArmed || client == null || client.player == null) return;

        if ((!smartSprintReset.isEnabled() || !onlyCrit.isEnabled()) && client.player.age >= sprintResetReadyTick) {
            finishSprintReset(client, true);
            return;
        }

        if (client.player.age > sprintResetExpireTick) {
            finishSprintReset(client, true);
        }
    }

    private int getTicksUntilAttackWindow(MinecraftClient client, long nowMs) {
        int delayTicks = msToTicksCeil(nextAttackAtMs - nowMs);
        float requiredCooldown = onlyCrit.isEnabled() ? CRITICAL_ATTACK_COOLDOWN : FULL_ATTACK_COOLDOWN;
        int cooldownTicks = getTicksUntilCooldownReady(client, requiredCooldown);
        int critTicks = getTicksUntilEarliestCritical(client);
        return Math.max(delayTicks, Math.max(cooldownTicks, critTicks));
    }

    private int getConfiguredSprintResetTicks() {
        if (sprintResetTicks == null) return SPRINT_RESET_ARM_TICKS;
        int value = Math.round(sprintResetTicks.getCurrent());
        if (value < SPRINT_RESET_MIN_TICKS) return SPRINT_RESET_MIN_TICKS;
        if (value > SMART_SPRINT_RESET_MAX_ARM_TICKS) return SMART_SPRINT_RESET_MAX_ARM_TICKS;
        return value;
    }

    private static int msToTicksCeil(long ms) {
        if (ms <= 0L) return 0;
        return (int) ((ms + 49L) / 50L);
    }

    private int getTicksUntilCooldownReady(MinecraftClient client, float requiredCooldown) {
        if (client == null || client.player == null) return 0;
        if (client.player.getAttackCooldownProgress(0.0f) >= requiredCooldown) return 0;
        for (int ticks = 1; ticks <= COOLDOWN_LOOKAHEAD_TICKS; ticks++) {
            if (client.player.getAttackCooldownProgress(ticks) >= requiredCooldown) {
                return ticks;
            }
        }
        return COOLDOWN_LOOKAHEAD_TICKS;
    }

    private int getTicksUntilEarliestCritical(MinecraftClient client) {
        if (client == null || client.player == null) return 0;
        if (!onlyCrit.isEnabled() || client.player.isGliding()) return 0;
        if (AttackUtil.isPlayerInCriticalState()) return 0;
        if (client.player.isOnGround()) return 2;
        return client.player.getVelocity().y > 0.0 ? 2 : 1;
    }

    private void finishSprintReset(MinecraftClient client, boolean restoreSprint) {
        if (restoreSprint
            && sprintResetShouldRestore
            && client != null
            && client.player != null
            && !client.player.isGliding()) {
            client.player.setSprinting(true);
        }
        restoreSprintKeyState(client);
        clearSprintResetState();
    }

    private void restoreSprintKeyState(MinecraftClient client) {
        if (!sprintKeyForcedRelease || client == null || client.options == null) return;
        // Check if physical key is still pressed
        boolean physicalPressed = false;
        try {
            // Use reflection or key binding check
            physicalPressed = client.options.sprintKey.isPressed();
        } catch (Exception ignored) {
        }
        client.options.sprintKey.setPressed(physicalPressed);
        sprintKeyForcedRelease = false;
    }

    private void clearSprintResetState() {
        sprintResetArmed = false;
        sprintResetShouldRestore = false;
        sprintKeyForcedRelease = false;
        sprintResetReadyTick = Integer.MIN_VALUE;
        sprintResetExpireTick = Integer.MIN_VALUE;
    }

    private boolean canAttackNow(MinecraftClient client) {
        if (client == null || client.player == null) return false;
        float tpsFix = tpsSync.isEnabled() ? 0.0F : 0.5F;
        float requiredCooldown = onlyCrit.isEnabled() ? CRITICAL_ATTACK_COOLDOWN : FULL_ATTACK_COOLDOWN;
        return hurtTimer.finished(450L) && client.player.getAttackCooldownProgress(tpsFix) > requiredCooldown + attackBonus;
    }

    private boolean prepareAttackTarget(MinecraftClient client, LivingEntity target) {
        if (target == null || client == null || client.player == null) return false;
        double maxRangeSq = distance.getCurrent() * distance.getCurrent();
        if (target.squaredDistanceTo(client.player) > maxRangeSq) return false;
        return true;
    }

    private void onAttackPerformed(MinecraftClient client, long nowMs) {
        nextAttackAtMs = nowMs + 500L; // base delay for next attack
        float perTick = 0.05F;
        try {
            perTick = client.player.getAttackCooldownProgressPerTick();
        } catch (Exception ignored) {
        }
        float period = perTick > 0 ? (1.0F / perTick) * 50.0F : 400.0F;
        int r;
        do {
            r = ThreadLocalRandom.current().nextInt(-7, 13);
        } while (r == lastRand);
        lastRand = r;
        attackBonus = period > 0 ? (float) r / period : 0;
    }

    private void afterHit() {
      float perTick = 0.05F;
      try {
         perTick = mc.player.getAttackCooldownProgressPerTick();
      } catch (Exception ignored) {
      }
      float period = perTick > 0 ? (1.0F / perTick) * 50.0F : 400.0F;
      int r;
      do {
         r = ThreadLocalRandom.current().nextInt(-7, 13);
      } while (r == lastRand);
      lastRand = r;
      attackBonus = period > 0 ? (float) r / period : 0;
   }

   // ---------------- щит ----------------

   @Native
   private void breakShieldAndAttack() {
      boolean swapped = false, swappedInv = false;
      int hotbar = PlayerInventoryUtil.find(List.of(Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE,
              Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE), 0, 8);
      int inv = PlayerInventoryUtil.find(List.of(Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE,
              Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE), 8, 35);

      if (hotbar != -1 && shieldBreak.isEnabled() && target.isBlocking()) {
         if (legitSwap.isEnabled()) {
            lastSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = hotbar;
         } else {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbar));
         }
         swapped = true;
      }
      if (hotbar == -1 && inv != -1 && shieldBreak.isEnabled() && target.isBlocking()) {
         mc.interactionManager.clickSlot(0, inv, 8, SlotActionType.SWAP, mc.player);
         mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
         lastSlot = mc.player.getInventory().selectedSlot;
         mc.player.getInventory().selectedSlot = 8;
         swappedInv = true;
      }
      mc.interactionManager.attackEntity(mc.player, target);
      mc.player.swingHand(Hand.MAIN_HAND);
      if (swapped || swappedInv) {
         int slotInv = inv;
         boolean wasInv = swappedInv;
         if (legitSwap.isEnabled()) {
            Lumens.getInstance().getScriptManager().addTask(script);
            script.schedule(EventUpdate.class, ev -> {
               mc.player.getInventory().selectedSlot = lastSlot;
               if (wasInv) {
                  mc.interactionManager.clickSlot(0, slotInv, 8, SlotActionType.SWAP, mc.player);
                  mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
               }
               lastSlot = -1;
               return true;
            });
         } else {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
            if (wasInv) {
               mc.interactionManager.clickSlot(0, slotInv, 8, SlotActionType.SWAP, mc.player);
               mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
            }
         }
      }
   }

   // ---------------- ротация ----------------

   @EventTarget
   @Native
   public void eventRotate(EventGameUpdate e) {
      if (target == null) {
         spookyActive = false;
         spookyLastTarget = null;
         return;
      }
      Lumens.getInstance().getModuleManager().setAcceleration(0.0F);
      Box box = target.getBoundingBox();
      Vec3d eyes = mc.player.getEyePos();
      Vec3d point = isMode("Vanilla") ? box.getCenter()
              : MultipointUtils.getMultipoint(target, distance.getCurrent());
      if (target instanceof PlayerEntity && predictOnElytra.isEnabled()
              && mc.player.isGliding() && target.isGliding()) {
         double d = eyes.distanceTo(box.getCenter());
         point = PredictUtils.predict(target, target.getPos(), d > 8 ? 8.0F : predict.getCurrent());
      }
      Rotation angle = RotationUtil.fromVec3d(point.subtract(eyes));
      String mode = rotationMode.get();

      if (isMode("Vanilla") || isMode("FunTime") || isMode("Matrix")) {
         // Плавная + GCD-фикс + лёгкий гуманайзер; FunTime/Matrix — строже скорость
         float speed = isMode("Vanilla") ? 360.0F : isMode("FunTime") ? 180.0F : 90.0F;
         float yaw = lastYaw == 0 && lastPitch == 0 ? mc.player.getYaw() : lastYaw;
         float pitch = lastYaw == 0 && lastPitch == 0 ? mc.player.getPitch() : lastPitch;
         float dYaw = MathHelper.wrapDegrees(angle.getYaw() - yaw);
         float dPitch = angle.getPitch() - pitch;
         float ny = yaw + dYaw * Math.min(1.0F, speed / Math.max(1.0F, Math.abs(dYaw) * 10.0F));
         float np = pitch + dPitch * 0.6F;
         ny -= (ny - yaw) % Rotation.gcd();
         np -= (np - pitch) % Rotation.gcd();
         RotationComponent.update(new Rotation(ny, np), speed, speed, speed, speed, 0, 1, false);
         lastYaw = ny;
         lastPitch = np;
} else if (isMode("SpookyTime")) {
           // === SpookyTime = порт SPAngle: мультипоинт + нейро-скорость + дыхание ===
           if (!spookyActive || spookyLastTarget != target) {
              spookyActive = true;
              spookyLastTarget = target;
              lastYaw = mc.player.getYaw();
              lastPitch = mc.player.getPitch();
              spPoint = null;
              spPointAt = 0L;
              spBodyAt = 0L;
              lostAimAt = -1L;
           }
           long nowMs = System.currentTimeMillis();
           if (spPoint == null || nowMs - spPointAt > 50 + spSecure.nextInt(100)) {
              spPoint = spookyMultiPoint(target, nowMs);
              spPointAt = nowMs;
           }
           Rotation want = RotationUtil.fromVec3d(spPoint.subtract(eyes));
           float wantYaw = want.getYaw();
           float wantPitch = want.getPitch();

           // ВАЖНО: доводимся не чаще раза в тик, иначе кадры x3 разгоняют скорость и палят
           if (mc.player.age != spookyLastAge) {
              spookyLastAge = mc.player.age;
              float dYaw = MathHelper.wrapDegrees(wantYaw - lastYaw);
              float dPitch = wantPitch - lastPitch;
              // анти-ноль: чуть толкаем вторую ось, как в оригинале
              if (dYaw == 0 && dPitch != 0) dYaw += spRand(0.1F, 0.5F) + 0.1F * 1.0313F;
              if (dPitch == 0 && dYaw != 0) dPitch += spRand(0.1F, 0.5F) + 0.1F * 1.0313F;
              // капы за тик: yaw из настройки, pitch 23-26 как в оригинале
              float yawCap = Math.min(60.0F + spSecure.nextFloat() * 1.0329834F, spookySpeed.getCurrent() * 2.0F);
              dYaw = Math.min(Math.abs(dYaw), yawCap) * Math.signum(dYaw);
              dPitch = Math.min(Math.abs(dPitch), spRand(23.133F, 26.477F)) * Math.signum(dPitch);

              float dist = mc.player.distanceTo(target);
              float speed = spookyDynamicSpeed(dist, dYaw, dPitch);
              
              // Marlboro: отслеживание потери прицела и история скоростей для сглаживания
              boolean looking = RaytracingUtil.rayTrace(mc.player.getRotationVector(), distance.getCurrent() + distanceRotation.getCurrent() + 0.1, target.getBoundingBox());
              if (looking) {
                 lostAimAt = -1L;
              } else {
                 if (lostAimAt == -1L) lostAimAt = nowMs;
                 // Увеличиваем скорость пропорционально времени без прицела (как в Marlboro)
                 float aimLossFactor = Math.min(1.0F + (float)(nowMs - lostAimAt) / 380.0F, 1.9F);
                 speed *= aimLossFactor;
              }
              
              // Marlboro: история скоростей для капа суммарной скорости
              if (nowMs != lastHistoryTick) {
                 speedHistory[speedHistoryIndex] = Math.abs(speed);
                 speedHistoryIndex = (speedHistoryIndex + 1) % speedHistory.length;
                 lastHistoryTick = nowMs;
              }
              float speedSum = 0;
              for (float v : speedHistory) speedSum += v;
              if (speedSum > 22.0F && looking) {
                 float factor = 22.0F / speedSum;
                 int lastIdx = (speedHistoryIndex - 1 + speedHistory.length) % speedHistory.length;
                 speed *= factor;
                 speedHistory[lastIdx] *= factor;
              }

              // дыхание руки + редкий джиттер
              float breathX = (float) Math.sin(nowMs / 300.0) * 0.03F;
              float breathY = (float) Math.cos(nowMs / 500.0) * 0.02F;
              float jitYaw = 0, jitPitch = 0;
              if (spSecure.nextFloat() < 0.02F) {
                 jitYaw = (spSecure.nextFloat() - 0.5F) * 1.2F;
                 jitPitch = (spSecure.nextFloat() - 0.5F) * 0.8F;
              }
              float smoothYaw = dYaw * speed * 0.7F + breathX + jitYaw;
              float smoothPitch = dPitch * speed * 0.5F + breathY + jitPitch;
              // редкий овершут на больших доводках
              if (spSecure.nextFloat() < 0.05F && Math.abs(dYaw) > 5.0F) {
                 smoothYaw *= 1.1F + spSecure.nextFloat() * 0.3F;
              }
              // === УЛУЧШЕНИЯ SPOOKYTIME: плавная отводка + редкие промахи ===
              // Отводка: периодически смотрим чуть в сторону от цели (имитация живого игрока)
              if (spSecure.nextFloat() < 0.03F) { // ~3% шанс отводки в тик
                 float dodgeAngle = (spSecure.nextFloat() - 0.5F) * 8.0F; // ±4 градуса
                 smoothYaw += dodgeAngle;
              }
              // Редкие промахи: специально сбиваем прицел перед ударом (~2% шанс)
              if (spSecure.nextFloat() < 0.02F && hurtTimer.finished(400L)) {
                 float missYaw = (spSecure.nextFloat() - 0.5F) * 3.0F; // ±1.5 градуса
                 float missPitch = (spSecure.nextFloat() - 0.5F) * 2.0F; // ±1 градус
                 smoothYaw += missYaw;
                 smoothPitch += missPitch;
              }
              float ny = lastYaw + smoothYaw;
              float np = MathHelper.clamp(lastPitch + smoothPitch, -90.0F, 90.0F);
              ny -= (ny - lastYaw) % Rotation.gcd();
              np -= (np - lastPitch) % Rotation.gcd();
              RotationComponent.update(new Rotation(ny, np),
                      Math.abs(smoothYaw) + 8.0F, Math.abs(smoothPitch) + 4.0F, 8.0F, 4.0F, 0, 1, false);
              lastYaw = ny;
              lastPitch = np;
           }
           spookyYawErr = Math.abs(MathHelper.wrapDegrees(wantYaw - mc.player.getYaw()));
           spookyPitchErr = Math.abs(wantPitch - mc.player.getPitch());
        } else if (isMode("SlothAC")) {
           // === SlothAC: HelixWave-подобная ротация с плавными волнами ===
           if (!slothActive || slothLastTarget != target) {
              slothActive = true;
              slothLastTarget = target;
              lastYaw = mc.player.getYaw();
              lastPitch = mc.player.getPitch();
           }
           long nowMs = System.currentTimeMillis();
           float currentTime = nowMs / 1000.0F; // секунды как в Lua
           
           Rotation want = RotationUtil.fromVec3d(point.subtract(eyes));
           float targetYaw = want.getYaw();
           float targetPitch = want.getPitch();
           
           float diffYaw = MathHelper.wrapDegrees(targetYaw - lastYaw);
           float diffPitch = targetPitch - lastPitch;
           
           // Base smoothing (0.68 factor from Lua)
           float baseYaw = lastYaw + (diffYaw * 0.68F);
           float basePitch = lastPitch + (diffPitch * 0.68F);
           
           // Helix wave offsets (converted from Lua: sin/cos with time multipliers)
           float helixYaw = (float) (Math.sin(currentTime * 18.0) * 14.0 + Math.cos(currentTime * 9.0) * 6.0);
           float helixPitch = (float) (Math.cos(currentTime * 18.0) * 10.0 + Math.sin(currentTime * 9.0) * 5.0);
           
           // Добавляем естественный джиттер и дыхание
           float breathX = (float) Math.sin(currentTime / 0.3) * 0.05F;
           float breathY = (float) Math.cos(currentTime / 0.5) * 0.03F;
           float microJitterYaw = (slothSecure.nextFloat() - 0.5F) * 0.15F;
           float microJitterPitch = (slothSecure.nextFloat() - 0.5F) * 0.1F;
           
           float finalYaw = baseYaw + helixYaw + breathX + microJitterYaw;
           float finalPitch = MathHelper.clamp(basePitch + helixPitch + breathY + microJitterPitch, -89.9F, 89.9F);
           
           // GCD alignment
           finalYaw -= (finalYaw - lastYaw) % Rotation.gcd();
           finalPitch -= (finalPitch - lastPitch) % Rotation.gcd();
           
           RotationComponent.update(new Rotation(finalYaw, finalPitch),
                   Math.abs(finalYaw - lastYaw) + 10.0F, Math.abs(finalPitch - lastPitch) + 5.0F, 10.0F, 5.0F, 0, 1, false);
           lastYaw = finalYaw;
           lastPitch = finalPitch;
        } else {
         // LonyGrief / CakeWorld / LegendsGrief — ускорение с откатом как в Marlboro
         if (mc.player.isGliding()) {
            if (!isBack) {
               acceleration += 0.005F;
               if (acceleration >= 0.13F) isBack = true;
            } else {
               acceleration = Math.max(-0.02F, acceleration - 0.005F);
               if (acceleration <= -0.02F) isBack = false;
            }
         } else if (!RaytracingUtil.rayTrace(mc.player.getRotationVector(), 1488.0D, target.getBoundingBox())) {
            acceleration += 0.0015F;
         } else if (acceleration > 0) {
            acceleration -= 0.01F;
         }
         float yaw0 = lastYaw == 0 ? mc.player.getYaw() : lastYaw;
         float pitch0 = lastYaw == 0 ? mc.player.getPitch() : lastPitch;
         float dYaw = MathHelper.wrapDegrees(angle.getYaw() - yaw0);
         float dPitch = angle.getPitch() - pitch0;
         float k = MathHelper.clamp(Math.max(acceleration, 0.0F), 0.0F, 1.0F);
         float ny = yaw0 + dYaw * k;
         float np = pitch0 + dPitch * Math.min(k / 2.0F, 1.0F);
         ny -= (ny - yaw0) % Rotation.gcd();
         np -= (np - pitch0) % Rotation.gcd();
         RotationComponent.update(new Rotation(ny, np), 360.0F, 360.0F, 360.0F, 360.0F, 0, 1, false);
         lastYaw = ny;
         lastPitch = np;
      }

      // Не палимся в F5: не доводим вне видимости камеры
      float camYaw = mc.gameRenderer.getCamera().getYaw();
      float camPitch = mc.gameRenderer.getCamera().getPitch();
      if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
         camYaw = MathHelper.wrapDegrees(camYaw - 180.0F);
         camPitch = -camPitch;
      }
      float dy = Math.abs(MathHelper.wrapDegrees(camYaw - lastYaw));
      float dp = Math.abs(camPitch - lastPitch);
      float allow = (dy > 3.0F || dp > 3.0F) ? 0.0F : 360.0F;
      if (allow == 0.0F) {
         Lumens.getInstance().getModuleManager().setAcceleration(0.0F);
      }
   }

   private boolean isMode(String m) {
      return rotationMode.is(m);
   }

   // ---------------- SPAngle-порт: мультипоинт + нейро-скорость ----------------

   private Vec3d spookyMultiPoint(LivingEntity entity, long nowMs) {
      if (nowMs - spBodyAt > 800 + spSecure.nextInt(400)) {
         spBodyPart = spSecure.nextInt(3);
         spBodyAt = nowMs;
      }
      double w = entity.getWidth();
      double h = entity.getHeight();
      double rx = entity.getX() + spSecure.nextGaussian() * 0.4 * w;
      double rz = entity.getZ() + spSecure.nextGaussian() * 0.4 * w;
      double ry;
      if (spBodyPart == 0) ry = entity.getY() + h * spRand(0.6F, 0.8F);
      else if (spBodyPart == 1) ry = entity.getY() + h * spRand(0.85F, 0.95F);
      else ry = entity.getY() + h * spRand(0.4F, 0.6F);
      rx += Math.sin(nowMs / 200.0) * w * 0.1;
      rz += Math.cos(nowMs / 200.0) * w * 0.1;
      return new Vec3d(rx, ry, rz);
   }

   private float spookyDynamicSpeed(float dist, float yawDelta, float pitchDelta) {
      float neuro = spSecure.nextFloat();
      float distFactor;
      if (dist < 2.0F) distFactor = 1.4F + neuro * 0.6F;
      else if (dist < 4.0F) distFactor = 1.2F + neuro * 0.6F;
      else distFactor = 0.9F + neuro * 0.5F;
      float base;
      if (dist < 3.0F && Math.abs(yawDelta) < 10.0F) base = 0.25F + spSecure.nextFloat() * 0.15F;
      else if (dist > 5.0F || Math.abs(yawDelta) > 30.0F) base = 0.35F + spSecure.nextFloat() * 0.2F;
      else base = 0.28F + spSecure.nextFloat() * 0.15F;
      return base * distFactor;
   }

   private float spRand(float min, float max) {
      return min + spSecure.nextFloat() * (max - min);
   }

   // ---------------- условия атаки ----------------

   private boolean skipByItem() {
      boolean eating = false;
      try {
         eating = mc.player.isUsingItem() && (mc.player.getActiveItem().contains(net.minecraft.component.DataComponentTypes.FOOD)
                 || mc.player.getActiveItem().getItem() == Items.POTION
                 || mc.player.getActiveItem().getItem() == Items.MILK_BUCKET);
      } catch (Exception ignored) {
      }
      if (eating && noEatAttack.isEnabled()) return true;
      if (onlyWeapon.isEnabled() && !(mc.player.getMainHandStack().getItem().getTranslationKey().contains("sword")
              || mc.player.getMainHandStack().getItem().getTranslationKey().contains("axe")
              || mc.player.getMainHandStack().getItem() == Items.MACE
              || mc.player.getMainHandStack().getItem() == Items.TRIDENT)) return true;
      return false;
   }

   private boolean isCanAttack() {
      if (mc.player.getAttackCooldownProgress(0.5F) < 0.9F) return false;
      if (!AttackUtil.canAttack()) return false;
      if (skipByItem()) return false;
      if (noWalls.isEnabled() && isBehindWall()) return false;
      if (target instanceof PlayerEntity && predictOnElytra.isEnabled() && mc.player.isGliding() && target.isGliding()) {
         double d1 = mc.player.getEyePos().distanceTo(PredictUtils.predict(target, target.getPos(), predict.getCurrent()));
         double d2 = mc.player.getEyePos().distanceTo(target.getBoundingBox().getCenter());
         if (d1 > 3.0D && d2 > 3.0D) return false;
      } else if ((!mc.player.isGliding() || !target.isGliding())
              && mc.player.getEyePos().distanceTo(MultipointUtils.getNearestPoint(target, distance.getCurrent())) > distance.getCurrent()) {
         return false;
      }
      if (raycastCheck.isEnabled() && !RaytracingUtil.rayTrace(mc.player.getRotationVector(), distance.getCurrent(), target.getBoundingBox())
              && mc.targetedEntity == null && !mc.player.isGliding() && !target.isGliding()) return false;
      // криты как в Marlboro
      float tpsFix = tpsSync.isEnabled() ? 0.0F : 0.5F;
      boolean cd = hurtTimer.finished(450L) && mc.player.getAttackCooldownProgress(tpsFix) > 0.91F + attackBonus;
      if (!cd) return false;
      if (!onlyCrit.isEnabled()) return true;
      if (System.currentTimeMillis() < critCooldownUntil) return false;
      // СТРОГО только криты: бьём лишь когда сервер засчитает крит
      return canCritStrict();
   }

   /**
    * Проверка крита 1в1 как на сервере: падение + не на земле + нет запретов.
    * Убирает некрит-ударчики, по которым SpookyAC палит киллауру.
    */
   private boolean canCritStrict() {
      // Там где крит невозможен в принципе — не душим ауру, бьём как раньше
      if (mc.player.isGliding() || mc.player.hasVehicle()
              || mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.isClimbing()) {
         return !mc.player.isOnGround() || mc.player.fallDistance > 0;
      }
      // Эффекты, при которых сервер крит не засчитает
      if (mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
              || mc.player.hasStatusEffect(StatusEffects.LEVITATION)
              || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
         return false;
      }
      if (smartCrits.isEnabled()) {
         // Умный режим: ждём устоявшееся падение — сервер точно видит fallDistance > 0
         return !mc.player.isOnGround() && mc.player.fallDistance > 0.08F;
      }
      return !mc.player.isOnGround() && mc.player.fallDistance > 0.0F;
   }

   private boolean isBehindWall() {
      if (target == null) return false;
      return isBehindWall(target);
   }

   private boolean isBehindWall(LivingEntity e) {
      if (e == null) return false;
      Vec3d from = mc.player.getEyePos();
      Vec3d to = e.getEyePos();
      return mc.world.raycast(new net.minecraft.world.RaycastContext(from, to,
              net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
              net.minecraft.world.RaycastContext.FluidHandling.NONE, mc.player)).getType()
              == net.minecraft.util.hit.HitResult.Type.BLOCK;
   }

   // ---------------- цели ----------------

   private LivingEntity updateTarget() {
      List<LivingEntity> list = new ArrayList<>();
      for (Entity e : mc.world.getEntities()) {
         if (e instanceof LivingEntity le && isValid(le)) list.add(le);
      }
      if (list.isEmpty() || !isEnabled()) return null;
      // приоритет
      if (targetPriority.isEnabled() && priorityName != null) {
         for (LivingEntity le : list) {
            if (le instanceof PlayerEntity pl && pl.getName().getString().equalsIgnoreCase(priorityName)
                    && !(noWalls.isEnabled() && isBehindWall(le))) return le;
         }
      }
      String sort = sortMode.get();
      switch (sort) {
         case "Дистанции" -> list.sort(Comparator.comparingDouble(e -> mc.player.distanceTo(e)));
         case "Здоровью" -> list.sort(Comparator.comparingDouble(e -> e.getHealth() + e.getAbsorptionAmount()));
         case "Броне" -> list.sort(Comparator.comparingDouble(this::armorValue));
         default -> list.sort(Comparator
                 .comparingDouble(this::armorValue).reversed()
                 .thenComparing(e -> e.getHealth() + e.getAbsorptionAmount())
                 .thenComparing(e -> mc.player.distanceTo(e)));
      }
      return list.get(0);
   }

   private double armorValue(LivingEntity e) {
      try {
         return e.getArmor();
      } catch (Exception ex) {
         return 0;
      }
   }

   public boolean isValid(LivingEntity entity) {
      if (entity == mc.player || !entity.isAlive() || entity.getHealth() <= 0) return false;
      if (mc.player != null && (!mc.player.isAlive() || mc.player.getHealth() <= 0)) return false;
      if (entity instanceof PlayerEntity pl) {
         if (!targetTypeSetting.isEnable("Игроков")) return false;
         if (!targetTypeSetting.isEnable("Друзья")
                 && Lumens.getInstance().getFriendManager().isFriend(entity.getName().getString())) return false;
         if (!targetTypeSetting.isEnable("Голые") && pl.getArmor() == 0) return false;
         if (AntiBot.INSTANCE.isBot(pl)) return false;
      }
      if ((entity instanceof PassiveEntity || entity instanceof FishEntity)) {
         if (!targetTypeSetting.isEnable("Животных")) return false;
      }
      if ((entity instanceof HostileEntity || entity instanceof AmbientEntity)) {
         if (!targetTypeSetting.isEnable("Мобов")) return false;
      }
      double range = distance.getCurrent() + distanceRotation.getCurrent();
      if (mc.player.getEyePos().distanceTo(MultipointUtils.getNearestPoint(entity, range))
              > (mc.player.isGliding() ? 20.0F : range)) return false;
      return !(entity instanceof ArmorStandEntity);
   }

   @EventTarget
   private void setCorrection(EventMoveInput e) {
      if (target == null || correction.is("Нет")) return;
      if (correction.is("Фокус")) MovingUtil.fixMovementFocus(e, mc.player.getYaw());
      else MovingUtil.fixMovementFree(e);
   }

   public void setPriorityTarget(String name) {
      this.priorityName = name;
   }

   public void clearPriorityTarget() {
      this.priorityName = null;
   }

   public LivingEntity getTarget() {
      return isEnabled() ? target : null;
   }

public void onEnable() {
        target = null;
        acceleration = 0;
        isBack = false;
        spookyActive = false;
        spookyLastTarget = null;
        spookyLastAge = -1;
        spookyYawErr = 180.0F;
        spookyPitchErr = 90.0F;
        spookyNextHit = 0L;
        spookyLastJump = 0L;
        spPoint = null;
        spPointAt = 0L;
        spBodyAt = 0L;
        lostAimAt = -1L;
        Arrays.fill(speedHistory, 0.0F);
        speedHistoryIndex = 0;
        lastHistoryTick = 0L;
        // SlothAC
        slothActive = false;
        slothLastTarget = null;
        super.onEnable();
     }

public void onDisable() {
        Lumens.getInstance().getModuleManager().setAcceleration(0.0F);
        spookyActive = false;
        spookyLastTarget = null;
        lostAimAt = -1L;
        Arrays.fill(speedHistory, 0.0F);
        speedHistoryIndex = 0;
        lastHistoryTick = 0L;
        if (spookyJumped && mc.player != null) {
           try {
              mc.options.jumpKey.setPressed(false);
           } catch (Exception ignored) {
           }
           spookyJumped = false;
        }
        // SlothAC
        slothActive = false;
        slothLastTarget = null;
        super.onDisable();
     }
}
