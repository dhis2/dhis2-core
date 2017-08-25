package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.scheduling.TaskId;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public class JobConfiguration
{
    public TaskId taskId = null;

    public Runnable getRunnable() {
        return null;
    }
}
