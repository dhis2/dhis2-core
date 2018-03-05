package org.hisp.dhis.dxf2.metadata.objectbundle;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.amqp.AmqpService;
import org.hisp.dhis.cache.HibernateCacheManager;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.deletedobject.DeletedObjectQuery;
import org.hisp.dhis.deletedobject.DeletedObjectService;
import org.hisp.dhis.dxf2.metadata.FlushMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleCommitReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.preheat.PreheatParams;
import org.hisp.dhis.preheat.PreheatService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.MergeParams;
import org.hisp.dhis.schema.MergeService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.audit.MetadataAudit;
import org.hisp.dhis.schema.audit.MetadataAuditService;
import org.hisp.dhis.schema.patch.Patch;
import org.hisp.dhis.schema.patch.PatchParams;
import org.hisp.dhis.schema.patch.PatchService;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
@Transactional
public class DefaultObjectBundleService implements ObjectBundleService
{
    private static final Log log = LogFactory.getLog( DefaultObjectBundleService.class );

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private PreheatService preheatService;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private DbmsManager dbmsManager;

    @Autowired
    private HibernateCacheManager cacheManager;

    @Autowired
    private Notifier notifier;

    @Autowired
    private MergeService mergeService;

    @Autowired
    private DeletedObjectService deletedObjectService;

    @Autowired
    private PatchService patchService;

    @Autowired
    private MetadataAuditService metadataAuditService;

    @Autowired
    private RenderService renderService;

    @Autowired
    private SystemService systemService;

    @Autowired
    private AmqpService amqpService;

    @Autowired( required = false )
    private List<ObjectBundleHook> objectBundleHooks = new ArrayList<>();

    @Override
    public ObjectBundle create( ObjectBundleParams params )
    {
        PreheatParams preheatParams = params.getPreheatParams();

        if ( params.getUser() == null )
        {
            params.setUser( currentUserService.getCurrentUser() );
        }

        preheatParams.setUser( params.getUser() );
        preheatParams.setObjects( params.getObjects() );

        ObjectBundle bundle = new ObjectBundle( params, preheatService.preheat( preheatParams ), params.getObjects() );
        bundle.setObjectBundleStatus( ObjectBundleStatus.CREATED );
        bundle.setObjectReferences( preheatService.collectObjectReferences( params.getObjects() ) );

        return bundle;
    }

    @Override
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

        objectBundleHooks.forEach( hook -> hook.preCommit( bundle ) );

        for ( Class<? extends IdentifiableObject> klass : klasses )
        {
            List<IdentifiableObject> nonPersistedObjects = bundle.getObjects( klass, false );
            List<IdentifiableObject> persistedObjects = bundle.getObjects( klass, true );

            objectBundleHooks.forEach( hook -> hook.preTypeImport( klass, nonPersistedObjects, bundle ) );

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

            objectBundleHooks.forEach( hook -> hook.postTypeImport( klass, persistedObjects, bundle ) );

            if ( FlushMode.AUTO == bundle.getFlushMode() ) session.flush();
        }

        if ( !bundle.getImportMode().isDelete() )
        {
            objectBundleHooks.forEach( hook -> hook.postCommit( bundle ) );
        }

        dbmsManager.clearSession();
        cacheManager.clearCache();
        bundle.setObjectBundleStatus( ObjectBundleStatus.COMMITTED );

        return commitReport;
    }

    //-----------------------------------------------------------------------------------
    // Utility Methods
    //-----------------------------------------------------------------------------------

    private TypeReport handleCreates( Session session, Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, ObjectBundle bundle )
    {
        TypeReport typeReport = new TypeReport( klass );
        SystemInfo systemInfo = systemService.getSystemInfo();

        if ( objects.isEmpty() )
        {
            return typeReport;
        }

        String message = "(" + bundle.getUsername() + ") Creating " + objects.size() + " object(s) of type " + objects.get( 0 ).getClass().getSimpleName();

        log.info( message );

        if ( bundle.hasJobId() )
        {
            notifier.notify( bundle.getJobId(), message );
        }

        objects.forEach( object -> objectBundleHooks.forEach( hook -> {
            hook.preCreate( object, bundle );
        } ) );

        session.flush();

        for ( int idx = 0; idx < objects.size(); idx++ )
        {
            IdentifiableObject object = objects.get( idx );

            ObjectReport objectReport = new ObjectReport( klass, idx, object.getUid() );
            objectReport.setDisplayName( IdentifiableObjectUtils.getDisplayName( object ) );
            typeReport.addObjectReport( objectReport );

            preheatService.connectReferences( object, bundle.getPreheat(), bundle.getPreheatIdentifier() );

            if ( bundle.getOverrideUser() != null )
            {
                ((BaseIdentifiableObject) object).setUser( bundle.getOverrideUser() );

                if ( User.class.isInstance( object ) )
                {
                    ((User) object).getUserCredentials().setUser( bundle.getOverrideUser() );
                }
            }

            session.save( object );

            bundle.getPreheat().replace( bundle.getPreheatIdentifier(), object );

            MetadataAudit audit = new MetadataAudit();
            audit.setCreatedAt( new Date() );
            audit.setCreatedBy( bundle.getUsername() );
            audit.setKlass( klass.getName() );
            audit.setUid( object.getUid() );
            audit.setCode( object.getCode() );
            audit.setType( AuditType.CREATE );

            if ( amqpService.isEnabled() )
            {
                audit.setValue( renderService.toJsonAsString( object ) );
                amqpService.publish( audit );
            }

            if ( log.isDebugEnabled() )
            {
                String msg = "(" + bundle.getUsername() + ") Created object '" + bundle.getPreheatIdentifier().getIdentifiersWithName( object ) + "'";
                log.debug( msg );
            }

            if ( systemInfo.getMetadataAudit().isAudit() )
            {
                if ( audit.getValue() == null )
                {
                    audit.setValue( renderService.toJsonAsString( object ) );
                }

                String auditJson = renderService.toJsonAsString( audit );

                if ( systemInfo.getMetadataAudit().isLog() )
                {
                    log.info( "MetadataAuditEvent: " + auditJson );
                }

                if ( systemInfo.getMetadataAudit().isPersist() )
                {
                    metadataAuditService.addMetadataAudit( audit );
                }
            }

            if ( FlushMode.OBJECT == bundle.getFlushMode() ) session.flush();
        }

        session.flush();

        objects.forEach( object -> objectBundleHooks.forEach( hook -> {
            hook.postCreate( object, bundle );
        } ) );

        return typeReport;
    }

    private TypeReport handleUpdates( Session session, Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, ObjectBundle bundle )
    {
        TypeReport typeReport = new TypeReport( klass );
        SystemInfo systemInfo = systemService.getSystemInfo();

        if ( objects.isEmpty() )
        {
            return typeReport;
        }

        String message = "(" + bundle.getUsername() + ") Updating " + objects.size() + " object(s) of type " + objects.get( 0 ).getClass().getSimpleName();

        log.info( message );

        if ( bundle.hasJobId() )
        {
            notifier.notify( bundle.getJobId(), message );
        }

        objects.forEach( object ->
        {
            IdentifiableObject persistedObject = bundle.getPreheat().get( bundle.getPreheatIdentifier(), object );
            objectBundleHooks.forEach( hook -> hook.preUpdate( object, persistedObject, bundle ) );
        } );

        session.flush();

        for ( int idx = 0; idx < objects.size(); idx++ )
        {
            Patch patch = null;
            IdentifiableObject object = objects.get( idx );
            IdentifiableObject persistedObject = bundle.getPreheat().get( bundle.getPreheatIdentifier(), object );

            ObjectReport objectReport = new ObjectReport( klass, idx, object.getUid() );
            objectReport.setDisplayName( IdentifiableObjectUtils.getDisplayName( object ) );
            typeReport.addObjectReport( objectReport );

            preheatService.connectReferences( object, bundle.getPreheat(), bundle.getPreheatIdentifier() );

            if ( systemInfo.getMetadataAudit().isAudit() )
            {
                patch = patchService.diff( new PatchParams( persistedObject, object ).setIgnoreTransient( true ) );
            }

            if ( bundle.getMergeMode() != MergeMode.NONE )
            {
                mergeService.merge( new MergeParams<>( object, persistedObject )
                    .setMergeMode( bundle.getMergeMode() )
                    .setSkipSharing( bundle.isSkipSharing() ) );
            }

            if ( bundle.getOverrideUser() != null )
            {
                ((BaseIdentifiableObject) persistedObject).setUser( bundle.getOverrideUser() );

                if ( User.class.isInstance( object ) )
                {
                    ((User) object).getUserCredentials().setUser( bundle.getOverrideUser() );
                }
            }

            session.update( persistedObject );

            bundle.getPreheat().replace( bundle.getPreheatIdentifier(), persistedObject );

            MetadataAudit audit = new MetadataAudit();
            audit.setCreatedAt( new Date() );
            audit.setCreatedBy( bundle.getUsername() );
            audit.setKlass( klass.getName() );
            audit.setUid( object.getUid() );
            audit.setCode( object.getCode() );
            audit.setType( AuditType.UPDATE );

            if ( amqpService.isEnabled() )
            {
                audit.setValue( renderService.toJsonAsString( patch ) );
                amqpService.publish( audit );
            }

            if ( log.isDebugEnabled() )
            {
                String msg = "(" + bundle.getUsername() + ") Updated object '" + bundle.getPreheatIdentifier().getIdentifiersWithName( persistedObject ) + "'";
                log.debug( msg );
            }

            if ( systemInfo.getMetadataAudit().isAudit() )
            {
                if ( audit.getValue() == null )
                {
                    audit.setValue( renderService.toJsonAsString( patch ) );
                }

                String auditJson = renderService.toJsonAsString( audit );

                if ( systemInfo.getMetadataAudit().isLog() )
                {
                    log.info( "MetadataAuditEvent: " + auditJson );
                }

                if ( systemInfo.getMetadataAudit().isPersist() )
                {
                    metadataAuditService.addMetadataAudit( audit );
                }
            }

            if ( FlushMode.OBJECT == bundle.getFlushMode() ) session.flush();
        }

        session.flush();

        objects.forEach( object ->
        {
            IdentifiableObject persistedObject = bundle.getPreheat().get( bundle.getPreheatIdentifier(), object );
            objectBundleHooks.forEach( hook -> hook.postUpdate( persistedObject, bundle ) );
        } );

        return typeReport;
    }

    private TypeReport handleDeletes( Session session, Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, ObjectBundle bundle )
    {
        TypeReport typeReport = new TypeReport( klass );
        SystemInfo systemInfo = systemService.getSystemInfo();

        if ( objects.isEmpty() )
        {
            return typeReport;
        }

        String message = "(" + bundle.getUsername() + ") Deleting " + objects.size() + " object(s) of type " + objects.get( 0 ).getClass().getSimpleName();

        log.info( message );

        if ( bundle.hasJobId() )
        {
            notifier.notify( bundle.getJobId(), message );
        }

        List<IdentifiableObject> persistedObjects = bundle.getPreheat().getAll( bundle.getPreheatIdentifier(), objects );

        for ( int idx = 0; idx < persistedObjects.size(); idx++ )
        {
            IdentifiableObject object = persistedObjects.get( idx );
            ObjectReport objectReport = new ObjectReport( klass, idx, object.getUid() );
            objectReport.setDisplayName( IdentifiableObjectUtils.getDisplayName( object ) );
            typeReport.addObjectReport( objectReport );

            objectBundleHooks.forEach( hook -> hook.preDelete( object, bundle ) );
            manager.delete( object, bundle.getUser() );

            if ( MetadataObject.class.isInstance( object ) )
            {
                deletedObjectService.deleteDeletedObjects( new DeletedObjectQuery( object ) );
            }

            bundle.getPreheat().remove( bundle.getPreheatIdentifier(), object );

            MetadataAudit audit = new MetadataAudit();
            audit.setCreatedAt( new Date() );
            audit.setCreatedBy( bundle.getUsername() );
            audit.setKlass( klass.getName() );
            audit.setUid( object.getUid() );
            audit.setCode( object.getCode() );
            audit.setType( AuditType.DELETE );

            if ( amqpService.isEnabled() )
            {
                audit.setValue( renderService.toJsonAsString( object ) );
                amqpService.publish( audit );
            }

            if ( log.isDebugEnabled() )
            {
                String msg = "(" + bundle.getUsername() + ") Deleted object '" + bundle.getPreheatIdentifier().getIdentifiersWithName( object ) + "'";
                log.debug( msg );
            }

            if ( systemInfo.getMetadataAudit().isAudit() )
            {
                if ( audit.getValue() == null )
                {
                    audit.setValue( renderService.toJsonAsString( object ) );
                }

                String auditJson = renderService.toJsonAsString( audit );

                if ( systemInfo.getMetadataAudit().isLog() )
                {
                    log.info( "MetadataAuditEvent: " + auditJson );
                }

                if ( systemInfo.getMetadataAudit().isPersist() )
                {
                    metadataAuditService.addMetadataAudit( audit );
                }
            }

            if ( FlushMode.OBJECT == bundle.getFlushMode() ) session.flush();
        }

        return typeReport;
    }

    @SuppressWarnings( "unchecked" )
    private List<Class<? extends IdentifiableObject>> getSortedClasses( ObjectBundle bundle )
    {
        List<Class<? extends IdentifiableObject>> klasses = new ArrayList<>();

        schemaService.getMetadataSchemas().forEach( schema ->
        {
            Class<? extends IdentifiableObject> klass = (Class<? extends IdentifiableObject>) schema.getKlass();

            if ( bundle.getObjectMap().containsKey( klass ) )
            {
                klasses.add( klass );
            }
        } );

        return klasses;
    }
}
