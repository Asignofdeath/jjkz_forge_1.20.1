package com.darkness.jujutsukaisenzone.technique.skill;

/**
 * 「指控」辩护选项。
 */
public enum JudgmentAccuseResponse
{
    DENY(0, "否认", "Deny"),
    LIE(1, "撒谎", "Lie"),
    CONFESS(2, "坦白", "Confess"),
    NONE(3, "未作回应", "No Response");

    private final int id;
    private final String chinese;
    private final String english;

    JudgmentAccuseResponse(int id, String chinese, String english)
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

    public static JudgmentAccuseResponse fromId(int id)
    {
        for (JudgmentAccuseResponse response : values())
        {
            if (response.id == id)
            {
                return response;
            }
        }
        return NONE;
    }
}
