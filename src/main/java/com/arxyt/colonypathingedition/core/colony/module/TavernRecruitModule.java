package com.arxyt.colonypathingedition.core.colony.module;

import com.minecolonies.api.colony.IVisitorData;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModule;
import com.minecolonies.api.colony.buildings.modules.IBuildingModule;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenSkillHandler;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;

public class TavernRecruitModule extends AbstractBuildingModule implements IBuildingModule {

    @Override
    public void serializeToView(FriendlyByteBuf buf) {
        List<Integer> externalCitizens = this.building.getModule(BuildingModules.TAVERN_VISITOR).getExternalCitizens();
        buf.writeInt(externalCitizens.size());
        externalCitizens.forEach(citizen -> {
            IVisitorData visitor = building.getColony().getVisitorManager().getVisitor(citizen);
            Map<Skill, CitizenSkillHandler.SkillData> skills = visitor.getCitizenSkillHandler().getSkills();
            for (Skill skill : Skill.values()) {
                buf.writeInt(skills.get(skill).getLevel());
            }
            buf.writeUtf(visitor.getName());
            buf.writeItem(visitor.getRecruitCost());
        });
    }
}
