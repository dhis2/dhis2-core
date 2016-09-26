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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.preheat.PreheatErrorReport;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.validation.SchemaValidator;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
@Transactional
public class DefaultObjectBundleValidationService implements ObjectBundleValidationService
{
    private static final Log log = LogFactory.getLog( DefaultObjectBundleValidationService.class );

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private SchemaValidator schemaValidator;

    @Autowired
    private AclService aclService;

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

            typeReport.merge( checkDuplicateIds( klass, persistedObjects, nonPersistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );

            if ( bundle.getImportMode().isCreateAndUpdate() )
            {
                typeReport.merge( validateSecurity( klass, nonPersistedObjects, bundle, ImportStrategy.CREATE ) );
                typeReport.merge( validateSecurity( klass, persistedObjects, bundle, ImportStrategy.UPDATE ) );
                typeReport.merge( validateBySchemas( klass, nonPersistedObjects, bundle ) );
                typeReport.merge( validateBySchemas( klass, persistedObjects, bundle ) );
                typeReport.merge( checkUniqueness( klass, nonPersistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );
                typeReport.merge( checkUniqueness( klass, persistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );
                typeReport.merge( checkMandatoryAttributes( klass, nonPersistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );
                typeReport.merge( checkMandatoryAttributes( klass, persistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );
                typeReport.merge( checkUniqueAttributes( klass, nonPersistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );
                typeReport.merge( checkUniqueAttributes( klass, persistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );

                TypeReport checkReferences = checkReferences( klass, allObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() );

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
                typeReport.merge( checkUniqueness( klass, nonPersistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );
                typeReport.merge( checkMandatoryAttributes( klass, nonPersistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );
                typeReport.merge( checkUniqueAttributes( klass, nonPersistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );

                TypeReport checkReferences = checkReferences( klass, allObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() );

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
                typeReport.merge( checkUniqueness( klass, persistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );
                typeReport.merge( checkMandatoryAttributes( klass, persistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );
                typeReport.merge( checkUniqueAttributes( klass, persistedObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() ) );

                TypeReport checkReferences = checkReferences( klass, allObjects, bundle.getPreheat(), bundle.getPreheatIdentifier() );

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

    //----------------------------------------------------------------------------------------------------
    // Helpers
    //----------------------------------------------------------------------------------------------------

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
                    objectReport.setDisplayName( IdentifiableObjectUtils.getDisplayName( object ) );
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
                        objectReport.setDisplayName( IdentifiableObjectUtils.getDisplayName( object ) );
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
                        objectReport.setDisplayName( IdentifiableObjectUtils.getDisplayName( object ) );
                        objectReport.addErrorReport( new ErrorReport( klass, ErrorCode.E3002, identifier.getIdentifiersWithName( bundle.getUser() ),
                            identifier.getIdentifiersWithName( object ) ) );

                        typeReport.addObjectReport( objectReport );
                        typeReport.getStats().incIgnored();

                        iterator.remove();
                    }
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
                objectReport.setDisplayName( IdentifiableObjectUtils.getDisplayName( object ) );
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
                objectReport.setDisplayName( IdentifiableObjectUtils.getDisplayName( object ) );
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
                objectReport.setDisplayName( IdentifiableObjectUtils.getDisplayName( object ) );
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
                objectReport.setDisplayName( IdentifiableObjectUtils.getDisplayName( object ) );
                objectReport.addErrorReports( validationErrorReports );

                typeReport.addObjectReport( objectReport );
                typeReport.getStats().incIgnored();

                iterator.remove();
            }

            idx++;
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

    private TypeReport checkReferences( Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, Preheat preheat, PreheatIdentifier identifier )
    {
        TypeReport typeReport = new TypeReport( klass );

        if ( objects.isEmpty() )
        {
            return typeReport;
        }

        for ( int idx = 0; idx < objects.size(); idx++ )
        {
            IdentifiableObject object = objects.get( idx );
            List<PreheatErrorReport> errorReports = checkReferences( klass, object, preheat, identifier );

            if ( errorReports.isEmpty() ) continue;

            ObjectReport objectReport = new ObjectReport( object.getClass(), idx );
            objectReport.addErrorReports( errorReports );
            typeReport.addObjectReport( objectReport );
        }

        return typeReport;
    }

    private List<PreheatErrorReport> checkReferences( Class<? extends IdentifiableObject> klass, IdentifiableObject object, Preheat preheat, PreheatIdentifier identifier )
    {
        List<PreheatErrorReport> preheatErrorReports = new ArrayList<>();

        if ( object == null )
        {
            return preheatErrorReports;
        }

        Schema schema = schemaService.getDynamicSchema( object.getClass() );
        schema.getProperties().stream()
            .filter( p -> p.isPersisted() && p.isOwner() && (PropertyType.REFERENCE == p.getPropertyType() || PropertyType.REFERENCE == p.getItemPropertyType()) )
            .forEach( p ->
            {
                if ( skipCheck( p.getKlass() ) || skipCheck( p.getItemKlass() ) )
                {
                    return;
                }

                if ( !p.isCollection() )
                {
                    IdentifiableObject refObject = ReflectionUtils.invokeMethod( object, p.getGetterMethod() );
                    IdentifiableObject ref = preheat.get( identifier, refObject );

                    if ( ref == null && refObject != null && !Preheat.isDefaultClass( refObject.getClass() ) )
                    {
                        preheatErrorReports.add( new PreheatErrorReport( identifier, object.getClass(), ErrorCode.E5002,
                            identifier.getIdentifiersWithName( refObject ), identifier.getIdentifiersWithName( object ), p.getName() ) );
                    }
                }
                else
                {
                    Collection<IdentifiableObject> objects = ReflectionUtils.newCollectionInstance( p.getKlass() );
                    Collection<IdentifiableObject> refObjects = ReflectionUtils.invokeMethod( object, p.getGetterMethod() );

                    for ( IdentifiableObject refObject : refObjects )
                    {
                        if ( Preheat.isDefault( refObject ) ) continue;

                        IdentifiableObject ref = preheat.get( identifier, refObject );

                        if ( ref == null && refObject != null && !Preheat.isDefaultClass( refObject.getClass() ) )
                        {
                            preheatErrorReports.add( new PreheatErrorReport( identifier, object.getClass(), ErrorCode.E5002,
                                identifier.getIdentifiersWithName( refObject ), identifier.getIdentifiersWithName( object ), p.getCollectionName() ) );
                        }
                        else
                        {
                            objects.add( refObject );
                        }
                    }

                    ReflectionUtils.invokeMethod( object, p.getSetterMethod(), objects );
                }
            } );

        if ( schema.havePersistedProperty( "attributeValues" ) )
        {
            object.getAttributeValues().stream()
                .filter( attributeValue -> attributeValue.getAttribute() != null && preheat.get( identifier, attributeValue.getAttribute() ) == null )
                .forEach( attributeValue -> preheatErrorReports.add( new PreheatErrorReport( identifier, object.getClass(), ErrorCode.E5002,
                    identifier.getIdentifiersWithName( attributeValue.getAttribute() ), identifier.getIdentifiersWithName( object ), "attributeValues" ) ) );
        }

        if ( schema.havePersistedProperty( "userGroupAccesses" ) )
        {
            object.getUserGroupAccesses().stream()
                .filter( userGroupAccess -> userGroupAccess.getUserGroup() != null && preheat.get( identifier, userGroupAccess.getUserGroup() ) == null )
                .forEach( attributeValue -> preheatErrorReports.add( new PreheatErrorReport( identifier, object.getClass(), ErrorCode.E5002,
                    identifier.getIdentifiersWithName( attributeValue.getUserGroup() ), identifier.getIdentifiersWithName( object ), "userGroupAccesses" ) ) );
        }

        return preheatErrorReports;
    }

    private TypeReport checkDuplicateIds( Class<? extends IdentifiableObject> klass,
        List<IdentifiableObject> persistedObjects, List<IdentifiableObject> nonPersistedObjects, Preheat preheat, PreheatIdentifier identifier )
    {
        TypeReport typeReport = new TypeReport( klass );

        if ( persistedObjects.isEmpty() && nonPersistedObjects.isEmpty() )
        {
            return typeReport;
        }

        Map<Class<?>, String> idMap = new HashMap<>();

        Iterator<IdentifiableObject> iterator = persistedObjects.iterator();
        int idx = 0;

        while ( iterator.hasNext() )
        {
            IdentifiableObject object = iterator.next();

            if ( idMap.containsKey( object.getClass() ) && idMap.get( object.getClass() ).equals( object.getUid() ) )
            {
                ErrorReport errorReport = new ErrorReport( object.getClass(), ErrorCode.E5004, object.getUid(), object.getClass() );

                ObjectReport objectReport = new ObjectReport( object.getClass(), idx );
                objectReport.setDisplayName( IdentifiableObjectUtils.getDisplayName( object ) );
                objectReport.addErrorReport( errorReport );
                typeReport.addObjectReport( objectReport );
                typeReport.getStats().incIgnored();

                iterator.remove();
            }
            else
            {
                idMap.put( object.getClass(), object.getUid() );
            }

            idx++;
        }

        iterator = nonPersistedObjects.iterator();
        idx = 0;

        while ( iterator.hasNext() )
        {
            IdentifiableObject object = iterator.next();

            if ( idMap.containsKey( object.getClass() ) && idMap.get( object.getClass() ).equals( object.getUid() ) )
            {
                ErrorReport errorReport = new ErrorReport( object.getClass(), ErrorCode.E5004, object.getUid(), object.getClass() );

                ObjectReport objectReport = new ObjectReport( object.getClass(), idx );
                objectReport.setDisplayName( IdentifiableObjectUtils.getDisplayName( object ) );
                objectReport.addErrorReport( errorReport );
                typeReport.addObjectReport( objectReport );
                typeReport.getStats().incIgnored();

                iterator.remove();
            }
            else
            {
                idMap.put( object.getClass(), object.getUid() );
            }

            idx++;
        }

        return typeReport;
    }

    private TypeReport checkUniqueness( Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, Preheat preheat, PreheatIdentifier identifier )
    {
        TypeReport typeReport = new TypeReport( klass );

        if ( objects.isEmpty() )
        {
            return typeReport;
        }

        Iterator<IdentifiableObject> iterator = objects.iterator();
        int idx = 0;

        while ( iterator.hasNext() )
        {
            IdentifiableObject object = iterator.next();
            List<ErrorReport> errorReports = new ArrayList<>();

            if ( User.class.isInstance( object ) )
            {
                User user = (User) object;
                errorReports.addAll( checkUniqueness( User.class, user, preheat, identifier ) );
                errorReports.addAll( checkUniqueness( UserCredentials.class, user.getUserCredentials(), preheat, identifier ) );
            }
            else
            {
                errorReports = checkUniqueness( klass, object, preheat, identifier );
            }


            if ( !errorReports.isEmpty() )
            {
                ObjectReport objectReport = new ObjectReport( object.getClass(), idx );
                objectReport.setDisplayName( IdentifiableObjectUtils.getDisplayName( object ) );
                objectReport.addErrorReports( errorReports );
                typeReport.addObjectReport( objectReport );
                typeReport.getStats().incIgnored();

                iterator.remove();
            }

            idx++;
        }

        return typeReport;
    }

    private List<ErrorReport> checkUniqueness( Class<? extends IdentifiableObject> klass, IdentifiableObject object, Preheat preheat, PreheatIdentifier identifier )
    {
        List<ErrorReport> errorReports = new ArrayList<>();

        if ( object == null || Preheat.isDefault( object ) ) return errorReports;

        if ( !preheat.getUniquenessMap().containsKey( object.getClass() ) )
        {
            preheat.getUniquenessMap().put( object.getClass(), new HashMap<>() );
        }

        Map<String, Map<Object, String>> uniquenessMap = preheat.getUniquenessMap().get( object.getClass() );

        Schema schema = schemaService.getDynamicSchema( object.getClass() );
        List<Property> uniqueProperties = schema.getProperties().stream()
            .filter( p -> p.isPersisted() && p.isOwner() && p.isUnique() && p.isSimple() )
            .collect( Collectors.toList() );

        uniqueProperties.forEach( property ->
        {
            if ( !uniquenessMap.containsKey( property.getName() ) )
            {
                uniquenessMap.put( property.getName(), new HashMap<>() );
            }

            Object value = ReflectionUtils.invokeMethod( object, property.getGetterMethod() );

            if ( value != null )
            {
                String persistedUid = uniquenessMap.get( property.getName() ).get( value );

                if ( persistedUid != null )
                {
                    if ( !object.getUid().equals( persistedUid ) )
                    {
                        errorReports.add( new ErrorReport( object.getClass(), ErrorCode.E5003, property.getName(), value,
                            identifier.getIdentifiersWithName( object ), persistedUid ) );
                    }
                }
                else
                {
                    uniquenessMap.get( property.getName() ).put( value, object.getUid() );
                }
            }
        } );

        return errorReports;
    }

    private TypeReport checkMandatoryAttributes( Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, Preheat preheat, PreheatIdentifier identifier )
    {
        TypeReport typeReport = new TypeReport( klass );
        Schema schema = schemaService.getDynamicSchema( klass );

        if ( objects.isEmpty() || !schema.havePersistedProperty( "attributeValues" ) )
        {
            return typeReport;
        }

        Iterator<IdentifiableObject> iterator = objects.iterator();
        int idx = 0;

        while ( iterator.hasNext() )
        {
            IdentifiableObject object = iterator.next();
            List<ErrorReport> errorReports = checkMandatoryAttributes( klass, object, preheat, identifier );

            if ( !errorReports.isEmpty() )
            {
                ObjectReport objectReport = new ObjectReport( object.getClass(), idx );
                objectReport.setDisplayName( IdentifiableObjectUtils.getDisplayName( object ) );
                objectReport.addErrorReports( errorReports );
                typeReport.addObjectReport( objectReport );
                typeReport.getStats().incIgnored();

                iterator.remove();
            }

            idx++;
        }

        return typeReport;
    }

    private List<ErrorReport> checkMandatoryAttributes( Class<? extends IdentifiableObject> klass, IdentifiableObject object, Preheat preheat, PreheatIdentifier identifier )
    {
        List<ErrorReport> errorReports = new ArrayList<>();

        if ( object == null || Preheat.isDefault( object ) || !preheat.getMandatoryAttributes().containsKey( klass ) )
        {
            return errorReports;
        }

        Set<AttributeValue> attributeValues = object.getAttributeValues();
        Set<String> mandatoryAttributes = new HashSet<>( preheat.getMandatoryAttributes().get( klass ) ); // make copy for modification

        if ( mandatoryAttributes.isEmpty() )
        {
            return errorReports;
        }

        attributeValues.forEach( attributeValue -> mandatoryAttributes.remove( attributeValue.getAttribute().getUid() ) );
        mandatoryAttributes.forEach( att -> errorReports.add( new ErrorReport( Attribute.class, ErrorCode.E4011, att ) ) );

        return errorReports;
    }

    private TypeReport checkUniqueAttributes( Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, Preheat preheat, PreheatIdentifier identifier )
    {
        TypeReport typeReport = new TypeReport( klass );
        Schema schema = schemaService.getDynamicSchema( klass );

        if ( objects.isEmpty() || !schema.havePersistedProperty( "attributeValues" ) )
        {
            return typeReport;
        }

        Iterator<IdentifiableObject> iterator = objects.iterator();
        int idx = 0;

        while ( iterator.hasNext() )
        {
            IdentifiableObject object = iterator.next();
            List<ErrorReport> errorReports = checkUniqueAttributes( klass, object, preheat, identifier );

            if ( !errorReports.isEmpty() )
            {
                ObjectReport objectReport = new ObjectReport( object.getClass(), idx );
                objectReport.setDisplayName( IdentifiableObjectUtils.getDisplayName( object ) );
                objectReport.addErrorReports( errorReports );
                typeReport.addObjectReport( objectReport );
                typeReport.getStats().incIgnored();

                iterator.remove();
            }

            idx++;
        }

        return typeReport;
    }

    private List<ErrorReport> checkUniqueAttributes( Class<? extends IdentifiableObject> klass, IdentifiableObject object, Preheat preheat, PreheatIdentifier identifier )
    {
        List<ErrorReport> errorReports = new ArrayList<>();

        if ( object == null || Preheat.isDefault( object ) || !preheat.getUniqueAttributes().containsKey( klass ) )
        {
            return errorReports;
        }

        Set<AttributeValue> attributeValues = object.getAttributeValues();
        List<String> uniqueAttributes = new ArrayList<>( preheat.getUniqueAttributes().get( klass ) ); // make copy for modification

        if ( !preheat.getUniqueAttributeValues().containsKey( klass ) )
        {
            preheat.getUniqueAttributeValues().put( klass, new HashMap<>() );
        }

        Map<String, Map<String, String>> uniqueAttributeValues = preheat.getUniqueAttributeValues().get( klass );

        if ( uniqueAttributes.isEmpty() )
        {
            return errorReports;
        }

        attributeValues.forEach( attributeValue ->
        {
            Attribute attribute = preheat.get( identifier, attributeValue.getAttribute() );

            if ( attribute == null || !attribute.isUnique() || StringUtils.isEmpty( attributeValue.getValue() ) )
            {
                return;
            }

            if ( uniqueAttributeValues.containsKey( attribute.getUid() ) )
            {
                Map<String, String> values = uniqueAttributeValues.get( attribute.getUid() );

                if ( values.containsKey( attributeValue.getValue() ) && !values.get( attributeValue.getValue() ).equals( object.getUid() ) )
                {
                    errorReports.add( new ErrorReport( Attribute.class, ErrorCode.E4009, IdentifiableObjectUtils.getDisplayName( attribute ),
                        attributeValue.getValue() ) );
                }
                else
                {
                    uniqueAttributeValues.get( attribute.getUid() ).put( attributeValue.getValue(), object.getUid() );
                }
            }
            else
            {
                uniqueAttributeValues.put( attribute.getUid(), new HashMap<>() );
                uniqueAttributeValues.get( attribute.getUid() ).put( attributeValue.getValue(), object.getUid() );
            }
        } );

        return errorReports;
    }

    private boolean skipCheck( Class<?> klass )
    {
        return klass != null && (
            UserCredentials.class.isAssignableFrom( klass ) || DataElementOperand.class.isAssignableFrom( klass )
                || Period.class.isAssignableFrom( klass ) || PeriodType.class.isAssignableFrom( klass ));
    }
}
