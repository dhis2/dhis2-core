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

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.dxf2.metadata.objectbundle.EventReportCompatibilityGuard.handleDeprecationIfEventReport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.cache.HibernateCacheManager;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.metadata.FlushMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleCommitReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.preheat.PreheatParams;
import org.hisp.dhis.preheat.PreheatService;
import org.hisp.dhis.schema.MergeParams;
import org.hisp.dhis.schema.MergeService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Service( "org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService" )
@AllArgsConstructor
public class DefaultObjectBundleService implements ObjectBundleService
{
    private final CurrentUserService currentUserService;

    private final PreheatService preheatService;

    private final SchemaService schemaService;

    private final SessionFactory sessionFactory;

    private final IdentifiableObjectManager manager;

    private final DbmsManager dbmsManager;

    private final HibernateCacheManager cacheManager;

    private final Notifier notifier;

    private final MergeService mergeService;

    private final ObjectBundleHooks objectBundleHooks;

    @Override
    @Transactional( readOnly = true )
    public ObjectBundle create( ObjectBundleParams params )
    {
        PreheatParams preheatParams = params.getPreheatParams();

        if ( params.getUser() == null )
        {
            params.setUser( currentUserService.getCurrentUser() );
        }

        preheatParams.setUser( params.getUser() );
        preheatParams.setObjects( params.getObjects() );

        Preheat preheat = preheatService.preheat( preheatParams );

        ObjectBundle bundle = new ObjectBundle( params, preheat, params.getObjects() );
        bundle.setObjectBundleStatus( ObjectBundleStatus.CREATED );
        bundle.setObjectReferences( preheatService.collectObjectReferences( params.getObjects() ) );

        return bundle;
    }

    @Override
    @Transactional
    public ObjectBundleCommitReport commit( ObjectBundle bundle )
    {
        Map<Class<?>, TypeReport> typeReports = new HashMap<>();
        ObjectBundleCommitReport commitReport = new ObjectBundleCommitReport( typeReports );

        if ( ObjectBundleMode.VALIDATE == bundle.getObjectBundleMode() )
        {
            return commitReport; // skip if validate only
        }

        List<Class<? extends IdentifiableObject>> klasses = getSortedClasses( bundle );
        Session session = sessionFactory.getCurrentSession();

        List<ObjectBundleHook<?>> commitHooks = objectBundleHooks.getCommitHooks( klasses );
        commitHooks.forEach( hook -> hook.preCommit( bundle ) );

        for ( Class<? extends IdentifiableObject> klass : klasses )
        {
            commitObjectType( bundle, typeReports, session, klass );
        }

        if ( !bundle.getImportMode().isDelete() )
        {
            commitHooks.forEach( hook -> hook.postCommit( bundle ) );
        }

        dbmsManager.clearSession();
        cacheManager.clearCache();

        bundle.setObjectBundleStatus( ObjectBundleStatus.COMMITTED );

        return commitReport;
    }

    private <T extends IdentifiableObject> void commitObjectType( ObjectBundle bundle,
        Map<Class<?>, TypeReport> typeReports, Session session, Class<T> klass )
    {
        List<T> nonPersistedObjects = bundle.getObjects( klass, false );
        List<T> persistedObjects = bundle.getObjects( klass, true );

        List<ObjectBundleHook<? super T>> importHooks = objectBundleHooks.getTypeImportHooks( klass );
        importHooks.forEach( hook -> hook.preTypeImport( klass, nonPersistedObjects, bundle ) );

        if ( bundle.getImportMode().isCreateAndUpdate() )
        {
            TypeReport typeReport = new TypeReport( klass );
            typeReport.merge( handleCreates( session, klass, nonPersistedObjects, bundle ) );
            typeReport.merge( handleUpdates( session, klass, persistedObjects, bundle ) );

            typeReports.put( klass, typeReport );
        }
        else if ( bundle.getImportMode().isCreate() )
        {
            typeReports.put( klass, handleCreates( session, klass, nonPersistedObjects, bundle ) );
        }
        else if ( bundle.getImportMode().isUpdate() )
        {
            typeReports.put( klass, handleUpdates( session, klass, persistedObjects, bundle ) );
        }
        else if ( bundle.getImportMode().isDelete() )
        {
            typeReports.put( klass, handleDeletes( session, klass, persistedObjects, bundle ) );
        }

        importHooks.forEach( hook -> hook.postTypeImport( klass, persistedObjects, bundle ) );

        if ( FlushMode.AUTO == bundle.getFlushMode() )
        {
            session.flush();
        }
    }

    // -----------------------------------------------------------------------------------
    // Utility Methods
    // -----------------------------------------------------------------------------------

    private <T extends IdentifiableObject> TypeReport handleCreates( Session session, Class<T> klass,
        List<T> objects, ObjectBundle bundle )
    {
        TypeReport typeReport = new TypeReport( klass );

        handleDeprecationIfEventReport( klass, objects );

        if ( objects.isEmpty() )
        {
            return typeReport;
        }

        String message = "(" + bundle.getUsername() + ") Creating " + objects.size() + " object(s) of type "
            + objects.get( 0 ).getClass().getSimpleName();

        log.info( message );

        if ( bundle.hasJobId() )
        {
            notifier.notify( bundle.getJobId(), message );
        }

        objects.forEach( object -> objectBundleHooks.getObjectHooks( object )
            .forEach( hook -> hook.preCreate( object, bundle ) ) );

        session.flush();

        for ( T object : objects )
        {
            ObjectReport objectReport = new ObjectReport( object, bundle );
            objectReport.setDisplayName( IdentifiableObjectUtils.getDisplayName( object ) );
            typeReport.addObjectReport( objectReport );

            preheatService.connectReferences( object, bundle.getPreheat(), bundle.getPreheatIdentifier() );

            if ( bundle.getOverrideUser() != null )
            {
                object.setCreatedBy( bundle.getOverrideUser() );

                if ( object instanceof User )
                {
                    (object).setCreatedBy( bundle.getOverrideUser() );
                }
            }

            session.save( object );

            bundle.getPreheat().replace( bundle.getPreheatIdentifier(), object );

            if ( log.isDebugEnabled() )
            {
                String msg = "(" + bundle.getUsername() + ") Created object '"
                    + bundle.getPreheatIdentifier().getIdentifiersWithName( object ) + "'";
                log.debug( msg );
            }

            if ( FlushMode.OBJECT == bundle.getFlushMode() )
            {
                session.flush();
            }
        }

        session.flush();

        objects.forEach( object -> objectBundleHooks.getObjectHooks( object )
            .forEach( hook -> hook.postCreate( object, bundle ) ) );

        return typeReport;
    }

    private <T extends IdentifiableObject> TypeReport handleUpdates( Session session, Class<T> klass,
        List<T> objects, ObjectBundle bundle )
    {
        TypeReport typeReport = new TypeReport( klass );

        if ( objects.isEmpty() )
        {
            return typeReport;
        }

        String message = "(" + bundle.getUsername() + ") Updating " + objects.size() + " object(s) of type "
            + objects.get( 0 ).getClass().getSimpleName();

        log.info( message );

        if ( bundle.hasJobId() )
        {
            notifier.notify( bundle.getJobId(), message );
        }

        objects.forEach( object -> {
            T persistedObject = bundle.getPreheat().get( bundle.getPreheatIdentifier(), object );
            objectBundleHooks.getObjectHooks( object )
                .forEach( hook -> hook.preUpdate( object, persistedObject, bundle ) );
        } );

        session.flush();

        for ( T object : objects )
        {
            T persistedObject = bundle.getPreheat().get( bundle.getPreheatIdentifier(), object );

            ObjectReport objectReport = new ObjectReport( object, bundle );
            objectReport.setDisplayName( IdentifiableObjectUtils.getDisplayName( object ) );
            typeReport.addObjectReport( objectReport );

            preheatService.connectReferences( object, bundle.getPreheat(), bundle.getPreheatIdentifier() );

            if ( bundle.getMergeMode() != MergeMode.NONE )
            {
                mergeService.merge( new MergeParams<>( object, persistedObject )
                    .setMergeMode( bundle.getMergeMode() )
                    .setSkipSharing( bundle.isSkipSharing() )
                    .setSkipTranslation( bundle.isSkipTranslation() ) );
            }

            if ( bundle.getOverrideUser() != null )
            {
                persistedObject.setCreatedBy( bundle.getOverrideUser() );

                if ( object instanceof User )
                {
                    (object).setCreatedBy( bundle.getOverrideUser() );
                }
            }

            session.merge( persistedObject );

            bundle.getPreheat().replace( bundle.getPreheatIdentifier(), persistedObject );

            if ( log.isDebugEnabled() )
            {
                String msg = "(" + bundle.getUsername() + ") Updated object '"
                    + bundle.getPreheatIdentifier().getIdentifiersWithName( persistedObject ) + "'";
                log.debug( msg );
            }

            if ( FlushMode.OBJECT == bundle.getFlushMode() )
            {
                session.flush();
            }
        }

        session.flush();

        objects.forEach( object -> {
            T persistedObject = bundle.getPreheat().get( bundle.getPreheatIdentifier(), object );
            objectBundleHooks.getObjectHooks( object )
                .forEach( hook -> hook.postUpdate( persistedObject, bundle ) );
        } );

        return typeReport;
    }

    private <T extends IdentifiableObject> TypeReport handleDeletes( Session session, Class<T> klass,
        List<T> objects, ObjectBundle bundle )
    {
        TypeReport typeReport = new TypeReport( klass );

        if ( objects.isEmpty() )
        {
            return typeReport;
        }

        String message = "(" + bundle.getUsername() + ") Deleting " + objects.size() + " object(s) of type "
            + objects.get( 0 ).getClass().getSimpleName();

        log.info( message );

        if ( bundle.hasJobId() )
        {
            notifier.notify( bundle.getJobId(), message );
        }

        List<T> persistedObjects = bundle.getPreheat().getAll( bundle.getPreheatIdentifier(), objects );

        for ( T object : persistedObjects )
        {
            ObjectReport objectReport = new ObjectReport( object, bundle );
            objectReport.setDisplayName( IdentifiableObjectUtils.getDisplayName( object ) );
            typeReport.addObjectReport( objectReport );

            objectBundleHooks.getObjectHooks( object ).forEach( hook -> hook.preDelete( object, bundle ) );
            manager.delete( object, bundle.getUser() );

            bundle.getPreheat().remove( bundle.getPreheatIdentifier(), object );

            if ( log.isDebugEnabled() )
            {
                String msg = "(" + bundle.getUsername() + ") Deleted object '"
                    + bundle.getPreheatIdentifier().getIdentifiersWithName( object ) + "'";
                log.debug( msg );
            }

            if ( FlushMode.OBJECT == bundle.getFlushMode() )
            {
                session.flush();
            }
        }

        return typeReport;
    }

    @SuppressWarnings( "unchecked" )
    private List<Class<? extends IdentifiableObject>> getSortedClasses( ObjectBundle bundle )
    {
        return schemaService.getMetadataSchemas().stream()
            .map( schema -> (Class<? extends IdentifiableObject>) schema.getKlass() )
            .filter( bundle::hasObjects )
            .collect( toList() );
    }
}
