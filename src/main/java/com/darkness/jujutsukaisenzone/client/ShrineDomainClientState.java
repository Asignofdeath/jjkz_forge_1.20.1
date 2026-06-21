package com.darkness.jujutsukaisenzone.client;

/**
 * 客户端：施法者是否处于自己的伏魔神龛领域存续期（由服务端同步）。
 */
public final class ShrineDomainClientState
{
    private static boolean inOwnActiveShrineDomain;

    private ShrineDomainClientState()
    {
    }

    public static boolean isInOwnActiveShrineDomain()
    {
        return inOwnActiveShrineDomain;
    }

    public static void setInOwnActiveShrineDomain(boolean active)
    {
        inOwnActiveShrineDomain = active;
    }
}
