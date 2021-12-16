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
package org.hisp.dhis.dxf2.metadata.version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.hisp.dhis.dxf2.metadata.sync.exception.RemoteServerUnavailableException;
import org.hisp.dhis.dxf2.metadata.systemsettings.DefaultMetadataSystemSettingService;
import org.hisp.dhis.dxf2.metadata.version.exception.MetadataVersionServiceException;
import org.hisp.dhis.dxf2.synch.AvailabilityStatus;
import org.hisp.dhis.dxf2.synch.SynchronizationManager;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionService;
import org.hisp.dhis.metadata.version.VersionType;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.system.util.DhisHttpResponse;
import org.hisp.dhis.system.util.HttpUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;

/**
 * @author anilkumk
 */
@RunWith( MockitoJUnitRunner.class )
public class MetadataVersionDelegateTest
{

    @Mock
    private SynchronizationManager synchronizationManager;

    @Mock
    private DefaultMetadataSystemSettingService metadataSystemSettingService;

    @Mock
    private MetadataVersionService metadataVersionService;

    @Mock
    private RenderService renderService;

    private HttpResponse httpResponse;

    private MetadataVersionDelegate target;

    private MetadataVersion metadataVersion;

    private int VERSION_TIMEOUT = 120000;

    private int DOWNLOAD_TIMEOUT = 300000;

    private String username = "username";

    private String password = "password";

    private String versionUrl = "http://localhost:9080/api/metadata/version?versionName=Version_Name";

    private String baselineUrl = "http://localhost:9080/api/metadata/version/history?baseline=testVersion";

    private String downloadUrl = "http://localhost:9080/api/metadata/version/testVersion/data.gz";

    private String response = "{\"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";

    @Before
    public void setup()
    {
        httpResponse = mock( HttpResponse.class );
        metadataVersion = new MetadataVersion( "testVersion", VersionType.BEST_EFFORT );
        metadataVersion.setHashCode( "12wa32d4f2et3tyt5yu6i" );

        target = new MetadataVersionDelegate( metadataSystemSettingService, synchronizationManager, renderService,
            metadataVersionService );

    }

    @Test
    public void testShouldThrowExceptionWhenServerNotAvailable()
    {

        AvailabilityStatus availabilityStatus = new AvailabilityStatus( false, "test_message", null );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );

        try ( MockedStatic<HttpUtils> mocked = mockStatic( HttpUtils.class ) )
        {
            mocked
                .when( () -> HttpUtils.httpGET( downloadUrl, true, username, password, null, DOWNLOAD_TIMEOUT, true ) )
                .thenReturn( null );

            RemoteServerUnavailableException ex = assertThrows( RemoteServerUnavailableException.class,
                () -> target
                    .downloadMetadataVersionSnapshot( new MetadataVersion( "testVersion", VersionType.BEST_EFFORT ) ) );
            assertEquals( "test_message", ex.getMessage() );

            mocked.verifyNoInteractions();
        }
    }

    @Test
    public void testShouldGetRemoteVersionNullWhenDhisResponseReturnsNull()
    {

        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );

        try ( MockedStatic<HttpUtils> mocked = mockStatic( HttpUtils.class ) )
        {
            mocked.when( () -> HttpUtils.httpGET( versionUrl, true, username, password, null, VERSION_TIMEOUT, true ) )
                .thenReturn( null );

            MetadataVersion version = target.getRemoteMetadataVersion( "testVersion" );

            assertNull( version );
        }
    }

    @Test
    public void testShouldThrowExceptionWhenHTTPRequestFails()
    {
        when( metadataSystemSettingService.getRemoteInstanceUserName() ).thenReturn( username );
        when( metadataSystemSettingService.getRemoteInstancePassword() ).thenReturn( password );

        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "testMessage", null );

        when( metadataSystemSettingService.getVersionDetailsUrl( "testVersion" ) ).thenReturn( versionUrl );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );

        try ( MockedStatic<HttpUtils> mocked = mockStatic( HttpUtils.class ) )
        {
            mocked.when( () -> HttpUtils.httpGET( versionUrl, true, username, password, null, VERSION_TIMEOUT, true ) )
                .thenThrow( new Exception( "" ) );

            assertThrows( MetadataVersionServiceException.class,
                () -> target.getRemoteMetadataVersion( "testVersion" ) );
        }
    }

    @Test
    public void testShouldGetRemoteMetadataVersionWithStatusOk()
        throws Exception
    {
        when( metadataSystemSettingService.getRemoteInstanceUserName() ).thenReturn( username );
        when( metadataSystemSettingService.getRemoteInstancePassword() ).thenReturn( password );

        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "testMessage", null );
        DhisHttpResponse dhisHttpResponse = new DhisHttpResponse( httpResponse, response, HttpStatus.OK.value() );

        when( metadataSystemSettingService.getVersionDetailsUrl( "testVersion" ) ).thenReturn( versionUrl );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );

        try ( MockedStatic<HttpUtils> mocked = mockStatic( HttpUtils.class ) )
        {
            mocked.when( () -> HttpUtils.httpGET( versionUrl, true, username, password, null, VERSION_TIMEOUT, true ) )
                .thenReturn( dhisHttpResponse );

            when( renderService.fromJson( response, MetadataVersion.class ) ).thenReturn( metadataVersion );
            MetadataVersion remoteMetadataVersion = target.getRemoteMetadataVersion( "testVersion" );

            assertEquals( metadataVersion.getType(), remoteMetadataVersion.getType() );
            assertEquals( metadataVersion.getHashCode(), remoteMetadataVersion.getHashCode() );
            assertEquals( metadataVersion.getName(), remoteMetadataVersion.getName() );
            assertEquals( metadataVersion, remoteMetadataVersion );
        }
    }

    @Test
    public void testShouldGetMetaDataDifferenceWithStatusOk()
        throws Exception
    {
        when( metadataSystemSettingService.getRemoteInstanceUserName() ).thenReturn( username );
        when( metadataSystemSettingService.getRemoteInstancePassword() ).thenReturn( password );

        String response = "{\"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
        MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.BEST_EFFORT );
        metadataVersion.setHashCode( "12wa32d4f2et3tyt5yu6i" );

        String url = "http://localhost:9080/api/metadata/version/history?baseline=testVersion";

        when( metadataSystemSettingService.getMetaDataDifferenceURL( "testVersion" ) ).thenReturn( url );

        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );

        HttpResponse httpResponse = mock( HttpResponse.class );
        DhisHttpResponse dhisHttpResponse = new DhisHttpResponse( httpResponse, response, HttpStatus.OK.value() );

        try ( MockedStatic<HttpUtils> mocked = mockStatic( HttpUtils.class ) )
        {
            mocked.when( () -> HttpUtils.httpGET( baselineUrl, true, username, password, null, VERSION_TIMEOUT, true ) )
                .thenReturn( dhisHttpResponse );

            List<MetadataVersion> metadataVersionList = new ArrayList<>();
            metadataVersionList.add( metadataVersion );

            when( metadataSystemSettingService.getMetaDataDifferenceURL( "testVersion" ) ).thenReturn( baselineUrl );
            when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );
            when( renderService.fromMetadataVersion( any( ByteArrayInputStream.class ), eq( RenderFormat.JSON ) ) )
                .thenReturn( metadataVersionList );

            List<MetadataVersion> metaDataDifference = target.getMetaDataDifference( metadataVersion );

            assertEquals( metaDataDifference.size(), metadataVersionList.size() );
            assertEquals( metadataVersionList.get( 0 ).getType(), metaDataDifference.get( 0 ).getType() );
            assertEquals( metadataVersionList.get( 0 ).getName(), metaDataDifference.get( 0 ).getName() );
            assertEquals( metadataVersionList.get( 0 ).getHashCode(), metaDataDifference.get( 0 ).getHashCode() );
        }
    }

    @Test
    public void testShouldThrowExceptionWhenRenderServiceThrowsExceptionWhileGettingMetadataDifference()
        throws Exception
    {
        when( metadataSystemSettingService.getRemoteInstanceUserName() ).thenReturn( username );
        when( metadataSystemSettingService.getRemoteInstancePassword() ).thenReturn( password );

        String response = "{\"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
        MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.BEST_EFFORT );
        metadataVersion.setHashCode( "12wa32d4f2et3tyt5yu6i" );

        String url = "http://localhost:9080/api/metadata/version/history?baseline=testVersion";

        when( metadataSystemSettingService.getMetaDataDifferenceURL( "testVersion" ) ).thenReturn( url );

        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );

        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );

        DhisHttpResponse dhisHttpResponse = new DhisHttpResponse( httpResponse, response, HttpStatus.OK.value() );

        try ( MockedStatic<HttpUtils> mocked = mockStatic( HttpUtils.class ) )
        {
            mocked.when( () -> HttpUtils.httpGET( baselineUrl, true, username, password, null, VERSION_TIMEOUT, true ) )
                .thenReturn( dhisHttpResponse );
            when( renderService.fromMetadataVersion( any( ByteArrayInputStream.class ), eq( RenderFormat.JSON ) ) )
                .thenThrow( new IOException( "" ) );

            assertThrows( "Exception occurred while trying to do JSON conversion. Caused by: ",
                MetadataVersionServiceException.class,
                () -> target.getMetaDataDifference( metadataVersion ) );
        }
    }

    @Test
    public void testShouldReturnEmptyMetadataDifference()
    {
        when( metadataSystemSettingService.getRemoteInstanceUserName() ).thenReturn( username );
        when( metadataSystemSettingService.getRemoteInstancePassword() ).thenReturn( password );

        String response = "{\"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
        MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.BEST_EFFORT );
        metadataVersion.setHashCode( "12wa32d4f2et3tyt5yu6i" );

        String url = "http://localhost:9080/api/metadata/version/history?baseline=testVersion";

        when( metadataSystemSettingService.getMetaDataDifferenceURL( "testVersion" ) ).thenReturn( url );

        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );

        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );

        DhisHttpResponse dhisHttpResponse = new DhisHttpResponse( httpResponse, response,
            HttpStatus.BAD_REQUEST.value() );

        try ( MockedStatic<HttpUtils> mocked = mockStatic( HttpUtils.class ) )
        {
            mocked.when( () -> HttpUtils.httpGET( baselineUrl, true, username, password, null, VERSION_TIMEOUT, true ) )
                .thenReturn( dhisHttpResponse );

            assertThrows(
                "Client Error. Http call failed with status code: 400 Caused by: " + dhisHttpResponse.getResponse(),
                MetadataVersionServiceException.class,
                () -> target.getMetaDataDifference( metadataVersion ) );
        }
    }

    @Test
    public void testShouldThrowExceptionWhenGettingRemoteMetadataVersionWithClientError()
    {
        when( metadataSystemSettingService.getRemoteInstanceUserName() ).thenReturn( username );
        when( metadataSystemSettingService.getRemoteInstancePassword() ).thenReturn( password );

        String response = "{\"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
        MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.BEST_EFFORT );
        metadataVersion.setHashCode( "12wa32d4f2et3tyt5yu6i" );

        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );

        HttpResponse httpResponse = mock( HttpResponse.class );
        DhisHttpResponse dhisHttpResponse = new DhisHttpResponse( httpResponse, response, HttpStatus.CONFLICT.value() );

        when( metadataSystemSettingService.getVersionDetailsUrl( "testVersion" ) ).thenReturn( versionUrl );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );

        try ( MockedStatic<HttpUtils> mocked = mockStatic( HttpUtils.class ) )
        {
            mocked.when( () -> HttpUtils.httpGET( versionUrl, true, username, password, null, VERSION_TIMEOUT, true ) )
                .thenReturn( dhisHttpResponse );

            assertThrows(
                "Client Error. Http call failed with status code: "
                    + HttpStatus.CONFLICT.value() + " Caused by: " + response,
                MetadataVersionServiceException.class,
                () -> target.getRemoteMetadataVersion( "testVersion" ) );
        }
    }

    @Test
    public void testShouldThrowExceptionWhenGettingRemoteMetadataVersionWithServerError()
    {
        when( metadataSystemSettingService.getRemoteInstanceUserName() ).thenReturn( username );
        when( metadataSystemSettingService.getRemoteInstancePassword() ).thenReturn( password );

        String response = "{\"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
        MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.BEST_EFFORT );
        metadataVersion.setHashCode( "12wa32d4f2et3tyt5yu6i" );

        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );

        HttpResponse httpResponse = mock( HttpResponse.class );

        DhisHttpResponse dhisHttpResponse = new DhisHttpResponse( httpResponse, response,
            HttpStatus.GATEWAY_TIMEOUT.value() );

        when( metadataSystemSettingService.getVersionDetailsUrl( "testVersion" ) ).thenReturn( versionUrl );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );

        try ( MockedStatic<HttpUtils> mocked = mockStatic( HttpUtils.class ) )
        {
            mocked.when( () -> HttpUtils.httpGET( versionUrl, true, username, password, null, VERSION_TIMEOUT, true ) )
                .thenReturn( dhisHttpResponse );

            assertThrows(
                "Server Error. Http call failed with status code: "
                    + HttpStatus.GATEWAY_TIMEOUT.value() + " Caused by: " + response,
                MetadataVersionServiceException.class,
                () -> target.getRemoteMetadataVersion( "testVersion" ) );
        }
    }

    @Test
    public void testShouldThrowExceptionWhenRenderServiceThrowsException()
        throws Exception
    {
        when( metadataSystemSettingService.getRemoteInstanceUserName() ).thenReturn( username );
        when( metadataSystemSettingService.getRemoteInstancePassword() ).thenReturn( password );

        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "testMessage", null );
        DhisHttpResponse dhisHttpResponse = new DhisHttpResponse( httpResponse, response, HttpStatus.OK.value() );

        when( metadataSystemSettingService.getVersionDetailsUrl( "testVersion" ) ).thenReturn( versionUrl );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );

        try ( MockedStatic<HttpUtils> mocked = mockStatic( HttpUtils.class ) )
        {
            mocked.when( () -> HttpUtils.httpGET( versionUrl, true, username, password, null, VERSION_TIMEOUT, true ) )
                .thenReturn( dhisHttpResponse );
            when( renderService.fromJson( response, MetadataVersion.class ) )
                .thenThrow( new MetadataVersionServiceException( "" ) );

            assertThrows(
                "Exception occurred while trying to do JSON conversion for metadata version",
                MetadataVersionServiceException.class,
                () -> target.getRemoteMetadataVersion( "testVersion" ) );
        }
    }

    @Test
    public void testShouldDownloadMetadataVersion()
    {
        when( metadataSystemSettingService.getDownloadVersionSnapshotURL( "testVersion" ) )
            .thenReturn( downloadUrl );
        when( synchronizationManager.isRemoteServerAvailable() )
            .thenReturn( new AvailabilityStatus( true, "test_message", null ) );
        when( metadataSystemSettingService.getRemoteInstanceUserName() ).thenReturn( username );
        when( metadataSystemSettingService.getRemoteInstancePassword() ).thenReturn( password );

        try ( MockedStatic<HttpUtils> mocked = mockStatic( HttpUtils.class ) )
        {
            String response = "{\"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
            DhisHttpResponse dhisHttpResponse = new DhisHttpResponse( httpResponse, response, HttpStatus.OK.value() );
            mocked
                .when( () -> HttpUtils.httpGET( downloadUrl, true, username, password, null, DOWNLOAD_TIMEOUT, true ) )
                .thenReturn( dhisHttpResponse );
            MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.BEST_EFFORT );
            metadataVersion.setHashCode( "12wa32d4f2et3tyt5yu6i" );

            String actualVersionSnapShot = target.downloadMetadataVersionSnapshot( metadataVersion );

            assertEquals( response, actualVersionSnapShot );
        }
    }

    @Test
    public void testShouldReturnNullOnInValidDHISResponse()
    {
        when( metadataSystemSettingService.getDownloadVersionSnapshotURL( "testVersion" ) )
            .thenReturn( downloadUrl );
        when( synchronizationManager.isRemoteServerAvailable() )
            .thenReturn( new AvailabilityStatus( true, "test_message", null ) );
        when( metadataSystemSettingService.getRemoteInstanceUserName() ).thenReturn( username );
        when( metadataSystemSettingService.getRemoteInstancePassword() ).thenReturn( password );

        try ( MockedStatic<HttpUtils> mocked = mockStatic( HttpUtils.class ) )
        {
            mocked
                .when( () -> HttpUtils.httpGET( downloadUrl, true, username, password, null, DOWNLOAD_TIMEOUT, true ) )
                .thenReturn( null );

            String actualVersionSnapShot = target
                .downloadMetadataVersionSnapshot( new MetadataVersion( "testVersion", VersionType.BEST_EFFORT ) );

            assertNull( actualVersionSnapShot );
        }
    }

    @Test
    public void testShouldNotGetMetadataVersionIfRemoteServerIsUnavailable()
    {
        when( metadataSystemSettingService.getDownloadVersionSnapshotURL( "testVersion" ) )
            .thenReturn( downloadUrl );
        when( synchronizationManager.isRemoteServerAvailable() )
            .thenReturn( new AvailabilityStatus( false, "test_message", null ) );

        try ( MockedStatic<HttpUtils> mocked = mockStatic( HttpUtils.class ) )
        {
            mocked
                .when( () -> HttpUtils.httpGET( downloadUrl, true, username, password, null, DOWNLOAD_TIMEOUT, true ) )
                .thenReturn( null );

            assertThrows( RemoteServerUnavailableException.class,
                () -> target
                    .downloadMetadataVersionSnapshot( new MetadataVersion( "testVersion", VersionType.BEST_EFFORT ) ) );

            mocked.verifyNoInteractions();
        }
    }

    @Test
    public void testShouldAddNewMetadataVersion()
    {
        target.addNewMetadataVersion( metadataVersion );

        verify( metadataVersionService, times( 1 ) ).addVersion( metadataVersion );
    }
}