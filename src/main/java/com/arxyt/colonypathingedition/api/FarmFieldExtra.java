package com.arxyt.colonypathingedition.api;

import com.minecolonies.api.colony.IColony;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface FarmFieldExtra {
    /** 设置轮作季节数（限制在 1–4） */
    void setSeasonCount(int num);

    /** 获取轮作季节数 */
    int getSeasonCount();

    /** 设置当前所在季节（自动限定范围） */
    void setCurrentSeason(int season);

    /** 获取当前所在季节 */
    int getCurrentSeason();

    /** 设置当前天数（自动限定范围） */
    void setCurrentDay(int day);

    /** 获取当前天数 */
    int getCurrentDay();

    // ======== 每个季节的设置 ========

    /** 设置指定季节持续的天数 */
    void setSeasonDuration(int season, int days);

    /** 获取指定季节的天数（默认 1） */
    int getSeasonDuration(int season);

    /** 设置指定季节的种子 */
    void setSeasonSeed(int season, @NotNull ItemStack stack);

    /** 获取指定季节的种子 */
    @NotNull ItemStack getSeasonSeed(int season);

    void advanceDay(IColony colony);
}
