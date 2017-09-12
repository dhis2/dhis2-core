package org.hisp.dhis.scheduling;

import org.hisp.dhis.message.MessageService;

/**
 * Created by henninghakonsen on 05/09/2017.
 * Project: dhis-2.
 */
public interface JobInstance
{
    /**
     * This method will try to execute the actual job.
     * It will verify that no other jobs of the same JobType is running.
     *
     * If the JobConfiguration is disabled it will not run.
     * @param jobConfiguration the configuration of the job
     * @param schedulingManager manager of scheduling
     */
    void execute( JobConfiguration jobConfiguration, SchedulingManager schedulingManager, MessageService messageService );
}
