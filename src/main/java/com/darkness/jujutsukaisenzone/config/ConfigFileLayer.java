package com.darkness.jujutsukaisenzone.config;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数值覆盖表的文件层：按需读取、不监听、不自动写回。
 * <p>
 * <b>这是本模组唯一碰配置文件的地方</b>，刻意做得极窄，为的是从构造上排除 Forge 那套的竞态：
 * <ul>
 *   <li><b>没有 watcher，没有第二个线程</b>。只在两个明确时机读：启动时（{@code Config} 类初始化期间）
 *       与客户端断开连接后（{@code com.darkness.jujutsukaisenzone.Config#reloadFromLocalFile()}）。
 *       没人会读到「写了一半」的内容。</li>
 *   <li><b>只在文件不存在时生成模板</b>，之后 <b>永不重写</b>。玩家写进去的值不存在被覆盖的可能。</li>
 *   <li>写模板也走「临时文件 + 原子改名」，即使生成过程中崩了也不会留下半个文件。</li>
 *   <li>任何异常都只降级成「全部使用代码默认值」并打日志，<b>绝不抛出去</b> —— 一个手滑的字符
 *       不能让游戏起不来。</li>
 * </ul>
 * <p>
 * 模板内容由 {@link CfgSpec#templateText()} 从代码默认值生成，见那里的注释（为什么全部写成注释行）。
 */
public final class ConfigFileLayer
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 放在 config/&lt;modid&gt;/ 子目录里：不和 Forge 自己追踪的 *-common.toml 抢命名空间。 */
    private static final String DIR_NAME = JuJutsuKaisenZone.MODID;
    private static final String FILE_NAME = "settings.toml";

    private static final String HEADER = """
            # ============================================================================
            #  JujutsuKaisenZone · 数值覆盖表
            # ============================================================================
            #  本文件里的每一项都是【注释】。默认值只有一处权威：代码里 Config.java 的默认值。
            #  所以本文件里不会出现「第二份默认值」—— 改了代码默认值，这里没被取消注释的项跟着变。
            #
            #  怎么改：
            #    1. 删掉下面某一行行首的 '#'
            #    2. 把等号右边的值改成你要的
            #    3. 保存，然后【重启游戏】（本表只在启动时读一次）
            #
            #  规则：
            #    · 语法：  键 = 值        '#' 之后到行尾都算注释，行内注释同样支持
            #    · 本文件【永远不会被自动重写】。你写进去的值不会被覆盖、不会被重置。
            #    · 值超出合法范围 → 夹到范围边界并打一条警告
            #      无法解析、键名不认识、该项不开放 → 忽略该行并打一条警告
            #      以上任何一种都只会「退回代码默认值」，不会让游戏启动失败。
            #    · 生效情况每次启动都会打进日志，搜 '[配置]' 即可看到。
            # ============================================================================

            """;

    private ConfigFileLayer()
    {
    }

    /** 覆盖表位置：{@code config/<modid>/settings.toml}。 */
    public static Path resolveFile()
    {
        return FMLPaths.CONFIGDIR.get().resolve(DIR_NAME).resolve(FILE_NAME);
    }

    /**
     * 读覆盖表并写进 spec 的生效值。由 {@code Config} 的静态初始化块在读取字段<b>之前</b>调用，
     * 客户端断开连接后恢复本地值时也走这里。
     * <p>
     * <b>语义：本文件就是全部真相</b> —— 先把所有可覆盖项退回出厂默认值再读，
     * 所以「文件里没写的键」等于默认值，而不是「保留上一次的值」。
     * <p>
     * 文件不存在 → 生成模板，本次全部使用代码默认值。
     */
    public static void apply(final CfgSpec spec)
    {
        Path file = null;

        try
        {
            // ⛔ 这一步不能省，而且必须在读文件之前。CfgSpec#apply 只按键改写，
            // 文件里没写的键会保留原值 —— 而模板里 88 项默认可覆盖项通常只有寥寥几行被取消注释，
            // 漏掉这步的后果就是：联机断开后重读本地文件，剩下几十项继续沿用服务端下发的值。
            spec.resetOverridableToDefaults();

            file = resolveFile();

            if (!Files.exists(file))
            {
                writeTemplate(spec, file);
                LOGGER.info("[配置] 本次没有覆盖表，全部使用代码默认值。");
                return;
            }

            List<String> problems = new ArrayList<>();
            Map<String, String> overrides = parse(file, problems);
            CfgSpec.Report report = spec.apply(overrides);

            for (String problem : problems)
            {
                LOGGER.warn("[配置] {}", problem);
            }
            LOGGER.info("[配置] {} —— {}", file, report.summary());
            for (String detail : report.details())
            {
                LOGGER.warn("[配置] {}", detail);
            }
        }
        catch (Throwable t)
        {
            LOGGER.warn("[配置] 覆盖表读取失败，全部使用代码默认值: {}", file, t);
        }
    }

    /** 解析成一个「键 → 原始字符串」的表；问题写进 {@code problems}，不抛异常。 */
    static Map<String, String> parse(final Path file, final List<String> problems) throws IOException
    {
        Map<String, String> out = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        for (int i = 0; i < lines.size(); i++)
        {
            String line = lines.get(i);
            if (i == 0 && !line.isEmpty() && line.charAt(0) == (char) 0xFEFF)
            {
                line = line.substring(1); // 记事本存出来的 UTF-8 BOM
            }

            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#"))
            {
                continue;
            }

            int eq = trimmed.indexOf('=');
            if (eq < 0)
            {
                problems.add("第 " + (i + 1) + " 行：没有 '='，已忽略 → " + trimmed);
                continue;
            }

            String key = trimmed.substring(0, eq).strip();
            String value = trimmed.substring(eq + 1).strip();

            if (key.isEmpty())
            {
                problems.add("第 " + (i + 1) + " 行：键名为空，已忽略");
                continue;
            }

            // 值不是引号开头时才剥行内注释，免得把字符串里的 '#' 也吃掉。
            if (!value.startsWith("\""))
            {
                int hash = value.indexOf('#');
                if (hash >= 0)
                {
                    value = value.substring(0, hash).strip();
                }
            }

            if (value.isEmpty())
            {
                problems.add("第 " + (i + 1) + " 行：'" + key + "' 的值为空，已忽略");
                continue;
            }

            if (out.put(key, value) != null)
            {
                problems.add("第 " + (i + 1) + " 行：键 '" + key + "' 重复，以后出现的那次为准");
            }
        }

        return out;
    }

    static void appendHeader(final StringBuilder sb)
    {
        sb.append(HEADER);
    }

    /**
     * 生成模板。<b>只在文件不存在时调用</b>。
     * <p>
     * 流程 = 写临时文件 → 原子改名。中途失败不留下半个文件，也不影响启动。
     */
    private static void writeTemplate(final CfgSpec spec, final Path file)
    {
        Path tmp = null;

        try
        {
            Path dir = file.getParent();
            Files.createDirectories(dir);
            tmp = dir.resolve(FILE_NAME + ".tmp");
            Files.write(tmp, spec.templateText().getBytes(StandardCharsets.UTF_8));

            try
            {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (FileAlreadyExistsException e)
            {
                // 竞态：别人（或上一次启动）已经建好了，让他的。
                Files.deleteIfExists(tmp);
                return;
            }
            catch (AtomicMoveNotSupportedException e)
            {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }

            LOGGER.info("[配置] 已生成数值覆盖表模板 {}", file);
        }
        catch (Throwable t)
        {
            LOGGER.warn("[配置] 模板生成失败（不影响启动，继续使用代码默认值）: {}", file, t);
            if (tmp != null)
            {
                try
                {
                    Files.deleteIfExists(tmp);
                }
                catch (IOException ignored)
                {
                    // 清不掉就算了，下次写会覆盖它
                }
            }
        }
    }
}
