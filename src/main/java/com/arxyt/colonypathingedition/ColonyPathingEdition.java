package com.arxyt.colonypathingedition;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.data.farmlandmap.SpecialSeedManager;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod("colonypathingedition")
public class ColonyPathingEdition {
    public static final String MODID = "colonypathingedition";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ColonyPathingEdition(FMLJavaModLoadingContext context) {
        // 注册配置文件
        context.registerConfig(
                ModConfig.Type.COMMON,
                PathingConfig.init(new ForgeConfigSpec.Builder())
        );
        MinecraftForge.EVENT_BUS.register(new SpecialSeedManager());
        LOGGER.info("Colony Pathing Edition mod loaded");
    }
}