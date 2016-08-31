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

package org.hisp.dhis.dxf2.metadata.version;

import org.apache.http.HttpResponse;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dxf2.metadata.systemsettings.DefaultMetadataSystemSettingService;
import org.hisp.dhis.dxf2.metadata.version.exception.MetadataVersionServiceException;
import org.hisp.dhis.dxf2.synch.AvailabilityStatus;
import org.hisp.dhis.dxf2.synch.SynchronizationManager;
import org.hisp.dhis.exception.RemoteServerUnavailableException;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionService;
import org.hisp.dhis.metadata.version.VersionType;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.system.util.DhisHttpResponse;
import org.hisp.dhis.system.util.HttpUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.omg.CORBA.portable.InputStream;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author anilkumk
 */
@RunWith( PowerMockRunner.class )
@PrepareForTest( HttpUtils.class )
public class MetadataVersionDelegateTest
    extends DhisSpringTest
{
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Autowired
    @InjectMocks
    private MetadataVersionDelegate metadataVersionDelegate;

    @Autowired
    @Mock
    private SynchronizationManager synchronizationManager;

    @Autowired
    @Mock
    private DefaultMetadataSystemSettingService metadataSystemSettingService;

    @Autowired
    @Mock
    private MetadataVersionService metadataVersionService;

    @Autowired
    @Mock
    private RenderService renderService;

    private HttpResponse httpResponse;
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
        MockitoAnnotations.initMocks( this );

        PowerMockito.mockStatic( HttpUtils.class );
        httpResponse = mock( HttpResponse.class );
        metadataVersion = new MetadataVersion( "testVersion", VersionType.BEST_EFFORT );
        metadataVersion.setHashCode( "12wa32d4f2et3tyt5yu6i" );

        when( metadataSystemSettingService.getRemoteInstanceUserName() ).thenReturn( username );
        when( metadataSystemSettingService.getRemoteInstancePassword() ).thenReturn( password );
    }

    @Test
    public void testShouldThrowExceptionWhenServerNotAvailable() throws Exception
    {
        String url = "http://localhost:9080/api/metadata/version?versionName=Version_Name";

        when( metadataSystemSettingService.getVersionDetailsUrl( "testversion" ) ).thenReturn( url );

        AvailabilityStatus availabilityStatus = new AvailabilityStatus( false, "test_message", null );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );

        expectedException.expect( RemoteServerUnavailableException.class );
        expectedException.expectMessage( "test_message" );

        metadataVersionDelegate.getRemoteMetadataVersion( "testVersion" );
    }


    @Test
    public void testShouldGetRemoteVersionNullWhenDhisResponseReturnsNull() throws Exception
    {
        String url = "http://localhost:9080/api/metadata/version?versionName=Version_Name";

        when( metadataSystemSettingService.getVersionDetailsUrl( "testversion" ) ).thenReturn( url );

        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );
        PowerMockito.when( HttpUtils.httpGET( versionUrl, true, username, password, null, VERSION_TIMEOUT, true ) ).thenReturn( null );
        MetadataVersion version = metadataVersionDelegate.getRemoteMetadataVersion( "testVersion" );

        assertEquals( null, version );
    }

    @Test
    public void testShouldThrowExceptionWhenHTTPRequestFails() throws Exception
    {
        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "testMessage", null );

        when( metadataSystemSettingService.getVersionDetailsUrl( "testVersion" ) ).thenReturn( versionUrl );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );
        PowerMockito.when( HttpUtils.httpGET( versionUrl, true, username, password, null, VERSION_TIMEOUT, true ) ).thenThrow( new Exception( "" ) );

        expectedException.expect( MetadataVersionServiceException.class );
        expectedException.expectMessage( "Exception occurred while trying to make the GET call to" + versionUrl );

        metadataVersionDelegate.getRemoteMetadataVersion( "testVersion" );
    }

    @Test
    public void testShouldGetRemoteMetadataVersionWithStatusOk() throws Exception
    {
        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "testMessage", null );
        DhisHttpResponse dhisHttpResponse = new DhisHttpResponse( httpResponse, response, HttpStatus.OK.value() );

        when( metadataSystemSettingService.getVersionDetailsUrl( "testVersion" ) ).thenReturn( versionUrl );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );
        PowerMockito.when( HttpUtils.httpGET( versionUrl, true, username, password, null, VERSION_TIMEOUT, true ) ).thenReturn( dhisHttpResponse );
        when( renderService.fromJson( response, MetadataVersion.class ) ).thenReturn( metadataVersion );
        MetadataVersion remoteMetadataVersion = metadataVersionDelegate.getRemoteMetadataVersion( "testVersion" );

        assertEquals( metadataVersion.getType(), remoteMetadataVersion.getType() );
        assertEquals( metadataVersion.getHashCode(), remoteMetadataVersion.getHashCode() );
        assertEquals( metadataVersion.getName(), remoteMetadataVersion.getName() );
        assertEquals( metadataVersion, remoteMetadataVersion );
    }

    @Test
    public void testShouldGetMetaDataDifferenceWithStatusOk() throws Exception
    {
        String response = "{\"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
        MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.BEST_EFFORT );
        metadataVersion.setHashCode( "12wa32d4f2et3tyt5yu6i" );

        String url = "http://localhost:9080/api/metadata/version/history?baseline=testVersion";

        when( metadataSystemSettingService.getMetaDataDifferenceURL( "testVersion" ) ).thenReturn( url );

        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );

        HttpResponse httpResponse = mock( HttpResponse.class );
        DhisHttpResponse dhisHttpResponse = new DhisHttpResponse( httpResponse, response, HttpStatus.OK.value() );

        PowerMockito.mockStatic( HttpUtils.class );

        PowerMockito.when( HttpUtils.httpGET( url, true, username, password, null, VERSION_TIMEOUT, true ) ).thenReturn( dhisHttpResponse );

        List<MetadataVersion> metadataVersionList = new ArrayList<>();
        metadataVersionList.add( metadataVersion );

        when( metadataSystemSettingService.getMetaDataDifferenceURL( "testVersion" ) ).thenReturn( baselineUrl );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );
        PowerMockito.when( HttpUtils.httpGET( baselineUrl, true, username, password, null, VERSION_TIMEOUT, true ) ).thenReturn( dhisHttpResponse );
        when( renderService.fromMetadataVersion( any( InputStream.class ), eq( RenderFormat.JSON ) ) ).thenReturn( metadataVersionList );

        List<MetadataVersion> metaDataDifference = metadataVersionDelegate.getMetaDataDifference( metadataVersion );

        assertTrue( metaDataDifference.size() == metadataVersionList.size() );
        assertEquals( metadataVersionList.get( 0 ).getType(), metaDataDifference.get( 0 ).getType() );
        assertEquals( metadataVersionList.get( 0 ).getName(), metaDataDifference.get( 0 ).getName() );
        assertEquals( metadataVersionList.get( 0 ).getHashCode(), metaDataDifference.get( 0 ).getHashCode() );
    }

    @Test
    public void testShouldThrowExceptionWhenRenderServiceThrowsExceptionWhileGettingMetadataDifference() throws Exception
    {
        String response = "{\"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
        MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.BEST_EFFORT );
        metadataVersion.setHashCode( "12wa32d4f2et3tyt5yu6i" );

        String url = "http://localhost:9080/api/metadata/version/history?baseline=testVersion";

        when( metadataSystemSettingService.getMetaDataDifferenceURL( "testVersion" ) ).thenReturn( url );

        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );

        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );

        DhisHttpResponse dhisHttpResponse = new DhisHttpResponse( httpResponse, response, HttpStatus.OK.value() );

        PowerMockito.when( HttpUtils.httpGET( baselineUrl, true, username, password, null, VERSION_TIMEOUT, true ) ).thenReturn( dhisHttpResponse );
        when( renderService.fromMetadataVersion( any( InputStream.class ), eq( RenderFormat.JSON ) ) ).thenThrow( new IOException( "" ) );

        expectedException.expect( MetadataVersionServiceException.class );
        expectedException.expectMessage( "Exception occurred while trying to do JSON conversion. Caused by: " );

        metadataVersionDelegate.getMetaDataDifference( metadataVersion );
    }

    @Test
    public void testShouldReturnEmptyMetadataDifference() throws Exception
    {
        String response = "{\"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
        MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.BEST_EFFORT );
        metadataVersion.setHashCode( "12wa32d4f2et3tyt5yu6i" );

        String url = "http://localhost:9080/api/metadata/version/history?baseline=testVersion";

        when( metadataSystemSettingService.getMetaDataDifferenceURL( "testVersion" ) ).thenReturn( url );

        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );

        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );

        DhisHttpResponse dhisHttpResponse = new DhisHttpResponse( httpResponse, response, HttpStatus.BAD_REQUEST.value() );

        PowerMockito.when( HttpUtils.httpGET( baselineUrl, true, username, password, null, VERSION_TIMEOUT, true ) ).thenReturn( dhisHttpResponse );
        when( renderService.fromMetadataVersion( any( InputStream.class ), eq( RenderFormat.JSON ) ) ).thenThrow( new IOException( "" ) );

        expectedException.expect( MetadataVersionServiceException.class );
        expectedException.expectMessage( "Client Error. Http call failed with status code: 400 Caused by: " + dhisHttpResponse.getResponse() );

        metadataVersionDelegate.getMetaDataDifference( metadataVersion );
    }

    @Test
    public void testShouldThrowExceptionWhenGettingRemoteMetadataVersionWithClientError() throws Exception
    {
        String response = "{\"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
        MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.BEST_EFFORT );
        metadataVersion.setHashCode( "12wa32d4f2et3tyt5yu6i" );

        String url = "http://localhost:9080/api/metadata/version?versionName=Version_Name";

        when( metadataSystemSettingService.getVersionDetailsUrl( "testversion" ) ).thenReturn( url );

        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );

        HttpResponse httpResponse = mock( HttpResponse.class );
        DhisHttpResponse dhisHttpResponse = new DhisHttpResponse( httpResponse, response, HttpStatus.CONFLICT.value() );

        when( metadataSystemSettingService.getVersionDetailsUrl( "testVersion" ) ).thenReturn( versionUrl );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );
        PowerMockito.when( HttpUtils.httpGET( versionUrl, true, username, password, null, VERSION_TIMEOUT, true ) ).thenReturn( dhisHttpResponse );
        when( renderService.fromJson( response, MetadataVersion.class ) ).thenReturn( metadataVersion );

        expectedException.expect( MetadataVersionServiceException.class );
        expectedException.expectMessage( "Client Error. Http call failed with status code: " + HttpStatus.CONFLICT.value() + " Caused by: " + response );

        metadataVersionDelegate.getRemoteMetadataVersion( "testVersion" );
    }

    @Test
    public void testShouldThrowExceptionWhenGettingRemoteMetadataVersionWithServerError() throws Exception
    {
        String response = "{\"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
        MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.BEST_EFFORT );
        metadataVersion.setHashCode( "12wa32d4f2et3tyt5yu6i" );

        String url = "http://localhost:9080/api/metadata/version?versionName=Version_Name";

        when( metadataSystemSettingService.getVersionDetailsUrl( "testversion" ) ).thenReturn( url );

        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );

        HttpResponse httpResponse = mock( HttpResponse.class );

        DhisHttpResponse dhisHttpResponse = new DhisHttpResponse( httpResponse, response, HttpStatus.GATEWAY_TIMEOUT.value() );

        when( metadataSystemSettingService.getVersionDetailsUrl( "testVersion" ) ).thenReturn( versionUrl );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );
        PowerMockito.when( HttpUtils.httpGET( versionUrl, true, username, password, null, VERSION_TIMEOUT, true ) ).thenReturn( dhisHttpResponse );
        when( renderService.fromJson( response, MetadataVersion.class ) ).thenReturn( metadataVersion );

        expectedException.expect( MetadataVersionServiceException.class );
        expectedException.expectMessage( "Server Error. Http call failed with status code: " + HttpStatus.GATEWAY_TIMEOUT.value() + " Caused by: " + response );

        metadataVersionDelegate.getRemoteMetadataVersion( "testVersion" );
    }

    @Test
    public void testShouldThrowExceptionWhenRenderServiceThrowsException() throws Exception
    {
        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "testMessage", null );
        DhisHttpResponse dhisHttpResponse = new DhisHttpResponse( httpResponse, response, HttpStatus.OK.value() );

        when( metadataSystemSettingService.getVersionDetailsUrl( "testVersion" ) ).thenReturn( versionUrl );
        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );
        PowerMockito.when( HttpUtils.httpGET( versionUrl, true, username, password, null, VERSION_TIMEOUT, true ) ).thenReturn( dhisHttpResponse );
        when( renderService.fromJson( response, MetadataVersion.class ) ).thenThrow( new MetadataVersionServiceException( "" ) );

        expectedException.expect( MetadataVersionServiceException.class );
        expectedException.expectMessage( "Exception occurred while trying to do JSON conversion for metadata version" );

        metadataVersionDelegate.getRemoteMetadataVersion( "testVersion" );
    }

    @Test
    public void testShouldDownloadMetadataVersion() throws Exception
    {
        MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.BEST_EFFORT );
        metadataVersion.setHashCode( "12wa32d4f2et3tyt5yu6i" );

        String url = "http://localhost:9080/api/metadata/version/testVersion/data.gz";

        String response = "{\"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";

        when( metadataSystemSettingService.getDownloadVersionSnapshotURL( "testVersion" ) ).thenReturn( url );

        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );
        DhisHttpResponse dhisHttpResponse = new DhisHttpResponse( httpResponse, response, HttpStatus.OK.value() );

        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );
        PowerMockito.when( HttpUtils.httpGET( downloadUrl, true, username, password, null, DOWNLOAD_TIMEOUT, true ) ).thenReturn( dhisHttpResponse );
        String actualVersionSnapShot = metadataVersionDelegate.downloadMetadataVersion( metadataVersion );

        assertEquals( response, actualVersionSnapShot );
    }

    @Test
    public void testShouldNotDownloadMetadataVersion() throws Exception
    {
        MetadataVersion metadataVersion = new MetadataVersion( "testVersion", VersionType.BEST_EFFORT );
        metadataVersion.setHashCode( "12wa32d4f2et3tyt5yu6i" );

        String url = "http://localhost:9080/api/metadata/version/testVersion/data.gz";

        when( metadataSystemSettingService.getDownloadVersionSnapshotURL( "testVersion" ) ).thenReturn( url );

        AvailabilityStatus availabilityStatus = new AvailabilityStatus( true, "test_message", null );

        when( synchronizationManager.isRemoteServerAvailable() ).thenReturn( availabilityStatus );
        PowerMockito.when( HttpUtils.httpGET( downloadUrl, true, username, password, null, DOWNLOAD_TIMEOUT, true ) ).thenReturn( null );
        String actualMetadataVersionSnapshot = metadataVersionDelegate.downloadMetadataVersion( metadataVersion );

        assertEquals( null, actualMetadataVersionSnapshot );
    }

    @Test
    public void testShouldAddNewMetadataVersion() throws Exception
    {
        metadataVersionDelegate.addNewMetadataVersion( metadataVersion );

        verify( metadataVersionService, times( 1 ) ).addVersion( metadataVersion );
    }
}