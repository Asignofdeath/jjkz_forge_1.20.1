package com.darkness.jujutsukaisenzone.player;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家「罪行记录」：每次新获得【罪恶者】状态时追加一条。
 */
public final class PlayerCrimeRecordData
{
    private static final String TAG_ROOT = "jujutsukaisenzone_crime_records";
    /** 每个罪行类别最多保留的记录条数；超出时丢弃最旧的一条。 */
    private static final int MAX_RECORDS_PER_CATEGORY = 3;

    private PlayerCrimeRecordData()
    {
    }

    public enum Category
    {
        NEUTRAL_KILL("neutral_kill", "击杀中立及好生物"),
        JUDGMENT_TECHNIQUE("judgment_technique", "被审判术式审判"),
        ILLEGAL_LOGGING("illegal_logging", "盗伐林木"),
        ILLEGAL_MINING("illegal_mining", "非法采矿"),
        THEFT("theft", "盗窃 / 侵占"),
        PUBLIC_SAFETY("public_safety", "危害公共安全"),
        CORPSE_DESECRATION("corpse_desecration", "侮辱尸体"),
        DANGEROUS_WEAPON("dangerous_weapon", "非法制造、买卖危险器具"),
        ILLEGAL_BORDER_CROSSING("illegal_border_crossing", "非法穿越边境");

        private final String id;
        private final String displayName;

        Category(String id, String displayName)
        {
            this.id = id;
            this.displayName = displayName;
        }

        public String id()
        {
            return id;
        }

        public String displayName()
        {
            return displayName;
        }

        static Category fromId(String id)
        {
            for (Category category : values())
            {
                if (category.id.equals(id))
                {
                    return category;
                }
            }
            return JUDGMENT_TECHNIQUE;
        }
    }

    public enum TimeOfDay
    {
        MORNING("早"),
        NOON("中"),
        EVENING("晚");

        private final String label;

        TimeOfDay(String label)
        {
            this.label = label;
        }

        public String label()
        {
            return label;
        }

        static TimeOfDay fromDayTime(long dayTime)
        {
            long time = Math.floorMod(dayTime, 24000L);
            if (time < 8000L)
            {
                return MORNING;
            }
            if (time < 16000L)
            {
                return NOON;
            }
            return EVENING;
        }

        public String englishLabel()
        {
            return switch (this)
            {
                case MORNING -> "Morning";
                case NOON -> "Noon";
                case EVENING -> "Evening";
            };
        }

        public static TimeOfDay pickRandom(RandomSource random)
        {
            TimeOfDay[] values = values();
            return values[random.nextInt(values.length)];
        }
    }

    public record Record(TimeOfDay timeOfDay, BlockPos position, String description)
    {
        private static final String TAG_TIME = "time";
        private static final String TAG_X = "x";
        private static final String TAG_Y = "y";
        private static final String TAG_Z = "z";
        private static final String TAG_DESC = "desc";

        CompoundTag save()
        {
            CompoundTag tag = new CompoundTag();
            tag.putString(TAG_TIME, timeOfDay.name());
            tag.putInt(TAG_X, position.getX());
            tag.putInt(TAG_Y, position.getY());
            tag.putInt(TAG_Z, position.getZ());
            tag.putString(TAG_DESC, description);
            return tag;
        }

        static Record load(CompoundTag tag)
        {
            TimeOfDay timeOfDay;
            try
            {
                timeOfDay = TimeOfDay.valueOf(tag.getString(TAG_TIME));
            }
            catch (IllegalArgumentException ignored)
            {
                timeOfDay = TimeOfDay.MORNING;
            }
            BlockPos pos = new BlockPos(tag.getInt(TAG_X), tag.getInt(TAG_Y), tag.getInt(TAG_Z));
            String description = tag.getString(TAG_DESC);
            return new Record(timeOfDay, pos, description);
        }

        public String formatLine()
        {
            return formatAccusationLine(timeOfDay, position, description);
        }

        public String formatEnglishLine()
        {
            return formatAccusationEnglishLine(timeOfDay, position, description);
        }
    }

    public static String formatAccusationLine(TimeOfDay timeOfDay, BlockPos position, String description)
    {
        return timeOfDay.label()
                + " ("
                + position.getX() + ", " + position.getY() + ", " + position.getZ()
                + ") "
                + description;
    }

    public static String formatAccusationEnglishLine(TimeOfDay timeOfDay, BlockPos position, String description)
    {
        return timeOfDay.englishLabel()
                + " ("
                + position.getX() + ", " + position.getY() + ", " + position.getZ()
                + ") "
                + description;
    }

    public static BlockPos randomFakePosition(RandomSource random)
    {
        return new BlockPos(
                random.nextInt(6001) - 3000,
                random.nextInt(61) + 60,
                random.nextInt(6001) - 3000);
    }

    public static void addRecord(ServerPlayer player, Category category, long dayTime, BlockPos position, String description)
    {
        if (category == null || position == null || description == null || description.isBlank())
        {
            return;
        }

        CompoundTag root = player.getPersistentData().getCompound(TAG_ROOT);
        ListTag recordsTag = root.getList(category.id, Tag.TAG_COMPOUND);
        while (recordsTag.size() >= MAX_RECORDS_PER_CATEGORY)
        {
            recordsTag.remove(0);
        }
        recordsTag.add(new Record(TimeOfDay.fromDayTime(dayTime), position, description).save());
        root.put(category.id, recordsTag);
        player.getPersistentData().put(TAG_ROOT, root);
    }

    public static Map<Category, List<Record>> getAllRecords(ServerPlayer player)
    {
        Map<Category, List<Record>> result = new LinkedHashMap<>();
        CompoundTag root = player.getPersistentData().getCompound(TAG_ROOT);
        for (Category category : Category.values())
        {
            ListTag recordsTag = root.getList(category.id, Tag.TAG_COMPOUND);
            if (recordsTag.isEmpty())
            {
                continue;
            }
            List<Record> records = new ArrayList<>(recordsTag.size());
            for (Tag tag : recordsTag)
            {
                if (tag instanceof CompoundTag compoundTag)
                {
                    records.add(Record.load(compoundTag));
                }
            }
            if (!records.isEmpty())
            {
                result.put(category, records);
            }
        }
        return result;
    }

    public static List<Record> getAllRecordsFlat(ServerPlayer player)
    {
        List<Record> allRecords = new ArrayList<>();
        for (List<Record> records : getAllRecords(player).values())
        {
            allRecords.addAll(records);
        }
        return allRecords;
    }

    public static void appendDisplayLines(ServerPlayer subject, List<Component> lines)
    {
        Map<Category, List<Record>> recordsByCategory = getAllRecords(subject);
        if (recordsByCategory.isEmpty())
        {
            return;
        }

        lines.add(Component.literal("--- 罪行记录 ---"));
        for (Map.Entry<Category, List<Record>> entry : recordsByCategory.entrySet())
        {
            Category category = entry.getKey();
            List<Record> records = entry.getValue();
            lines.add(Component.literal(category.displayName() + "，" + records.size() + "条记录在案"));
            for (Record record : records)
            {
                lines.add(Component.literal("  " + record.formatLine()));
            }
        }
    }
}
