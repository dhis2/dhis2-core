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
import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.metadata.tasks.MetadataRetryContext;
import org.hisp.dhis.dxf2.metadata.version.MetadataVersionDelegate;
import org.hisp.dhis.dxf2.synch.AvailabilityStatus;
import org.hisp.dhis.dxf2.synch.SynchronizationManager;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionService;
import org.hisp.dhis.metadata.version.VersionType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author aamerm
 */
@Category( IntegrationTest.class )
public class MetadataSyncPreProcessorTest
    extends DhisSpringTest
{
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Autowired
    @Mock
    private SynchronizationManager synchronizationManager;
    
    @Autowired
    @Mock
    private SystemSettingManager systemSettingManager;
    
    @InjectMocks
    @Autowired
    private MetadataSyncPreProcessor metadataSyncPreProcessor;
    
    @Autowired
    @Mock
    private MetadataVersionService metadataVersionService;
    
    @Autowired
    @Mock
    private MetadataVersionDelegate metadataVersionDelegate;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks( this );
    }

    // TODO: Do not assert for methods to be executed. Assert for the result not on how it happens.
    
    @Test
    public void testHandleDataPushShouldCallDataPush() throws Exception
    {
        MetadataRetryContext mockRetryContext = mock( MetadataRetryContext.class );
        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );

        metadataSyncPreProcessor.handleAggregateDataPush( mockRetryContext );
        verify( synchronizationManager, times( 1 ) ).executeDataPush();
    }

    @Test( expected = MetadataSyncServiceException.class )
    public void testhandleAggregateDataPushShouldThrowExceptionWhenDataPushIsUnsuccessful() throws Exception
    {
        MetadataRetryContext mockRetryContext = mock( MetadataRetryContext.class );
        ImportSummary expectedSummary = new ImportSummary();
        expectedSummary.setStatus( ImportStatus.ERROR );
        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );
        when( metadataSyncPreProcessor.handleAggregateDataPush( mockRetryContext ) ).thenReturn( expectedSummary );

        ImportSummary actualSummary = metadataSyncPreProcessor.handleAggregateDataPush( mockRetryContext );
        assertEquals( expectedSummary.getStatus(), actualSummary.getStatus() );
    }

    @Test
    public void testhandleAggregateDataPushShouldNotThrowExceptionWhenDataPushIsSuccessful() throws Exception
    {
        MetadataRetryContext mockRetryContext = mock( MetadataRetryContext.class );
        ImportSummary expectedSummary = new ImportSummary();
        expectedSummary.setStatus( ImportStatus.SUCCESS );
        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );
        when( metadataSyncPreProcessor.handleAggregateDataPush( mockRetryContext ) ).thenReturn( expectedSummary );

        ImportSummary actualSummary = metadataSyncPreProcessor.handleAggregateDataPush( mockRetryContext );
        assertEquals( expectedSummary.getStatus(), actualSummary.getStatus() );
    }

    @Test
    public void testHandleEventDataPushShouldCallEventDataPush() throws Exception
    {
        MetadataRetryContext mockRetryContext = mock( MetadataRetryContext.class );
        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );

        metadataSyncPreProcessor.handleEventDataPush( mockRetryContext );
        verify( synchronizationManager, times( 1 ) ).executeEventPush();
    }

    @Test( expected = MetadataSyncServiceException.class )
    public void testHandleEventDataPushShouldThrowExceptionWhenEventDataPushIsUnsuccessful() throws Exception
    {
        MetadataRetryContext mockRetryContext = mock( MetadataRetryContext.class );
        ImportSummaries expectedSummary = new ImportSummaries();
        ImportSummary summary = new ImportSummary();
        summary.setStatus( ImportStatus.ERROR );
        expectedSummary.addImportSummary( summary );
        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );
        when( metadataSyncPreProcessor.handleEventDataPush( mockRetryContext ) ).thenReturn( expectedSummary );

        ImportSummaries actualSummary = metadataSyncPreProcessor.handleEventDataPush( mockRetryContext );
        assertEquals( expectedSummary.getImportSummaries().get( 0 ).getStatus(), actualSummary.getImportSummaries().get( 0 ).getStatus() );
    }

    @Test
    public void testHandleEventDataPushShouldNotThrowExceptionWhenDataPushIsSuccessful() throws Exception
    {
        MetadataRetryContext mockRetryContext = mock( MetadataRetryContext.class );
        ImportSummaries expectedSummary = new ImportSummaries();
        ImportSummary summary = new ImportSummary();
        summary.setStatus( ImportStatus.SUCCESS );
        expectedSummary.addImportSummary( summary );
        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );
        when( metadataSyncPreProcessor.handleEventDataPush( mockRetryContext ) ).thenReturn( expectedSummary );

        ImportSummaries actualSummary = metadataSyncPreProcessor.handleEventDataPush( mockRetryContext );
        assertEquals( expectedSummary.getImportSummaries().get( 0 ).getStatus(), actualSummary.getImportSummaries().get( 0 ).getStatus() );
    }

    @Test
    public void testHandleMetadataVersionsListShouldReturnDifferenceOfVersionsFromBaselineVersion() throws Exception
    {
        MetadataRetryContext mockRetryContext = mock( MetadataRetryContext.class );

        MetadataVersion currentVersion = new MetadataVersion();
        currentVersion.setType( VersionType.BEST_EFFORT );
        currentVersion.setName( "test_version1" );
        currentVersion.setCreated( new Date() );
        currentVersion.setHashCode( "samplehashcode1" );

        MetadataVersion newVersion = new MetadataVersion();
        newVersion.setType( VersionType.ATOMIC );
        newVersion.setName( "test_version2" );
        newVersion.setCreated( new Date() );
        newVersion.setHashCode( "samplehashcode2" );
        List<MetadataVersion> listOfVersions = new ArrayList<>();
        listOfVersions.add( newVersion );

        when( metadataVersionDelegate.getMetaDataDifference( currentVersion ) ).thenReturn( listOfVersions );
        List<MetadataVersion> expectedListOfVersions = metadataSyncPreProcessor.handleMetadataVersionsList( mockRetryContext, currentVersion );
        assertEquals( listOfVersions.size(), expectedListOfVersions.size() );
        assertEquals( expectedListOfVersions, listOfVersions );
    }

    @Test
    public void testHandleMetadataVersionsListShouldReturnNullIfInstanceDoesNotHaveAnyVersions() throws Exception
    {
        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );
        MetadataRetryContext mockRetryContext = mock( MetadataRetryContext.class );

        MetadataVersion currentVersion = new MetadataVersion();
        currentVersion.setType( VersionType.BEST_EFFORT );
        currentVersion.setName( "test_version" );
        currentVersion.setCreated( new Date() );
        currentVersion.setHashCode( "samplehashcode" );

        when( metadataVersionDelegate.getMetaDataDifference( currentVersion ) ).thenReturn( new ArrayList<MetadataVersion>() );
        List<MetadataVersion> expectedListOfVersions = metadataSyncPreProcessor.handleMetadataVersionsList( mockRetryContext, currentVersion );
        assertTrue( expectedListOfVersions.size() == 0 );
    }

    @Test
    public void testHandleMetadataVersionsListShouldReturnEmptyListIfInstanceIsAlreadyOnLatestVersion() throws Exception
    {
        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );
        MetadataRetryContext mockRetryContext = mock( MetadataRetryContext.class );
        MetadataVersion currentVersion = new MetadataVersion();
        currentVersion.setType( VersionType.BEST_EFFORT );
        currentVersion.setName( "test_version" );
        currentVersion.setCreated( new Date() );
        currentVersion.setHashCode( "samplehashcode" );

        List<MetadataVersion> listOfVersions = new ArrayList<>();

        when( metadataVersionDelegate.getMetaDataDifference( currentVersion ) ).thenReturn( listOfVersions );
        List<MetadataVersion> expectedListOfVersions = metadataSyncPreProcessor.handleMetadataVersionsList( mockRetryContext, currentVersion );
        assertEquals( 0, expectedListOfVersions.size() );
    }

    @Test( expected = MetadataSyncServiceException.class )
    public void testHandleMetadataVersionsListShouldThrowExceptionIfAnyIssueWithMetadataDifference() throws Exception
    {
        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );
        MetadataRetryContext mockRetryContext = mock( MetadataRetryContext.class );
        MetadataVersion currentVersion = new MetadataVersion();
        currentVersion.setType( VersionType.BEST_EFFORT );
        currentVersion.setName( "test_version" );
        currentVersion.setCreated( new Date() );
        currentVersion.setHashCode( "samplehashcode" );

        List<MetadataVersion> listOfVersions = new ArrayList<>();
        listOfVersions.add( currentVersion );

        when( metadataVersionDelegate.getMetaDataDifference( currentVersion ) ).thenThrow( new MetadataSyncServiceException( "test_message" ) );
        metadataSyncPreProcessor.handleMetadataVersionsList( mockRetryContext, currentVersion );
    }

    @Test
    public void testHandleCurrentMetadataVersionShouldReturnCurrentVersionOfSystem() throws Exception
    {
        MetadataRetryContext mockRetryContext = mock( MetadataRetryContext.class );
        MetadataVersion currentVersion = new MetadataVersion();
        currentVersion.setType( VersionType.BEST_EFFORT );
        currentVersion.setName( "test_version" );
        currentVersion.setCreated( new Date() );
        currentVersion.setHashCode( "samplehashcode" );

        when( metadataVersionService.getCurrentVersion() ).thenReturn( currentVersion );
        MetadataVersion actualVersion = metadataSyncPreProcessor.handleCurrentMetadataVersion( mockRetryContext );
        assertEquals( currentVersion, actualVersion );
    }

    @Test
    public void testShouldGetLatestMetadataVersionForTheGivenVersionList() throws ParseException
    {
        MetadataRetryContext mockRetryContext = mock( MetadataRetryContext.class );
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern( "yyyy-MM-dd HH:mm:ssZ" );

        MetadataVersion currentVersion = new MetadataVersion();
        currentVersion.setType( VersionType.BEST_EFFORT );
        currentVersion.setName( "test_version1" );
        currentVersion.setCreated( new Date() );
        currentVersion.setHashCode( "samplehashcode1" );

        MetadataVersion version2 = new MetadataVersion( "Version2", VersionType.ATOMIC );
        org.joda.time.DateTime dateTime = dateTimeFormatter.parseDateTime( "2016-06-21 10:45:50Z" );
        version2.setCreated( dateTime.toDate() );

        MetadataVersion version3 = new MetadataVersion( "Version3", VersionType.ATOMIC );
        org.joda.time.DateTime dateTime2 = dateTimeFormatter.parseDateTime( "2016-06-21 10:45:54Z" );
        version3.setCreated( dateTime2.toDate() );

        MetadataVersion version4 = new MetadataVersion( "Version4", VersionType.ATOMIC );
        org.joda.time.DateTime dateTime3 = dateTimeFormatter.parseDateTime( "2016-06-21 10:45:58Z" );
        version4.setCreated( dateTime3.toDate() );

        List<MetadataVersion> metadataVersionList = new ArrayList<>();
        metadataVersionList.add( version2 );
        metadataVersionList.add( version3 );
        metadataVersionList.add( version4 );


        when( metadataVersionDelegate.getMetaDataDifference( currentVersion ) ).thenReturn( metadataVersionList );
        List<MetadataVersion> expectedListOfVersions = metadataSyncPreProcessor.handleMetadataVersionsList( mockRetryContext, currentVersion );
        verify( systemSettingManager ).saveSystemSetting( SettingKey.REMOTE_METADATA_VERSION, version4.getName() );
        assertEquals( 3, expectedListOfVersions.size() );
    }
}
