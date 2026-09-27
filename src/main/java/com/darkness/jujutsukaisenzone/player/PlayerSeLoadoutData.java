package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.item.CursedObjectSeTrait;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.technique.se.PanelStat;
import com.darkness.jujutsukaisenzone.technique.se.SeEffectDef;
import com.darkness.jujutsukaisenzone.technique.se.SeEffectRegistry;
import com.darkness.jujutsukaisenzone.technique.se.SeMechanicRegistry;
import com.darkness.jujutsukaisenzone.technique.se.SePenalty;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * SE 术式侵入装载：装备时从背包扣除咒物，卸下退回；装备中不可作升级耗材。
 * <p>
 * <b>不再是定长槽位</b>：装载是集合语义，条数只受「质量」约束
 * （与 SP 节点共用一个质量池，见 {@link PlayerQualityPoolData}）。
 * <p>
 * 装载要过闸，顺序即 {@link #tryEquip}：
 * 已知 SE → 未重复 → 专用术式 → 互斥组 → 指定槽已探查且特质吻合 → 质量硬顶。
 * <p>
 * 特质与代价来自咒物<strong>咒力探查</strong>后的实例 NBT，装载时写入 {@code seEquipped}，卸下按原快照退回。
 */
public final class PlayerSeLoadoutData
{
    private static final String CLIENT_TAG = "jujutsukaisenzone_se_equipped";

    private PlayerSeLoadoutData()
    {
    }

    /** 已装载的 SE id（服务端读存档，客户端读同步缓存）。顺序稳定（LinkedHashSet）。 */
    public static Set<String> getEquippedIds(Player player)
    {
        return Collections.unmodifiableSet(new LinkedHashSet<>(getEquippedMap(player).keySet()));
    }

    /** 已装载实例快照（含特质与代价）。 */
    public static Map<String, SeEquippedEntry> getEquippedMap(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return Collections.unmodifiableMap(
                    JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer).seEquipped);
        }
        CompoundTag root = player.getPersistentData().getCompound(CLIENT_TAG);
        ListTag list = root.getList("entries", Tag.TAG_COMPOUND);
        Map<String, SeEquippedEntry> result = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag entryTag = list.getCompound(i);
            String id = entryTag.getString("id");
            if (id == null || id.isBlank())
            {
                continue;
            }
            result.put(id, SeEquippedEntry.fromTag(entryTag, CursedEnergyTrait.NORMAL.id()));
        }
        return result;
    }

    public static Optional<SePenalty> getEquippedPenalty(Player player, String seId)
    {
        SeEquippedEntry entry = getEquippedMap(player).get(seId);
        return entry == null ? Optional.empty() : Optional.of(entry.penalty);
    }

    /** 已装载的 SE 定义（未知 id 被跳过）。 */
    public static List<SeEffectDef> getAllEquipped(Player player)
    {
        List<SeEffectDef> result = new ArrayList<>();
        for (String id : getEquippedIds(player))
        {
            SeEffectRegistry.find(id).ifPresent(result::add);
        }
        return result;
    }

    public static boolean isSeEquipped(Player player, String seId)
    {
        if (seId == null || seId.isBlank())
        {
            return false;
        }
        return getEquippedIds(player).contains(seId);
    }

    public static boolean isItemEquippedAsSe(Player player, Item item)
    {
        return SeEffectRegistry.findByItem(item)
                .map(def -> isSeEquipped(player, def.seId()))
                .orElse(false);
    }

    /**
     * 装载一条 SE（自动选第一件已探查且特质吻合的）。
     */
    public static SeEquipResult tryEquip(ServerPlayer player, String seId)
    {
        return tryEquip(player, seId, findCompatibleSlot(player,
                SeEffectRegistry.find(seId).map(SeEffectDef::item).orElse(null)));
    }

    /**
     * 装载一条 SE，从指定背包槽扣除已探查咒物。
     *
     * @param inventorySlot 背包槽；&lt; 0 时自动找第一件吻合件
     */
    public static SeEquipResult tryEquip(ServerPlayer player, String seId, int inventorySlot)
    {
        Optional<SeEffectDef> defOpt = SeEffectRegistry.find(seId);
        if (defOpt.isEmpty())
        {
            return SeEquipResult.UNKNOWN_SE;
        }
        SeEffectDef def = defOpt.get();
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);

        if (data.seEquipped.containsKey(seId))
        {
            return SeEquipResult.ALREADY_EQUIPPED;
        }
        if (!def.scope().matches(ownedTechniqueIds(player)))
        {
            return SeEquipResult.TECHNIQUE_MISMATCH;
        }
        if (hasMutexConflict(data, def))
        {
            return SeEquipResult.MUTEX_CONFLICT;
        }

        int matchSlot = inventorySlot;
        if (matchSlot < 0)
        {
            matchSlot = findCompatibleSlot(player, def.item());
        }
        if (matchSlot < 0)
        {
            if (!hasAnyItem(player, def.item()))
            {
                return SeEquipResult.MISSING_ITEM;
            }
            if (!hasAnyProbed(player, def.item()))
            {
                return SeEquipResult.NOT_PROBED;
            }
            return SeEquipResult.TRAIT_MISMATCH;
        }
        if (!isValidEquipSlot(player, def.item(), matchSlot))
        {
            ItemStack at = player.getInventory().getItem(matchSlot);
            if (at.isEmpty() || !at.is(def.item()))
            {
                return SeEquipResult.MISSING_ITEM;
            }
            if (!CursedObjectSeTrait.isProbed(at))
            {
                return SeEquipResult.NOT_PROBED;
            }
            return SeEquipResult.TRAIT_MISMATCH;
        }
        if (PlayerQualityPoolData.wouldExceedHardLimit(player, def.quality()))
        {
            return SeEquipResult.QUALITY_EXCEEDED;
        }

        ItemStack matched = player.getInventory().getItem(matchSlot);
        CursedEnergyTrait trait = CursedObjectSeTrait.getTrait(matched);
        SePenalty penalty = CursedObjectSeTrait.getPenalty(matched);
        if (trait == null || penalty == null)
        {
            return SeEquipResult.NOT_PROBED;
        }
        CursedObjectSeTrait.ensureInstanceId(matched);

        matched.shrink(1);
        player.inventoryMenu.broadcastChanges();
        data.seEquipped.put(seId, new SeEquippedEntry(trait.id(), penalty));
        JjkzPlayerDataStorage.get(player).setDirty();
        afterLoadoutChanged(player);
        return SeEquipResult.SUCCESS;
    }

    /**
     * 卸下一条 SE，按装载时记下的特质与代价退回咒物。
     */
    public static SeEquipResult tryUnequip(ServerPlayer player, String seId)
    {
        if (seId == null || seId.isBlank())
        {
            return SeEquipResult.NOT_EQUIPPED;
        }
        JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
        SeEquippedEntry entry = data.seEquipped.remove(seId);
        if (entry == null)
        {
            return SeEquipResult.NOT_EQUIPPED;
        }
        SeEffectRegistry.find(seId).ifPresent(def ->
        {
            CursedEnergyTrait trait = CursedEnergyTrait.byId(entry.traitId);
            ItemStack stack = CursedObjectSeTrait.withTraitAndPenalty(
                    new ItemStack(def.item()), trait, entry.penalty);
            if (!player.getInventory().add(stack))
            {
                player.drop(stack, false);
            }
            player.inventoryMenu.broadcastChanges();
        });
        JjkzPlayerDataStorage.get(player).setDirty();
        afterLoadoutChanged(player);
        return SeEquipResult.SUCCESS;
    }

    /**
     * 玩家当前持有的术式 id。
     * <p>
     * ⚠️ 现在是单术式（单元素集合）。将来「同时持有多个术式」上线时<b>只改这里</b>，
     * {@link com.darkness.jujutsukaisenzone.technique.se.SeScope} 收的就是集合，校验链不用动。
     */
    private static Set<String> ownedTechniqueIds(ServerPlayer player)
    {
        return Set.of(PlayerTechniqueData.getInnateTechnique(player));
    }

    /**
     * 互斥有两层：
     * <ol>
     *   <li>显式 {@link SeEffectDef#mutexGroup()}——设计师额外声明的冲突。</li>
     *   <li>隐式窄口互斥——同一窄口（见 {@link SeMechanicRegistry}）的 SE 至多装一条；
     *       {@code findEquipped} 只认先注册者，装第二条等于白装，所以必须在装载闸上拦掉。</li>
     * </ol>
     */
    private static boolean hasMutexConflict(JjkzPlayerDataStorage.PlayerDataEntry data, SeEffectDef def)
    {
        for (String otherId : data.seEquipped.keySet())
        {
            if (!def.mutexGroup().isEmpty())
            {
                Optional<SeEffectDef> other = SeEffectRegistry.find(otherId);
                if (other.isPresent() && def.mutexGroup().equals(other.get().mutexGroup()))
                {
                    return true;
                }
            }
            if (SeMechanicRegistry.hasNarrowInterfaceConflict(List.of(otherId), def.seId()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 找一件<strong>已探查</strong>且与玩家咒力特质吻合的对应咒物槽位；没有则 -1。
     */
    public static int findCompatibleSlot(Player player, Item item)
    {
        if (item == null)
        {
            return -1;
        }
        List<Integer> slots = listProbedSlots(player, item);
        CursedEnergyTrait playerTrait = PlayerCursedEnergyTraitData.getTrait(player);
        for (int slot : slots)
        {
            if (CursedObjectSeTrait.getTrait(player.getInventory().getItem(slot)) == playerTrait)
            {
                return slot;
            }
        }
        return -1;
    }

    /** 背包中该物品全部已探查槽位（顺序稳定）。 */
    public static List<Integer> listProbedSlots(Player player, Item item)
    {
        List<Integer> result = new ArrayList<>();
        if (player == null || item == null)
        {
            return result;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
        {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item) && CursedObjectSeTrait.isProbed(stack))
            {
                result.add(i);
            }
        }
        return result;
    }

    public static boolean hasCompatibleItem(Player player, Item item)
    {
        return findCompatibleSlot(player, item) >= 0;
    }

    public static boolean isValidEquipSlot(Player player, Item item, int slot)
    {
        if (player == null || item == null || slot < 0 || slot >= player.getInventory().getContainerSize())
        {
            return false;
        }
        ItemStack stack = player.getInventory().getItem(slot);
        if (!stack.is(item) || !CursedObjectSeTrait.isProbed(stack))
        {
            return false;
        }
        return CursedObjectSeTrait.getTrait(stack) == PlayerCursedEnergyTraitData.getTrait(player);
    }

    private static boolean hasAnyItem(Player player, Item item)
    {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
        {
            if (player.getInventory().getItem(i).is(item))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnyProbed(Player player, Item item)
    {
        return !listProbedSlots(player, item).isEmpty();
    }

    /**
     * 装载状态变化后的统一善后：同步客户端 + 重算 Attribute 层代价 + 刷新强度面板 / 咒力上限。
     */
    private static void afterLoadoutChanged(ServerPlayer player)
    {
        syncToClient(player);
        float oldMaxHp = player.getMaxHealth();
        float oldHp = player.getHealth();
        PlayerSePenaltyApplier.recompute(player);
        PlayerHealthData.scaleHealthAfterMaxChange(player, oldMaxHp, oldHp);
        PlayerCursedEnergyData.recalculateAfterGradeChange(player);
        PlayerCritPowerStackData.clearIfNoMechanic(player);
        PlayerCombatPowerData.syncToClient(player);
        PlayerDomainGaugeData.syncToClient(player);
    }

    public static void applyClientSnapshot(Player player, Map<String, SeEquippedEntry> entries)
    {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        if (entries != null)
        {
            for (Map.Entry<String, SeEquippedEntry> e : entries.entrySet())
            {
                if (e.getKey() == null || e.getKey().isBlank() || e.getValue() == null)
                {
                    continue;
                }
                list.add(e.getValue().toTag(e.getKey()));
            }
        }
        root.put("entries", list);
        player.getPersistentData().put(CLIENT_TAG, root);
    }

    public static void syncToClient(ServerPlayer player)
    {
        ModNetwork.sendSeLoadoutSync(player, getEquippedMap(player));
    }

    /** 已装载实例代价中，指定结算通道的 amount 合计。 */
    public static double sumPenaltyAmount(Player player, PanelStat.ApplyKind kind)
    {
        double sum = 0.0D;
        for (SeEquippedEntry entry : getEquippedMap(player).values())
        {
            if (entry.penalty.stat().applyKind() == kind)
            {
                sum += entry.penalty.amount();
            }
        }
        return sum;
    }

    public enum SeEquipResult
    {
        SUCCESS,
        UNKNOWN_SE,
        ALREADY_EQUIPPED,
        MUTEX_CONFLICT,
        MISSING_ITEM,
        /** 背包有咒物，但没有与玩家特质吻合的已探查件。 */
        TRAIT_MISMATCH,
        /** 背包有该咒物，但没有任何已探查实例。 */
        NOT_PROBED,
        /** 专用 SE，但玩家持有的术式不是它指定的那个。 */
        TECHNIQUE_MISMATCH,
        /** 质量占用越过「上限 + 宽限」。 */
        QUALITY_EXCEEDED,
        /** 卸下时该 SE 本来就没装载。 */
        NOT_EQUIPPED
    }
}
