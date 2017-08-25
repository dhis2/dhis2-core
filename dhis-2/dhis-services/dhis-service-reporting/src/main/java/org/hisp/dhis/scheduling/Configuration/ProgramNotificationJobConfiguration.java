package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.program.notification.ProgramNotificationJob;
import org.hisp.dhis.scheduling.TaskId;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public class ProgramNotificationJobConfiguration extends JobConfiguration
{
    public ProgramNotificationJobConfiguration( TaskId taskId )
    {
        this.taskId = taskId;
    }

    @Override
    public Runnable getRunnable()
    {
        return new ProgramNotificationJob( taskId );
    }
}
