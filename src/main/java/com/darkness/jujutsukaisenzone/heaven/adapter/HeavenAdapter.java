package com.darkness.jujutsukaisenzone.heaven.adapter;

/**
 * 天道平衡适配器：世界随术师变强的一类规则入口。
 * <p>
 * 对策 SE、外模怪均血缩放等各自实现本接口并登记到 {@link HeavenAdapterRegistry}；
 * 效果逻辑仍落在各子系统，这里只统一叙事与目录。
 */
public interface HeavenAdapter
{
    String id();

    /** 文档 / 调试用短说明。 */
    String description();
}
