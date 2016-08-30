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

package org.hisp.dhis.dxf2.metadata.sync;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dxf2.common.Status;
import org.hisp.dhis.dxf2.metadata.tasks.MetadataRetryContext;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.email.EmailService;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.VersionType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.RetryContext;

import java.util.Date;
import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author aamerm
 */
public class MetadataSyncPostProcessorTest
    extends DhisSpringTest
{
    @Autowired
    @Mock
    private EmailService emailService;

    @Autowired
    @Mock
    private MetadataRetryContext metadataRetryContext;

    @InjectMocks
    @Autowired
    private MetadataSyncPostProcessor metadataSyncPostProcessor;

    private MetadataVersion dataVersion;
    private MetadataSyncSummary metadataSyncSummary;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks( this );

        dataVersion = new MetadataVersion();
        dataVersion.setType( VersionType.BEST_EFFORT );
        dataVersion.setName( "testVersion" );
        dataVersion.setCreated( new Date() );
        dataVersion.setHashCode( "samplehashcode" );
        metadataSyncSummary = new MetadataSyncSummary();
    }

    @Test
    public void testShouldSendSuccessEmailIfSyncSummaryIsOk() throws Exception
    {
        metadataSyncSummary.setImportReport( new ImportReport() );
        metadataSyncSummary.getImportReport().setStatus( Status.OK );
        metadataSyncSummary.setMetadataVersion( dataVersion );
        MetadataRetryContext mockRetryContext = mock( MetadataRetryContext.class );

        boolean status = metadataSyncPostProcessor.handleSyncNotificationsAndAbortStatus( metadataSyncSummary, mockRetryContext, dataVersion );

        assertFalse( status );
    }

    @Test
    public void testShouldSendSuccessEmailIfSyncSummaryIsWarning() throws Exception
    {
        metadataSyncSummary.setImportReport( new ImportReport() );
        metadataSyncSummary.getImportReport().setStatus( Status.WARNING );
        metadataSyncSummary.setMetadataVersion( dataVersion );
        MetadataRetryContext mockRetryContext = mock( MetadataRetryContext.class );

        boolean status = metadataSyncPostProcessor.handleSyncNotificationsAndAbortStatus( metadataSyncSummary, mockRetryContext, dataVersion );

        assertFalse( status );

    }

    @Test
    public void testShouldSendSuccessEmailIfSyncSummaryIsError() throws Exception
    {
        metadataSyncSummary.setImportReport( new ImportReport() );
        metadataSyncSummary.getImportReport().setStatus( Status.ERROR );
        metadataSyncSummary.setMetadataVersion( dataVersion );
        MetadataRetryContext mockMetadataRetryContext = mock( MetadataRetryContext.class );
        RetryContext mockRetryContext = mock( RetryContext.class );

        when( mockMetadataRetryContext.getRetryContext() ).thenReturn( mockRetryContext );
        boolean status = metadataSyncPostProcessor.handleSyncNotificationsAndAbortStatus( metadataSyncSummary, mockMetadataRetryContext, dataVersion );

        assertTrue( status );
    }

    @Test
    public void testShouldSendEmailToAdminWithProperSubjectAndBody() throws Exception
    {
        ImportReport importReport = mock( ImportReport.class );

        when( importReport.getTypeReportMap() ).thenReturn( new HashMap<>() );

        metadataSyncSummary.setImportReport( importReport );
        metadataSyncSummary.getImportReport().setStatus( Status.OK );
        metadataSyncSummary.setMetadataVersion( dataVersion );
        MetadataRetryContext mockRetryContext = mock( MetadataRetryContext.class );

        boolean status = metadataSyncPostProcessor.handleSyncNotificationsAndAbortStatus( metadataSyncSummary, mockRetryContext, dataVersion );

        assertFalse( status );
    }
}