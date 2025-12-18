package com.arxyt.colonypathingedition.core.colony.module;

import com.arxyt.colonypathingedition.core.window.TavernRecruitModuleWindow;
import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModuleView;
import com.minecolonies.api.colony.buildings.modules.IBuildingModuleView;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public class TavernRecruitModuleView extends AbstractBuildingModuleView implements IBuildingModuleView {
    @Override
    public void deserialize(@NotNull FriendlyByteBuf buf) {

    }

    @Override
    public BOWindow getWindow() {
        return new TavernRecruitModuleWindow(buildingView);
    }

    @Override
    public String getDesc() {
        return "com.arxyt.colonypathingedition.core.tavern_recruit";
    }
}
