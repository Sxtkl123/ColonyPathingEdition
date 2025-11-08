package com.arxyt.colonypathingedition.core.easycolony.event;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.arxyt.colonypathingedition.core.easycolony.manager.LinkageManager;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 此功能为从简易殖民地迁移而来。
 * This feature has been migrated from EasyColony.
 * @author sxtkl
 * @since 2025/11/8
 */
@Mod.EventBusSubscriber(modid = ColonyPathingEdition.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class LinkageEvent {

    @SubscribeEvent
    public static void onFMLCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (ModList.get().isLoaded("jecharacters")) {
                LinkageManager.setup();
            }
        });
    }

}