package com.arxyt.colonypathingedition.core.costants;


import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import com.minecolonies.core.colony.jobs.JobChef;
import com.minecolonies.core.colony.jobs.JobCook;
import com.minecolonies.core.colony.jobs.JobHealer;

import java.util.Set;

public final class AdditionalContants {
    public static final String SICK_TIME = "sick_time";

    public static final Set<Class<?>> JOBS_EAT_IMMEDIATELY = Set.of(JobChef.class, JobCook.class);
    public static final Set<Class<?>> JOBS_MAY_NOT_SLEEP = Set.of(AbstractJobGuard.class, JobHealer.class);

    public static final String UNSAFE_UPDATE = "com.arxyt.colonypathingedition.core.update.unstable";
    public static final String SAFE_UPDATE = "com.arxyt.colonypathingedition.core.update.stable";
    public static final String UPDATE_MESSAGE = "com.arxyt.colonypathingedition.core.update.latest";
    public static final String OUT_OF_DATE_MESSAGE = "com.arxyt.colonypathingedition.core.update.out_of_date";
    public static final String CHANGELOG = "com.arxyt.colonypathingedition.core.update.changelog";
}
