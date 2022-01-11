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
package org.hisp.dhis.dxf2.metadata.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncImportException;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.metadata.version.MetadataVersionDelegate;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.VersionType;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author anilkumk
 */
@ExtendWith( MockitoExtension.class )
class MetadataSyncImportHandlerTest
{
    @Mock
    MetadataImportService metadataImportService;

    @Mock
    MetadataVersionDelegate metadataVersionDelegate;

    @Mock
    private RenderService renderService;

    @InjectMocks
    private MetadataSyncImportHandler metadataSyncImportHandler;

    private MetadataVersion metadataVersion;

    private String expectedMetadataSnapshot;

    private MetadataSyncParams syncParams;

    private ImportReport importReport;

    @BeforeEach
    public void setup()
    {
        metadataVersion = new MetadataVersion( "testVersion", VersionType.ATOMIC );
        expectedMetadataSnapshot = "{\"date\":\"2016-05-24T05:27:25.128+0000\"}";
        syncParams = new MetadataSyncParams();
        importReport = new ImportReport();
    }

    @Test
    void testShouldThrowExceptionWhenNoVersionSet()
    {
        syncParams.setImportParams( null );
        assertThrows( MetadataSyncServiceException.class,
            () -> metadataSyncImportHandler.importMetadata( syncParams, expectedMetadataSnapshot ),
            "MetadataImportParams for the Sync cant be null." );
    }

    @Test
    void testShouldThrowExceptionWhenNoImportParams()
    {
        syncParams.setVersion( metadataVersion );
        syncParams.setImportParams( null );

        assertThrows( MetadataSyncServiceException.class,
            () -> metadataSyncImportHandler.importMetadata( syncParams, expectedMetadataSnapshot ),
            "MetadataImportParams for the Sync cant be null." );
    }

    @Test
    void testShouldThrowExceptionWhenImportServiceFails()
    {
        syncParams.setImportParams( new MetadataImportParams() );
        syncParams.setVersion( metadataVersion );

        when( metadataImportService.importMetadata( syncParams.getImportParams() ) )
            .thenThrow( new MetadataSyncServiceException( "" ) );
        assertThrows( MetadataSyncImportException.class,
            () -> metadataSyncImportHandler.importMetadata( syncParams, expectedMetadataSnapshot ) );
        verify( metadataVersionDelegate, never() ).addNewMetadataVersion( metadataVersion );
    }

    @Test
    void testShouldImportMetadata()
    {
        syncParams.setImportParams( new MetadataImportParams() );
        syncParams.setVersion( metadataVersion );
        MetadataSyncSummary metadataSyncSummary = new MetadataSyncSummary();
        importReport.setStatus( Status.OK );

        when( metadataImportService.importMetadata( syncParams.getImportParams() ) ).thenReturn( importReport );

        metadataSyncSummary.setImportReport( importReport );
        metadataSyncSummary.setMetadataVersion( metadataVersion );

        doNothing().when( metadataVersionDelegate ).addNewMetadataVersion( metadataVersion );

        MetadataSyncSummary actualMetadataSyncSummary = metadataSyncImportHandler.importMetadata( syncParams,
            expectedMetadataSnapshot );

        verify( metadataVersionDelegate ).addNewMetadataVersion( metadataVersion );
        assertEquals( metadataSyncSummary.getImportReport(), actualMetadataSyncSummary.getImportReport() );
        assertEquals( metadataSyncSummary.getImportSummary(), actualMetadataSyncSummary.getImportSummary() );
        assertEquals( metadataSyncSummary.getMetadataVersion(), actualMetadataSyncSummary.getMetadataVersion() );
        assertEquals( metadataSyncSummary.getMetadataVersion().getType(),
            actualMetadataSyncSummary.getMetadataVersion().getType() );
        assertEquals( metadataSyncSummary.getImportReport().getStatus(),
            actualMetadataSyncSummary.getImportReport().getStatus() );
    }

    @Test
    void testShouldImportMetadataWhenBestEffortWithWarnings()
    {
        syncParams.setImportParams( new MetadataImportParams() );
        syncParams.setVersion( metadataVersion );
        MetadataSyncSummary metadataSyncSummary = new MetadataSyncSummary();
        importReport.setStatus( Status.WARNING );
        metadataVersion.setType( VersionType.BEST_EFFORT );

        when( metadataImportService.importMetadata( syncParams.getImportParams() ) ).thenReturn( importReport );

        metadataSyncSummary.setImportReport( importReport );
        metadataSyncSummary.setMetadataVersion( metadataVersion );

        doNothing().when( metadataVersionDelegate ).addNewMetadataVersion( metadataVersion );

        MetadataSyncSummary actualMetadataSyncSummary = metadataSyncImportHandler.importMetadata( syncParams,
            expectedMetadataSnapshot );
        verify( metadataVersionDelegate ).addNewMetadataVersion( metadataVersion );
        assertEquals( metadataSyncSummary.getImportReport(), actualMetadataSyncSummary.getImportReport() );
        assertEquals( metadataSyncSummary.getImportSummary(), actualMetadataSyncSummary.getImportSummary() );
        assertEquals( metadataSyncSummary.getMetadataVersion(), actualMetadataSyncSummary.getMetadataVersion() );
        assertEquals( metadataSyncSummary.getMetadataVersion().getType(),
            actualMetadataSyncSummary.getMetadataVersion().getType() );
        assertEquals( metadataSyncSummary.getImportReport().getStatus(),
            actualMetadataSyncSummary.getImportReport().getStatus() );
    }

    @Test
    void testShouldThrowExceptionWhenClassListMapIsNull()
        throws IOException
    {
        syncParams.setImportParams( new MetadataImportParams() );
        syncParams.setVersion( metadataVersion );
        importReport.setStatus( Status.OK );

        when( renderService.fromMetadata( any( InputStream.class ), eq( RenderFormat.JSON ) ) ).thenReturn( null );

        assertThrows( MetadataSyncServiceException.class,
            () -> metadataSyncImportHandler.importMetadata( syncParams, expectedMetadataSnapshot ),
            "ClassListMap can't be null" );

        verify( metadataImportService, never() ).importMetadata( syncParams.getImportParams() );
        verify( metadataVersionDelegate, never() ).addNewMetadataVersion( metadataVersion );
        verify( metadataVersionDelegate, never() ).addNewMetadataVersion( metadataVersion );
    }

    @Test
    void testShouldThrowExceptionWhenParsingClassListMap()
        throws IOException
    {
        syncParams.setImportParams( new MetadataImportParams() );
        syncParams.setVersion( metadataVersion );
        importReport.setStatus( Status.OK );

        when( renderService.fromMetadata( any( InputStream.class ), eq( RenderFormat.JSON ) ) )
            .thenThrow( new IOException() );

        assertThrows( MetadataSyncServiceException.class,
            () -> metadataSyncImportHandler.importMetadata( syncParams, expectedMetadataSnapshot ),
            "Exception occurred while trying to do JSON conversion while parsing class list map" );

        verify( metadataImportService, never() ).importMetadata( syncParams.getImportParams() );
        verify( metadataVersionDelegate, never() ).addNewMetadataVersion( metadataVersion );
        verify( metadataVersionDelegate, never() ).addNewMetadataVersion( metadataVersion );
    }

    @Test
    void testShouldReturnDefaultSummaryWhenImportStatusIsError()
    {
        syncParams.setImportParams( new MetadataImportParams() );
        syncParams.setVersion( metadataVersion );
        MetadataSyncSummary metadataSyncSummary = new MetadataSyncSummary();

        metadataSyncSummary.setImportReport( new ImportReport() );
        metadataSyncSummary.setMetadataVersion( metadataVersion );

        importReport.setStatus( Status.ERROR );

        when( metadataImportService.importMetadata( syncParams.getImportParams() ) ).thenReturn( importReport );

        MetadataSyncSummary actualMetadataSyncSummary = metadataSyncImportHandler.importMetadata( syncParams,
            expectedMetadataSnapshot );

        verify( metadataVersionDelegate, never() ).addNewMetadataVersion( metadataVersion );
        assertEquals( metadataSyncSummary.getImportReport().toString(),
            actualMetadataSyncSummary.getImportReport().toString() );
        assertEquals( metadataSyncSummary.getImportSummary(), actualMetadataSyncSummary.getImportSummary() );
        assertEquals( metadataSyncSummary.getMetadataVersion(), actualMetadataSyncSummary.getMetadataVersion() );
    }
}