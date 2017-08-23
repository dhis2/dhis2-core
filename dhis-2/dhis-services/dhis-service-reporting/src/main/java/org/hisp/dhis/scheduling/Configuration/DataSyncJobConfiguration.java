package org.hisp.dhis.scheduling.Configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.dxf2.synch.DataSynchronizationTask;
import org.hisp.dhis.dxf2.synch.SynchronizationManager;
import org.hisp.dhis.dxf2.webmessage.WebMessageParseException;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public class DataSyncJobConfiguration extends JobConfiguration
{
    @Autowired
    private SynchronizationManager synchronizationManager;

    @Autowired
    private MessageService messageService;

    @Autowired
    private Notifier notifier;

    private TaskId taskId;

    private static final Log log = LogFactory.getLog( DataSynchronizationTask.class );

    public void setTaskId( TaskId taskId )
    {
        this.taskId = taskId;
    }

    // -------------------------------------------------------------------------
    // Runnable implementation
    // -------------------------------------------------------------------------

    @Override
    public void run()
    {
        try
        {
            synchronizationManager.executeDataPush();
        }
        catch ( RuntimeException ex )
        {
            notifier.notify( taskId, "Data synch failed: " + ex.getMessage() );
        }
        catch ( WebMessageParseException e )
        {
            log.error("Error while executing data sync task. "+ e.getMessage(), e );
        }

        try
        {
            synchronizationManager.executeEventPush();
        }
        catch ( RuntimeException ex )
        {
            notifier.notify( taskId, "Event synch failed: " + ex.getMessage() );

            messageService.sendSystemErrorNotification( "Event synch failed", ex );
        }
        catch ( WebMessageParseException e )
        {
            log.error("Error while executing event sync task. "+ e.getMessage(), e );
        }

        notifier.notify( taskId, "Data/Event synch successful" );
    }
}
