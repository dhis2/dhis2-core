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
package org.hisp.dhis.dxf2.sync;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistrationExchangeService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.system.util.CodecUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

/**
 * @author David Katuscak <katuscak.d@gmail.com>
 */
@Slf4j
@Component
public class CompleteDataSetRegistrationSynchronization extends DataSynchronizationWithoutPaging
{
    private final SystemSettingManager systemSettingManager;

    private final RestTemplate restTemplate;

    private final CompleteDataSetRegistrationService completeDataSetRegistrationService;

    private final CompleteDataSetRegistrationExchangeService completeDataSetRegistrationExchangeService;

    private Date lastUpdatedAfter;

    public CompleteDataSetRegistrationSynchronization( SystemSettingManager systemSettingManager,
        RestTemplate restTemplate, CompleteDataSetRegistrationService completeDataSetRegistrationService,
        CompleteDataSetRegistrationExchangeService completeDataSetRegistrationExchangeService )
    {
        checkNotNull( systemSettingManager );
        checkNotNull( restTemplate );
        checkNotNull( completeDataSetRegistrationService );
        checkNotNull( completeDataSetRegistrationExchangeService );

        this.systemSettingManager = systemSettingManager;
        this.restTemplate = restTemplate;
        this.completeDataSetRegistrationService = completeDataSetRegistrationService;
        this.completeDataSetRegistrationExchangeService = completeDataSetRegistrationExchangeService;
    }

    @Override
    public SynchronizationResult synchronizeData()
    {
        if ( !SyncUtils.testServerAvailability( systemSettingManager, restTemplate ).isAvailable() )
        {
            return SynchronizationResult.newFailureResultWithMessage(
                "Complete data set registration synchronization failed. Remote server is unavailable." );
        }

        initializeSyncVariables();

        if ( objectsToSynchronize == 0 )
        {
            SyncUtils.setLastSyncSuccess( systemSettingManager,
                SettingKey.LAST_SUCCESSFUL_COMPLETE_DATA_SET_REGISTRATION_SYNC, new Date( clock.getStartTime() ) );
            log.info( "Skipping completeness synchronization, no new or updated data" );
            return SynchronizationResult
                .newSuccessResultWithMessage( "Skipping completeness synchronization, no new or updated data" );
        }

        if ( sendSyncRequest() )
        {
            String resultMsg = "Complete data set registration synchronization is done. It took ";
            clock.logTime( "SUCCESS! " + resultMsg );
            SyncUtils.setLastSyncSuccess( systemSettingManager,
                SettingKey.LAST_SUCCESSFUL_COMPLETE_DATA_SET_REGISTRATION_SYNC,
                new Date( clock.getStartTime() ) );

            return SynchronizationResult.newSuccessResultWithMessage( resultMsg + clock.getTime() + " ms." );
        }

        return SynchronizationResult
            .newFailureResultWithMessage( "Complete data set registration synchronization failed." );
    }

    private void initializeSyncVariables()
    {
        clock = new Clock( log ).startClock().logTime( "Starting Complete data set registration synchronization job." );

        final Date lastSuccessTime = SyncUtils.getLastSyncSuccess( systemSettingManager,
            SettingKey.LAST_SUCCESSFUL_COMPLETE_DATA_SET_REGISTRATION_SYNC );
        final Date skipChangedBefore = (Date) systemSettingManager
            .getSystemSetting( SettingKey.SKIP_SYNCHRONIZATION_FOR_DATA_CHANGED_BEFORE );
        lastUpdatedAfter = lastSuccessTime.after( skipChangedBefore ) ? lastSuccessTime : skipChangedBefore;
        objectsToSynchronize = completeDataSetRegistrationService
            .getCompleteDataSetCountLastUpdatedAfter( lastUpdatedAfter );

        log.info(
            "CompleteDataSetRegistrations last changed before " + skipChangedBefore + " will not be synchronized." );

        if ( objectsToSynchronize != 0 )
        {
            instance = SyncUtils.getRemoteInstance( systemSettingManager,
                SyncEndpoint.COMPLETE_DATA_SET_REGISTRATIONS );

            log.info( objectsToSynchronize + " completed data set registrations to synchronize were found." );
            log.info( "Remote server URL for completeness POST synchronization: " + instance.getUrl() );
        }
    }

    private boolean sendSyncRequest()
    {
        final RequestCallback requestCallback = request -> {
            request.getHeaders().setContentType( MediaType.APPLICATION_JSON );
            request.getHeaders().add( SyncUtils.HEADER_AUTHORIZATION,
                CodecUtils.getBasicAuthString( instance.getUsername(), instance.getPassword() ) );

            completeDataSetRegistrationExchangeService
                .writeCompleteDataSetRegistrationsJson( lastUpdatedAfter, request.getBody(), new IdSchemes() );
        };

        return SyncUtils.sendSyncRequest( systemSettingManager, restTemplate, requestCallback, instance,
            SyncEndpoint.COMPLETE_DATA_SET_REGISTRATIONS );
    }
}
