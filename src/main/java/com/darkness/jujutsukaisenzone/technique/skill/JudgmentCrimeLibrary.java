package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.player.PlayerCrimeRecordData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 「审判」罪名词汇库（中英双语）。
 */
public final class JudgmentCrimeLibrary
{
    public record CrimeEntry(String chinese, String english)
    {
        public Component toChatMessage()
        {
            return Component.literal("罪名：")
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(chinese).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(english).withStyle(ChatFormatting.WHITE));
        }

        /** 本条指控对【死刑】概率的额外加成（%）。 */
        public double deathSentenceBonusPercent()
        {
            return getDeathSentenceBonusPercent(chinese);
        }
    }

    /**
     * 重罪对【死刑】概率的额外加成（%）。
     * 匹配时取指控文案（含时间坐标前缀）中的最高档加成。
     */
    private static final Map<String, Double> DEATH_SENTENCE_BONUS_BY_CRIME = new LinkedHashMap<>();

    static
    {
        putDeathBonus("虐杀无辜", 40.0D);
        putDeathBonus("劫掠村庄", 35.0D);
        putDeathBonus("纵火", 35.0D);
        putDeathBonus("引爆TNT或末地水晶", 35.0D);
        putDeathBonus("故意伤害", 30.0D);
        putDeathBonus("非法拘禁", 25.0D);
        putDeathBonus("亵渎术式", 25.0D);
        putDeathBonus("击杀中立及好生物", 20.0D);
        putDeathBonus("恶意破坏", 20.0D);
        putDeathBonus("聚众闹事", 20.0D);
        putDeathBonus("私藏咒物", 15.0D);
        putDeathBonus("破坏遗迹", 15.0D);
        putDeathBonus("教唆", 10.0D);
        putDeathBonus("伪造证词", 10.0D);
        putDeathBonus("走私禁品", 10.0D);
        putDeathBonus("盗取宝箱", 10.0D);
    }

    private static void putDeathBonus(String crimeKeyword, double bonusPercent)
    {
        DEATH_SENTENCE_BONUS_BY_CRIME.put(crimeKeyword, bonusPercent);
    }

    private static final List<CrimeEntry> CRIMES = List.of(
            new CrimeEntry("劫掠村庄", "Raiding Villages"),
            new CrimeEntry("虐杀无辜", "Slaughtering Innocents"),
            new CrimeEntry("盗取宝箱", "Stealing from Chests"),
            new CrimeEntry("教唆", "Incitement"),
            new CrimeEntry("贪婪", "Greed"),
            new CrimeEntry("故意伤害", "Intentional Harm"),
            new CrimeEntry("破坏遗迹", "Desecrating Ruins"),
            new CrimeEntry("私藏咒物", "Hoarding Cursed Relics"),
            new CrimeEntry("伪造证词", "Perjury"),
            new CrimeEntry("聚众闹事", "Inciting a Riot"),
            new CrimeEntry("纵火", "Arson"),
            new CrimeEntry("亵渎术式", "Blasphemy against Technique"),
            new CrimeEntry("私猎禁兽", "Poaching Forbidden Beasts"),
            new CrimeEntry("走私禁品", "Smuggling Contraband"),
            new CrimeEntry("非法拘禁", "Unlawful Imprisonment"),
            new CrimeEntry("恶意破坏", "Malicious Destruction"),
            new CrimeEntry("见死不救", "Willful Neglect"),
            new CrimeEntry("击杀中立及好生物", "Killing Neutral and Friendly Creatures"),
            new CrimeEntry("盗伐林木", "Illegal Logging"),
            new CrimeEntry("非法采矿", "Illegal Mining"),
            new CrimeEntry("盗窃 / 侵占", "Theft / Misappropriation"),
            new CrimeEntry("引爆TNT或末地水晶", "Detonating TNT or End Crystals"),
            new CrimeEntry("食用怪物掉落物", "Consuming Monster Drops"),
            new CrimeEntry("合成武器", "Crafting Weapons"),
            new CrimeEntry("附魔武器", "Enchanting Weapons"),
            new CrimeEntry("购买武器", "Purchasing Weapons"),
            new CrimeEntry("进入下界", "Entering the Nether"),
            new CrimeEntry("进入末地", "Entering the End"));

    private JudgmentCrimeLibrary()
    {
    }

    public static CrimeEntry pickRandom(RandomSource random)
    {
        return CRIMES.get(random.nextInt(CRIMES.size()));
    }

    /**
     * 为被告挑选罪名：玩家目标时 50% 从罪行记录、50% 从词汇库（均带时间+坐标格式）；
     * 非玩家或无可用记录时从词汇库挑选并伪造时间+坐标。
     */
    public static CrimeEntry pickForDefendant(LivingEntity defendant, RandomSource random)
    {
        if (defendant instanceof ServerPlayer player && random.nextBoolean())
        {
            List<PlayerCrimeRecordData.Record> records = PlayerCrimeRecordData.getAllRecordsFlat(player);
            if (!records.isEmpty())
            {
                PlayerCrimeRecordData.Record record = records.get(random.nextInt(records.size()));
                return new CrimeEntry(record.formatLine(), record.formatEnglishLine());
            }
        }
        return pickRandomWithFakeRecord(random);
    }

    private static CrimeEntry pickRandomWithFakeRecord(RandomSource random)
    {
        CrimeEntry base = pickRandom(random);
        PlayerCrimeRecordData.TimeOfDay timeOfDay = PlayerCrimeRecordData.TimeOfDay.pickRandom(random);
        BlockPos position = PlayerCrimeRecordData.randomFakePosition(random);
        return new CrimeEntry(
                PlayerCrimeRecordData.formatAccusationLine(timeOfDay, position, base.chinese()),
                PlayerCrimeRecordData.formatAccusationEnglishLine(timeOfDay, position, base.english()));
    }

    public static void announceAccusation(ServerPlayer caster, LivingEntity target, CrimeEntry crime)
    {
        Component message = accusationMessage(crime);
        caster.sendSystemMessage(message);
        if (target instanceof ServerPlayer targetPlayer && !targetPlayer.getUUID().equals(caster.getUUID()))
        {
            targetPlayer.sendSystemMessage(message);
        }
    }

    public static Component accusationMessage(CrimeEntry crime)
    {
        return Component.literal("【指控】审判者指控：")
                .withStyle(ChatFormatting.DARK_RED)
                .append(Component.literal(crime.chinese()).withStyle(ChatFormatting.RED))
                .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(crime.english()).withStyle(ChatFormatting.WHITE));
    }

    public static void announceRandomCrime(ServerPlayer caster)
    {
        caster.sendSystemMessage(pickRandomWithFakeRecord(caster.getRandom()).toChatMessage());
    }

    /**
     * 根据指控文案（中文）查询【死刑】概率加成；无匹配重罪时返回 0。
     */
    public static double getDeathSentenceBonusPercent(String accusationChinese)
    {
        if (accusationChinese == null || accusationChinese.isBlank())
        {
            return 0.0D;
        }

        double bonus = 0.0D;
        for (Map.Entry<String, Double> entry : DEATH_SENTENCE_BONUS_BY_CRIME.entrySet())
        {
            if (accusationChinese.contains(entry.getKey()))
            {
                bonus = Math.max(bonus, entry.getValue());
            }
        }
        return bonus;
    }

    public static double getDeathSentenceBonusPercent(CrimeEntry crime)
    {
        return crime == null ? 0.0D : crime.deathSentenceBonusPercent();
    }
}
