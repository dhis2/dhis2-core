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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.dxf2.metadata.systemsettings.DefaultMetadataSystemSettingService;
import org.hisp.dhis.dxf2.metadata.version.exception.MetadataVersionServiceException;
import org.hisp.dhis.dxf2.synch.AvailabilityStatus;
import org.hisp.dhis.dxf2.synch.SynchronizationManager;
import org.hisp.dhis.exception.RemoteServerUnavailableException;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionService;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.system.util.DhisHttpResponse;
import org.hisp.dhis.system.util.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Handling remote calls for metadata version
 *
 * @author anilkumk
 */
public class MetadataVersionDelegate
{
    private static final Log log = LogFactory.getLog( MetadataVersionDelegate.class );

    @Autowired
    DefaultMetadataSystemSettingService metadataSystemSettingService;

    @Autowired
    private SynchronizationManager synchronizationManager;

    @Autowired
    private RenderService renderService;

    @Autowired
    private MetadataVersionService metadataVersionService;

    private int VERSION_TIMEOUT = 120000;

    private int DOWNLOAD_TIMEOUT = 300000;

    public MetadataVersion getRemoteMetadataVersion( String versionName )
    {
        String versionDetailsUrl = metadataSystemSettingService.getVersionDetailsUrl( versionName );
        DhisHttpResponse dhisHttpResponse = getDhisHttpResponse( versionDetailsUrl, VERSION_TIMEOUT );
        MetadataVersion dataVersion = null;

        if ( isValidDhisHttpResponse( dhisHttpResponse ) )
        {
            try
            {
                dataVersion = renderService.fromJson( dhisHttpResponse.getResponse(), MetadataVersion.class );
            }
            catch ( Exception e )
            {
                String message = "Exception occurred while trying to do JSON conversion for metadata version";
                log.error( message, e );
                throw new MetadataVersionServiceException( message, e );
            }
        }

        return dataVersion;
    }

    public List<MetadataVersion> getMetaDataDifference( MetadataVersion metadataVersion )
    {
        String url;
        List<MetadataVersion> metadataVersions = new ArrayList<>();
        if ( metadataVersion == null )
        {
            url = metadataSystemSettingService.getEntireVersionHistory();
        }
        else
        {
            url = metadataSystemSettingService.getMetaDataDifferenceURL( metadataVersion.getName() );
        }

        DhisHttpResponse dhisHttpResponse = getDhisHttpResponse( url, VERSION_TIMEOUT );

        if ( isValidDhisHttpResponse( dhisHttpResponse ) )
        {
            try
            {
                metadataVersions = renderService
                    .fromMetadataVersion( new ByteArrayInputStream( dhisHttpResponse.getResponse().getBytes() ),
                        RenderFormat.JSON );
                return metadataVersions;
            }
            catch ( IOException io )
            {
                String message =
                    "Exception occurred while trying to do JSON conversion. Caused by:  " + io.getMessage();
                log.error( message, io );
                throw new MetadataVersionServiceException( message, io );
            }
        }

        log.warn( "Returning empty for the metadata versions difference" );
        return metadataVersions;
    }

    public String downloadMetadataVersion( MetadataVersion version )
        throws MetadataVersionServiceException
    {
        String downloadVersionSnapshotURL = metadataSystemSettingService.getDownloadVersionSnapshotURL( version.getName() );
        DhisHttpResponse dhisHttpResponse = getDhisHttpResponse( downloadVersionSnapshotURL, DOWNLOAD_TIMEOUT );

        if ( isValidDhisHttpResponse( dhisHttpResponse ) )
        {
            return dhisHttpResponse.getResponse();
        }

        return null;
    }

    public void addNewMetadataVersion( MetadataVersion version )
    {
        version.setImportDate( new Date() );
        boolean isVersionExists = metadataVersionService.getVersionByName( version.getName() ) != null;

        try
        {
            if ( !isVersionExists )
            {
                metadataVersionService.addVersion( version );
            }
        }
        catch ( Exception e )
        {
            throw new MetadataVersionServiceException( "Exception occurred while trying to add metadata version" + version, e );
        }

    }

    //----------------------------------------------------------------------------------------
    // Private Methods
    //----------------------------------------------------------------------------------------

    private DhisHttpResponse getDhisHttpResponse( String url, int timeout )
    {
        AvailabilityStatus remoteServerAvailable = synchronizationManager.isRemoteServerAvailable();

        if ( !(remoteServerAvailable.isAvailable()) )
        {
            String message = remoteServerAvailable.getMessage();
            log.error( message );
            throw new RemoteServerUnavailableException( message );
        }

        String username = metadataSystemSettingService.getRemoteInstanceUserName();
        String password = metadataSystemSettingService.getRemoteInstancePassword();

        log.info( "Remote server metadata version  URL: " + url + ", username: " + username );
        DhisHttpResponse dhisHttpResponse = null;

        try
        {
            dhisHttpResponse = HttpUtils.httpGET( url, true, username, password, null, timeout, true );
        }
        catch ( Exception e )
        {
            String message = "Exception occurred while trying to make the GET call to" + url;
            log.error( message, e );
            throw new MetadataVersionServiceException( message, e );
        }

        return dhisHttpResponse;

    }

    private boolean isValidDhisHttpResponse( DhisHttpResponse dhisHttpResponse )
    {

        if ( dhisHttpResponse == null || dhisHttpResponse.getResponse().isEmpty() )
        {
            log.warn( "Dhis http response is null" );
            return false;
        }

        if ( HttpStatus.valueOf( dhisHttpResponse.getStatusCode() ).is2xxSuccessful() )
        {
            return true;
        }

        if ( HttpStatus.valueOf( dhisHttpResponse.getStatusCode() ).is4xxClientError() )
        {
            StringBuilder clientErrorMessage = buildErrorMessage( "Client Error. ", dhisHttpResponse );
            log.warn( clientErrorMessage.toString() );
            throw new MetadataVersionServiceException( clientErrorMessage.toString() );
        }

        if ( HttpStatus.valueOf( dhisHttpResponse.getStatusCode() ).is5xxServerError() )
        {
            StringBuilder serverErrorMessage = buildErrorMessage( "Server Error. ", dhisHttpResponse );
            log.warn( serverErrorMessage.toString() );
            throw new MetadataVersionServiceException( serverErrorMessage.toString() );
        }

        return false;
    }

    private StringBuilder buildErrorMessage( String errorType, DhisHttpResponse dhisHttpResponse )
    {
        StringBuilder message = new StringBuilder();
        message.append( errorType ).append( "Http call failed with status code: " )
            .append( dhisHttpResponse.getStatusCode() ).append( " Caused by: " )
            .append( dhisHttpResponse.getResponse() );
        return message;
    }
}
