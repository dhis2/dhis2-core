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
import org.hisp.dhis.dxf2.metadata.sync.exception.DhisVersionMismatchException;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.metadata.systemsettings.DefaultMetadataSystemSettingService;
import org.hisp.dhis.dxf2.metadata.version.MetadataVersionDelegate;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionService;
import org.hisp.dhis.metadata.version.VersionType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author sultanm
 */
public class DefaultMetadataSyncServiceTest
    extends DhisSpringTest
{

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    @Autowired
    private MetadataSyncService metadataSyncService;

    @Autowired
    @Mock
    private MetadataVersionDelegate metadataVersionDelegate;

    @Autowired
    @Mock
    private MetadataSyncDelegate metadataSyncDelegate;

    @Autowired
    @Mock
    private DefaultMetadataSystemSettingService metadataSystemSettingService;

    @Autowired
    @Mock
    private MetadataVersionService metadataVersionService;

    @Autowired
    @Mock
    private MetadataSyncImportHandler metadataSyncImportHandler;

    private Map<String, List<String>> parameters;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks( this );

        parameters = new HashMap<>();
    }

    @Test
    public void testShouldThrowExceptionWhenVersionNameNotPresentInParameters() throws MetadataSyncServiceException
    {
        expectedException.expect( MetadataSyncServiceException.class );
        expectedException.expectMessage( "Missing required parameter: 'versionName'" );

        metadataSyncService.getParamsFromMap( parameters );
    }

    @Test
    public void testShouldThrowExceptionWhenParametersAreNull() throws MetadataSyncServiceException
    {
        expectedException.expect( MetadataSyncServiceException.class );
        expectedException.expectMessage( "Missing required parameter: 'versionName'" );

        metadataSyncService.getParamsFromMap( null );
    }

    @Test
    public void testShouldThrowExceptionWhenParametersHaveVersionNameAsNull() throws MetadataSyncServiceException
    {
        parameters.put( "versionName", null );

        expectedException.expect( MetadataSyncServiceException.class );
        expectedException.expectMessage( "Missing required parameter: 'versionName'" );

        metadataSyncService.getParamsFromMap( parameters );
    }

    @Test
    public void testShouldThrowExceptionWhenParametersHaveVersionNameAssignedToEmptyList() throws MetadataSyncServiceException
    {
        parameters.put( "versionName", new ArrayList<>() );

        expectedException.expect( MetadataSyncServiceException.class );
        expectedException.expectMessage( "Missing required parameter: 'versionName'" );

        metadataSyncService.getParamsFromMap( parameters );
    }

    @Test
    public void testShouldReturnNullWhenVersionNameIsAssignedToListHavingNullEntry() throws Exception
    {
        parameters.put( "versionName", new ArrayList<>() );
        parameters.get( "versionName" ).add( null );

        MetadataSyncParams paramsFromMap = metadataSyncService.getParamsFromMap( parameters );

        assertEquals( null, paramsFromMap.getVersion() );
    }

    @Test
    public void testShouldReturnNullWhenVersionNameIsAssignedToListHavingEmptyString() throws Exception
    {
        parameters.put( "versionName", new ArrayList<>() );
        parameters.get( "versionName" ).add( "" );

        MetadataSyncParams paramsFromMap = metadataSyncService.getParamsFromMap( parameters );

        assertEquals( null, paramsFromMap.getVersion() );
    }

    @Test
    public void testShouldGetExceptionIfRemoteVersionIsNotAvailable() throws Exception
    {
        parameters.put( "versionName", new ArrayList<>() );
        parameters.get( "versionName" ).add( "testVersion" );

        when( metadataVersionDelegate.getRemoteMetadataVersion( "testVersion" ) ).thenReturn( null );

        expectedException.expect( MetadataSyncServiceException.class );
        expectedException.expectMessage( "The MetadataVersion could not be fetched from the remote server for the versionName: testVersion" );

        metadataSyncService.getParamsFromMap( parameters );
    }

    @Test
    public void testShouldGetMetadataVersionForGivenVersionName() throws Exception
    {
        parameters.put( "versionName", new ArrayList<>() );
        parameters.get( "versionName" ).add( "testVersion" );
        MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.ATOMIC );

        when( metadataVersionDelegate.getRemoteMetadataVersion( "testVersion" ) ).thenReturn( metadataVersion );
        MetadataSyncParams metadataSyncParams = metadataSyncService.getParamsFromMap( parameters );

        assertEquals( metadataVersion, metadataSyncParams.getVersion() );
    }

    @Test
    public void testShouldThrowExceptionWhenSyncParamsIsNull() throws DhisVersionMismatchException
    {
        expectedException.expect( MetadataSyncServiceException.class );
        expectedException.expectMessage( "MetadataSyncParams cant be null" );

        metadataSyncService.doMetadataSync( null );
    }

    @Test
    public void testShouldThrowExceptionWhenVersionIsNulInSyncParams() throws DhisVersionMismatchException
    {
        MetadataSyncParams syncParams = new MetadataSyncParams();
        syncParams.setVersion( null );

        expectedException.expect( MetadataSyncServiceException.class );
        expectedException.expectMessage( "MetadataVersion for the Sync cant be null. The ClassListMap could not be constructed." );

        metadataSyncService.doMetadataSync( syncParams );
    }

    @Test
    public void testShouldThrowExceptionWhenSnapshotReturnsNullForGivenVersion() throws DhisVersionMismatchException
    {

        MetadataSyncParams syncParams = Mockito.mock( MetadataSyncParams.class );
        MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.ATOMIC );

        when( syncParams.getVersion() ).thenReturn( metadataVersion );
        when( metadataVersionDelegate.downloadMetadataVersionSnapshot( metadataVersion ) ).thenReturn( null );

        expectedException.expect( MetadataSyncServiceException.class );
        expectedException.expectMessage( "Metadata snapshot can't be null." );

        metadataSyncService.doMetadataSync( syncParams );
    }

    @Test
    public void testShouldThrowExceptionWhenDHISVersionsMismatch() throws DhisVersionMismatchException
    {
        MetadataSyncParams syncParams = Mockito.mock( MetadataSyncParams.class );
        MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.ATOMIC );
        String expectedMetadataSnapshot = "{\"date\":\"2016-05-24T05:27:25.128+0000\"}";

        when( syncParams.getVersion() ).thenReturn( metadataVersion );
        when( metadataVersionDelegate.downloadMetadataVersionSnapshot( metadataVersion ) ).thenReturn( expectedMetadataSnapshot );
        when ( metadataSyncDelegate.shouldStopSync( expectedMetadataSnapshot ) ).thenReturn( true );
        when( metadataVersionService.isMetadataPassingIntegrity( metadataVersion, expectedMetadataSnapshot)).thenReturn( true );

        expectedException.expect( DhisVersionMismatchException.class );
        expectedException.expectMessage( "Metadata sync failed because your version of DHIS does not match the master version" );

        metadataSyncService.doMetadataSync( syncParams );
    }

    @Test
    public void testShouldNotThrowExceptionWhenDHISVersionsMismatch() throws DhisVersionMismatchException
    {
        MetadataSyncParams syncParams = Mockito.mock( MetadataSyncParams.class );
        MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.ATOMIC );
        String expectedMetadataSnapshot = "{\"date\":\"2016-05-24T05:27:25.128+0000\", \"version\": \"2.26\"}";

        when( syncParams.getVersion() ).thenReturn( metadataVersion );
        when( metadataVersionDelegate.downloadMetadataVersionSnapshot( metadataVersion ) ).thenReturn( expectedMetadataSnapshot );
        when ( metadataSyncDelegate.shouldStopSync( expectedMetadataSnapshot ) ).thenReturn( false );
        when( metadataVersionService.isMetadataPassingIntegrity( metadataVersion, expectedMetadataSnapshot ) ).thenReturn( true );

        metadataSyncService.doMetadataSync( syncParams );
    }

    @Test
    public void testShouldThrowExceptionWhenSnapshotNotPassingIntegrity() throws DhisVersionMismatchException
    {
        MetadataSyncParams syncParams = Mockito.mock( MetadataSyncParams.class );
        MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.ATOMIC );
        String expectedMetadataSnapshot = "{\"date\":\"2016-05-24T05:27:25.128+0000\"}";

        when( syncParams.getVersion() ).thenReturn( metadataVersion );
        when( metadataVersionDelegate.downloadMetadataVersionSnapshot( metadataVersion ) ).thenReturn( expectedMetadataSnapshot );
        when( metadataVersionService.isMetadataPassingIntegrity( metadataVersion, expectedMetadataSnapshot ) ).thenReturn( false );

        expectedException.expect( MetadataSyncServiceException.class );
        expectedException.expectMessage( "Metadata snapshot is corrupted." );

        metadataSyncService.doMetadataSync( syncParams );
    }

    @Test
    public void testShouldStoreMetadataSnapshotInDataStoreAndImport() throws DhisVersionMismatchException
    {
        MetadataSyncParams syncParams = Mockito.mock( MetadataSyncParams.class );
        MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.ATOMIC );
        MetadataImportParams metadataImportParams = new MetadataImportParams();
        MetadataSyncSummary metadataSyncSummary = new MetadataSyncSummary();
        metadataSyncSummary.setMetadataVersion( metadataVersion );
        String expectedMetadataSnapshot = "{\"date\":\"2016-05-24T05:27:25.128+0000\"}";

        when( syncParams.getVersion() ).thenReturn( metadataVersion );
        when( syncParams.getImportParams() ).thenReturn( metadataImportParams );
        when( metadataVersionService.getVersionData( "testVersion" ) ).thenReturn( null );
        when( metadataVersionDelegate.downloadMetadataVersionSnapshot( metadataVersion ) ).thenReturn( expectedMetadataSnapshot );
        when( metadataVersionService.isMetadataPassingIntegrity( metadataVersion, expectedMetadataSnapshot ) ).thenReturn( true );
        when( metadataSyncImportHandler.importMetadata(syncParams,expectedMetadataSnapshot) ).thenReturn( metadataSyncSummary );

        MetadataSyncSummary actualSummary = metadataSyncService.doMetadataSync( syncParams );

        verify( metadataVersionService, times( 1 ) ).createMetadataVersionInDataStore( metadataVersion.getName(), expectedMetadataSnapshot );
        assertEquals( null, actualSummary.getImportReport() );
        assertEquals( null, actualSummary.getImportSummary() );
        assertEquals( metadataVersion, actualSummary.getMetadataVersion() );
    }

    @Test
    public void testShouldNotStoreMetadataSnapshotInDataStoreWhenAlreadyExistsInLocalStore() throws DhisVersionMismatchException
    {
        MetadataSyncParams syncParams = Mockito.mock( MetadataSyncParams.class );

        MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.ATOMIC );
        MetadataImportParams metadataImportParams = new MetadataImportParams();

        MetadataSyncSummary metadataSyncSummary = new MetadataSyncSummary();
        metadataSyncSummary.setMetadataVersion( metadataVersion );

        String expectedMetadataSnapshot = "{\"date\":\"2016-05-24T05:27:25.128+0000\"}";

        when( syncParams.getVersion() ).thenReturn( metadataVersion );
        when( syncParams.getImportParams() ).thenReturn( metadataImportParams );
        when( metadataVersionService.getVersionData( "testVersion" ) ).thenReturn( expectedMetadataSnapshot );

        when( metadataVersionService.isMetadataPassingIntegrity( metadataVersion, expectedMetadataSnapshot ) ).thenReturn( true );
        when( metadataSyncImportHandler.importMetadata( syncParams, expectedMetadataSnapshot ) ).thenReturn( metadataSyncSummary );

        MetadataSyncSummary actualSummary = metadataSyncService.doMetadataSync( syncParams );

        verify( metadataVersionService, never() ).createMetadataVersionInDataStore( metadataVersion.getName(), expectedMetadataSnapshot );
        verify( metadataVersionDelegate, never() ).downloadMetadataVersionSnapshot( metadataVersion );
        assertEquals( null, actualSummary.getImportReport() );
        assertEquals( null, actualSummary.getImportSummary() );
        assertEquals( metadataVersion, actualSummary.getMetadataVersion() );
    }

    @Test
    public void testShouldVerifyImportParamsAtomicTypeForTheGivenBestEffortVersion() throws DhisVersionMismatchException
    {
        MetadataSyncParams syncParams = new MetadataSyncParams();

        MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.BEST_EFFORT);
        MetadataImportParams metadataImportParams = new MetadataImportParams();

        syncParams.setVersion( metadataVersion);
        syncParams.setImportParams( metadataImportParams );

        MetadataSyncSummary metadataSyncSummary = new MetadataSyncSummary();
        metadataSyncSummary.setMetadataVersion( metadataVersion );
        String expectedMetadataSnapshot = "{\"date\":\"2016-05-24T05:27:25.128+0000\"}";

        when( metadataVersionService.getVersionData( "testVersion" ) ).thenReturn(expectedMetadataSnapshot );
        when( metadataVersionService.isMetadataPassingIntegrity( metadataVersion, expectedMetadataSnapshot ) ).thenReturn( true );

        metadataSyncService.doMetadataSync( syncParams );

        verify( metadataSyncImportHandler, times( 1 )).importMetadata( (argThat( new ArgumentMatcher<MetadataSyncParams>() {
            @Override
            public boolean matches(Object syncParams) {
                return ((MetadataSyncParams) syncParams).getImportParams().getAtomicMode().equals(AtomicMode.NONE);
            }
        })), eq(expectedMetadataSnapshot));

        verify( metadataVersionService, never() ).createMetadataVersionInDataStore( metadataVersion.getName(), expectedMetadataSnapshot );
        verify( metadataVersionDelegate, never() ).downloadMetadataVersionSnapshot( metadataVersion );

    }
}