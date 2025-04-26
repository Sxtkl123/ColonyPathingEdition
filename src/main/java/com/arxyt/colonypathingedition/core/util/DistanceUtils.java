package com.arxyt.colonypathingedition.core.util;

import net.minecraft.core.BlockPos;

public class DistanceUtils {
    /**
     * 曼哈顿距离 (L1)
     */
    public static double manhattanDistance(int x, int y, int z, BlockPos pos) {
        return Math.abs(x - pos.getX()) + Math.abs(y - pos.getY())/5.0 + Math.abs(z - pos.getZ());
    }

    /**
     * 欧氏距离 (L2)
     */
    public static double dist(int x, int y, int z, BlockPos pos) {
        double dx = x - pos.getX();
        double dy = (y - pos.getY())/5.0;
        double dz = z - pos.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * 切比雪夫距离 (L∞)
     */
    public static double chebyshevDistance(int x, int y, int z, BlockPos pos) {
        return Math.max(
                Math.max(Math.abs(x - pos.getX()), Math.abs(y - pos.getY())/5.0),
                Math.abs(z - pos.getZ())
        );
    }

}

