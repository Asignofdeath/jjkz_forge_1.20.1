package com.darkness.jujutsukaisenzone.registry;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 模组物品标签。
 * <p>
 * 十划等技能用 {@link #SHARP_WEAPONS} 判定「利器」。
 * 数据包：{@code data/jujutsukaisenzone/tags/items/sharp_weapons.json}。
 * <ul>
 *   <li>原版：{@code #minecraft:swords} / {@code #minecraft:axes}、三叉戟</li>
 *   <li>本模组：处刑人之剑</li>
 *   <li>Epic Fight 20.14：匕首 / 长剑 / 大剑 / 太刀 / 枪 / 打刀（不含手套、木刀、鞘）</li>
 * </ul>
 * 法槌不是 {@code AxeItem}，不会被 {@code #minecraft:axes} 带进本标签。
 * 其它模组刃：往上述 JSON 追加物品 id 即可。
 */
public final class ModItemTags
{
    /** 利器：十划断肢 / 一文字优待等。 */
    public static final TagKey<Item> SHARP_WEAPONS = TagKey.create(
            Registries.ITEM,
            new ResourceLocation(JuJutsuKaisenZone.MODID, "sharp_weapons"));

    private ModItemTags()
    {
    }

    public static boolean isSharpWeapon(ItemStack stack)
    {
        return !stack.isEmpty() && stack.is(SHARP_WEAPONS);
    }
}
