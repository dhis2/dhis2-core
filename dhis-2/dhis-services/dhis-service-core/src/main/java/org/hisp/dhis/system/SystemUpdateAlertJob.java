/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.system;

import static com.google.common.base.Preconditions.checkNotNull;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Component( "systemUpdateAlertJob" )
public class SystemUpdateAlertJob implements Job
{
    private final SystemSettingManager systemSettingManager;

    private final SystemUpdateService systemUpdateService;

    private final Notifier notifier;

    private final MessageService messageService;

    public SystemUpdateAlertJob( SystemSettingManager systemSettingManager, SystemUpdateService systemUpdateService,
        Notifier notifier,
        MessageService messageService )
    {
        checkNotNull( systemSettingManager );
        checkNotNull( systemUpdateService );
        checkNotNull( notifier );
        checkNotNull( messageService );

        this.systemSettingManager = systemSettingManager;
        this.systemUpdateService = systemUpdateService;
        this.notifier = notifier;
        this.messageService = messageService;
    }

    @Override
    public JobType getJobType()
    {
        return JobType.SYSTEM_SOFTWARE_UPDATE;
    }

    @Override
    public void execute( JobConfiguration jobConfiguration )
    {
        boolean isSoftwareUpdateAlertEnabled = systemSettingManager.getBoolSetting(
            SettingKey.SOFTWARE_UPDATE_ALERT_ENABLED );

        if ( !isSoftwareUpdateAlertEnabled )
        {
            log.info( String.format( "%s aborted. System update alerts are disabled",
                "messageSystemSoftwareUpdateAvailableJob" ) );
            return;
        }

        try
        {
            systemUpdateService.sendMessageForEachVersion( SystemUpdateService.getLatestNewerThanCurrent() );

            log.info( "SOMETHING SHOULD HAPPEN HERE" );
        }
        catch ( Exception e )
        {
            log.error( "Failed to fetch latest versions.", e );
            notifier.notify( jobConfiguration, "Fetch latest software updates failed: " + e.getMessage() );
            messageService.sendSystemErrorNotification( "Fetch latest software updates failed", e );
        }
    }
}
