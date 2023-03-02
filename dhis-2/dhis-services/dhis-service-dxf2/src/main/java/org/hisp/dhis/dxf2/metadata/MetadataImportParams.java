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
package org.hisp.dhis.dxf2.metadata;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.csv.CsvImportClass;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.metadatapackage.MetadataPackage;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.preheat.PreheatMode;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.user.User;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Shared
@JacksonXmlRootElement( localName = "metadataImportParams", namespace = DxfNamespaces.DXF_2_0 )
public class MetadataImportParams
{
    /**
     * User to use for import job (important for threaded imports).
     */
    private User user;

    /**
     * How should the user property be handled, by default it is left as is. You
     * can override this to use current user, or a selected user instead (not
     * yet supported).
     */
    private UserOverrideMode userOverrideMode = UserOverrideMode.NONE;

    /**
     * User to use for override, can be current or a selected user.
     */
    private User overrideUser;

    /**
     * Should import be imported or just validated.
     */
    private ObjectBundleMode importMode = ObjectBundleMode.COMMIT;

    /**
     * What identifiers to match on.
     */
    private PreheatIdentifier identifier = PreheatIdentifier.UID;

    /**
     * Preheat mode to use (default is REFERENCE and should not be changed).
     */
    private PreheatMode preheatMode = PreheatMode.REFERENCE;

    /**
     * Sets import strategy (create, update, etc).
     */
    private ImportStrategy importStrategy = ImportStrategy.CREATE_AND_UPDATE;

    /**
     * Should import be treated as a atomic import (all or nothing).
     */
    private AtomicMode atomicMode = AtomicMode.ALL;

    /**
     * Merge mode for object updates (default is REPLACE).
     */
    private MergeMode mergeMode = MergeMode.REPLACE;

    /**
     * Flush for every object or per type.
     */
    private FlushMode flushMode = FlushMode.AUTO;

    /**
     * Decides how much to report back to the user (errors only, or a more full
     * per object report).
     */
    private ImportReportMode importReportMode = ImportReportMode.ERRORS;

    /**
     * Should sharing be considered when importing objects.
     */
    private boolean skipSharing;

    /**
     * Should translation be considered when importing objects.
     */
    private boolean skipTranslation;

    /**
     * Skip validation of objects (not recommended).
     */
    private boolean skipValidation;

    /**
     * Is this import request from Metadata Sync service.
     */
    private boolean metadataSyncImport;

    /**
     * Name of file that was used for import (if available).
     */
    private String filename;

    /**
     * Temporary file containing the data to import.
     */
    private File tempFile;

    /**
     * Metadata Class name for importing using CSV
     */
    private CsvImportClass csvImportClass;

    /**
     * Specify whether the first row is header in CSV import
     */
    private boolean firstRowIsHeader = true;

    private MetadataPackage metadataPackage;

    /**
     * Job id to use for threaded imports.
     */
    private JobConfiguration id;

    /**
     * Objects to import.
     */
    private Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects = new HashMap<>();

    public MetadataImportParams()
    {
    }

    public MetadataImportParams( List<? extends IdentifiableObject> objects )
    {
        addObjects( objects );
    }

    @OpenApi.Ignore
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getUsername()
    {
        return user != null ? user.getUsername() : "system-process";
    }

    public User getUser()
    {
        return user;
    }

    public MetadataImportParams setUser( User user )
    {
        this.user = user;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public UserOverrideMode getUserOverrideMode()
    {
        return userOverrideMode;
    }

    public MetadataImportParams setUserOverrideMode( UserOverrideMode userOverrideMode )
    {
        this.userOverrideMode = userOverrideMode;
        return this;
    }

    public User getOverrideUser()
    {
        return overrideUser;
    }

    public void setOverrideUser( User overrideUser )
    {
        this.overrideUser = overrideUser;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ObjectBundleMode getImportMode()
    {
        return importMode;
    }

    public MetadataImportParams setImportMode( ObjectBundleMode importMode )
    {
        this.importMode = importMode;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public PreheatIdentifier getIdentifier()
    {
        return identifier;
    }

    public MetadataImportParams setIdentifier( PreheatIdentifier identifier )
    {
        this.identifier = identifier;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public PreheatMode getPreheatMode()
    {
        return preheatMode;
    }

    public MetadataImportParams setPreheatMode( PreheatMode preheatMode )
    {
        this.preheatMode = preheatMode;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ImportStrategy getImportStrategy()
    {
        return importStrategy;
    }

    public MetadataImportParams setImportStrategy( ImportStrategy importStrategy )
    {
        this.importStrategy = importStrategy;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public AtomicMode getAtomicMode()
    {
        return atomicMode;
    }

    public MetadataImportParams setAtomicMode( AtomicMode atomicMode )
    {
        this.atomicMode = atomicMode;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public MergeMode getMergeMode()
    {
        return mergeMode;
    }

    public MetadataImportParams setMergeMode( MergeMode mergeMode )
    {
        this.mergeMode = mergeMode;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public FlushMode getFlushMode()
    {
        return flushMode;
    }

    public MetadataImportParams setFlushMode( FlushMode flushMode )
    {
        this.flushMode = flushMode;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ImportReportMode getImportReportMode()
    {
        return importReportMode;
    }

    public MetadataImportParams setImportReportMode( ImportReportMode importReportMode )
    {
        this.importReportMode = importReportMode;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isSkipSharing()
    {
        return skipSharing;
    }

    public MetadataImportParams setSkipSharing( boolean skipSharing )
    {
        this.skipSharing = skipSharing;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isSkipTranslation()
    {
        return skipTranslation;
    }

    public MetadataImportParams setSkipTranslation( boolean skipTranslation )
    {
        this.skipTranslation = skipTranslation;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isMetadataSyncImport()
    {
        return metadataSyncImport;
    }

    public void setMetadataSyncImport( boolean metadataSyncImport )
    {
        this.metadataSyncImport = metadataSyncImport;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isSkipValidation()
    {
        return skipValidation;
    }

    public MetadataImportParams setSkipValidation( boolean skipValidation )
    {
        this.skipValidation = skipValidation;
        return this;
    }

    @OpenApi.Ignore
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getFilename()
    {
        return filename;
    }

    public MetadataImportParams setFilename( String filename )
    {
        this.filename = filename;
        return this;
    }

    @OpenApi.Ignore
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public CsvImportClass getCsvImportClass()
    {
        return this.csvImportClass;
    }

    public void setCsvImportClass( CsvImportClass csvImportClass )
    {
        this.csvImportClass = csvImportClass;
    }

    public JobConfiguration getId()
    {
        return id;
    }

    public MetadataImportParams setId( JobConfiguration id )
    {
        this.id = id;
        return this;
    }

    @OpenApi.Ignore
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isFirstRowIsHeader()
    {
        return firstRowIsHeader;
    }

    public void setFirstRowIsHeader( boolean firstRowIsHeader )
    {
        this.firstRowIsHeader = firstRowIsHeader;
    }

    @OpenApi.Ignore
    @JsonIgnore
    public MetadataPackage getMetadataPackage()
    {
        return metadataPackage;
    }

    public void setMetadataPackage( MetadataPackage metadataPackage )
    {
        this.metadataPackage = metadataPackage;
    }

    @OpenApi.Ignore
    @JsonIgnore
    public File getTempFile()
    {
        return tempFile;
    }

    public void setTempFile( File tempFile )
    {
        this.tempFile = tempFile;
    }

    public boolean hasJobId()
    {
        return id != null;
    }

    public Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> getObjects()
    {
        return objects;
    }

    public MetadataImportParams setObjects( Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects )
    {
        this.objects = objects;
        return this;
    }

    public List<Class<? extends IdentifiableObject>> getClasses()
    {
        return new ArrayList<>( objects.keySet() );
    }

    public List<? extends IdentifiableObject> getObjects( Class<? extends IdentifiableObject> klass )
    {
        return objects.get( klass );
    }

    public MetadataImportParams addObject( IdentifiableObject object )
    {
        if ( object == null )
        {
            return this;
        }

        Class<? extends IdentifiableObject> klass = HibernateProxyUtils.getRealClass( object );

        if ( !objects.containsKey( klass ) )
        {
            objects.put( klass, new ArrayList<>() );
        }

        objects.get( klass ).add( klass.cast( object ) );

        return this;
    }

    public MetadataImportParams addObjects( List<? extends IdentifiableObject> objects )
    {
        objects.forEach( this::addObject );
        return this;
    }

    @SuppressWarnings( "unchecked" )
    public MetadataImportParams addMetadata( List<Schema> schemas, Metadata metadata )
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objectMap = new HashMap<>();

        for ( Schema schema : schemas )
        {
            if ( schema.isIdentifiableObject() )
            {
                Class<? extends IdentifiableObject> key = (Class<? extends IdentifiableObject>) schema.getKlass();
                List<? extends IdentifiableObject> schemaObjects = metadata.getValues( key );

                if ( !schemaObjects.isEmpty() )
                {
                    objectMap.put( key, new ArrayList<>( schemaObjects ) );
                }
            }
        }

        setObjects( objectMap );
        return this;
    }

    public boolean hasMetadataPackage()
    {
        return metadataPackage != null;
    }

    public ObjectBundleParams toObjectBundleParams()
    {
        ObjectBundleParams params = new ObjectBundleParams();
        params.setUser( user );
        params.setUserOverrideMode( userOverrideMode );
        params.setOverrideUser( overrideUser );
        params.setSkipSharing( skipSharing );
        params.setSkipTranslation( skipTranslation );
        params.setSkipValidation( skipValidation );
        params.setJobId( id );
        params.setImportStrategy( importStrategy );
        params.setAtomicMode( atomicMode );
        params.setObjects( objects );
        params.setPreheatIdentifier( identifier );
        params.setPreheatMode( preheatMode );
        params.setObjectBundleMode( importMode );
        params.setMergeMode( mergeMode );
        params.setFlushMode( flushMode );
        params.setImportReportMode( importReportMode );
        params.setMetadataSyncImport( metadataSyncImport );

        return params;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "user", user )
            .add( "importMode", importMode )
            .add( "identifier", identifier )
            .add( "preheatMode", preheatMode )
            .add( "importStrategy", importStrategy )
            .add( "mergeMode", mergeMode )
            .toString();
    }
}
