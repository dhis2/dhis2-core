package org.hisp.dhis.dxf2.synch;

/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.dxf2.webmessage.WebMessageParseException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.scheduling.AbstractJob;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class DataSynchronizationJob
    extends AbstractJob
{
    @Autowired
    private SynchronizationManager synchronizationManager;
    
    @Autowired
    private MessageService messageService;

    @Autowired
    private Notifier notifier;

    private static final Log log = LogFactory.getLog( DataSynchronizationJob.class );

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public JobType getJobType()
    {
        return JobType.DATA_SYNC;
    }

    @Override
    public void execute( JobConfiguration jobConfiguration )
    {
        try
        {
            synchronizationManager.executeDataPush();
        }
        catch ( RuntimeException ex )
        {
            notifier.notify( jobConfiguration, "Data synch failed: " + ex.getMessage() );
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
            notifier.notify( jobConfiguration, "Event synch failed: " + ex.getMessage() );
            
            messageService.sendSystemErrorNotification( "Event synch failed", ex );
        }
        catch ( WebMessageParseException e )
        {
            log.error("Error while executing event sync task. "+ e.getMessage(), e );
        }

        notifier.notify( jobConfiguration, "Data/Event synch successful" );
    }

    @Override
    public ErrorReport validate()
    {
        AvailabilityStatus isRemoteServerAvailable = synchronizationManager.isRemoteServerAvailable();

        if ( !isRemoteServerAvailable.isAvailable() )
        {
            return new ErrorReport( DataSynchronizationJob.class, ErrorCode.E7010, isRemoteServerAvailable.getMessage() );
        }

        return super.validate();
    }

}
