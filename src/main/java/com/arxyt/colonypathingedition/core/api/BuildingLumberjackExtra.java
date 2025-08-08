package com.arxyt.colonypathingedition.core.api;

import net.minecraft.core.BlockPos;

public interface BuildingLumberjackExtra {
    BlockPos getLastTree();
    void thisTreeToLast();
    BlockPos getThisTree();
    void setThisTree(BlockPos treePos);
}
