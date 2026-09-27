package com.darkness.jujutsukaisenzone.technique.se;

/**
 * 御厨子「收刀」窄口：解打出开刀印后，可对印记目标远程施放捌。
 */
public interface ShrineFinishCutSeMechanic extends SeMechanic
{
    /** 开刀印持续 tick。 */
    int markDurationTicks();

    /** 对带印目标施放捌的最大距离（格）。 */
    double remoteCastRange();
}
