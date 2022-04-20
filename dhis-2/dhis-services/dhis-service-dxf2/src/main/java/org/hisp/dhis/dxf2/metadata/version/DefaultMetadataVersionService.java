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
package org.hisp.dhis.dxf2.metadata.version;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.datastore.DatastoreEntry;
import org.hisp.dhis.datastore.MetadataDatastoreService;
import org.hisp.dhis.dxf2.common.HashCodeGenerator;
import org.hisp.dhis.dxf2.metadata.MetadataExportParams;
import org.hisp.dhis.dxf2.metadata.MetadataExportService;
import org.hisp.dhis.dxf2.metadata.MetadataWrapper;
import org.hisp.dhis.dxf2.metadata.systemsettings.MetadataSystemSettingService;
import org.hisp.dhis.dxf2.metadata.version.exception.MetadataVersionServiceException;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionService;
import org.hisp.dhis.metadata.version.MetadataVersionStore;
import org.hisp.dhis.metadata.version.VersionType;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

/**
 * Service implementation for the {@link MetadataVersionService}.
 *
 * @author aamerm
 */
@Slf4j
@Service
@AllArgsConstructor
public class DefaultMetadataVersionService implements MetadataVersionService
{

    private final MetadataVersionStore versionStore;

    private final MetadataExportService metadataExportService;

    private final MetadataDatastoreService metaDataDatastoreService;

    private final MetadataSystemSettingService metadataSystemSettingService;

    private final RenderService renderService;

    // -------------------------------------------------------------------------
    // MetadataVersionService implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addVersion( MetadataVersion version )
    {
        versionStore.save( version );

        return version.getId();
    }

    @Override
    @Transactional
    public void updateVersion( MetadataVersion version )
    {
        versionStore.update( version );
    }

    @Override
    @Transactional
    public void updateVersionName( long id, String name )
    {
        MetadataVersion version = getVersionById( id );

        if ( version != null )
        {
            version.setName( name );
            updateVersion( version );
        }
    }

    @Override
    @Transactional
    public void deleteVersion( MetadataVersion version )
    {
        versionStore.delete( version );
    }

    @Override
    @Transactional( readOnly = true )
    public MetadataVersion getVersionById( long id )
    {
        return versionStore.getVersionByKey( id );
    }

    @Override
    @Transactional( readOnly = true )
    public MetadataVersion getVersionByName( String versionName )
    {
        return versionStore.getVersionByName( versionName );
    }

    @Override
    @Transactional( readOnly = true )
    public List<MetadataVersion> getAllVersions()
    {
        return versionStore.getAll();
    }

    @Override
    @Transactional( readOnly = true )
    public MetadataVersion getCurrentVersion()
    {
        try
        {
            return versionStore.getCurrentVersion();
        }
        catch ( Exception ex ) // Will have to catch Exception, as we want to
                               // throw a deterministic exception from this
                               // layer
        {
            log.error( ex.getMessage(), ex );
            throw new MetadataVersionServiceException( ex.getMessage(), ex );
        }
    }

    @Override
    @Transactional( readOnly = true )
    public MetadataVersion getInitialVersion()
    {
        try
        {
            return versionStore.getInitialVersion();
        }
        catch ( Exception ex )
        {
            log.error( ex.getMessage(), ex );
            throw new MetadataVersionServiceException( ex.getMessage(), ex );
        }
    }

    @Override
    @Transactional( readOnly = true )
    public List<MetadataVersion> getAllVersionsInBetween( Date startDate, Date endDate )
    {
        return versionStore.getAllVersionsInBetween( startDate, endDate );
    }

    @Override
    @Transactional( readOnly = true )
    public Date getCreatedDate( String versionName )
    {
        MetadataVersion version = getVersionByName( versionName );
        return (version == null ? null : version.getCreated());
    }

    /**
     * This method is taking care of 3 steps: 1. Generating a metadata snapshot
     * (using the ExportService) 2. Saving that snapshot to the DataStore 3.
     * Creating the actual MetadataVersion entry.
     */
    @Override
    @Transactional
    public synchronized boolean saveVersion( VersionType versionType )
    {
        MetadataVersion currentVersion = getCurrentVersion();
        String versionName = MetadataVersionNameGenerator.getNextVersionName( currentVersion );

        Date minDate;

        if ( currentVersion == null )
        {
            minDate = null;
        }
        else
        {
            minDate = currentVersion.getCreated();
        }

        // 1. Get export of metadata
        ByteArrayOutputStream os = getMetadataExport( minDate );

        // 2. Save the metadata snapshot in DHIS Data Store
        String value = getBodyAsString( StandardCharsets.UTF_8, os );
        createMetadataVersionInDataStore( versionName, value );

        // 3. Create an entry for the MetadataVersion
        MetadataVersion version = new MetadataVersion();
        version.setName( versionName );
        version.setCreated( new Date() );
        version.setType( versionType );
        try
        {
            String hashCode = HashCodeGenerator.getHashCode( value );
            version.setHashCode( hashCode );
        }
        catch ( NoSuchAlgorithmException e )
        {
            String message = "Exception occurred while generating MetadataVersion HashCode " + e.getMessage();
            log.error( message, e );
            throw new MetadataVersionServiceException( message, e );
        }

        try
        {
            addVersion( version );
            metadataSystemSettingService.setSystemMetadataVersion( version.getName() );
        }
        catch ( Exception ex )
        {
            String message = "Exception occurred while saving a new MetadataVersion " + ex.getMessage();
            log.error( message, ex );
            throw new MetadataVersionServiceException( message, ex );
        }

        return true;
    }

    @Override
    @Transactional( readOnly = true )
    public String getVersionData( String versionName )
    {
        DatastoreEntry entry = metaDataDatastoreService.getMetaDataVersion( versionName );

        if ( entry != null )
        {
            try
            {
                return renderService.fromJson( entry.getValue(), MetadataWrapper.class ).getMetadata();
            }
            catch ( IOException e )
            {
                log.error( "Exception occurred while deserializing metadata.", e );
            }
        }

        return null;
    }

    @Override
    @Transactional
    public void createMetadataVersionInDataStore( String versionName, String versionSnapshot )
    {
        if ( StringUtils.isEmpty( versionSnapshot ) )
        {
            throw new MetadataVersionServiceException(
                "The Metadata Snapshot is null while trying to create a Metadata Version entry in DataStore." );
        }

        DatastoreEntry entry = new DatastoreEntry();
        entry.setKey( versionName );
        entry.setNamespace( MetadataDatastoreService.METADATA_STORE_NS );

        // MetadataWrapper is used to avoid Metadata keys reordering by jsonb
        // (jsonb does not preserve keys order)
        entry.setValue( renderService.toJsonAsString( new MetadataWrapper( versionSnapshot ) ) );

        try
        {
            metaDataDatastoreService.addMetaEntry( entry );

        }
        catch ( Exception ex )
        {
            String message = "Exception occurred while saving the Metadata snapshot in Data Store" + ex.getMessage();
            log.error( message, ex );
            throw new MetadataVersionServiceException( message, ex );
        }
    }

    @Override
    @Transactional
    public void deleteMetadataVersionInDataStore( String nameSpaceKey )
    {
        DatastoreEntry entry = metaDataDatastoreService.getMetaDataVersion( nameSpaceKey );

        try
        {
            metaDataDatastoreService.deleteMetaEntry( entry );
        }
        catch ( Exception ex )
        {
            String message = "Exception occurred while trying to delete the metadata snapshot in Data Store"
                + ex.getMessage();
            log.error( message, ex );
            throw new MetadataVersionServiceException( message, ex );
        }
    }

    @Override
    public boolean isMetadataPassingIntegrity( MetadataVersion version, String versionSnapshot )
    {
        String metadataVersionHashCode;

        if ( version == null || versionSnapshot == null )
        {
            throw new MetadataVersionServiceException( "Version/Version Snapshot can't be null" );
        }

        try
        {
            metadataVersionHashCode = HashCodeGenerator.getHashCode( versionSnapshot );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new MetadataVersionServiceException( "Algorithm to hash metadata is not found in the system", e );
        }

        return (metadataVersionHashCode.equals( version.getHashCode() ));
    }

    // --------------------------------------------------------------------------
    // Private methods
    // --------------------------------------------------------------------------

    /**
     * Generates the metadata export based on the created date of the current
     * version.
     */
    private ByteArrayOutputStream getMetadataExport( Date minDate )
    {
        ByteArrayOutputStream os;

        try
        {
            MetadataExportParams exportParams = new MetadataExportParams();

            if ( minDate != null )
            {
                List<String> defaultFilterList = new ArrayList<>();
                defaultFilterList.add( "lastUpdated:gte:" + DateUtils.getLongGmtDateString( minDate ) );
                exportParams.setDefaultFilter( defaultFilterList );
                exportParams.setDefaultFields( Lists.newArrayList( ":all" ) );
                metadataExportService.validate( exportParams );
            }

            os = new ByteArrayOutputStream( 1024 );
            ObjectNode metadata = metadataExportService.getMetadataAsObjectNode( exportParams );
            renderService.toJson( os, metadata );
        }
        catch ( Exception ex ) // We have to catch the "Exception" object as no
                               // specific exception on the contract.
        {
            String message = "Exception occurred while exporting metadata for capturing a metadata version"
                + ex.getMessage();
            log.error( message, ex );
            throw new MetadataVersionServiceException( message, ex );
        }

        return os;
    }

    private String getBodyAsString( Charset charset, ByteArrayOutputStream os )
    {
        if ( os != null )
        {

            byte[] bytes = os.toByteArray();

            try
            {
                return new String( bytes, charset.name() );
            }
            catch ( UnsupportedEncodingException ex )
            {
                log.error( "Exception occurred while trying to convert ByteArray to String. ", ex );
            }
        }

        return null;
    }
}
