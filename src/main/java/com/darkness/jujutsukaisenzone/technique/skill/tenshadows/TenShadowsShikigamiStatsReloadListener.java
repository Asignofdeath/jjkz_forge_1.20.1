package com.darkness.jujutsukaisenzone.technique.skill.tenshadows;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
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

import java.util.EnumMap;
import java.util.Map;

/**
 * 加载 {@code data/<namespace>/ten_shadows_shikigami_stats/*.json}。
 * <p>
 * 约定文件名：
 * <ul>
 *   <li>{@code untamed.json} — 未调伏固定面板（不含魔虚罗）</li>
 *   <li>{@code tamed_base.json} — 已调伏基础面板 + 可选 {@code owner_scaling}</li>
 * </ul>
 */
public final class TenShadowsShikigamiStatsReloadListener extends SimpleJsonResourceReloadListener
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final String DIRECTORY = "ten_shadows_shikigami_stats";

    public TenShadowsShikigamiStatsReloadListener()
    {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager resourceManager,
                         ProfilerFiller profiler)
    {
        Map<TenShadowsShikigami, TenShadowsShikigamiStatRow> untamed = new EnumMap<>(TenShadowsShikigami.class);
        Map<TenShadowsShikigami, TenShadowsShikigamiStatRow> tamed = new EnumMap<>(TenShadowsShikigami.class);
        TenShadowsOwnerScaling scaling = TenShadowsOwnerScaling.DEFAULT;

        for (Map.Entry<ResourceLocation, JsonElement> file : prepared.entrySet())
        {
            if (!file.getValue().isJsonObject())
            {
                LOGGER.warn("[{}] skip non-object ten shadows stats file {}", JuJutsuKaisenZone.MODID, file.getKey());
                continue;
            }
            JsonObject root = file.getValue().getAsJsonObject();
            String path = file.getKey().getPath();
            boolean isUntamed = path.endsWith("untamed") || path.contains("untamed");
            boolean isTamed = path.endsWith("tamed_base") || path.contains("tamed_base");
            if (!isUntamed && !isTamed)
            {
                LOGGER.warn("[{}] unrecognized ten shadows stats file {}, expected untamed/tamed_base",
                        JuJutsuKaisenZone.MODID, file.getKey());
                continue;
            }
            if (isTamed && root.has("owner_scaling") && root.get("owner_scaling").isJsonObject())
            {
                scaling = parseScaling(root.getAsJsonObject("owner_scaling"));
            }
            JsonObject entries = root.has("entries") && root.get("entries").isJsonObject()
                    ? root.getAsJsonObject("entries")
                    : root;
            Map<TenShadowsShikigami, TenShadowsShikigamiStatRow> target = isUntamed ? untamed : tamed;
            for (Map.Entry<String, JsonElement> entry : entries.entrySet())
            {
                String key = entry.getKey();
                if (key.startsWith("_") || "entries".equals(key) || "owner_scaling".equals(key))
                {
                    continue;
                }
                TenShadowsShikigami shikigami = TenShadowsShikigami.byId(key);
                if (shikigami == null)
                {
                    LOGGER.warn("[{}] unknown shikigami id '{}' in {}", JuJutsuKaisenZone.MODID, key, file.getKey());
                    continue;
                }
                if (shikigami == TenShadowsShikigami.MAHORAGA)
                {
                    LOGGER.warn("[{}] ignore mahoraga in {} (uses fixed profile)", JuJutsuKaisenZone.MODID, file.getKey());
                    continue;
                }
                if (!entry.getValue().isJsonObject())
                {
                    continue;
                }
                target.put(shikigami, parseRow(entry.getValue().getAsJsonObject()));
            }
        }

        TenShadowsShikigamiStatsRegistry.replace(untamed, tamed, scaling);
        LOGGER.info("[{}] ten shadows stats: untamed={} tamed_base={}",
                JuJutsuKaisenZone.MODID, untamed.size(), tamed.size());
    }

    private static TenShadowsShikigamiStatRow parseRow(JsonObject json)
    {
        return new TenShadowsShikigamiStatRow(
                readDouble(json, "max_health", 20.0D),
                readDouble(json, "armor", 0.0D),
                readDouble(json, "physical_resistance", 0.0D),
                readDouble(json, "technique_resistance", 0.0D),
                readDouble(json, "physical_penetration", 0.0D),
                readDouble(json, "technique_penetration", 0.0D),
                (int) Math.round(readDouble(json, "physical_power", 0.0D)),
                (int) Math.round(readDouble(json, "technique_power", 0.0D)));
    }

    private static TenShadowsOwnerScaling parseScaling(JsonObject json)
    {
        TenShadowsOwnerScaling d = TenShadowsOwnerScaling.DEFAULT;
        return new TenShadowsOwnerScaling(
                readDouble(json, "physical_power_per_owner_physical", d.physicalPowerPerOwnerPhysical()),
                readDouble(json, "technique_power_per_owner_technique", d.techniquePowerPerOwnerTechnique()),
                readDouble(json, "max_health_per_owner_physical", d.maxHealthPerOwnerPhysical()),
                readDouble(json, "armor_per_owner_physical", d.armorPerOwnerPhysical()),
                readDouble(json, "physical_resistance_per_owner_physical", d.physicalResistancePerOwnerPhysical()),
                readDouble(json, "technique_resistance_per_owner_technique", d.techniqueResistancePerOwnerTechnique()),
                readDouble(json, "physical_penetration_per_owner_physical", d.physicalPenetrationPerOwnerPhysical()),
                readDouble(json, "technique_penetration_per_owner_technique", d.techniquePenetrationPerOwnerTechnique()));
    }

    private static double readDouble(JsonObject json, String key, double fallback)
    {
        if (!json.has(key))
        {
            return fallback;
        }
        return GsonHelper.getAsDouble(json, key);
    }
}
