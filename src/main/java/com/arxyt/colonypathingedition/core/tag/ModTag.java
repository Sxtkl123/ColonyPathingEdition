package com.arxyt.colonypathingedition.core.tag;

import com.arxyt.colonypathingedition.core.ColonyPathingEdition;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTag {
    public static final TagKey<Item> SEEDS_UNDERWATER = createTag("seeds_underwater");
    public static final TagKey<Item> SEEDS_NOFARMLAND = createTag("seeds_nofarmland");

    private static TagKey<Item> createTag(String name) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(ColonyPathingEdition.MODID, name));
    }
}
