package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.effect.SinfulOneEffect;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.registry.ModItems;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.JudgmentTechniqueSkillSet;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 审判术式武器：法槌与处刑人之剑的发放、替换与判定。
 */
public final class JudgmentWeaponLogic
{
    /** 处刑人之剑变回法槌的时长（5 分钟，与【死刑】一致）。 */
    public static final int EXECUTIONERS_SWORD_REVERT_TICKS = 20 * 60 * 5;
    /** 法槌对【罪恶者】目标的伤害倍率。 */
    public static final float SINFUL_ONE_DAMAGE_MULTIPLIER = 1.5F;
    public static final int GAVEL_FORM_STANDARD = 0;
    public static final int GAVEL_FORM_LONG_HANDLE = 1;
    public static final int GAVEL_FORM_BIG_HAMMER = 2;
    public static final int GAVEL_FORM_COUNT = 3;

    private static final String TAG_REVERT_AT = "jujutsukaisenzone_revert_at";
    private static final String TAG_CUSTOM_MODEL_DATA = "CustomModelData";

    private JudgmentWeaponLogic()
    {
    }

    /** 生物被判死刑时：法槌变为处刑人之剑；无法槌则发放处刑人之剑（不重复发放）。 */
    public static void grantExecutionersSwordOnDeathSentence(ServerPlayer caster)
    {
        long revertAt = caster.level().getGameTime() + EXECUTIONERS_SWORD_REVERT_TICKS;
        for (int slot = 0; slot < caster.getInventory().getContainerSize(); slot++)
        {
            ItemStack stack = caster.getInventory().getItem(slot);
            if (stack.is(ModItems.SM_GAVEL_WEAPON.get()))
            {
                caster.getInventory().setItem(slot, createTimedExecutionersSword(revertAt));
                caster.inventoryMenu.broadcastChanges();
                notifyExecutionersSwordGranted(caster);
                return;
            }
        }

        if (hasExecutionersSword(caster))
        {
            return;
        }

        ItemStack sword = createTimedExecutionersSword(revertAt);
        if (!caster.getInventory().add(sword))
        {
            caster.drop(sword, false);
        }
        caster.inventoryMenu.broadcastChanges();
        notifyExecutionersSwordGranted(caster);
    }

    private static void notifyExecutionersSwordGranted(ServerPlayer caster)
    {
        ModNetwork.sendExecutionersSwordActivationSync(caster);
    }

    public static void tickWeaponRevert(ServerPlayer player)
    {
        long now = player.level().getGameTime();
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
        {
            ItemStack stack = player.getInventory().getItem(slot);
            if (shouldRevertToGavel(stack, now))
            {
                ItemStack gavel = new ItemStack(ModItems.SM_GAVEL_WEAPON.get());
                setGavelForm(gavel, GAVEL_FORM_STANDARD);
                player.getInventory().setItem(slot, gavel);
                changed = true;
            }
            else if (stack.is(ModItems.SM_GAVEL_WEAPON.get()))
            {
                if (ensureGavelFormTag(stack))
                {
                    changed = true;
                }
            }
        }
        if (changed)
        {
            player.inventoryMenu.broadcastChanges();
        }
    }

    public static ItemStack createTimedExecutionersSword(long revertAtGameTime)
    {
        ItemStack stack = new ItemStack(ModItems.EXECUTIONERS_SWORD.get());
        stack.getOrCreateTag().putLong(TAG_REVERT_AT, revertAtGameTime);
        return stack;
    }

    public static boolean shouldRevertToGavel(ItemStack stack, long gameTime)
    {
        if (!stack.is(ModItems.EXECUTIONERS_SWORD.get()))
        {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null
                && tag.contains(TAG_REVERT_AT)
                && gameTime >= tag.getLong(TAG_REVERT_AT);
    }

    public static boolean hasGavelWeapon(ServerPlayer player)
    {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
        {
            if (player.getInventory().getItem(slot).is(ModItems.SM_GAVEL_WEAPON.get()))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean hasExecutionersSword(ServerPlayer player)
    {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
        {
            if (player.getInventory().getItem(slot).is(ModItems.EXECUTIONERS_SWORD.get()))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isWieldingExecutionersSword(ServerPlayer player)
    {
        return player.getMainHandItem().is(ModItems.EXECUTIONERS_SWORD.get())
                || player.getOffhandItem().is(ModItems.EXECUTIONERS_SWORD.get());
    }

    public static boolean isWieldingGavel(ServerPlayer player)
    {
        return player.getMainHandItem().is(ModItems.SM_GAVEL_WEAPON.get())
                || player.getOffhandItem().is(ModItems.SM_GAVEL_WEAPON.get());
    }

    public static float boostDamageAgainstSinfulOne(LivingEntity target, float amount)
    {
        if (!SinfulOneEffect.isActive(target))
        {
            return amount;
        }
        return amount * SINFUL_ONE_DAMAGE_MULTIPLIER;
    }

    /** 对处于死刑状态下的目标造成足以处决的伤害量。 */
    public static float lethalDamageAmount(LivingEntity target)
    {
        return target.getHealth() + target.getAbsorptionAmount() + 1024.0F;
    }

    public static void tryGrantGavelWeapon(ServerPlayer player)
    {
        if (hasGavelWeapon(player))
        {
            cycleGavelForm(player);
            return;
        }
        ItemStack gavel = new ItemStack(ModItems.SM_GAVEL_WEAPON.get());
        setGavelForm(gavel, GAVEL_FORM_STANDARD);
        if (!player.getInventory().add(gavel))
        {
            player.drop(gavel, false);
        }
        player.inventoryMenu.broadcastChanges();
    }

    public static int getGavelForm(ItemStack stack)
    {
        if (!stack.is(ModItems.SM_GAVEL_WEAPON.get()))
        {
            return GAVEL_FORM_STANDARD;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_CUSTOM_MODEL_DATA))
        {
            return GAVEL_FORM_STANDARD;
        }
        int form = tag.getInt(TAG_CUSTOM_MODEL_DATA);
        if (form < 0 || form >= GAVEL_FORM_COUNT)
        {
            return GAVEL_FORM_STANDARD;
        }
        return form;
    }

    public static void setGavelForm(ItemStack stack, int form)
    {
        if (!stack.is(ModItems.SM_GAVEL_WEAPON.get()))
        {
            return;
        }
        stack.getOrCreateTag().putInt(TAG_CUSTOM_MODEL_DATA, Math.floorMod(form, GAVEL_FORM_COUNT));
    }

    public static ItemStack copyWithGavelForm(ItemStack source, int form)
    {
        ItemStack stack = source.copy();
        setGavelForm(stack, form);
        return stack;
    }

    /** Epic Fight 的 NBT 变体在缺少 CustomModelData 时会 NPE，因此标准形态也写入 0。 */
    public static boolean ensureGavelFormTag(ItemStack stack)
    {
        if (!stack.is(ModItems.SM_GAVEL_WEAPON.get()))
        {
            return false;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_CUSTOM_MODEL_DATA))
        {
            return false;
        }
        setGavelForm(stack, GAVEL_FORM_STANDARD);
        return true;
    }

    /**
     * 循环法槌形态（标准 → 长柄 → 大锤）。
     * <p>
     * <b>机制型技能「三式法槌」的门控点</b>：未解锁时法槌恒为标准形态，这里提示一句便返回，
     * 不写任何标签——于是「再按一次没反应」在代码层面就是「可切换形态数 = 1」，
     * 而不是靠界面拦着。解锁后照旧 {@link #GAVEL_FORM_COUNT} 种循环。
     */
    private static void cycleGavelForm(ServerPlayer player)
    {
        if (!gavelFormVariantsUnlocked(player))
        {
            player.displayClientMessage(
                    Component.literal("法槌仅有标准形态——可在术式开发树解锁「三式法槌」")
                            .withStyle(ChatFormatting.YELLOW),
                    true);
            return;
        }
        int slot = findGavelSlotToCycle(player);
        if (slot < 0)
        {
            return;
        }
        ItemStack oldStack = player.getInventory().getItem(slot);
        ItemStack newStack = copyWithGavelForm(oldStack, getGavelForm(oldStack) + 1);
        player.getInventory().setItem(slot, newStack);
        InteractionHand hand = handForInventorySlot(player, slot);
        if (hand != null)
        {
            EpicFightCompat.refreshGavelWeaponHeldItem(player, hand, oldStack, newStack);
        }
        player.inventoryMenu.broadcastChanges();
    }

    /** 机制型技能「三式法槌」是否已解锁；未解锁时法槌恒为标准形态（见 {@link #cycleGavelForm}）。 */
    private static boolean gavelFormVariantsUnlocked(ServerPlayer player)
    {
        return SkillUnlockLogic.isSkillUnlockedById(player, PlayerTechniqueData.TECHNIQUE_JUDGMENT,
                JudgmentTechniqueSkillSet.SKILL_GAVEL_FORMS);
    }

    private static int findGavelSlotToCycle(ServerPlayer player)
    {
        if (player.getMainHandItem().is(ModItems.SM_GAVEL_WEAPON.get()))
        {
            return player.getInventory().selected;
        }
        if (player.getOffhandItem().is(ModItems.SM_GAVEL_WEAPON.get()))
        {
            return Inventory.SLOT_OFFHAND;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
        {
            if (player.getInventory().getItem(slot).is(ModItems.SM_GAVEL_WEAPON.get()))
            {
                return slot;
            }
        }
        return -1;
    }

    private static InteractionHand handForInventorySlot(ServerPlayer player, int slot)
    {
        if (slot == player.getInventory().selected)
        {
            return InteractionHand.MAIN_HAND;
        }
        if (slot == Inventory.SLOT_OFFHAND)
        {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    public static void normalizeInventoryGavels(ServerPlayer player)
    {
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
        {
            ItemStack stack = player.getInventory().getItem(slot);
            if (ensureGavelFormTag(stack))
            {
                player.getInventory().setItem(slot, copyWithGavelForm(stack, GAVEL_FORM_STANDARD));
                changed = true;
            }
        }
        if (changed)
        {
            player.inventoryMenu.broadcastChanges();
        }
    }
}
