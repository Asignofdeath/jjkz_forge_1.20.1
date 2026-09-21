package com.darkness.jujutsukaisenzone.technique.skill;

/**
 * 「指控」证据结论。
 */
public enum JudgmentAccuseEvidence
{
    CONFIRMED(0, "罪行确认", "Crime Confirmed"),
    CANNOT_CONFIRM(1, "无法确认", "Cannot Confirm");

    private final int id;
    private final String chinese;
    private final String english;

    JudgmentAccuseEvidence(int id, String chinese, String english)
    {
        this.id = id;
        this.chinese = chinese;
        this.english = english;
    }

    public int id()
    {
        return id;
    }

    public String chinese()
    {
        return chinese;
    }

    public String english()
    {
        return english;
    }

    public static JudgmentAccuseEvidence fromId(int id)
    {
        return id == CONFIRMED.id ? CONFIRMED : CANNOT_CONFIRM;
    }
}
