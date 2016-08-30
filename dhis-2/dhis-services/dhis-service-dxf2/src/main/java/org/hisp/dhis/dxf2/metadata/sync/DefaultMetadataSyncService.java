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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.metadata.version.MetadataVersionDelegate;
import org.hisp.dhis.dxf2.metadata.version.exception.MetadataVersionServiceException;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionService;
import org.hisp.dhis.metadata.version.VersionType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * Performs the metadata sync related tasks in service layer
 *
 * @author vanyas
 */
public class DefaultMetadataSyncService
    implements MetadataSyncService
{
    private static final Log log = LogFactory.getLog( DefaultMetadataSyncService.class );

    @Autowired
    MetadataVersionDelegate metadataVersionDelegate;

    @Autowired
    private MetadataVersionService metadataVersionService;

    @Autowired
    private MetadataSyncImportHandler metadataSyncImportHandler;

    @Override
    public MetadataSyncParams getParamsFromMap( Map<String, List<String>> parameters )
    {
        List<String> versionName = getVersionsFromParams( parameters );
        MetadataSyncParams syncParams = new MetadataSyncParams();
        syncParams.setImportParams( new MetadataImportParams() );
        String versionNameStr = versionName.get( 0 );

        if ( StringUtils.isNotEmpty( versionNameStr ) )
        {
            MetadataVersion version;

            try
            {
                version = metadataVersionDelegate.getRemoteMetadataVersion( versionNameStr );
            }
            catch ( MetadataVersionServiceException e )
            {
                throw new MetadataSyncServiceException( e.getMessage(), e );
            }

            if ( version == null )
            {
                throw new MetadataSyncServiceException(
                    "The MetadataVersion could not be fetched from the remote server for the versionName: " +
                        versionNameStr );
            }

            syncParams.setVersion( version );
        }

        syncParams.setParameters( parameters );

        return syncParams;
    }

    @Override
    public MetadataSyncSummary doMetadataSync( MetadataSyncParams syncParams )
        throws MetadataSyncServiceException
    {
        MetadataVersion version = getMetadataVersion( syncParams );

        if ( VersionType.BEST_EFFORT.equals( version.getType() ) )
        {
            syncParams.getImportParams().setAtomicMode( AtomicMode.NONE );
        }

        String metadataVersionSnapshot = getLocalVersionSnapshot( version );

        if ( metadataVersionSnapshot == null )
        {
            try
            {
                metadataVersionSnapshot = metadataVersionDelegate.downloadMetadataVersion( version );
            }
            catch ( MetadataVersionServiceException e )
            {
                throw new MetadataSyncServiceException( e.getMessage(), e );
            }

            if ( metadataVersionSnapshot == null )
            {
                throw new MetadataSyncServiceException( "Metadata snapshot can't be null." );
            }

            if ( ( metadataVersionService.isMetadataPassingIntegrity( version, metadataVersionSnapshot ) ) )
            {
                metadataVersionService.createMetadataVersionInDataStore( version.getName(), metadataVersionSnapshot );
                log.info( "Downloaded the metadata snapshot from remote and saved in Data Store for the version: " + version );
            }
            else
            {
                throw new MetadataSyncServiceException( "Metadata snapshot is corrupted. Not saving it locally" );
            }
        }

        if ( !(metadataVersionService.isMetadataPassingIntegrity( version, metadataVersionSnapshot ) ) )
        {
            metadataVersionService.deleteMetadataVersionInDataStore( version.getName() );
            throw new MetadataSyncServiceException(
                "Metadata snapshot is corrupted. Deleting from locally the version: " + version );
        }

        MetadataSyncSummary metadataSyncSummary = metadataSyncImportHandler.importMetadata( syncParams, metadataVersionSnapshot );
        log.info( "Metadata Sync Summary: " + metadataSyncSummary );
        return metadataSyncSummary;
    }

    //----------------------------------------------------------------------------------------
    // Private Methods
    //----------------------------------------------------------------------------------------

    private String getLocalVersionSnapshot( MetadataVersion version )
    {
        String metadataVersionSnapshot = metadataVersionService.getVersionData( version.getName() );

        if ( StringUtils.isNotEmpty( metadataVersionSnapshot ) )
        {
            log.info( "Rendering the MetadataVersion from local DataStore" );
            return metadataVersionSnapshot;
        }

        return null;
    }

    private List<String> getVersionsFromParams( Map<String, List<String>> parameters )
    {
        if ( parameters == null )
        {
            throw new MetadataSyncServiceException( "Missing required parameter: 'versionName'" );
        }

        List<String> versionName = parameters.get( "versionName" );

        if ( versionName == null || versionName.size() == 0 )
        {
            throw new MetadataSyncServiceException( "Missing required parameter: 'versionName'" );
        }

        return versionName;
    }

    private MetadataVersion getMetadataVersion( MetadataSyncParams syncParams )
    {
        if ( syncParams == null )
        {
            throw new MetadataSyncServiceException( "MetadataSyncParams cant be null" );
        }

        MetadataVersion version = syncParams.getVersion();

        if ( version == null )
        {
            throw new MetadataSyncServiceException(
                "MetadataVersion for the Sync cant be null. The ClassListMap could not be constructed." );
        }

        return version;
    }
}
