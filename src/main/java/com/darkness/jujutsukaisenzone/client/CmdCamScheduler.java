package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * CMDCam 客户端命令调度：{@link ClientCommandHandler#runCommand(String)}（无前导斜杠）、独立队列、默认间隔。
 * <p>
 * 每条演出各自持有实例，互不并队；镜头字符串与专用间隔由各 {@code *CmdCamClient} 决定。
 * <p>
 * {@code cam save}/{@code cam load} 只用于<strong>多组镜头</strong>（先存 A、清点、排 B，再 load 回放）。
 * 单组路径不要 save/load：{@code mode} → {@code clear} → {@code add}… → {@code start}。
 */
public final class CmdCamScheduler
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<CmdCamScheduler> REGISTERED = new CopyOnWriteArrayList<>();

    /** 普通命令后的间隔。 */
    public static final int TICKS_AFTER_NORMAL_COMMAND = 1;
    /** 仅 {@code cam save}/{@code cam load} 之后用；单组镜头不应走到这两条命令。 */
    public static final int TICKS_AFTER_SAVE_OR_LOAD = 5;

    private final Object lock = new Object();
    private final ArrayDeque<String> queue = new ArrayDeque<>();
    private final ToIntFunction<String> delayAfter;
    private int ticksUntilNext;
    private Consumer<String> afterCommand = command ->
    {
    };
    private Runnable beforeTick = () ->
    {
    };

    private CmdCamScheduler(ToIntFunction<String> delayAfter)
    {
        this.delayAfter = delayAfter;
        REGISTERED.add(this);
    }

    public static CmdCamScheduler create(ToIntFunction<String> delayAfter)
    {
        return new CmdCamScheduler(delayAfter);
    }

    public static CmdCamScheduler createDefaultDelays()
    {
        return create(CmdCamScheduler::defaultDelayAfter);
    }

    public CmdCamScheduler afterEach(Consumer<String> afterCommand)
    {
        this.afterCommand = afterCommand == null ? command ->
        {
        } : afterCommand;
        return this;
    }

    public CmdCamScheduler beforeEachTick(Runnable beforeTick)
    {
        this.beforeTick = beforeTick == null ? () ->
        {
        } : beforeTick;
        return this;
    }

    public static boolean dispatcherReady()
    {
        return ClientCommandHandler.getDispatcher() != null;
    }

    public static int defaultDelayAfter(String commandJustRun)
    {
        if (commandJustRun.startsWith("cam save ") || commandJustRun.startsWith("cam load "))
        {
            return TICKS_AFTER_SAVE_OR_LOAD;
        }
        return TICKS_AFTER_NORMAL_COMMAND;
    }

    public void clear()
    {
        synchronized (this.lock)
        {
            this.queue.clear();
            this.ticksUntilNext = 0;
        }
    }

    public void replaceAndStart(String... commands)
    {
        replaceAndStart(Arrays.asList(commands));
    }

    public void replaceAndStart(Collection<String> commands)
    {
        synchronized (this.lock)
        {
            this.queue.clear();
            this.ticksUntilNext = 0;
            this.queue.addAll(commands);
            pumpNextLocked();
        }
    }

    public void append(String... commands)
    {
        synchronized (this.lock)
        {
            this.queue.addAll(Arrays.asList(commands));
        }
    }

    private void tick()
    {
        this.beforeTick.run();
        synchronized (this.lock)
        {
            if (this.ticksUntilNext > 0)
            {
                this.ticksUntilNext--;
                if (this.ticksUntilNext > 0)
                {
                    return;
                }
            }
            pumpNextLocked();
        }
    }

    private void pumpNextLocked()
    {
        if (this.queue.isEmpty())
        {
            return;
        }
        String cmd = this.queue.pollFirst();
        if (cmd == null)
        {
            return;
        }
        runCamImmediate(cmd);
        this.afterCommand.accept(cmd);
        if (!this.queue.isEmpty())
        {
            this.ticksUntilNext = this.delayAfter.applyAsInt(cmd);
        }
    }

    private static void runCamImmediate(String commandWithoutSlash)
    {
        boolean consumed = ClientCommandHandler.runCommand(commandWithoutSlash);
        if (!consumed)
        {
            LOGGER.warn("[CMDCam] Command not handled as client command (is CMDCam installed on this client?): {}",
                    commandWithoutSlash);
        }
    }

    @Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ClientTickDriver
    {
        private ClientTickDriver()
        {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event)
        {
            if (event.phase != TickEvent.Phase.END)
            {
                return;
            }
            for (CmdCamScheduler scheduler : REGISTERED)
            {
                scheduler.tick();
            }
        }
    }
}
