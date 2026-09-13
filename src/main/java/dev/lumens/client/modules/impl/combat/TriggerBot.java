package dev.lumens.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import com.google.common.collect.Lists;
import java.util.Arrays;

import dev.lumens.base.events.impl.server.EventPacket;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import dev.lumens.utility.game.player.PlayerIntersectionUtil;
import dev.lumens.base.events.impl.other.EventTick;
import dev.lumens.base.events.impl.other.EventTickMovement;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.BooleanSetting;
import dev.lumens.client.modules.api.setting.impl.MultiBooleanSetting;
import dev.lumens.utility.game.player.RaytracingUtil;
import dev.lumens.utility.interfaces.IClient;

@ModuleAnnotation(
        name = "TriggerBot",
        category = Category.COMBAT,
        description = "Автоматически бьет когда наведен на цель"
)
public final class TriggerBot extends Module {
    public static final TriggerBot INSTANCE = new TriggerBot();

    private final MultiBooleanSetting targetTypeSetting = MultiBooleanSetting.create("Атаковать",
            Lists.newArrayList("Игроков", "Мобов", "Животных", "Стойки для брони"));

    private final BooleanSetting onlyCritical = new BooleanSetting("Только криты", "Бьёт только критами", true);
    private final BooleanSetting smartCritical = new BooleanSetting("Умные криты", "Чередует удары критами и на земле", false,
            () -> onlyCritical.isEnabled());

    private final TargetFinder finder = new TargetFinder();
    private final TriggerClicker clicker = new TriggerClicker();

    private TriggerBot() {}

    @EventTarget
    public void onTick(EventTick event) {
        for (Entity entity : mc.world.getEntities()) {
            if (RaytracingUtil.rayTrace(mc.player.getRotationVector(), 3.2F, entity.getBoundingBox())) {
                if (finder.hasAccessTarget(entity, getSelectedTypes())) {
                    clicker.predictCritical();
                }
            }
        }

        clicker.updateCriticalSettings(onlyCritical.isEnabled(), smartCritical.isEnabled());

        Entity entity = finder.getCrossTarget(getSelectedTypes());

        if (entity != null) {
            clicker.onAttackEntity(entity);
        }
    }

    @EventTarget
    public void onTickMovement(EventTickMovement event) {
    }

    private TargetType[] getSelectedTypes() {
        return Arrays.stream(TargetType.values())
                .filter(targetType -> targetTypeSetting.isEnable(targetType.getTargetName()))
                .toArray(TargetType[]::new);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        clicker.resetClickTime();
    }

    // ==================== Supporting Classes ====================

    @Getter
    public enum TargetType {
        PLAYER("Игроков"),
        MOB("Мобов"),
        ANIMAL("Животных"),
        STAND("Стойки для брони");

        private final String targetName;

        TargetType(String targetName) {
            this.targetName = targetName;
        }

        public String getTargetName() {
            return targetName;
        }
    }

    public static class TargetFinder implements IClient {
        public Entity getCrossTarget(TargetType... targetTypes) {
            if (mc.crosshairTarget instanceof EntityHitResult hitResult) {
                Entity entity = hitResult.getEntity();
                if (hasAccessTarget(entity, targetTypes)) return entity;
            }
            return null;
        }

        public boolean hasAccessTarget(Entity entity, TargetType... targetTypes) {
            for (TargetType type : targetTypes) {
                if (matches(entity, type)) {
                    return true;
                }
            }
            return false;
        }

        private boolean matches(Entity entity, TargetType type) {
            return switch (type) {
                case PLAYER -> entity instanceof PlayerEntity;
                case MOB -> entity instanceof MobEntity;
                case ANIMAL -> entity instanceof AnimalEntity;
                case STAND -> entity instanceof ArmorStandEntity;
            };
        }
    }

    public static class TriggerClicker implements IClient {
        private final CooldownClicker clicker = new CooldownClicker();
        private final AttackCritical attackCritical = new AttackCritical();
        private boolean onlyCritical, smartCritical;

        public void updateCriticalSettings(boolean onlyCritical, boolean smartCritical) {
            this.onlyCritical = onlyCritical;
            this.smartCritical = smartCritical;
        }

        public void predictCritical() {
            if (canAttack() || hasDistanceFix()) {
                attackCritical.preCritical();
            }
        }

        public void onAttackEntity(Entity entity) {
            if (canAttack() || hasDistanceFix()) {
                attackCritical.preCritical();
            }

            if (!canAttack()) return;

            if (!attackCritical.isSprinting()) {
                attackEntity(entity);
            }
        }

        public void resetClickTime() {
            clicker.resetClickTime();
        }

        private void attackEntity(Entity entity) {
            clicker.resetClickTime();
            mc.interactionManager.attackEntity(mc.player, entity);
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        private boolean canAttack() {
            if (!clicker.isCooldownPassed()) return false;

            boolean noRestrict = !hasMovementRestrictions();

            if (smartCritical && onlyCritical) {
                if (noRestrict) {
                    return canCrit() || mc.player.isOnGround();
                } else {
                    return true;
                }
            }

            if (onlyCritical && !hasMovementRestrictions()) {
                return canCrit();
            }
            return true;
        }

        private boolean hasMovementRestrictions() {
            return mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                    || mc.player.hasStatusEffect(StatusEffects.LEVITATION)
                    || PlayerIntersectionUtil.isPlayerInBlock(Blocks.COBWEB)
                    || mc.player.isSubmergedInWater()
                    || mc.player.isInLava()
                    || mc.player.isClimbing()
                    || !PlayerIntersectionUtil.canChangeIntoPose(EntityPose.STANDING)
                    || mc.player.getAbilities().flying;
        }

        private boolean canCrit() {
            return !mc.player.isOnGround() && mc.player.fallDistance > 0;
        }

        private boolean hasDistanceFix() {
            return !mc.player.isOnGround() && mc.player.fallDistance < 0.0001;
        }
    }

    public static class CooldownClicker implements IClient {
        private long lastClickTime = System.currentTimeMillis();

        public boolean isCooldownPassed() {
            if (leftClickPassed() < 230) return false;
            return mc.player.getAttackCooldownProgress(1) >= 0.92F;
        }

        private long leftClickPassed() {
            return System.currentTimeMillis() - lastClickTime;
        }

        public void resetClickTime() {
            lastClickTime = System.currentTimeMillis();
        }
    }

    public static class AttackCritical {
        private final SprintControl sprintControl = new SprintControl();

        public AttackCritical() {
            com.darkmagician6.eventapi.EventManager.register(sprintControl);
        }

        public void preCritical() {
            if (sprintControl.getClient().player.isSprinting()) {
                sprintControl.setSprintResetTicks(1);
                sprintControl.getClient().options.sprintKey.setPressed(false);
                sprintControl.getClient().player.setSprinting(false);
            }
        }

        public boolean isSprinting() {
            return sprintControl.isServerSprint()
                    && !sprintControl.getClient().player.isGliding()
                    && !sprintControl.getClient().player.isTouchingWater();
        }
    }

    @Getter @Setter
    public static class SprintControl implements IClient {
        private final MinecraftClient client = MinecraftClient.getInstance();
        private boolean serverSprint;
        private int sprintResetTicks;
        private boolean lastSprint;

        @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
        private boolean hasReset;

        public MinecraftClient getClient() {
            return client;
        }

        public boolean isServerSprint() {
            return serverSprint;
        }

        public void setSprintResetTicks(int ticks) {
            this.sprintResetTicks = ticks;
        }

@EventTarget
    public void onPacket(EventPacket event) {
            if (event.getPacket() instanceof ClientCommandC2SPacket command) {
                serverSprint = switch (command.getMode()) {
                    case ClientCommandC2SPacket.Mode.START_SPRINTING -> true;
                    case ClientCommandC2SPacket.Mode.STOP_SPRINTING -> false;
                    default -> serverSprint;
                };
            }
        }

        @EventTarget
        public void onTick(EventTick ignored) {
            if (sprintResetTicks > 0) {
                if (!hasReset) {
                    hasReset = true;
                }
                client.player.setSprinting(false);
                sprintResetTicks--;
            } else if (hasReset) {
                client.options.sprintKey.setPressed(true);
                client.player.setSprinting(true);
                hasReset = false;
            }
        }
    }
}