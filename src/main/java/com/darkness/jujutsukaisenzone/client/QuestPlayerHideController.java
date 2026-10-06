package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 主线 1.3 演出期间隐藏本地玩家本体（替身负责「玩家外观」）。
 * <p>
 * 真身隐身由<b>服务端</b> {@code player.setInvisible(true)} 权威下发（含 EpicFight 渲染路径），
 * 本客户端类只做「客户端渲染兜底」：拦截原版 {@link RenderPlayerEvent.Pre} 与
 * {@link RenderLivingEvent.Pre}，防止隐身标记未及时同步、或第三方渲染器忽略 isInvisible 时露出本体。
 * <p>
 * ⛔ 两套事件<b>必须都在</b>：{@code RenderPlayerEvent.Pre} 只覆盖玩家<b>主模型</b>，
 * 盔甲 / 手持物等 render layer 不在其范围内；只取消前者会出现「人没了、盔甲还在飘」。
 * {@code RenderLivingEvent.Pre} 在整具生物（含全部图层）渲染前触发，能一并掐掉。
 * <p>
 * 退出世界 / 演出结束务必 {@link #setHidden} false。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class QuestPlayerHideController
{
    private static boolean hidden;

    private QuestPlayerHideController()
    {
    }

    public static void setHidden(boolean value)
    {
        hidden = value;
    }

    public static boolean isHidden()
    {
        return hidden;
    }

    /** 供混入 / 其它渲染路径复用。 */
    public static boolean shouldHide(Entity entity)
    {
        if (!hidden)
        {
            return false;
        }
        return entity != null && entity == Minecraft.getInstance().player;
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event)
    {
        if (shouldHide(event.getEntity()))
        {
            event.setCanceled(true);
        }
    }

    /**
     * 整具生物（含盔甲 / 手持物等全部 render layer）渲染前拦截。
     * <p>
     * ⛔ 单靠 {@link #onRenderPlayer} 不够：它只覆盖玩家主模型，盔甲层会漏出去。
     * 本方法在全部图层之前取消，是「连盔甲一起隐藏」的关键。
     */
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event)
    {
        if (shouldHide(event.getEntity()))
        {
            event.setCanceled(true);
        }
    }
}
