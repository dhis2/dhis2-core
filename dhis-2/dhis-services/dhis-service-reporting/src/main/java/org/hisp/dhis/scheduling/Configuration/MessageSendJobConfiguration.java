package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.sms.scheduling.SendScheduledMessageJob;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public class MessageSendJobConfiguration extends JobConfiguration
{
    public MessageSendJobConfiguration( TaskId taskId )
    {
        this.taskId = taskId;
    }

    @Override
    public Runnable getRunnable()
    {
        return new SendScheduledMessageJob( taskId );
    }
}
