package org.hisp.dhis.dxf2.sync;
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
import org.hisp.dhis.dxf2.synch.AvailabilityStatus;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.scheduling.AbstractJob;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

/**
 * @author David Katuscak
 */
public class ProgramDataSynchronizationJob extends AbstractJob
{
    private static final Log log = LogFactory.getLog( ProgramDataSynchronizationJob.class );

    private final SystemSettingManager systemSettingManager;
    private final RestTemplate restTemplate;
    private final Notifier notifier;
    private final MessageService messageService;
    private final TrackerSynchronization trackerSync;
    private final EventSynchronization eventSync;

    @Autowired
    public ProgramDataSynchronizationJob( SystemSettingManager systemSettingManager, RestTemplate restTemplate, Notifier notifier, MessageService messageService, TrackerSynchronization trackerSync, EventSynchronization eventSync )
    {
        this.systemSettingManager = systemSettingManager;
        this.restTemplate = restTemplate;
        this.notifier = notifier;
        this.messageService = messageService;
        this.trackerSync = trackerSync;
        this.eventSync = eventSync;
    }


    @Override public JobType getJobType()
    {
        return JobType.PROGRAM_DATA_SYNC;
    }

    @Override public void execute( JobConfiguration jobConfiguration )
    {
        try
        {
            eventSync.syncEventProgramData();
            notifier.notify( jobConfiguration, "Event programs data sync successful" );
        }
        catch ( Exception e )
        {
            log.error( "EventPrograms data sync failed.", e );
            notifier.notify( jobConfiguration, "Event sync failed: " + e.getMessage() );
            messageService.sendSystemErrorNotification( "Event sync failed", e );
        }

        try
        {
            trackerSync.syncTrackerProgramData();
            notifier.notify( jobConfiguration, "Tracker programs data sync successful" );
        }
        catch ( Exception e )
        {
            log.error( "TrackerPrograms data sync failed.", e );
            notifier.notify( jobConfiguration, "TrackerPrograms data sync failed: " + e.getMessage() );
            messageService.sendSystemErrorNotification( "TrackerProgram data sync failed", e );
        }
    }

    @Override
    public ErrorReport validate()
    {
        AvailabilityStatus isRemoteServerAvailable = SyncUtils.testServerAvailability( systemSettingManager, restTemplate );

        if ( !isRemoteServerAvailable.isAvailable() )
        {
            return new ErrorReport( ProgramDataSynchronizationJob.class, ErrorCode.E7010, isRemoteServerAvailable.getMessage() );
        }

        return super.validate();
    }
}
