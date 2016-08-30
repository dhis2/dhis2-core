package org.hisp.dhis.dxf2.metadata;

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

import com.google.common.base.MoreObjects;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.preheat.PreheatMode;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.user.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class MetadataImportParams
{
    private User user;

    private ObjectBundleMode importMode = ObjectBundleMode.COMMIT;

    private PreheatIdentifier identifier = PreheatIdentifier.UID;

    private PreheatMode preheatMode = PreheatMode.REFERENCE;

    private ImportStrategy importStrategy = ImportStrategy.CREATE_AND_UPDATE;

    private AtomicMode atomicMode = AtomicMode.ALL;

    private MergeMode mergeMode = MergeMode.REPLACE;

    private FlushMode flushMode = FlushMode.AUTO;

    private boolean skipSharing;

    private boolean skipValidation;

    private TaskId taskId;

    private Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects = new HashMap<>();

    public MetadataImportParams()
    {
    }

    public MetadataImportParams( List<? extends IdentifiableObject> objects )
    {
        addObjects( objects );
    }

    public String getUsername()
    {
        return user != null ? user.getUsername() : "system-process";
    }

    public User getUser()
    {
        return user;
    }

    public void setUser( User user )
    {
        this.user = user;
    }

    public ObjectBundleMode getImportMode()
    {
        return importMode;
    }

    public void setImportMode( ObjectBundleMode importMode )
    {
        this.importMode = importMode;
    }

    public PreheatIdentifier getIdentifier()
    {
        return identifier;
    }

    public void setIdentifier( PreheatIdentifier identifier )
    {
        this.identifier = identifier;
    }

    public PreheatMode getPreheatMode()
    {
        return preheatMode;
    }

    public void setPreheatMode( PreheatMode preheatMode )
    {
        this.preheatMode = preheatMode;
    }

    public ImportStrategy getImportStrategy()
    {
        return importStrategy;
    }

    public void setImportStrategy( ImportStrategy importStrategy )
    {
        this.importStrategy = importStrategy;
    }

    public AtomicMode getAtomicMode()
    {
        return atomicMode;
    }

    public void setAtomicMode( AtomicMode atomicMode )
    {
        this.atomicMode = atomicMode;
    }

    public MergeMode getMergeMode()
    {
        return mergeMode;
    }

    public void setMergeMode( MergeMode mergeMode )
    {
        this.mergeMode = mergeMode;
    }

    public FlushMode getFlushMode()
    {
        return flushMode;
    }

    public void setFlushMode( FlushMode flushMode )
    {
        this.flushMode = flushMode;
    }

    public boolean isSkipSharing()
    {
        return skipSharing;
    }

    public void setSkipSharing( boolean skipSharing )
    {
        this.skipSharing = skipSharing;
    }

    public boolean isSkipValidation()
    {
        return skipValidation;
    }

    public void setSkipValidation( boolean skipValidation )
    {
        this.skipValidation = skipValidation;
    }

    public TaskId getTaskId()
    {
        return taskId;
    }

    public void setTaskId( TaskId taskId )
    {
        this.taskId = taskId;
    }

    public boolean hasTaskId()
    {
        return taskId != null;
    }

    public Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> getObjects()
    {
        return objects;
    }

    public void setObjects( Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects )
    {
        this.objects = objects;
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

        Class<? extends IdentifiableObject> klass = object.getClass();

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
            Object value = ReflectionUtils.invokeGetterMethod( schema.getPlural(), metadata );

            if ( value != null )
            {
                if ( Collection.class.isAssignableFrom( value.getClass() ) && schema.isIdentifiableObject() )
                {
                    List<IdentifiableObject> objects = new ArrayList<>( (Collection<IdentifiableObject>) value );

                    if ( !objects.isEmpty() )
                    {
                        objectMap.put( (Class<? extends IdentifiableObject>) schema.getKlass(), objects );
                    }
                }
            }
        }

        setObjects( objectMap );
        return this;
    }

    public ObjectBundleParams toObjectBundleParams()
    {
        ObjectBundleParams params = new ObjectBundleParams();
        params.setUser( user );
        params.setSkipSharing( skipSharing );
        params.setSkipValidation( skipValidation );
        params.setTaskId( taskId );
        params.setImportStrategy( importStrategy );
        params.setAtomicMode( atomicMode );
        params.setObjects( objects );
        params.setPreheatIdentifier( identifier );
        params.setPreheatMode( preheatMode );
        params.setObjectBundleMode( importMode );
        params.setMergeMode( mergeMode );
        params.setFlushMode( flushMode );

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
