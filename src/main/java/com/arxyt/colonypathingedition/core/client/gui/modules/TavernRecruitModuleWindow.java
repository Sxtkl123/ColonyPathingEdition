package com.arxyt.colonypathingedition.core.client.gui.modules;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.core.client.gui.AbstractModuleWindow;

public class TavernRecruitModuleWindow extends AbstractModuleWindow {

    private static final String RESOURCE_STRING = ":gui/layouthuts/layouttavernrecruit.xml";

    public TavernRecruitModuleWindow(final IBuildingView building) {
        super(building, ColonyPathingEdition.MODID + RESOURCE_STRING);
    }
}
