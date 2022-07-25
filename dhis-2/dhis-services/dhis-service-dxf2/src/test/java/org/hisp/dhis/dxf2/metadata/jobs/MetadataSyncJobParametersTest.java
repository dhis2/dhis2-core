/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dxf2.metadata.jobs;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncParams;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncPostProcessor;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncPreProcessor;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncService;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncSummary;
import org.hisp.dhis.dxf2.metadata.sync.exception.DhisVersionMismatchException;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.synch.SynchronizationManager;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.scheduling.parameters.MetadataSyncJobParameters;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.support.RetryTemplate;

/**
 * @author aamerm
 */
@ExtendWith( MockitoExtension.class )
class MetadataSyncJobParametersTest
{
    private static final JobProgress JOB_PROGRESS = NoopJobProgress.INSTANCE;

    @Mock
    private SystemSettingManager systemSettingManager;

    @Mock
    private RetryTemplate retryTemplate;

    @Mock
    private MetadataRetryContext metadataRetryContext;

    @Mock
    private MetadataSyncPreProcessor metadataSyncPreProcessor;

    @Mock
    private MetadataSyncPostProcessor metadataSyncPostProcessor;

    @Mock
    private SynchronizationManager synchronizationManager;

    @Mock
    private MetadataSyncService metadataSyncService;

    private MetadataSyncJob metadataSyncJob;

    private final MetadataSyncJobParameters metadataSyncJobParameters = new MetadataSyncJobParameters();

    private MetadataSyncSummary metadataSyncSummary;

    private MetadataVersion metadataVersion;

    private List<MetadataVersion> metadataVersions;

    @BeforeEach
    public void setUp()
    {
        metadataSyncSummary = new MetadataSyncSummary();
        metadataSyncSummary.setImportReport( new ImportReport() );
        metadataVersion = mock( MetadataVersion.class );
        metadataVersions = new ArrayList<>();
        metadataVersions.add( metadataVersion );

        metadataSyncJob = new MetadataSyncJob( systemSettingManager, retryTemplate, synchronizationManager,
            metadataSyncPreProcessor, metadataSyncPostProcessor, metadataSyncService, metadataRetryContext );
    }

    // TODO: can we write more tests. This might cover a lot more tests.
    // TODO: don't test on how it happens. test for the result
    @Test
    void testShouldRunAllTasksInSequence()
        throws DhisVersionMismatchException
    {
        when( metadataSyncService.doMetadataSync( any( MetadataSyncParams.class ) ) ).thenReturn( metadataSyncSummary );
        when( metadataSyncPreProcessor.handleCurrentMetadataVersion( metadataRetryContext, JOB_PROGRESS ) )
            .thenReturn( metadataVersion );
        when(
            metadataSyncPreProcessor.handleMetadataVersionsList( metadataRetryContext, metadataVersion, JOB_PROGRESS ) )
                .thenReturn( metadataVersions );
        when( metadataSyncService.isSyncRequired( any( MetadataSyncParams.class ) ) ).thenReturn( true );
        metadataSyncJob.runSyncTask( metadataRetryContext, metadataSyncJobParameters, JOB_PROGRESS );

        verify( metadataSyncPreProcessor ).setUp( metadataRetryContext, JOB_PROGRESS );
        verify( metadataSyncPreProcessor ).handleDataValuePush( metadataRetryContext, metadataSyncJobParameters,
            JOB_PROGRESS );
        verify( metadataSyncPreProcessor ).handleEventProgramsDataPush( metadataRetryContext,
            metadataSyncJobParameters, JOB_PROGRESS );
        verify( metadataSyncPreProcessor ).handleTrackerProgramsDataPush( metadataRetryContext,
            metadataSyncJobParameters, JOB_PROGRESS );
        verify( metadataSyncPreProcessor ).handleCurrentMetadataVersion( metadataRetryContext, JOB_PROGRESS );
        verify( metadataSyncPreProcessor ).handleMetadataVersionsList( metadataRetryContext, metadataVersion,
            JOB_PROGRESS );
        verify( metadataSyncService ).doMetadataSync( any( MetadataSyncParams.class ) );
        verify( metadataSyncPostProcessor ).handleSyncNotificationsAndAbortStatus( metadataSyncSummary,
            metadataRetryContext, metadataVersion );
    }

    @Test
    void testHandleMetadataSyncIsThrowingException()
        throws DhisVersionMismatchException
    {
        when( metadataSyncService.doMetadataSync( any( MetadataSyncParams.class ) ) )
            .thenThrow( new MetadataSyncServiceException( "" ) );
        when( metadataSyncPreProcessor.handleCurrentMetadataVersion( metadataRetryContext, JOB_PROGRESS ) )
            .thenReturn( metadataVersion );
        when(
            metadataSyncPreProcessor.handleMetadataVersionsList( metadataRetryContext, metadataVersion, JOB_PROGRESS ) )
                .thenReturn( metadataVersions );
        doNothing().when( metadataRetryContext ).updateRetryContext( any( String.class ), any( String.class ),
            eq( metadataVersion ) );
        when( metadataSyncService.isSyncRequired( any( MetadataSyncParams.class ) ) ).thenReturn( true );

        assertThrows( MetadataSyncServiceException.class,
            () -> metadataSyncJob.runSyncTask( metadataRetryContext, metadataSyncJobParameters, JOB_PROGRESS ) );

        verify( metadataSyncPreProcessor ).setUp( metadataRetryContext, JOB_PROGRESS );
        verify( metadataSyncPreProcessor ).handleDataValuePush( metadataRetryContext, metadataSyncJobParameters,
            JOB_PROGRESS );
        verify( metadataSyncPreProcessor ).handleEventProgramsDataPush( metadataRetryContext,
            metadataSyncJobParameters, JOB_PROGRESS );
        verify( metadataSyncPreProcessor ).handleTrackerProgramsDataPush( metadataRetryContext,
            metadataSyncJobParameters, JOB_PROGRESS );
        verify( metadataSyncPreProcessor ).handleCurrentMetadataVersion( metadataRetryContext, JOB_PROGRESS );
        verify( metadataSyncPreProcessor ).handleMetadataVersionsList( metadataRetryContext, metadataVersion,
            JOB_PROGRESS );
        verify( metadataSyncService ).doMetadataSync( any( MetadataSyncParams.class ) );
        verify( metadataSyncPostProcessor, never() ).handleSyncNotificationsAndAbortStatus(
            metadataSyncSummary,
            metadataRetryContext, metadataVersion );
    }

    @Test
    void testShouldAbortIfDHISVersionMismatch()
        throws DhisVersionMismatchException
    {
        metadataVersions.add( metadataVersion );

        when( metadataSyncPreProcessor.handleCurrentMetadataVersion( metadataRetryContext, JOB_PROGRESS ) )
            .thenReturn( metadataVersion );
        when(
            metadataSyncPreProcessor.handleMetadataVersionsList( metadataRetryContext, metadataVersion, JOB_PROGRESS ) )
                .thenReturn( metadataVersions );
        when( metadataSyncService.doMetadataSync( any( MetadataSyncParams.class ) ) )
            .thenThrow( new DhisVersionMismatchException( "" ) );
        when( metadataSyncService.isSyncRequired( any( MetadataSyncParams.class ) ) ).thenReturn( true );

        assertThrows( DhisVersionMismatchException.class,
            () -> metadataSyncJob.runSyncTask( metadataRetryContext, metadataSyncJobParameters, JOB_PROGRESS ) );

        verify( metadataSyncPreProcessor, times( 1 ) ).setUp( metadataRetryContext, JOB_PROGRESS );
        verify( metadataSyncPreProcessor, times( 1 ) ).handleDataValuePush( metadataRetryContext,
            metadataSyncJobParameters, JOB_PROGRESS );
        verify( metadataSyncPreProcessor, times( 1 ) ).handleEventProgramsDataPush( metadataRetryContext,
            metadataSyncJobParameters, JOB_PROGRESS );
        verify( metadataSyncPreProcessor, times( 1 ) ).handleTrackerProgramsDataPush( metadataRetryContext,
            metadataSyncJobParameters, JOB_PROGRESS );
        verify( metadataSyncPreProcessor, times( 1 ) ).handleCurrentMetadataVersion( metadataRetryContext,
            JOB_PROGRESS );
        verify( metadataSyncPreProcessor, times( 1 ) ).handleMetadataVersionsList( metadataRetryContext,
            metadataVersion, JOB_PROGRESS );
        verify( metadataSyncService, times( 1 ) ).doMetadataSync( any( MetadataSyncParams.class ) );
    }

    @Test
    void testShouldAbortIfErrorInSyncSummary()
        throws DhisVersionMismatchException
    {
        metadataVersions.add( metadataVersion );

        when( metadataSyncPreProcessor.handleCurrentMetadataVersion( metadataRetryContext, JOB_PROGRESS ) )
            .thenReturn( metadataVersion );
        when(
            metadataSyncPreProcessor.handleMetadataVersionsList( metadataRetryContext, metadataVersion, JOB_PROGRESS ) )
                .thenReturn( metadataVersions );
        when( metadataSyncService.doMetadataSync( any( MetadataSyncParams.class ) ) ).thenReturn( metadataSyncSummary );
        when( metadataSyncPostProcessor.handleSyncNotificationsAndAbortStatus( metadataSyncSummary,
            metadataRetryContext, metadataVersion ) ).thenReturn( true );
        when( metadataSyncService.isSyncRequired( any( MetadataSyncParams.class ) ) ).thenReturn( true );
        metadataSyncJob.runSyncTask( metadataRetryContext, metadataSyncJobParameters, JOB_PROGRESS );

        verify( metadataSyncPreProcessor, times( 1 ) ).setUp( metadataRetryContext, JOB_PROGRESS );
        verify( metadataSyncPreProcessor, times( 1 ) ).handleDataValuePush( metadataRetryContext,
            metadataSyncJobParameters, JOB_PROGRESS );
        verify( metadataSyncPreProcessor, times( 1 ) ).handleEventProgramsDataPush( metadataRetryContext,
            metadataSyncJobParameters, JOB_PROGRESS );
        verify( metadataSyncPreProcessor, times( 1 ) ).handleTrackerProgramsDataPush( metadataRetryContext,
            metadataSyncJobParameters, JOB_PROGRESS );
        verify( metadataSyncPreProcessor, times( 1 ) ).handleCurrentMetadataVersion( metadataRetryContext,
            JOB_PROGRESS );
        verify( metadataSyncPreProcessor, times( 1 ) ).handleMetadataVersionsList( metadataRetryContext,
            metadataVersion, JOB_PROGRESS );
        verify( metadataSyncService, times( 1 ) ).doMetadataSync( any( MetadataSyncParams.class ) );
        verify( metadataSyncPostProcessor, times( 1 ) ).handleSyncNotificationsAndAbortStatus( metadataSyncSummary,
            metadataRetryContext, metadataVersion );
    }

}
