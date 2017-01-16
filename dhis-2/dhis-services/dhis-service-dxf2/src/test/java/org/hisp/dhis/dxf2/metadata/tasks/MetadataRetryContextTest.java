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
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncSummary;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.retry.RetryContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author aamerm
 */
public class MetadataRetryContextTest
    extends DhisSpringTest
{
    @Mock
    RetryContext retryContext;

    @InjectMocks
    MetadataRetryContext metadataRetryContext;

    private MetadataVersion mockVersion;
    private String testKey = "testKey";
    private String testMessage = "testMessage";

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks( this );

        mockVersion = mock( MetadataVersion.class );
    }

    @Test
    public void testShouldGetRetryContextCorrectly() throws Exception
    {
        assertEquals( retryContext, metadataRetryContext.getRetryContext() );
    }

    @Test
    public void testShouldSetRetryContextCorrectly() throws Exception
    {
        RetryContext newMock = mock( RetryContext.class );

        metadataRetryContext.setRetryContext( newMock );

        assertEquals( newMock, metadataRetryContext.getRetryContext() );
    }

    @Test
    public void testIfVersionIsNull() throws Exception
    {
        metadataRetryContext.updateRetryContext( testKey, testMessage, null );

        verify( retryContext ).setAttribute( testKey, testMessage );
        verify( retryContext, never() ).setAttribute( MetadataSyncTask.VERSION_KEY, null );
    }

    @Test
    public void testIfVersionIsNotNull() throws Exception
    {
        metadataRetryContext.updateRetryContext( testKey, testMessage, mockVersion );

        verify( retryContext ).setAttribute( testKey, testMessage );
        verify( retryContext ).setAttribute( MetadataSyncTask.VERSION_KEY, mockVersion );
    }

    @Test
    public void testIfSummaryIsNull() throws Exception
    {
        MetadataSyncSummary metadataSyncSummary = mock( MetadataSyncSummary.class );

        metadataRetryContext.updateRetryContext( testKey, testMessage, mockVersion, null );

        verify( retryContext ).setAttribute( testKey, testMessage );
        verify( metadataSyncSummary, never() ).getImportReport();

    }

    @Test
    public void testIfSummaryIsNotNull() throws Exception
    {
        MetadataSyncSummary testSummary = new MetadataSyncSummary();
        ImportReport importReport = new ImportReport();
        importReport.setStatus( Status.ERROR );
        testSummary.setImportReport( importReport );

        metadataRetryContext.updateRetryContext( testKey, testMessage, mockVersion, testSummary );

        verify( retryContext ).setAttribute( testKey, testMessage );
    }
}