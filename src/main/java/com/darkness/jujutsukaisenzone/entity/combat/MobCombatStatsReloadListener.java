package com.darkness.jujutsukaisenzone.entity.combat;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * 加载 {@code data/&lt;namespace&gt;/mob_combat_stats/*.json}。
 * <p>
 * 每个文件可以是：
 * <ul>
 *   <li>直接以实体 id 为键的对象；或</li>
 *   <li>{@code { "entries": { "minecraft:zombie": { ... } } }}</li>
 * </ul>
 * 以 {@code _} 开头的键视为注释忽略。多文件合并，后加载的同 id 覆盖先前的。
 */
public final class MobCombatStatsReloadListener extends SimpleJsonResourceReloadListener
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final String DIRECTORY = "mob_combat_stats";

    public MobCombatStatsReloadListener()
    {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager resourceManager,
                         ProfilerFiller profiler)
    {
        Map<ResourceLocation, MobCombatStats> next = new HashMap<>();
        int fileCount = 0;
        for (Map.Entry<ResourceLocation, JsonElement> file : prepared.entrySet())
        {
            if (!file.getValue().isJsonObject())
            {
                LOGGER.warn("[{}] skip non-object mob combat stats file {}", JuJutsuKaisenZone.MODID, file.getKey());
                continue;
            }
            fileCount++;
            JsonObject root = file.getValue().getAsJsonObject();
            JsonObject entries = root.has("entries") && root.get("entries").isJsonObject()
                    ? root.getAsJsonObject("entries")
                    : root;
            for (Map.Entry<String, JsonElement> entry : entries.entrySet())
            {
                String key = entry.getKey();
                if (key.startsWith("_") || "entries".equals(key))
                {
                    continue;
                }
                ResourceLocation entityId = ResourceLocation.tryParse(key);
                if (entityId == null)
                {
                    LOGGER.warn("[{}] invalid entity id '{}' in {}", JuJutsuKaisenZone.MODID, key, file.getKey());
                    continue;
                }
                if (!entry.getValue().isJsonObject())
                {
                    LOGGER.warn("[{}] stats for {} must be an object in {}", JuJutsuKaisenZone.MODID, key, file.getKey());
                    continue;
                }
                next.put(entityId, parseStats(entry.getValue().getAsJsonObject()));
            }
        }
        MobCombatStatsRegistry.replaceAll(next);
        LOGGER.info("[{}] loaded {} mob combat stats entr{} from {} file{}",
                JuJutsuKaisenZone.MODID,
                next.size(),
                next.size() == 1 ? "y" : "ies",
                fileCount,
                fileCount == 1 ? "" : "s");
    }

    private static MobCombatStats parseStats(JsonObject json)
    {
        return new MobCombatStats(
                readPoint(json, "physical_resistance"),
                readPoint(json, "technique_resistance"),
                readPoint(json, "physical_penetration"),
                readPoint(json, "technique_penetration"),
                parsePoise(json));
    }

    /**
     * 解析可选的 {@code poise} 块。返回 {@code null} 表示<b>没写</b>——
     * 与「写了 value: 0」（显式关闭）是两回事，调用方靠 {@link MobCombatStats#hasPoiseBlock()} 区分。
     */
    private static PoiseStats parsePoise(JsonObject json)
    {
        if (!json.has("poise") || !json.get("poise").isJsonObject())
        {
            return null;
        }
        JsonObject block = json.getAsJsonObject("poise");
        return new PoiseStats(
                readDouble(block, "value", 0.0D),
                readDouble(block, "regen", PoiseStats.DEFAULT_REGEN),
                readDouble(block, "damage_reduction", PoiseStats.REDUCTION_DERIVED),
                readBool(block, "specialized", true),
                readDouble(block, "resistance", PoiseStats.DEFAULT_POISE_RESISTANCE));
    }

    private static double readPoint(JsonObject json, String key)
    {
        if (!json.has(key))
        {
            return 0.0D;
        }
        return GsonHelper.getAsDouble(json, key);
    }

    private static double readDouble(JsonObject json, String key, double fallback)
    {
        if (!json.has(key) || !json.get(key).isJsonPrimitive())
        {
            return fallback;
        }
        return GsonHelper.getAsDouble(json, key, fallback);
    }

    private static boolean readBool(JsonObject json, String key, boolean fallback)
    {
        if (!json.has(key) || !json.get(key).isJsonPrimitive())
        {
            return fallback;
        }
        return GsonHelper.getAsBoolean(json, key, fallback);
    }
}
