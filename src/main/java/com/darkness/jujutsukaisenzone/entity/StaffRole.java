package com.darkness.jujutsukaisenzone.entity;

import javax.annotation.Nullable;

/**
 * 咒术议会工作人员岗位：同一实体类型，不同交互入口。
 */
public enum StaffRole
{
    /** 占位 / 未指定 */
    GENERIC("generic"),
    /** 接待：引导 */
    RECEPTION("reception"),
    /** 委托：任务告示 */
    CONTRACT("contract"),
    /** 出击：副本入口 */
    DISPATCH("dispatch");

    private final String id;

    StaffRole(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return this.id;
    }

    public String translationKey()
    {
        return "entity.jujutsukaisenzone.jujutsu_council_staff." + this.id;
    }

    public String interactMessageKey()
    {
        return "message.jujutsukaisenzone.council_staff.interact." + this.id;
    }

    public static StaffRole byId(@Nullable String raw)
    {
        if (raw == null || raw.isEmpty())
        {
            return GENERIC;
        }
        String normalized = raw.trim().toLowerCase();
        return switch (normalized)
        {
            case "reception", "接待" -> RECEPTION;
            case "contract", "委托" -> CONTRACT;
            case "dispatch", "出击" -> DISPATCH;
            case "generic", "其他事务" -> GENERIC;
            default ->
            {
                for (StaffRole role : values())
                {
                    if (role.id.equals(normalized))
                    {
                        yield role;
                    }
                }
                yield GENERIC;
            }
        };
    }

    public static boolean isKnownId(@Nullable String raw)
    {
        if (raw == null || raw.isEmpty())
        {
            return false;
        }
        String normalized = raw.trim().toLowerCase();
        return switch (normalized)
        {
            case "reception", "接待",
                 "contract", "委托",
                 "dispatch", "出击",
                 "generic", "其他事务" -> true;
            default ->
            {
                for (StaffRole role : values())
                {
                    if (role.id.equals(normalized))
                    {
                        yield true;
                    }
                }
                yield false;
            }
        };
    }
}
