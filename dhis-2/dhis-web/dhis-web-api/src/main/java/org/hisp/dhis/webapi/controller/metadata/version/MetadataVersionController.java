package org.hisp.dhis.webapi.controller.metadata.version;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.apache.commons.lang.StringUtils;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dxf2.metadata.version.exception.MetadataVersionServiceException;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionService;
import org.hisp.dhis.metadata.version.VersionType;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.schema.descriptors.MetadataVersionSchemaDescriptor;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.webapi.controller.CrudControllerAdvice;
import org.hisp.dhis.webapi.controller.exception.BadRequestException;
import org.hisp.dhis.webapi.controller.exception.MetadataVersionException;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Controller for MetadataVersion
 *
 * @author aamerm
 */
@Controller
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class MetadataVersionController
    extends CrudControllerAdvice
{
    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private MetadataVersionService versionService;

    @Autowired
    private ContextUtils contextUtils;

    //Gets the version by versionName or latest system version
    @RequestMapping( value = MetadataVersionSchemaDescriptor.API_ENDPOINT, method = RequestMethod.GET, produces = ContextUtils.CONTENT_TYPE_JSON )
    public @ResponseBody MetadataVersion getMetaDataVersion( @RequestParam( value = "versionName", required = false ) String versionName )
        throws MetadataVersionException, BadRequestException
    {
        MetadataVersion versionToReturn = null;
        boolean enabled = isMetadataVersioningEnabled();

        try
        {
            if ( !enabled )
            {
                throw new BadRequestException( "Metadata versioning is not enabled for this instance." );
            }

            if ( StringUtils.isNotEmpty( versionName ) )
            {
                versionToReturn = versionService.getVersionByName( versionName );

                if ( versionToReturn == null )
                {
                    throw new MetadataVersionException( "No metadata version with name " + versionName + " exists. Please check again later." );
                }

            }

            else
            {
                versionToReturn = versionService.getCurrentVersion();

                if ( versionToReturn == null )
                {
                    throw new MetadataVersionException( "No metadata versions exist. Please check again later." );
                }

            }

            return versionToReturn;
        }
        catch ( MetadataVersionServiceException ex )
        {
            throw new MetadataVersionException( "Exception occurred while getting metadata version." + (StringUtils.isNotEmpty( versionName ) ? versionName : " ") + ex.getMessage(), ex );
        }
    }

    //Gets the list of all versions in between the passed version name and latest system version
    @RequestMapping( value = MetadataVersionSchemaDescriptor.API_ENDPOINT + "/history", method = RequestMethod.GET, produces = ContextUtils.CONTENT_TYPE_JSON )
    public @ResponseBody RootNode getMetaDataVersionHistory( @RequestParam( value = "baseline", required = false ) String versionName )
        throws MetadataVersionException, BadRequestException
    {
        List<MetadataVersion> allVersionsInBetween = new ArrayList<>();
        boolean enabled = isMetadataVersioningEnabled();

        try
        {

            if ( !enabled )
            {
                throw new BadRequestException( "Metadata versioning is not enabled for this instance." );
            }

            Date startDate;

            if ( versionName == null || versionName.isEmpty() )
            {
                MetadataVersion initialVersion = versionService.getInitialVersion();

                if ( initialVersion == null )
                {
                    return versionService.getMetadataVersionsAsNode( allVersionsInBetween );
                }

                startDate = initialVersion.getCreated();
            }
            else
            {
                startDate = versionService.getCreatedDate( versionName );
            }

            if ( startDate == null )
            {
                throw new MetadataVersionException( "There is no such metadata version. The latest version is Version " + versionService.getCurrentVersion().getName() );
            }

            Date endDate = new Date();
            allVersionsInBetween = versionService.getAllVersionsInBetween( startDate, endDate );

            if ( allVersionsInBetween != null )
            {
                //now remove the baseline version details
                for ( Iterator<MetadataVersion> iterator = allVersionsInBetween.iterator(); iterator.hasNext(); )
                {
                    MetadataVersion m = iterator.next();

                    if ( m.getName().equals( versionName ) )
                    {
                        iterator.remove();
                        break;
                    }

                }

                if ( !allVersionsInBetween.isEmpty() )
                {
                    return versionService.getMetadataVersionsAsNode( allVersionsInBetween );
                }
            }

        }
        catch ( MetadataVersionServiceException ex )
        {
            throw new MetadataVersionException( ex.getMessage(), ex );
        }
        return null;
    }

    //Gets the list of all versions
    @RequestMapping( value = "/metadata/versions", method = RequestMethod.GET, produces = ContextUtils.CONTENT_TYPE_JSON )
    public @ResponseBody RootNode getAllVersion() throws MetadataVersionException,BadRequestException
    {
        boolean enabled = isMetadataVersioningEnabled();

        try
        {
            if ( !enabled )
            {
                throw new BadRequestException( "Metadata versioning is not enabled for this instance." );
            }

            List<MetadataVersion> allVersions = versionService.getAllVersions();
            return versionService.getMetadataVersionsAsNode( allVersions );

        }
        catch ( MetadataVersionServiceException ex )
        {
            throw new MetadataVersionException( "Exception occurred while getting all metadata versions. " + ex.getMessage() );
        }
    }


    //Creates version in versioning table, exports the metadata and saves the snapshot in datastore
    @PreAuthorize( "hasRole('ALL') or hasRole('F_METADATA_MANAGE')" )
    @RequestMapping( value = MetadataVersionSchemaDescriptor.API_ENDPOINT + "/create", method = RequestMethod.POST, produces = ContextUtils.CONTENT_TYPE_JSON )
    public @ResponseBody MetadataVersion createSystemVersion( @RequestParam( value = "type" ) VersionType versionType ) throws MetadataVersionException, BadRequestException
    {
        MetadataVersion versionToReturn = null;
        boolean enabled = isMetadataVersioningEnabled();

        try
        {
            if ( !enabled )
            {
                throw new BadRequestException( "Metadata versioning is not enabled for this instance." );
            }

            synchronized ( versionService )
            {
                versionService.saveVersion( versionType );
                versionToReturn = versionService.getCurrentVersion();
                return versionToReturn;
            }

        }
        catch ( MetadataVersionServiceException ex )
        {
            throw new MetadataVersionException( "Unable to create version in system. " + ex.getMessage() );
        }
    }

    //endpoint to download metadata
    @PreAuthorize( "hasRole('ALL') or hasRole('F_METADATA_MANAGE')" )
    @RequestMapping( value = MetadataVersionSchemaDescriptor.API_ENDPOINT + "/{versionName}/data", method = RequestMethod.GET, produces = "application/json" )
    public @ResponseBody String downloadVersion( @PathVariable( "versionName" ) String versionName ) throws MetadataVersionException, BadRequestException
    {
        boolean enabled = isMetadataVersioningEnabled();

        try
        {
            if ( !enabled )
            {
                throw new BadRequestException( "Metadata versioning is not enabled for this instance." );
            }

            String versionData = versionService.getVersionData( versionName );

            if ( versionData == null )
            {
                throw new MetadataVersionException( "No metadata version snapshot found for the given version " + versionName );
            }
            return versionData;
        }
        catch ( MetadataVersionServiceException ex )
        {
            throw new MetadataVersionException( "Unable to download version from system: " + versionName + ex.getMessage() );
        }
    }

    //endpoint to download metadata in gzip format
    @PreAuthorize( "hasRole('ALL') or hasRole('F_METADATA_MANAGE')" )
    @RequestMapping( value = MetadataVersionSchemaDescriptor.API_ENDPOINT + "/{versionName}/data.gz", method = RequestMethod.GET, produces = "*/*" )
    public void downloadGZipVersion( @PathVariable( "versionName" ) String versionName, HttpServletResponse response )
        throws MetadataVersionException, IOException, BadRequestException
    {
        boolean enabled = isMetadataVersioningEnabled();

        try
        {
            if ( !enabled )
            {
                throw new BadRequestException( "Metadata versioning is not enabled for this instance." );
            }

            contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_GZIP, CacheStrategy.NO_CACHE, "metadata.json.gz", true );
            response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );
            String versionData = versionService.getVersionData( versionName );

            if ( versionData == null )
            {
                throw new MetadataVersionException( "No metadata version snapshot found for the given version " + versionName );
            }

            GZIPOutputStream gos = new GZIPOutputStream( response.getOutputStream() );
            gos.write( versionData.getBytes( StandardCharsets.UTF_8 ) );
            gos.close();
        }
        catch ( MetadataVersionServiceException ex )
        {
            throw new MetadataVersionException( "Unable to download version from system: " + versionName + ex.getMessage() );
        }
    }


    //----------------------------------------------------------------------------------------
    // Private Methods
    //----------------------------------------------------------------------------------------

    private boolean isMetadataVersioningEnabled()
    {
        Boolean setting = (Boolean) systemSettingManager.getSystemSetting( SettingKey.METADATAVERSION_ENABLED );
        return setting.booleanValue();
    }
}
