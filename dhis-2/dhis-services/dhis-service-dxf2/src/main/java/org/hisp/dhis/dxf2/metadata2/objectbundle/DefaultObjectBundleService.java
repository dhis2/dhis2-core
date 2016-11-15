package org.hisp.dhis.dxf2.metadata2.objectbundle;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.metadata2.AtomicMode;
import org.hisp.dhis.dxf2.metadata2.FlushMode;
import org.hisp.dhis.dxf2.metadata2.objectbundle.feedback.ObjectBundleCommitReport;
import org.hisp.dhis.dxf2.metadata2.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.dxf2.metadata2.objectbundle.hooks.ObjectBundleHook;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.preheat.PreheatParams;
import org.hisp.dhis.preheat.PreheatService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.validation.SchemaValidator;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
    private SchemaValidator schemaValidator;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private DbmsManager dbmsManager;

    @Autowired
    private AclService aclService;

    @Autowired
    private UserService userService;

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
        bundle.setObjectReferences( preheatService.collectObjectReferences( params.getObjects() ) );

        return bundle;
    }

    @Override
    public ObjectBundleValidationReport validate( ObjectBundle bundle )
    {
        Timer timer = new SystemTimer().start();

        ObjectBundleValidationReport validation = new ObjectBundleValidationReport();

        if ( (bundle.getUser() == null || bundle.getUser().isSuper()) && bundle.isSkipValidation() )
        {
            log.warn( "Skipping validation for metadata import by user '" + bundle.getUsername() + "'. Not recommended." );
            return validation;
        }

        List<Class<? extends IdentifiableObject>> klasses = getSortedClasses( bundle );

        for ( Class<? extends IdentifiableObject> klass : klasses )
        {
            TypeReport typeReport = new TypeReport( klass );

            List<IdentifiableObject> nonPersistedObjects = bundle.getObjects( klass, false );
            List<IdentifiableObject> persistedObjects = bundle.getObjects( klass, true );
            List<IdentifiableObject> allObjects = bundle.getObjectMap().get( klass );

            handleDefaults( nonPersistedObjects );
            handleDefaults( persistedObjects );

            if ( bundle.getImportMode().isCreateAndUpdate() )
            {
                typeReport.merge( validateSecurity( klass, nonPersistedObjects, bundle, ImportStrategy.CREATE ) );
                typeReport.merge( validateSecurity( klass, persistedObjects, bundle, ImportStrategy.UPDATE ) );
                typeReport.merge( validateBySchemas( klass, nonPersistedObjects, bundle ) );
                typeReport.merge( validateBySchemas( klass, persistedObjects, bundle ) );
                typeReport.merge( preheatService.checkUniqueness( klass, nonPersistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );
                typeReport.merge( preheatService.checkUniqueness( klass, persistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );
                typeReport.merge( preheatService.checkMandatoryAttributes( klass, nonPersistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );
                typeReport.merge( preheatService.checkMandatoryAttributes( klass, persistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );
                typeReport.merge( preheatService.checkUniqueAttributes( klass, nonPersistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );
                typeReport.merge( preheatService.checkUniqueAttributes( klass, persistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );

                TypeReport checkReferences = preheatService.checkReferences( klass, allObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() );

                if ( !checkReferences.getErrorReports().isEmpty() && AtomicMode.ALL == bundle.getAtomicMode() )
                {
                    typeReport.getStats().incIgnored();
                }

                typeReport.merge( checkReferences );
            }
            else if ( bundle.getImportMode().isCreate() )
            {
                typeReport.merge( validateSecurity( klass, nonPersistedObjects, bundle, ImportStrategy.CREATE ) );
                typeReport.merge( validateForCreate( klass, persistedObjects, bundle ) );
                typeReport.merge( validateBySchemas( klass, nonPersistedObjects, bundle ) );
                typeReport.merge( preheatService.checkUniqueness( klass, nonPersistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );
                typeReport.merge( preheatService.checkMandatoryAttributes( klass, nonPersistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );
                typeReport.merge( preheatService.checkUniqueAttributes( klass, nonPersistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );

                TypeReport checkReferences = preheatService.checkReferences( klass, allObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() );

                if ( !checkReferences.getErrorReports().isEmpty() && AtomicMode.ALL == bundle.getAtomicMode() )
                {
                    typeReport.getStats().incIgnored();
                }

                typeReport.merge( checkReferences );
            }
            else if ( bundle.getImportMode().isUpdate() )
            {
                typeReport.merge( validateSecurity( klass, persistedObjects, bundle, ImportStrategy.UPDATE ) );
                typeReport.merge( validateForUpdate( klass, nonPersistedObjects, bundle ) );
                typeReport.merge( validateBySchemas( klass, persistedObjects, bundle ) );
                typeReport.merge( preheatService.checkUniqueness( klass, persistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );
                typeReport.merge( preheatService.checkMandatoryAttributes( klass, persistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );
                typeReport.merge( preheatService.checkUniqueAttributes( klass, persistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );

                TypeReport checkReferences = preheatService.checkReferences( klass, allObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() );

                if ( !checkReferences.getErrorReports().isEmpty() && AtomicMode.ALL == bundle.getAtomicMode() )
                {
                    typeReport.getStats().incIgnored();
                }

                typeReport.merge( checkReferences );
            }
            else if ( bundle.getImportMode().isDelete() )
            {
                typeReport.merge( validateSecurity( klass, persistedObjects, bundle, ImportStrategy.DELETE ) );
                typeReport.merge( validateForDelete( klass, nonPersistedObjects, bundle ) );
            }

            validation.addTypeReport( typeReport );
        }

        validateAtomicity( bundle, validation );
        bundle.setObjectBundleStatus( ObjectBundleStatus.VALIDATED );

        log.info( "(" + bundle.getUsername() + ") Import:Validation took " + timer.toString() );

        return validation;
    }

    private void handleDefaults( List<IdentifiableObject> objects )
    {
        Iterator<IdentifiableObject> iterator = objects.iterator();

        while ( iterator.hasNext() )
        {
            IdentifiableObject object = iterator.next();

            if ( Preheat.isDefault( object ) )
            {
                iterator.remove();
            }
        }
    }

    private void validateAtomicity( ObjectBundle bundle, ObjectBundleValidationReport validation )
    {
        if ( AtomicMode.NONE == bundle.getAtomicMode() )
        {
            return;
        }

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> nonPersistedObjects = bundle.getObjects( false );
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> persistedObjects = bundle.getObjects( true );

        if ( AtomicMode.ALL == bundle.getAtomicMode() )
        {
            if ( !validation.getErrorReports().isEmpty() )
            {
                nonPersistedObjects.clear();
                persistedObjects.clear();
            }
        }
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

        objectBundleHooks.forEach( hook -> hook.preImport( bundle ) );

        for ( Class<? extends IdentifiableObject> klass : klasses )
        {
            List<IdentifiableObject> nonPersistedObjects = bundle.getObjects( klass, false );
            List<IdentifiableObject> persistedObjects = bundle.getObjects( klass, true );

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

            if ( FlushMode.AUTO == bundle.getFlushMode() ) session.flush();
        }

        objectBundleHooks.forEach( hook -> hook.postImport( bundle ) );
        session.flush();

        dbmsManager.clearSession();
        bundle.setObjectBundleStatus( ObjectBundleStatus.COMMITTED );

        return commitReport;
    }

    //-----------------------------------------------------------------------------------
    // Utility Methods
    //-----------------------------------------------------------------------------------

    private TypeReport handleCreates( Session session, Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, ObjectBundle bundle )
    {
        TypeReport typeReport = new TypeReport( klass );

        if ( objects.isEmpty() )
        {
            return typeReport;
        }

        log.info( "(" + bundle.getUsername() + ") Creating " + objects.size() + " object(s) of type " + objects.get( 0 ).getClass().getSimpleName() );

        for ( int idx = 0; idx < objects.size(); idx++ )
        {
            IdentifiableObject object = objects.get( idx );

            if ( Preheat.isDefault( object ) ) continue;

            ObjectReport objectReport = new ObjectReport( klass, idx, object.getUid() );
            typeReport.addObjectReport( objectReport );

            objectBundleHooks.forEach( hook -> hook.preCreate( object, bundle ) );

            preheatService.connectReferences( object, bundle.getPreheat(), bundle.getPreheatIdentifier() );

            prepare( object, bundle );
            session.save( object );
            typeReport.getStats().incCreated();

            bundle.getPreheat().replace( bundle.getPreheatIdentifier(), object );

            objectBundleHooks.forEach( hook -> hook.postCreate( object, bundle ) );

            if ( log.isDebugEnabled() )
            {
                String msg = "(" + bundle.getUsername() + ") Created object '" + bundle.getPreheatIdentifier().getIdentifiersWithName( object ) + "'";
                log.debug( msg );
            }

            if ( FlushMode.OBJECT == bundle.getFlushMode() ) session.flush();
        }

        return typeReport;
    }

    private TypeReport handleUpdates( Session session, Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, ObjectBundle bundle )
    {
        TypeReport typeReport = new TypeReport( klass );

        if ( objects.isEmpty() )
        {
            return typeReport;
        }

        log.info( "(" + bundle.getUsername() + ") Updating " + objects.size() + " object(s) of type " + objects.get( 0 ).getClass().getSimpleName() );

        for ( IdentifiableObject object : objects )
        {
            IdentifiableObject persistedObject = bundle.getPreheat().get( bundle.getPreheatIdentifier(), object );
            objectBundleHooks.forEach( hook -> hook.preUpdate( object, persistedObject, bundle ) );
        }

        for ( int idx = 0; idx < objects.size(); idx++ )
        {
            IdentifiableObject object = objects.get( idx );

            if ( Preheat.isDefault( object ) ) continue;

            ObjectReport objectReport = new ObjectReport( klass, idx, object.getUid() );
            typeReport.addObjectReport( objectReport );

            IdentifiableObject persistedObject = bundle.getPreheat().get( bundle.getPreheatIdentifier(), object );

            preheatService.connectReferences( object, bundle.getPreheat(), bundle.getPreheatIdentifier() );

            persistedObject.mergeWith( object, bundle.getMergeMode() );

            if ( !bundle.isSkipSharing() )
            {
                persistedObject.mergeSharingWith( object );
            }

            prepare( persistedObject, bundle );
            session.update( persistedObject );
            typeReport.getStats().incUpdated();

            objectBundleHooks.forEach( hook -> hook.postUpdate( persistedObject, bundle ) );

            bundle.getPreheat().replace( bundle.getPreheatIdentifier(), persistedObject );

            if ( log.isDebugEnabled() )
            {
                String msg = "(" + bundle.getUsername() + ") Updated object '" + bundle.getPreheatIdentifier().getIdentifiersWithName( persistedObject ) + "'";
                log.debug( msg );
            }

            if ( FlushMode.OBJECT == bundle.getFlushMode() ) session.flush();
        }

        return typeReport;
    }

    private TypeReport handleDeletes( Session session, Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, ObjectBundle bundle )
    {
        TypeReport typeReport = new TypeReport( klass );

        if ( objects.isEmpty() )
        {
            return typeReport;
        }

        log.info( "(" + bundle.getUsername() + ") Deleting " + objects.size() + " object(s) of type " + objects.get( 0 ).getClass().getSimpleName() );

        List<IdentifiableObject> persistedObjects = bundle.getPreheat().getAll( bundle.getPreheatIdentifier(), objects );

        for ( int idx = 0; idx < persistedObjects.size(); idx++ )
        {
            IdentifiableObject object = persistedObjects.get( idx );
            ObjectReport objectReport = new ObjectReport( klass, idx, object.getUid() );
            typeReport.addObjectReport( objectReport );

            objectBundleHooks.forEach( hook -> hook.preDelete( object, bundle ) );
            manager.delete( object, bundle.getUser() );
            typeReport.getStats().incDeleted();

            bundle.getPreheat().remove( bundle.getPreheatIdentifier(), object );

            if ( log.isDebugEnabled() )
            {
                String msg = "(" + bundle.getUsername() + ") Deleted object '" + bundle.getPreheatIdentifier().getIdentifiersWithName( object ) + "'";
                log.debug( msg );
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

    private void prepare( IdentifiableObject object, ObjectBundle bundle )
    {
        if ( object == null )
        {
            return;
        }

        BaseIdentifiableObject identifiableObject = (BaseIdentifiableObject) object;

        if ( identifiableObject.getUser() == null ) identifiableObject.setUser( bundle.getUser() );
        if ( identifiableObject.getUserGroupAccesses() == null ) identifiableObject.setUserGroupAccesses( new HashSet<>() );
    }

    private TypeReport validateSecurity( Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, ObjectBundle bundle, ImportStrategy importMode )
    {
        TypeReport typeReport = new TypeReport( klass );

        if ( objects == null || objects.isEmpty() )
        {
            return typeReport;
        }

        Iterator<IdentifiableObject> iterator = objects.iterator();
        PreheatIdentifier identifier = bundle.getPreheatIdentifier();
        int idx = 0;

        while ( iterator.hasNext() )
        {
            IdentifiableObject object = iterator.next();

            if ( importMode.isCreate() )
            {
                if ( !aclService.canCreate( bundle.getUser(), klass ) )
                {
                    ObjectReport objectReport = new ObjectReport( klass, idx, object.getUid() );
                    objectReport.addErrorReport( new ErrorReport( klass, ErrorCode.E3000, identifier.getIdentifiersWithName( bundle.getUser() ),
                        identifier.getIdentifiersWithName( object ) ) );

                    typeReport.addObjectReport( objectReport );
                    typeReport.getStats().incIgnored();

                    iterator.remove();
                }
            }
            else
            {
                if ( importMode.isUpdate() )
                {
                    if ( !aclService.canUpdate( bundle.getUser(), object ) )
                    {
                        ObjectReport objectReport = new ObjectReport( klass, idx, object.getUid() );
                        objectReport.addErrorReport( new ErrorReport( klass, ErrorCode.E3001, identifier.getIdentifiersWithName( bundle.getUser() ),
                            identifier.getIdentifiersWithName( object ) ) );

                        typeReport.addObjectReport( objectReport );
                        typeReport.getStats().incIgnored();

                        iterator.remove();
                    }
                }
                else if ( importMode.isDelete() )
                {
                    if ( !aclService.canDelete( bundle.getUser(), object ) )
                    {
                        ObjectReport objectReport = new ObjectReport( klass, idx, object.getUid() );
                        objectReport.addErrorReport( new ErrorReport( klass, ErrorCode.E3002, identifier.getIdentifiersWithName( bundle.getUser() ),
                            identifier.getIdentifiersWithName( object ) ) );

                        typeReport.addObjectReport( objectReport );
                        typeReport.getStats().incIgnored();

                        iterator.remove();
                    }
                }
            }

            if ( User.class.isInstance( object ) )
            {
                User user = (User) object;
                List<ErrorReport> errorReports = userService.validateUser( bundle.getUser(), user );

                if ( !errorReports.isEmpty() )
                {
                    ObjectReport objectReport = new ObjectReport( klass, idx, object.getUid() );
                    objectReport.addErrorReports( errorReports );

                    typeReport.addObjectReport( objectReport );
                    typeReport.getStats().incIgnored();

                    iterator.remove();
                    continue;
                }
            }

            idx++;
        }

        return typeReport;
    }

    private TypeReport validateForCreate( Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, ObjectBundle bundle )
    {
        TypeReport typeReport = new TypeReport( klass );

        if ( objects == null || objects.isEmpty() )
        {
            return typeReport;
        }

        Iterator<IdentifiableObject> iterator = objects.iterator();
        int idx = 0;

        while ( iterator.hasNext() )
        {
            IdentifiableObject identifiableObject = iterator.next();
            IdentifiableObject object = bundle.getPreheat().get( bundle.getPreheatIdentifier(), identifiableObject );

            if ( object != null && object.getId() > 0 )
            {
                ObjectReport objectReport = new ObjectReport( klass, idx, object.getUid() );
                objectReport.addErrorReport( new ErrorReport( klass, ErrorCode.E5000, bundle.getPreheatIdentifier(),
                    bundle.getPreheatIdentifier().getIdentifiersWithName( identifiableObject ) ) );

                typeReport.addObjectReport( objectReport );
                typeReport.getStats().incIgnored();

                iterator.remove();
            }

            idx++;
        }

        return typeReport;
    }

    private TypeReport validateForUpdate( Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, ObjectBundle bundle )
    {
        TypeReport typeReport = new TypeReport( klass );

        if ( objects == null || objects.isEmpty() )
        {
            return typeReport;
        }

        Iterator<IdentifiableObject> iterator = objects.iterator();
        int idx = 0;

        while ( iterator.hasNext() )
        {
            IdentifiableObject identifiableObject = iterator.next();
            IdentifiableObject object = bundle.getPreheat().get( bundle.getPreheatIdentifier(), identifiableObject );

            if ( object == null || object.getId() == 0 )
            {
                if ( Preheat.isDefaultClass( identifiableObject.getClass() ) ) continue;

                ObjectReport objectReport = new ObjectReport( klass, idx, object != null ? object.getUid() : null );
                objectReport.addErrorReport( new ErrorReport( klass, ErrorCode.E5001, bundle.getPreheatIdentifier(),
                    bundle.getPreheatIdentifier().getIdentifiersWithName( identifiableObject ) ) );

                typeReport.addObjectReport( objectReport );
                typeReport.getStats().incIgnored();

                iterator.remove();
            }

            idx++;
        }

        return typeReport;
    }

    private TypeReport validateForDelete( Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, ObjectBundle bundle )
    {
        TypeReport typeReport = new TypeReport( klass );

        if ( objects == null || objects.isEmpty() )
        {
            return typeReport;
        }

        Iterator<IdentifiableObject> iterator = objects.iterator();
        int idx = 0;

        while ( iterator.hasNext() )
        {
            IdentifiableObject identifiableObject = iterator.next();
            IdentifiableObject object = bundle.getPreheat().get( bundle.getPreheatIdentifier(), identifiableObject );

            if ( object == null || object.getId() == 0 )
            {
                if ( Preheat.isDefaultClass( identifiableObject.getClass() ) ) continue;

                ObjectReport objectReport = new ObjectReport( klass, idx, object != null ? object.getUid() : null );
                objectReport.addErrorReport( new ErrorReport( klass, ErrorCode.E5001, bundle.getPreheatIdentifier(),
                    bundle.getPreheatIdentifier().getIdentifiersWithName( identifiableObject ) ) );

                typeReport.addObjectReport( objectReport );
                typeReport.getStats().incIgnored();

                iterator.remove();
            }

            idx++;
        }

        return typeReport;
    }

    private TypeReport validateBySchemas( Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, ObjectBundle bundle )
    {
        TypeReport typeReport = new TypeReport( klass );

        if ( objects == null || objects.isEmpty() )
        {
            return typeReport;
        }

        Iterator<IdentifiableObject> iterator = objects.iterator();
        int idx = 0;

        while ( iterator.hasNext() )
        {
            IdentifiableObject object = iterator.next();
            List<ErrorReport> validationErrorReports = schemaValidator.validate( object );

            if ( !validationErrorReports.isEmpty() )
            {
                ObjectReport objectReport = new ObjectReport( klass, idx, object.getUid() );
                objectReport.addErrorReports( validationErrorReports );

                typeReport.addObjectReport( objectReport );
                typeReport.getStats().incIgnored();

                iterator.remove();
            }

            idx++;
        }

        return typeReport;
    }
}
