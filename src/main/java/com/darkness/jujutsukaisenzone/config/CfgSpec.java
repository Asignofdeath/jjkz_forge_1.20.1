package com.darkness.jujutsukaisenzone.config;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 本模组自己的配置项容器（替代 Forge 的 {@code ForgeConfigSpec}）。
 * <p>
 * <b>为什么不用 Forge 那套</b>：Forge 的 {@code ConfigTracker} 加载配置后<b>立刻</b> {@code save()}，
 * 而 {@code Files.newOutputStream} 是「先截断成 0 字节、再写」且非原子，加上 {@code ConfigWatcher}
 * 跑在别的线程 ⇒ 可能读到截断/半写的文件 ⇒ 判定「not correct」⇒ 备份 + 用默认值重写，
 * 表现就是「玩家改的值随机被重置」。本类只负责<b>值与校验</b>；文件读写由
 * {@link ConfigFileLayer} 自己来：<b>读一次、不挂监听、不自动写回</b>，从构造上就不存在那个竞态。
 * <p>
 * <b>一个数值只写一次</b>：出厂默认值只在 {@code Config.java} 的 {@code defineInRange(...)} 里写一遍；
 * 给玩家看的覆盖表模板由 {@link #templateText()} 从这些默认值<b>生成</b> ⇒ 不许手写模板文件、
 * 不许在别处再抄一份默认值（抄两份的后果是改一处不生效还不报错）。
 * <p>
 * <b>两类值</b>：
 * <ul>
 *   <li><b>数值</b>（int / double / string）—— 开放给覆盖表；越界会夹回边界并打警告。</li>
 *   <li><b>开关与列表</b>（boolean / list）—— <b>不开放</b>。它们改的是玩法契约
 *       （比如「暴击开不开」「浮动开不开」），开放给玩家等于把刚定好的默认值又留了个洞；
 *       要开放某一项，改它注册处传的 {@code overridable} 即可。</li>
 * </ul>
 * <p>
 * 生效值读取一律走 {@link ConfigValue#get()}：它返回「出厂默认值 或 覆盖表改过的值」，
 * 与「配置有没有被加载」无关 ⇒ 不存在 Forge 那种 {@code get()} 未加载就抛异常的坑。
 */
public final class CfgSpec
{
    /** 插入顺序 = {@code Config.java} 里的定义顺序，模板也按这个顺序排。 */
    private final Map<String, ConfigValue<?>> values = new LinkedHashMap<>();

    private CfgSpec()
    {
    }

    private void register(final ConfigValue<?> value)
    {
        if (values.putIfAbsent(value.getPath(), value) != null)
        {
            // 这一条替代了 Forge BUILDER.build() 的重复键校验：复制粘贴配置项时当场炸，别等到实机。
            throw new IllegalStateException("配置键重复定义: " + value.getPath());
        }
    }

    /** 全部配置项（定义顺序）。 */
    public Collection<ConfigValue<?>> values()
    {
        return Collections.unmodifiableCollection(values.values());
    }

    @Nullable
    public ConfigValue<?> value(final String path)
    {
        return values.get(path);
    }

    /**
     * 导出当前<b>生效值</b>里的可覆盖项：{@code 键 -> 文本}。
     * <p>
     * 这是联机时「服主 / 房主的配置是唯一真相」的搬运工具 —— 服务端登录时把这张表推给客户端。
     * 只含 {@link ConfigValue#isOverridable()} 的项，所以布尔开关与列表不会进表
     * （它们本来就两边一致：都来自同一份代码默认值，且玩家改不动）。
     * <p>
     * 导的是「生效值」而不是「文件里的原始行」：服务端没写的项也要按它的默认值推过去，
     * 否则客户端自己写过的覆盖会留下来 ⇒ 又不是唯一真相了。
     */
    public Map<String, String> exportOverridable()
    {
        Map<String, String> out = new LinkedHashMap<>();
        for (ConfigValue<?> value : values.values())
        {
            if (value.isOverridable())
            {
                out.put(value.getPath(), value.serializeEffective());
            }
        }
        return out;
    }

    /**
     * 把所有可覆盖项退回出厂默认值。
     * <p>
     * ⛔ 客户端套用服务端推送<b>之前</b>必须先走这一步。原因：{@link #apply(Map)} 是「按键改写」，
     * 表里没有的键会保留原值 —— 那样客户端本地 {@code settings.toml} 写过的项、
     * 只要服务端这次没推，就会留下残值。先清空再灌，才能保证结果只取决于服务端。
     */
    public void resetOverridableToDefaults()
    {
        for (ConfigValue<?> value : values.values())
        {
            if (value.isOverridable())
            {
                value.resetToDefault();
            }
        }
    }

    /**
     * 用覆盖表（键 → 文件里的原始字符串）改写生效值。
     * <p>
     * 不认识 / 不开放 / 解析失败 / 越界的项一律<b>不改生效值</b>并记进 {@link Report}，
     * 绝不抛异常 —— 一个手滑的字符不能让游戏起不来。
     */
    public Report apply(final Map<String, String> overrides)
    {
        Report report = new Report();

        for (Map.Entry<String, String> entry : overrides.entrySet())
        {
            ConfigValue<?> value = values.get(entry.getKey());
            if (value == null)
            {
                report.unknown.add(entry.getKey());
                continue;
            }
            if (!value.isOverridable())
            {
                report.locked.add(entry.getKey());
                continue;
            }
            value.tryOverride(entry.getValue(), report);
        }

        return report;
    }

    /**
     * 生成给玩家看的覆盖表模板。
     * <p>
     * ⛔ 每一项都写成<b>注释</b>（行首 {@code #}）。这样文件里<b>不存在第二份生效的默认值</b>：
     * 出厂默认值始终只在代码里一处；玩家想改哪项，取消注释再改即可。
     * 若把默认值写成生效行，以后改代码默认值就对老存档不生效了 —— 那正是要避免的坑。
     */
    public String templateText()
    {
        StringBuilder sb = new StringBuilder(16384);
        ConfigFileLayer.appendHeader(sb);

        for (ConfigValue<?> value : values.values())
        {
            if (!value.isOverridable())
            {
                continue;
            }
            appendItem(sb, value);
        }

        sb.append('\n');
        sb.append("# ============================================================\n");
        sb.append("# 以下【不开放】：开关与列表改的是玩法契约，要改请改代码里的默认值。\n");
        sb.append("# 这里只列出来让你知道当前值，写进本文件也不会生效（启动时会打警告）。\n");
        sb.append("# ============================================================\n");
        for (ConfigValue<?> value : values.values())
        {
            if (value.isOverridable())
            {
                continue;
            }
            sb.append("# ").append(value.getPath()).append(" = ").append(format(value.getDefault())).append('\n');
        }

        return sb.toString();
    }

    private static void appendItem(final StringBuilder sb, final ConfigValue<?> value)
    {
        String comment = value.getComment();
        if (comment != null && !comment.isBlank())
        {
            for (String line : comment.split("\n"))
            {
                sb.append("# ").append(line.strip()).append('\n');
            }
        }
        sb.append("# ").append(value.getPath()).append(" = ").append(format(value.getDefault())).append('\n');
        sb.append('\n');
    }

    private static String format(@Nullable final Object def)
    {
        if (def == null)
        {
            return "null";
        }
        if (def instanceof String s)
        {
            return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
        }
        if (def instanceof List<?> list)
        {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++)
            {
                if (i > 0)
                {
                    sb.append(", ");
                }
                sb.append(format(list.get(i)));
            }
            return sb.append(']').toString();
        }
        return String.valueOf(def);
    }

    /**
     * 一个配置项。
     * <p>
     * {@link #get()} = 生效值，{@link #getDefault()} = 出厂默认值。
     * 业务代码只用 {@code get()}；{@code getDefault()} 只给模板生成与自检看。
     */
    public abstract static class ConfigValue<T>
    {
        private final String path;
        private final T rawDefault;
        @Nullable
        private final String comment;
        private final boolean overridable;
        private T effective;

        private ConfigValue(final String path, final T rawDefault, @Nullable final String comment,
                            final boolean overridable)
        {
            this.path = path;
            this.rawDefault = rawDefault;
            this.comment = comment;
            this.overridable = overridable;
            this.effective = rawDefault;
        }

        /** 生效值（出厂默认值，或被覆盖表改过的值）。 */
        public final T get()
        {
            return effective;
        }

        /** 出厂默认值。⛔ 业务代码别用这个 —— 用了就等于绕开覆盖表。 */
        public final T getDefault()
        {
            return rawDefault;
        }

        public final String getPath()
        {
            return path;
        }

        @Nullable
        public final String getComment()
        {
            return comment;
        }

        public final boolean isOverridable()
        {
            return overridable;
        }

        /** 把文件里的字符串解析成 T；失败返回 {@code null} 并把原因写进 {@code err}。 */
        @Nullable
        abstract T parse(String text, StringBuilder err);

        /**
         * 把值序列化成 {@link #parse} 认得的文本。
         * <p>
         * ⛔ 必须与 {@code parse} <b>严格互逆</b>：联机时服务端推的就是这个文本，
         * 客户端 {@code parse} 不回来就会静默退回默认值 —— 那种 bug 只看日志「无法解析 N 项」才能发现。
         */
        abstract String serialize(T value);

        /**
         * 当前生效值的文本形式（见 {@link CfgSpec#exportOverridable()}）。
         * <p>
         * 放在泛型类内部是为了避开外部的通配符捕获问题（与 {@link #tryOverride} 同理）。
         */
        public final String serializeEffective()
        {
            return serialize(effective);
        }

        /** 生效值退回出厂默认值，丢掉覆盖表 / 服务端推送留下的值。 */
        final void resetToDefault()
        {
            effective = rawDefault;
        }

        /** 越界时夹回合法范围；返回夹过的值，未越界返回 {@code null}。 */
        @Nullable
        T clamp(T value)
        {
            return null;
        }

        /** 供 {@link CfgSpec#apply(Map)} 调用。放在泛型类内部，免得外面陷进通配符地狱。 */
        private void tryOverride(final String text, final Report report)
        {
            StringBuilder err = new StringBuilder();
            T parsed = parse(text, err);
            if (parsed == null)
            {
                report.bad.add(path + " = " + text + "（" + err + "）");
                return;
            }
            T clamped = clamp(parsed);
            if (clamped != null)
            {
                report.clamped.add(path + " = " + parsed + " → " + clamped);
                parsed = clamped;
            }
            effective = parsed;
            report.applied.add(path);
        }
    }

    /** {@code define(path, true)} 的返回值。<b>不开放给覆盖表</b>，见 {@link CfgSpec} 类注释。 */
    public static final class BooleanValue extends ConfigValue<Boolean>
    {
        private BooleanValue(final String path, final boolean def, @Nullable final String comment,
                             final boolean overridable)
        {
            super(path, def, comment, overridable);
        }

        @Override
        @Nullable
        Boolean parse(final String text, final StringBuilder err)
        {
            if ("true".equalsIgnoreCase(text))
            {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(text))
            {
                return Boolean.FALSE;
            }
            err.append("只能是 true / false");
            return null;
        }

        @Override
        String serialize(final Boolean value)
        {
            return String.valueOf(value);
        }
    }

    /** 整数项。 */
    public static final class IntValue extends ConfigValue<Integer>
    {
        private final int min;
        private final int max;

        private IntValue(final String path, final int def, @Nullable final String comment,
                         final boolean overridable, final int min, final int max)
        {
            super(path, def, comment, overridable);
            this.min = min;
            this.max = max;
        }

        @Override
        @Nullable
        Integer parse(final String text, final StringBuilder err)
        {
            try
            {
                return Integer.valueOf(text.strip());
            }
            catch (NumberFormatException e)
            {
                err.append("不是整数：").append(text.strip());
                return null;
            }
        }

        @Override
        @Nullable
        Integer clamp(final Integer value)
        {
            if (value < min)
            {
                return min;
            }
            if (value > max)
            {
                return max;
            }
            return null;
        }

        @Override
        String serialize(final Integer value)
        {
            return Integer.toString(value);
        }
    }

    /** 小数项。 */
    public static final class DoubleValue extends ConfigValue<Double>
    {
        private final double min;
        private final double max;

        private DoubleValue(final String path, final double def, @Nullable final String comment,
                            final boolean overridable, final double min, final double max)
        {
            super(path, def, comment, overridable);
            this.min = min;
            this.max = max;
        }

        @Override
        @Nullable
        Double parse(final String text, final StringBuilder err)
        {
            try
            {
                double parsed = Double.parseDouble(text.strip());
                if (!Double.isFinite(parsed))
                {
                    // parseDouble 认得 "NaN" / "Infinity"，而 NaN 与任何数比较都是 false
                    // ⇒ 夹范围那段会整个失效，必须在这里拦掉。
                    err.append("不是有限数字：").append(text.strip());
                    return null;
                }
                return parsed;
            }
            catch (NumberFormatException e)
            {
                err.append("不是数字：").append(text.strip());
                return null;
            }
        }

        @Override
        @Nullable
        Double clamp(final Double value)
        {
            if (value < min)
            {
                return min;
            }
            if (value > max)
            {
                return max;
            }
            return null;
        }

        @Override
        String serialize(final Double value)
        {
            // Double.toString 给的是最短往返表示，parseDouble 能还原成同一个 double（位级一致）。
            return Double.toString(value);
        }
    }

    /** 字符串项。 */
    public static final class StringValue extends ConfigValue<String>
    {
        private StringValue(final String path, final String def, @Nullable final String comment,
                            final boolean overridable)
        {
            super(path, def, comment, overridable);
        }

        @Override
        @Nullable
        String parse(final String text, final StringBuilder err)
        {
            String t = text.strip();
            if (!t.startsWith("\""))
            {
                return t;
            }
            if (t.length() < 2 || !t.endsWith("\""))
            {
                err.append("引号没有闭合");
                return null;
            }
            return t.substring(1, t.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }

        @Override
        String serialize(final String value)
        {
            // 一律带引号输出：parse 只有见到行首引号才会走「整体取值」分支，
            // 否则首尾空格会被 strip 掉、值里的 '#' 会被当行内注释吃掉 —— 往返就不是无损的了。
            // 转义顺序不能反：必须先转反斜杠，再转引号。
            return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
    }

    /** 列表项。<b>不开放</b>（本文件格式还没做列表语法，也不需要）。 */
    public static final class ListValue extends ConfigValue<List<? extends String>>
    {
        private ListValue(final String path, final List<? extends String> def, @Nullable final String comment)
        {
            super(path, List.copyOf(def), comment, false);
        }

        @Override
        @Nullable
        List<? extends String> parse(final String text, final StringBuilder err)
        {
            err.append("列表项不开放");
            return null;
        }

        @Override
        String serialize(final List<? extends String> value)
        {
            // 列表不开放覆盖 ⇒ 不会进 exportOverridable()，这里只为满足基类。
            return String.join(",", value);
        }
    }

    /** {@link CfgSpec#apply(Map)} 的结果，只用来打日志。 */
    public static final class Report
    {
        private static final int MAX_DETAILS = 20;

        private final List<String> applied = new ArrayList<>();
        private final List<String> unknown = new ArrayList<>();
        private final List<String> locked = new ArrayList<>();
        private final List<String> bad = new ArrayList<>();
        private final List<String> clamped = new ArrayList<>();

        public int appliedCount()
        {
            return applied.size();
        }

        public String summary()
        {
            return "生效 " + applied.size()
                    + " 项 / 未识别键 " + unknown.size()
                    + " / 不开放 " + locked.size()
                    + " / 无法解析 " + bad.size()
                    + " / 越界夹回 " + clamped.size();
        }

        /** 需要人看的明细（生效的不算）。 */
        public List<String> details()
        {
            List<String> out = new ArrayList<>();
            for (String s : unknown)
            {
                out.add("未识别的键（已忽略）: " + s);
            }
            for (String s : locked)
            {
                out.add("该项不开放修改（已忽略）: " + s);
            }
            for (String s : bad)
            {
                out.add("无法解析（已用默认值）: " + s);
            }
            for (String s : clamped)
            {
                out.add("越界已夹回: " + s);
            }
            if (out.size() > MAX_DETAILS)
            {
                List<String> trimmed = new ArrayList<>(out.subList(0, MAX_DETAILS));
                trimmed.add("…… 还有 " + (out.size() - MAX_DETAILS) + " 条，见上方日志");
                return trimmed;
            }
            return out;
        }
    }

    /**
     * 定义配置项的入口，刻意做成和 {@code ForgeConfigSpec.Builder} 同名同形 ——
     * 这样 {@code Config.java} 里 98 条 {@code .comment(...).define*()} 链<b>一个字都不用改</b>，
     * 只换类型名。
     */
    public static final class Builder
    {
        private final CfgSpec spec = new CfgSpec();
        @Nullable
        private String pendingComment;

        public Builder comment(final String comment)
        {
            this.pendingComment = comment;
            return this;
        }

        /** 布尔开关。⛔ 刻意<b>不开放</b>给覆盖表，见 {@link CfgSpec} 类注释。 */
        public BooleanValue define(final String path, final boolean defaultValue)
        {
            BooleanValue value = new BooleanValue(checkPath(path), defaultValue, takeComment(), false);
            spec.register(value);
            return value;
        }

        public StringValue define(final String path, final String defaultValue)
        {
            StringValue value = new StringValue(checkPath(path), defaultValue, takeComment(), true);
            spec.register(value);
            return value;
        }

        public IntValue defineInRange(final String path, final int defaultValue, final int min, final int max)
        {
            if (min > max)
            {
                throw new IllegalArgumentException("配置项 " + path + " 的 min > max");
            }
            IntValue value = new IntValue(checkPath(path), defaultValue, takeComment(), true, min, max);
            spec.register(value);
            return value;
        }

        public DoubleValue defineInRange(final String path, final double defaultValue, final double min,
                                         final double max)
        {
            if (!(min <= max))
            {
                throw new IllegalArgumentException("配置项 " + path + " 的 min > max");
            }
            DoubleValue value = new DoubleValue(checkPath(path), defaultValue, takeComment(), true, min, max);
            spec.register(value);
            return value;
        }

        public ListValue defineListAllowEmpty(final String path, final List<? extends String> defaultValue,
                                              final Predicate<Object> elementValidator)
        {
            // 参数只为了和 Forge 的签名对齐；本模组不使用列表型配置项（内容只用于一条启动日志）。
            ListValue value = new ListValue(checkPath(path), defaultValue, takeComment());
            spec.register(value);
            return value;
        }

        public CfgSpec build()
        {
            return spec;
        }

        @Nullable
        private String takeComment()
        {
            String comment = pendingComment;
            pendingComment = null;
            return comment;
        }

        private static String checkPath(final String path)
        {
            if (path == null || path.isBlank() || path.indexOf('=') >= 0 || !path.equals(path.strip()))
            {
                throw new IllegalArgumentException("非法配置键: " + path);
            }
            return path;
        }
    }
}
