package com.arxyt.colonypathingedition.core.window;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingList;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.core.client.gui.AbstractModuleWindow;
import net.minecraft.network.chat.Component;

public class TavernRecruitModuleWindow extends AbstractModuleWindow {

    private static final String RESOURCE_STRING = ":gui/layouthuts/layouttavernrecruit.xml";

    private final ScrollingList visitors;

    public TavernRecruitModuleWindow(final IBuildingView building) {
        super(building, ColonyPathingEdition.MODID + RESOURCE_STRING);
        visitors = this.window.findPaneOfTypeByID("visitors", ScrollingList.class);
        visitors.enable();
        visitors.show();
        visitors.setDataProvider(new ScrollingList.DataProvider() {
            @Override
            public int getElementCount() {
                return 10;
            }

            @Override
            public void updateElement(int index, Pane rowPane) {
                rowPane.findPaneOfTypeByID("test", Text.class).setText(Component.literal("Test Name: " + index));
            }
        });
    }
}
