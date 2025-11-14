package com.arxyt.colonypathingedition.mixins.minecolonies.workersetting;

import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.apiimp.initializer.ModBuildingsInitializer;
import com.minecolonies.core.colony.buildings.views.EmptyView;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBlacksmith;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Supplier;

import static com.minecolonies.core.colony.buildings.modules.BuildingModules.*;
import static com.minecolonies.core.colony.buildings.modules.BuildingModules.STATS_MODULE;

@Mixin(value = ModBuildingsInitializer.class, remap = false)
abstract class ModBuildingsInitializerMixin {

    /**
     * 替换对 DeferredRegister.register(String, Supplier) 的第二个参数（index = 1）。
     * 需要注意：若 static block 中有多个 register 调用，需要通过 ordinal 指定目标调用（见下方说明）。
     */
    @ModifyArg(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    // DeferredRegister.register 方法描述符，请确保 runtime 为 forge 的包名
                    target = "Lnet/minecraftforge/registries/DeferredRegister;register(Ljava/lang/String;Ljava/util/function/Supplier;)Lnet/minecraftforge/registries/RegistryObject;"
                    // 若你用的是不同的 DeferredRegister 泛型/签名或 obf 名称，请调整
            ),
            index = 1 // 第二个参数 supplier
            // ordinal = 0 // 如有多次 register 调用，取消注释并改成 archery 那次的序号（从0开始）
    )
    private static Supplier<?> replaceRegisterSupplier(String id, Supplier<?> original) {
        switch (id) {
            case ModBuildings.BLACKSMITH_ID:
                return () -> new BuildingEntry.Builder()
                        .setBuildingBlock(ModBlocks.blockHutBlacksmith)
                        .setBuildingProducer(BuildingBlacksmith::new)
                        .setBuildingViewProducer(() -> EmptyView::new)
                        .setRegistryName(new ResourceLocation(Constants.MOD_ID, ModBuildings.BLACKSMITH_ID))
                        .addBuildingModuleProducer(BLACKSMITH_WORK)
                        .addBuildingModuleProducer(BLACKSMITH_CRAFT)
                        .addBuildingModuleProducer(SETTINGS_CRAFTER_RECIPE)
                        .addBuildingModuleProducer(CRAFT_TASK_VIEW)
                        .addBuildingModuleProducer(STATS_MODULE)
                        .addBuildingModuleProducer(MIN_STOCK)
                        .createBuildingEntry();
        }
        return original;
    }
}
