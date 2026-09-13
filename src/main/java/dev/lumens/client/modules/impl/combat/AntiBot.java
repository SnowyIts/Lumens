package dev.lumens.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import ru.nexusguard.protection.annotations.Native;
import dev.lumens.base.events.impl.player.EventUpdate;
import dev.lumens.client.modules.api.Category;
import dev.lumens.client.modules.api.Module;
import dev.lumens.client.modules.api.ModuleAnnotation;
import dev.lumens.client.modules.api.setting.impl.BooleanSetting;
import dev.lumens.client.modules.api.setting.impl.NumberSetting;
import dev.lumens.utility.math.Timer;

/**
 * AntiBot — полный порт Marlboro AntiBot + улучшения под FunTime/1.21.4:
 * - классика Marlboro: фулл броня без чар + offhand воздух + кожа/железо + еда 20
 * - FunTime: нет в табе / пинг 0 / ticksExisted маленький / без движения / дубли ника
 * - тихий сбор, автоочистка при смене мира и каждые 10с перепроверка
 */
@ModuleAnnotation(
    name = "AntiBot",
    category = Category.COMBAT,
    description = "Фильтрует ботов античита"
)
public final class AntiBot extends Module {
    public static final AntiBot INSTANCE = new AntiBot();

    private final BooleanSetting checkArmor = new BooleanSetting("Проверка брони", true);
    private final BooleanSetting checkTab = new BooleanSetting("Проверка таба", true);
    private final BooleanSetting checkTicks = new BooleanSetting("Проверка ticksExisted", true);
    private final BooleanSetting checkHunger = new BooleanSetting("Проверка голода", true);
    private final NumberSetting ticksExisted = new NumberSetting("Ticks бота", 60.0F, 10.0F, 200.0F, 5.0F);
    private final BooleanSetting checkDuplicates = new BooleanSetting("Проверка дублей ника", true);

    private final Set<PlayerEntity> bots = ConcurrentHashMap.newKeySet();
    private final Timer clearTimer = new Timer();

    private AntiBot() {
    }

    @EventTarget
    @Native
    public void onTick(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        // Периодическая перепроверка — боты могут "ожить"
        if (clearTimer.finished(10000L)) {
            bots.removeIf(p -> p == null || p.isRemoved() || !isMarlboroBot(p) && !isFunTimeBot(p));
            clearTimer.reset();
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player == mc.player) continue;
            if (bots.contains(player)) continue;

            boolean marlboro = checkArmor.isEnabled() && isMarlboroBot(player);
            boolean funtime = isFunTimeBot(player);
            if (marlboro || funtime) {
                bots.add(player);
            } else {
                // Снят флаг — убираем из ботов (игрок надел сет / поел / появился в табе)
                bots.remove(player);
            }
        }
        // Чистим вышедших
        bots.removeIf(p -> !mc.world.getPlayers().contains(p));
    }

    // ---- Marlboro-оригинал, адаптирован под 1.21.4 ----
    private boolean isMarlboroBot(PlayerEntity p) {
        try {
            boolean fullCleanEnchantable = p.getInventory().armor.stream()
                    .allMatch(s -> s.getItem() != Items.AIR && s.isEnchantable() && !s.isDamaged());
            boolean leatherOrIron = p.getInventory().armor.stream().anyMatch(s ->
                    s.getItem() == Items.LEATHER_BOOTS || s.getItem() == Items.LEATHER_LEGGINGS
                            || s.getItem() == Items.LEATHER_CHESTPLATE || s.getItem() == Items.LEATHER_HELMET
                            || s.getItem() == Items.IRON_BOOTS || s.getItem() == Items.IRON_LEGGINGS
                            || s.getItem() == Items.IRON_CHESTPLATE || s.getItem() == Items.IRON_HELMET);
            boolean offhandAir = p.getOffHandStack().getItem() == Items.AIR;
            boolean hungerFull = !checkHunger.isEnabled() || p.getHungerManager().getFoodLevel() == 20;
            return fullCleanEnchantable && leatherOrIron && offhandAir && hungerFull;
        } catch (Exception e) {
            return false;
        }
    }

    // ---- FunTime / общие эвристики ----
    private boolean isFunTimeBot(PlayerEntity p) {
        try {
            // 1. Нет в табе — почти всегда бот на FunTime
            if (checkTab.isEnabled() && mc.getNetworkHandler() != null) {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
                if (entry == null) return true;
                // пинг 0/1 у ботов-наживок
                if (entry.getLatency() <= 1 && p.age < 200) return true;
            }
            // 2. Только заспавнился и стоит без брони/движения
            if (checkTicks.isEnabled() && p.age < ticksExisted.getCurrent()
                    && p.getHealth() == 20.0F && p.getOffHandStack().isEmpty()) {
                // Бегает ли? У ботов часто нулевая скорость несколько тиков
                if (p.getVelocity().lengthSquared() < 1e-6 && armorCheck(p)) return true;
            }
            // 3. Инвиз-бот/ NPC без гаммы: нет хитбокса движения + голый кожаный сет
            if (armorCheck(p) && p.getHungerManager().getFoodLevel() == 20
                    && p.getOffHandStack().isEmpty()) {
                return true;
            }
            // 4. Дубликаты ников (кейс-инсенситив) — частый признак бот-ферм
            if (checkDuplicates.isEnabled()) {
                for (PlayerEntity other : mc.world.getPlayers()) {
                    if (other != p && other != mc.player
                            && other.getName().getString().equalsIgnoreCase(p.getName().getString())) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean armorCheck(PlayerEntity entity) {
        return isLeatherClean(entity, 3) || isLeatherClean(entity, 2)
                || isLeatherClean(entity, 1) || isLeatherClean(entity, 0)
                || isIronClean(entity, 2) || isIronClean(entity, 1);
    }

    private boolean isLeatherClean(PlayerEntity e, int slot) {
        ItemStack s = e.getInventory().getArmorStack(slot);
        boolean leather = switch (slot) {
            case 3 -> s.getItem() == Items.LEATHER_HELMET;
            case 2 -> s.getItem() == Items.LEATHER_CHESTPLATE;
            case 1 -> s.getItem() == Items.LEATHER_LEGGINGS;
            default -> s.getItem() == Items.LEATHER_BOOTS;
        };
        return leather && !s.contains(DataComponentTypes.DYED_COLOR) && !s.hasEnchantments();
    }

    private boolean isIronClean(PlayerEntity e, int slot) {
        ItemStack s = e.getInventory().getArmorStack(slot);
        boolean iron = slot == 2 ? s.getItem() == Items.IRON_CHESTPLATE : s.getItem() == Items.IRON_LEGGINGS;
        return iron && !s.hasEnchantments();
    }

    public void onEnable() {
        super.onEnable();
        bots.clear();
    }

    public void onDisable() {
        super.onDisable();
        bots.clear();
    }

    public boolean isBot(PlayerEntity player) {
        return isEnabled() && bots.contains(player);
    }

    public int botCount() {
        return bots.size();
    }
}