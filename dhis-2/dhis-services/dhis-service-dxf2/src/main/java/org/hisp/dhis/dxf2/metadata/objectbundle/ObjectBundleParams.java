package org.hisp.dhis.dxf2.metadata.objectbundle;

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
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.FlushMode;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.preheat.PreheatMode;
import org.hisp.dhis.preheat.PreheatParams;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.user.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ObjectBundleParams
{
    private User user;

    private ObjectBundleMode objectBundleMode = ObjectBundleMode.COMMIT;

    private PreheatIdentifier preheatIdentifier = PreheatIdentifier.UID;

    private PreheatMode preheatMode = PreheatMode.REFERENCE;

    private ImportStrategy importStrategy = ImportStrategy.CREATE_AND_UPDATE;

    private AtomicMode atomicMode = AtomicMode.ALL;

    private MergeMode mergeMode = MergeMode.REPLACE;

    private FlushMode flushMode = FlushMode.AUTO;

    private Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects = new HashMap<>();

    private boolean skipSharing;

    private boolean skipValidation;

    private TaskId taskId;

    public ObjectBundleParams()
    {

    }

    public User getUser()
    {
        return user;
    }

    public void setUser( User user )
    {
        this.user = user;
    }

    public ObjectBundleMode getObjectBundleMode()
    {
        return objectBundleMode;
    }

    public ObjectBundleParams setObjectBundleMode( ObjectBundleMode objectBundleMode )
    {
        this.objectBundleMode = objectBundleMode;
        return this;
    }

    public PreheatIdentifier getPreheatIdentifier()
    {
        return preheatIdentifier;
    }

    public ObjectBundleParams setPreheatIdentifier( PreheatIdentifier preheatIdentifier )
    {
        this.preheatIdentifier = preheatIdentifier;
        return this;
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

    public boolean haveTaskId()
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

    public void addObject( Class<? extends IdentifiableObject> klass, IdentifiableObject object )
    {
        if ( object == null )
        {
            return;
        }

        if ( !objects.containsKey( klass ) )
        {
            objects.put( klass, new ArrayList<>() );
        }

        objects.get( klass ).add( object );
    }

    public void addObject( IdentifiableObject object )
    {
        if ( object == null )
        {
            return;
        }

        if ( !objects.containsKey( object.getClass() ) )
        {
            objects.put( object.getClass(), new ArrayList<>() );
        }

        objects.get( object.getClass() ).add( object );
    }

    public PreheatParams getPreheatParams()
    {
        PreheatParams params = new PreheatParams();
        params.setPreheatIdentifier( preheatIdentifier );
        params.setPreheatMode( preheatMode );

        return params;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "user", user )
            .add( "objectBundleMode", objectBundleMode )
            .add( "preheatIdentifier", preheatIdentifier )
            .add( "preheatMode", preheatMode )
            .add( "importStrategy", importStrategy )
            .add( "mergeMode", mergeMode )
            .toString();
    }
}
