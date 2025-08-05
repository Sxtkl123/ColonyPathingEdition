package com.arxyt.colonypathingedition.core.mixins.costants;


import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import com.minecolonies.core.colony.jobs.JobChef;
import com.minecolonies.core.colony.jobs.JobCook;
import com.minecolonies.core.colony.jobs.JobHealer;

import java.util.Set;

public final class AdditionalContants {
    public static final String SICK_TIME = "sick_time";
    public static final Set<Class<?>> JOBS_EAT_IMMEDIATELY = Set.of(JobChef.class, JobCook.class);
    public static final Set<Class<?>> JOBS_MAY_NOT_SLEEP = Set.of(AbstractJobGuard.class, JobHealer.class);
}
