/*
 * Copyright (c) 2004-2016, University of Oslo
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

package org.hisp.dhis.dxf2.metadata.tasks;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncParams;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncPostProcessor;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncPreProcessor;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncService;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncSummary;
import org.hisp.dhis.dxf2.metadata.sync.exception.DhisVersionMismatchException;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * @author aamerm
 */
@Category( IntegrationTest.class )
public class MetadataSyncTaskTest
    extends DhisSpringTest
{
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Autowired
    @Mock
    private SystemSettingManager systemSettingManager;

    @Autowired
    @Mock
    private RetryTemplate retryTemplate;

    @Autowired
    @Mock
    private MetadataRetryContext metadataRetryContext;

    @Autowired
    @Mock
    private MetadataSyncPreProcessor metadataSyncPreProcessor;

    @Autowired
    @Mock
    private MetadataSyncPostProcessor metadataSyncPostProcessor;

    @Autowired
    @Mock
    private MetadataSyncService metadataSyncService;

    @InjectMocks
    @Autowired
    private MetadataSyncTask metadataSyncTask;

    private MetadataSyncSummary metadataSyncSummary;
    private MetadataVersion metadataVersion;
    private List<MetadataVersion> metadataVersions;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks( this );

        metadataSyncSummary = mock( MetadataSyncSummary.class );
        metadataVersion = mock( MetadataVersion.class );
        metadataVersions = new ArrayList<>();
        metadataVersions.add( metadataVersion );
    }

    // TODO: can we write more tests. This might cover a lot more tests.
    // TODO: don't test on how it happens. test for the result
    @Test
    public void testShouldRunAllTasksInSequence() throws Exception
    {
        when( metadataSyncService.doMetadataSync( any( MetadataSyncParams.class ) ) ).thenReturn( metadataSyncSummary );
        when( metadataSyncPreProcessor.handleCurrentMetadataVersion( metadataRetryContext ) ).thenReturn( metadataVersion );
        when( metadataSyncPreProcessor.handleMetadataVersionsList( metadataRetryContext, metadataVersion ) ).thenReturn( metadataVersions );
        when( metadataSyncService.isSyncRequired( any( MetadataSyncParams.class ) ) ).thenReturn( true );
        metadataSyncTask.runSyncTask( metadataRetryContext );

        verify( metadataSyncPreProcessor ).setUp( metadataRetryContext );
        verify( metadataSyncPreProcessor ).handleAggregateDataPush( metadataRetryContext );
        verify( metadataSyncPreProcessor ).handleEventDataPush( metadataRetryContext );
        verify( metadataSyncPreProcessor ).handleCurrentMetadataVersion( metadataRetryContext );
        verify( metadataSyncPreProcessor ).handleMetadataVersionsList( metadataRetryContext, metadataVersion );
        verify( metadataSyncService ).doMetadataSync( any( MetadataSyncParams.class ) );
        verify( metadataSyncPostProcessor ).handleSyncNotificationsAndAbortStatus( metadataSyncSummary, metadataRetryContext, metadataVersion );
    }

    @Test( expected = MetadataSyncServiceException.class )
    public void testHandleMetadataSyncIsThrowingException() throws Exception
    {
        when( metadataSyncService.doMetadataSync( any( MetadataSyncParams.class ) ) ).thenThrow( new MetadataSyncServiceException( "" ) );
        when( metadataSyncPreProcessor.handleCurrentMetadataVersion( metadataRetryContext ) ).thenReturn( metadataVersion );
        when( metadataSyncPreProcessor.handleMetadataVersionsList( metadataRetryContext, metadataVersion ) ).thenReturn( metadataVersions );
        doNothing().when( metadataRetryContext ).updateRetryContext( any( String.class ), any( String.class ), eq( metadataVersion ) );
        when( metadataSyncService.isSyncRequired( any( MetadataSyncParams.class ) ) ).thenReturn( true );
        metadataSyncTask.runSyncTask( metadataRetryContext );

        verify( metadataSyncPreProcessor ).setUp( metadataRetryContext );
        verify( metadataSyncPreProcessor ).handleAggregateDataPush( metadataRetryContext );
        verify( metadataSyncPreProcessor ).handleEventDataPush( metadataRetryContext );
        verify( metadataSyncPreProcessor ).handleCurrentMetadataVersion( metadataRetryContext );
        verify( metadataSyncPreProcessor ).handleMetadataVersionsList( metadataRetryContext, metadataVersion );
        verify( metadataSyncService ).doMetadataSync( any( MetadataSyncParams.class ) );
        verify( metadataSyncPostProcessor, never() ).handleSyncNotificationsAndAbortStatus( metadataSyncSummary, metadataRetryContext, metadataVersion );
    }

    @Test( expected = DhisVersionMismatchException.class )
    public void testShouldAbortIfDHISVersionMismatch() throws Exception
    {
        metadataVersions.add( metadataVersion );

        when( metadataSyncPreProcessor.handleCurrentMetadataVersion( metadataRetryContext ) ).thenReturn( metadataVersion );
        when( metadataSyncPreProcessor.handleMetadataVersionsList( metadataRetryContext, metadataVersion ) ).thenReturn( metadataVersions );
        when( metadataSyncService.doMetadataSync( any( MetadataSyncParams.class ) ) ).thenThrow( new DhisVersionMismatchException( "" ) );
        when( metadataSyncService.isSyncRequired( any( MetadataSyncParams.class ) ) ).thenReturn( true );
        metadataSyncTask.runSyncTask( metadataRetryContext );

        verify( metadataSyncPreProcessor, times( 1 ) ).setUp( metadataRetryContext );
        verify( metadataSyncPreProcessor, times( 1 ) ).handleAggregateDataPush( metadataRetryContext );
        verify( metadataSyncPreProcessor, times( 1 ) ).handleEventDataPush( metadataRetryContext );
        verify( metadataSyncPreProcessor, times( 1 ) ).handleCurrentMetadataVersion( metadataRetryContext );
        verify( metadataSyncPreProcessor, times( 1 ) ).handleMetadataVersionsList( metadataRetryContext, metadataVersion );
        verify( metadataSyncService, times( 1 ) ).doMetadataSync( any( MetadataSyncParams.class ) );
    }

    @Test
    public void testShouldAbortIfErrorInSyncSummary() throws Exception
    {
        metadataVersions.add( metadataVersion );

        when( metadataSyncPreProcessor.handleCurrentMetadataVersion( metadataRetryContext ) ).thenReturn( metadataVersion );
        when( metadataSyncPreProcessor.handleMetadataVersionsList( metadataRetryContext, metadataVersion ) ).thenReturn( metadataVersions );
        when( metadataSyncService.doMetadataSync( any( MetadataSyncParams.class ) ) ).thenReturn( metadataSyncSummary );
        when( metadataSyncPostProcessor.handleSyncNotificationsAndAbortStatus( metadataSyncSummary, metadataRetryContext, metadataVersion ) ).thenReturn( true );
        when( metadataSyncService.isSyncRequired( any( MetadataSyncParams.class ) ) ).thenReturn( true );
        metadataSyncTask.runSyncTask( metadataRetryContext );

        verify( metadataSyncPreProcessor, times( 1 ) ).setUp( metadataRetryContext );
        verify( metadataSyncPreProcessor, times( 1 ) ).handleAggregateDataPush( metadataRetryContext );
        verify( metadataSyncPreProcessor, times( 1 ) ).handleEventDataPush( metadataRetryContext );
        verify( metadataSyncPreProcessor, times( 1 ) ).handleCurrentMetadataVersion( metadataRetryContext );
        verify( metadataSyncPreProcessor, times( 1 ) ).handleMetadataVersionsList( metadataRetryContext, metadataVersion );
        verify( metadataSyncService, times( 1 ) ).doMetadataSync( any( MetadataSyncParams.class ) );
        verify( metadataSyncPostProcessor, times( 1 ) ).handleSyncNotificationsAndAbortStatus( metadataSyncSummary, metadataRetryContext, metadataVersion );
    }
}