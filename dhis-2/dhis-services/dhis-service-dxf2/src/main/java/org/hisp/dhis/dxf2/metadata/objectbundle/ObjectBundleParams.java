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
package org.hisp.dhis.dxf2.metadata.objectbundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.FlushMode;
import org.hisp.dhis.dxf2.metadata.UserOverrideMode;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.preheat.PreheatMode;
import org.hisp.dhis.preheat.PreheatParams;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.user.User;

import com.google.common.base.MoreObjects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ObjectBundleParams
{
    private User user;

    private UserOverrideMode userOverrideMode = UserOverrideMode.NONE;

    private User overrideUser;

    private ObjectBundleMode objectBundleMode = ObjectBundleMode.COMMIT;

    private PreheatIdentifier preheatIdentifier = PreheatIdentifier.UID;

    private PreheatMode preheatMode = PreheatMode.REFERENCE;

    private ImportStrategy importStrategy = ImportStrategy.CREATE_AND_UPDATE;

    private AtomicMode atomicMode = AtomicMode.ALL;

    private MergeMode mergeMode = MergeMode.REPLACE;

    private FlushMode flushMode = FlushMode.AUTO;

    private ImportReportMode importReportMode = ImportReportMode.ERRORS;

    private Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects = new HashMap<>();

    private boolean skipSharing;

    private boolean skipTranslation;

    private boolean skipValidation;

    private boolean metadataSyncImport;

    private JobConfiguration jobId;

    public ObjectBundleParams()
    {

    }

    public User getUser()
    {
        return user;
    }

    public ObjectBundleParams setUser( User user )
    {
        this.user = user;
        return this;
    }

    public UserOverrideMode getUserOverrideMode()
    {
        return userOverrideMode;
    }

    public ObjectBundleParams setUserOverrideMode( UserOverrideMode userOverrideMode )
    {
        this.userOverrideMode = userOverrideMode;
        return this;
    }

    public User getOverrideUser()
    {
        return overrideUser;
    }

    public ObjectBundleParams setOverrideUser( User overrideUser )
    {
        this.overrideUser = overrideUser;
        return this;
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

    public ObjectBundleParams setPreheatMode( PreheatMode preheatMode )
    {
        this.preheatMode = preheatMode;
        return this;
    }

    public ImportStrategy getImportStrategy()
    {
        return importStrategy;
    }

    public ObjectBundleParams setImportStrategy( ImportStrategy importStrategy )
    {
        this.importStrategy = importStrategy;
        return this;
    }

    public AtomicMode getAtomicMode()
    {
        return atomicMode;
    }

    public ObjectBundleParams setAtomicMode( AtomicMode atomicMode )
    {
        this.atomicMode = atomicMode;
        return this;
    }

    public MergeMode getMergeMode()
    {
        return mergeMode;
    }

    public ObjectBundleParams setMergeMode( MergeMode mergeMode )
    {
        this.mergeMode = mergeMode;
        return this;
    }

    public FlushMode getFlushMode()
    {
        return flushMode;
    }

    public ObjectBundleParams setFlushMode( FlushMode flushMode )
    {
        this.flushMode = flushMode;
        return this;
    }

    public ImportReportMode getImportReportMode()
    {
        return importReportMode;
    }

    public ObjectBundleParams setImportReportMode( ImportReportMode importReportMode )
    {
        this.importReportMode = importReportMode;
        return this;
    }

    public boolean isSkipSharing()
    {
        return skipSharing;
    }

    public ObjectBundleParams setSkipSharing( boolean skipSharing )
    {
        this.skipSharing = skipSharing;
        return this;
    }

    public boolean isSkipTranslation()
    {
        return skipTranslation;
    }

    public ObjectBundleParams setSkipTranslation( boolean skipTranslation )
    {
        this.skipTranslation = skipTranslation;
        return this;
    }

    public boolean isSkipValidation()
    {
        return skipValidation;
    }

    public ObjectBundleParams setSkipValidation( boolean skipValidation )
    {
        this.skipValidation = skipValidation;
        return this;
    }

    public boolean isMetadataSyncImport()
    {
        return metadataSyncImport;
    }

    public void setMetadataSyncImport( boolean metadataSyncImport )
    {
        this.metadataSyncImport = metadataSyncImport;
    }

    public JobConfiguration getJobId()
    {
        return jobId;
    }

    public ObjectBundleParams setJobId( JobConfiguration jobId )
    {
        this.jobId = jobId;
        return this;
    }

    public boolean haveJobId()
    {
        return jobId != null;
    }

    public Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> getObjects()
    {
        return objects;
    }

    public ObjectBundleParams setObjects( Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects )
    {
        this.objects = objects;
        return this;
    }

    public ObjectBundleParams addObject( Class<? extends IdentifiableObject> klass, IdentifiableObject object )
    {
        if ( object == null )
        {
            return this;
        }

        if ( !objects.containsKey( klass ) )
        {
            objects.put( klass, new ArrayList<>() );
        }

        objects.get( klass ).add( object );

        return this;
    }

    @SuppressWarnings( { "unchecked" } )
    public ObjectBundleParams addObject( IdentifiableObject object )
    {
        if ( object == null )
        {
            return this;
        }

        Class<? extends IdentifiableObject> realClass = HibernateProxyUtils.getRealClass( object );

        objects.computeIfAbsent( realClass, k -> new ArrayList<>() ).add( object );

        return this;
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
