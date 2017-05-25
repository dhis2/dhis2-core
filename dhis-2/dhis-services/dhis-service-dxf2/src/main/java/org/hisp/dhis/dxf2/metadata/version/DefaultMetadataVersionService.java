package org.hisp.dhis.dxf2.metadata.version;

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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.dxf2.common.HashCodeGenerator;
import org.hisp.dhis.dxf2.metadata.systemsettings.MetadataSystemSettingService;
import org.hisp.dhis.dxf2.metadata.version.exception.MetadataVersionServiceException;
import org.hisp.dhis.dxf2.metadata.MetadataExportParams;
import org.hisp.dhis.dxf2.metadata.MetadataExportService;
import org.hisp.dhis.keyjsonvalue.KeyJsonValue;
import org.hisp.dhis.keyjsonvalue.KeyJsonValueService;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionService;
import org.hisp.dhis.metadata.version.MetadataVersionStore;
import org.hisp.dhis.metadata.version.VersionType;
import org.hisp.dhis.node.NodeService;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.system.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service implementation for the MetadataVersionService.
 *
 * @author aamerm
 */
@Transactional
public class
DefaultMetadataVersionService
    implements MetadataVersionService
{
    private static final Log log = LogFactory.getLog( DefaultMetadataVersionService.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------
    
    @Autowired
    private MetadataVersionStore versionStore;

    @Autowired
    private MetadataExportService metadataExportService;

    @Autowired
    private KeyJsonValueService keyJsonValueService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private MetadataSystemSettingService metadataSystemSettingService;

    // -------------------------------------------------------------------------
    // MetadataVersionService implementation
    // -------------------------------------------------------------------------

    @Override
    public int addVersion( MetadataVersion version )
    {
        versionStore.save( version );

        return version.getId();
    }

    @Override
    public void updateVersion( MetadataVersion version )
    {
        versionStore.update( version );
    }

    @Override
    public void updateVersionName( int id, String name )
    {
        MetadataVersion version = getVersionById( id );

        if ( version != null )
        {
            version.setName( name );
            updateVersion( version );
        }
    }

    @Override
    public void deleteVersion( MetadataVersion version )
    {
        versionStore.delete( version );
    }

    @Override
    public MetadataVersion getVersionById( int id )
    {
        return versionStore.getVersionByKey( id );
    }

    @Override
    public MetadataVersion getVersionByName( String versionName )
    {
        return versionStore.getVersionByName( versionName );
    }

    @Override
    public List<MetadataVersion> getAllVersions()
    {
        return versionStore.getAll();
    }

    @Override
    public MetadataVersion getCurrentVersion()
    {
        try
        {
            return versionStore.getCurrentVersion();
        }
        catch ( Exception ex ) // Will have to catch Exception, as we want to throw a deterministic exception from this layer
        {
            log.error( ex.getMessage(), ex );
            throw new MetadataVersionServiceException( ex.getMessage(), ex );
        }
    }

    @Override
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
    public List<MetadataVersion> getAllVersionsInBetween( Date startDate, Date endDate )
    {
        return versionStore.getAllVersionsInBetween( startDate, endDate );
    }

    @Override
    public Date getCreatedDate( String versionName )
    {
        MetadataVersion version = getVersionByName( versionName );
        return (version == null ? null : version.getCreated());
    }

    /**
     * This method is taking care of 3 steps:
     * 1. Generating a metadata snapshot (using the ExportService)
     * 2. Saving that snapshot to the DataStore
     * 3. Creating the actual MetadataVersion entry.
     */
    @Override
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

        //1. Get export of metadata
        ByteArrayOutputStream os = getMetadataExport( minDate );

        //2. Save the metadata snapshot in DHIS Data Store
        String value = getBodyAsString( StandardCharsets.UTF_8, os );
        createMetadataVersionInDataStore( versionName, value );

        //3. Create an entry for the MetadataVersion
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
    public String getVersionData( String versionName )
    {
        KeyJsonValue keyJsonValue = keyJsonValueService.getKeyJsonValue( MetadataVersionService.METADATASTORE, versionName );
        return (keyJsonValue != null) ? keyJsonValue.getValue() : null;
    }

    @Override
    public RootNode getMetadataVersionsAsNode( List<MetadataVersion> versions )
    {
        RootNode rootNode = NodeUtils.createRootNode( "metadataversions" );
        CollectionNode collectionNode = new CollectionNode( "metadataversions", true );
        rootNode.addChild( collectionNode );

        for ( MetadataVersion version : versions )
        {
            ComplexNode complexNode = new ComplexNode( "" );
            complexNode.addChild( new SimpleNode( "name", version.getName() ) );
            complexNode.addChild( new SimpleNode( "type", version.getType() ) );
            complexNode.addChild( new SimpleNode( "created", version.getCreated() ) );
            complexNode.addChild( new SimpleNode( "id", version.getUid() ) );
            complexNode.addChild( new SimpleNode( "importdate", version.getImportDate() ) );
            complexNode.addChild( new SimpleNode( "hashCode", version.getHashCode() ) );

            collectionNode.addChild( complexNode );
        }

        return rootNode;
    }

    @Override
    public void createMetadataVersionInDataStore( String versionName, String versionSnapshot )
    {
        if ( StringUtils.isEmpty( versionSnapshot ) )
        {
            throw new MetadataVersionServiceException( "The Metadata Snapshot is null while trying to create a Metadata Version entry in DataStore." );
        }

        KeyJsonValue keyJsonValue = new KeyJsonValue();
        keyJsonValue.setKey( versionName );
        keyJsonValue.setNamespace( MetadataVersionService.METADATASTORE );
        keyJsonValue.setValue( versionSnapshot );

        try
        {
            keyJsonValueService.addKeyJsonValue( keyJsonValue );

        }
        catch ( Exception ex )
        {
            String message = "Exception occurred while saving the Metadata snapshot in Data Store" + ex.getMessage();
            log.error( message, ex );
            throw new MetadataVersionServiceException( message, ex );
        }
    }

    @Override
    public void deleteMetadataVersionInDataStore( String nameSpaceKey )
    {
        KeyJsonValue keyJsonValue = keyJsonValueService.getKeyJsonValue( MetadataVersionService.METADATASTORE, nameSpaceKey );

        try
        {
            keyJsonValueService.deleteKeyJsonValue( keyJsonValue );
        }
        catch ( Exception ex )
        {
            String message = "Exception occurred while trying to delete the metadata snapshot in Data Store" + ex.getMessage();
            log.error( message, ex );
            throw new MetadataVersionServiceException( message, ex );
        }
    }

    @Override
    public boolean isMetadataPassingIntegrity( MetadataVersion version, String versionSnapshot )
    {
        String metadataVersionHashCode = null;

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

    //--------------------------------------------------------------------------
    // Private methods
    //--------------------------------------------------------------------------

    /**
     * Generates the metadata export based on the created date of the current version.
     */
    private ByteArrayOutputStream getMetadataExport( Date minDate )
    {
        ByteArrayOutputStream os = null;

        try
        {
            MetadataExportParams exportParams = new MetadataExportParams();

            if ( minDate != null )
            {
                List<String> defaultFilterList = new ArrayList<String>();
                defaultFilterList.add( "lastUpdated:gte:" + DateUtils.getLongGmtDateString( minDate ) );
                exportParams.setDefaultFilter( defaultFilterList );
                metadataExportService.validate( exportParams );
            }

            os = new ByteArrayOutputStream( 1024 );
            RootNode metadata = metadataExportService.getMetadataAsNode( exportParams );
            nodeService.serialize( metadata, "application/json", os );
        }
        catch ( Exception ex ) //We have to catch the "Exception" object as no specific exception on the contract.
        {
            String message = "Exception occurred while exporting metadata for capturing a metadata version" + ex.getMessage();
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
               log.error("Exception occurred while trying to convert ByteArray to String. ", ex );
            }
        }

        return null;
    }
}
