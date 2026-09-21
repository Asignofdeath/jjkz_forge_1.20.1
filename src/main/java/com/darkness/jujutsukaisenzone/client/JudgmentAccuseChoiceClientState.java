package com.darkness.jujutsukaisenzone.client;

/**
 * 「指控」：被告客户端选择窗口状态。
 */
public final class JudgmentAccuseChoiceClientState
{
    private static boolean active;
    private static int remainingTicks;
    private static String crimeChinese = "";
    private static String crimeEnglish = "";

    private JudgmentAccuseChoiceClientState()
    {
    }

    public static void applySync(boolean active, int remainingTicks, String crimeChinese, String crimeEnglish)
    {
        JudgmentAccuseChoiceClientState.active = active;
        JudgmentAccuseChoiceClientState.remainingTicks = remainingTicks;
        JudgmentAccuseChoiceClientState.crimeChinese = crimeChinese != null ? crimeChinese : "";
        JudgmentAccuseChoiceClientState.crimeEnglish = crimeEnglish != null ? crimeEnglish : "";
        if (!active)
        {
            JudgmentAccuseChoiceHud.hide();
        }
    }

    public static boolean isActive()
    {
        return active;
    }

    public static int remainingTicks()
    {
        return remainingTicks;
    }

    public static String crimeChinese()
    {
        return crimeChinese;
    }

    public static String crimeEnglish()
    {
        return crimeEnglish;
    }
}
