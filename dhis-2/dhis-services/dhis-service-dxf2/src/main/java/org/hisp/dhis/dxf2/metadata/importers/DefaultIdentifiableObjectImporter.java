package org.hisp.dhis.dxf2.metadata.importers;

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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElementCategoryDimension;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementOperandService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.metadata.ImportTypeSummary;
import org.hisp.dhis.dxf2.metadata.Importer;
import org.hisp.dhis.dxf2.metadata.ObjectBridge;
import org.hisp.dhis.dxf2.metadata.handlers.ObjectHandler;
import org.hisp.dhis.dxf2.metadata.handlers.ObjectHandlerUtils;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.predictor.Predictor;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramValidation;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.validation.SchemaValidator;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.translation.ObjectTranslation;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupAccess;
import org.hisp.dhis.user.UserGroupAccessService;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.validation.ValidationRule;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hisp.dhis.system.util.PredicateUtils.idObjectCollectionsWithScanned;
import static org.hisp.dhis.system.util.PredicateUtils.idObjects;

/**
 * Importer that can handle IdentifiableObject and NameableObject.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultIdentifiableObjectImporter<T extends BaseIdentifiableObject>
    implements Importer<T>
{
    private static final Log log = LogFactory.getLog( DefaultIdentifiableObjectImporter.class );

    //-------------------------------------------------------------------------------------------------------
    // Dependencies
    //-------------------------------------------------------------------------------------------------------

    @Autowired
    private PeriodService periodService;

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private DataElementOperandService dataElementOperandService;

    @Autowired
    private ObjectBridge objectBridge;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private AclService aclService;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private SchemaValidator schemaValidator;

    @Autowired
    private UserService userService;

    @Autowired
    private UserGroupAccessService userGroupAccessService;

    @Autowired( required = false )
    private List<ObjectHandler<T>> objectHandlers;

    //-------------------------------------------------------------------------------------------------------
    // Constructor
    //-------------------------------------------------------------------------------------------------------

    public DefaultIdentifiableObjectImporter( Class<T> importerClass )
    {
        this.importerClass = importerClass;
    }

    private final Class<T> importerClass;

    //-------------------------------------------------------------------------------------------------------
    // Importer<T> Implementation
    //-------------------------------------------------------------------------------------------------------

    @Override
    public ImportTypeSummary importObjects( User user, List<T> objects, ObjectBridge objectBridge, ImportOptions options )
    {
        this.objectBridge = objectBridge;
        this.options = options;
        this.summaryType = new ImportTypeSummary( importerClass.getSimpleName() );

        if ( objects.isEmpty() )
        {
            return summaryType;
        }

        if ( EventReport.class.isInstance( objects.get( 0 ) ) || EventChart.class.isInstance( objects.get( 0 ) ) )
        {
            return summaryType;
        }

        ObjectHandlerUtils.preObjectsHandlers( objects, objectHandlers );

        for ( T object : objects )
        {
            ObjectHandlerUtils.preObjectHandlers( object, objectHandlers );
            importObjectLocal( user, object );
            ObjectHandlerUtils.postObjectHandlers( object, objectHandlers );
        }

        ObjectHandlerUtils.postObjectsHandlers( objects, objectHandlers );

        return summaryType;
    }

    @Override
    public ImportTypeSummary importObject( User user, T object, ObjectBridge objectBridge, ImportOptions options )
    {
        this.objectBridge = objectBridge;
        this.options = options;
        this.summaryType = new ImportTypeSummary( importerClass.getSimpleName() );

        if ( object == null )
        {
            summaryType.getImportCount().incrementIgnored();
            return summaryType;
        }

        ObjectHandlerUtils.preObjectHandlers( object, objectHandlers );
        importObjectLocal( user, object );
        ObjectHandlerUtils.postObjectHandlers( object, objectHandlers );

        return summaryType;
    }

    @Override
    public boolean canHandle( Class<?> clazz )
    {
        return importerClass.equals( clazz );
    }

    //-------------------------------------------------------------------------------------------------------
    // Generic implementations of deleteObject, newObject, updatedObject
    //-------------------------------------------------------------------------------------------------------

    /**
     * Called every time a idObject is to be deleted.
     *
     * @param user            User to check
     * @param persistedObject The current version of the idObject
     * @return An ImportConflict instance if there was a conflict, otherwise null
     */
    protected boolean deleteObject( User user, T persistedObject )
    {
        if ( !aclService.canDelete( user, persistedObject ) )
        {
            summaryType.getImportConflicts().add(
                new ImportConflict( IdentifiableObjectUtils.getDisplayName( persistedObject ), "Permission denied for deletion of object " +
                    persistedObject.getUid() ) );

            log.debug( "Permission denied for deletion of object " + persistedObject.getUid() );

            return false;
        }

        log.debug( "Trying to delete object => " + IdentifiableObjectUtils.getDisplayName( persistedObject ) + " (" + persistedObject.getClass()
            .getSimpleName() + ")" );

        try
        {
            objectBridge.deleteObject( persistedObject );
        }
        catch ( Exception ex )
        {
            summaryType.getImportConflicts().add(
                new ImportConflict( IdentifiableObjectUtils.getDisplayName( persistedObject ), ex.getMessage() ) );
            return false;
        }

        log.debug( "Delete successful." );

        return true;
    }


    /**
     * Called every time a new idObject is to be imported.
     *
     * @param currentUser User to check
     * @param object      Object to import
     * @return An ImportConflict instance if there was a conflict, otherwise null
     */
    protected boolean newObject( User currentUser, T object )
    {
        if ( !aclService.canCreate( currentUser, object.getClass() ) )
        {
            summaryType.getImportConflicts().add(
                new ImportConflict( IdentifiableObjectUtils.getDisplayName( object ), "Permission denied, you are not allowed to create objects of " +
                    "type " + object.getClass() ) );

            log.debug( "Permission denied, you are not allowed to create objects of type " + object.getClass() );

            return false;
        }

        List<ErrorReport> errorReports = schemaValidator.validate( object );

        if ( !errorReports.isEmpty() )
        {
            summaryType.getImportConflicts().add(
                new ImportConflict( IdentifiableObjectUtils.getDisplayName( object ), "Validation Violations: " + errorReports ) );

            return false;
        }

        if ( User.class.isInstance( object ) )
        {
            User user = (User) object;
            errorReports = userService.validateUser( user, currentUser );

            if ( !errorReports.isEmpty() )
            {
                summaryType.getImportConflicts().add(
                    new ImportConflict( IdentifiableObjectUtils.getDisplayName( object ), "Validation Violations: " + errorReports ) );

                return false;
            }
        }

        // make sure that the internalId is 0, so that the system will generate a ID
        object.setId( 0 );

        if ( !options.isSharing() )
        {
            object.clearSharing( true );
        }

        NonIdentifiableObjects nonIdentifiableObjects = new NonIdentifiableObjects( currentUser );
        errorReports = nonIdentifiableObjects.validate( object, object );

        if ( !errorReports.isEmpty() )
        {
            summaryType.getImportConflicts().add(
                new ImportConflict( IdentifiableObjectUtils.getDisplayName( object ), "Validation Violations: " + errorReports ) );

            return false;
        }

        nonIdentifiableObjects.extract( object );

        UserCredentials userCredentials = null;

        if ( object instanceof User )
        {
            userCredentials = ((User) object).getUserCredentials();

            if ( userCredentials == null )
            {
                summaryType.getImportConflicts().add(
                    new ImportConflict( IdentifiableObjectUtils.getDisplayName( object ), "User is missing userCredentials part." ) );

                return false;
            }
        }

        Map<Field, Object> fields = detachFields( object );
        Map<Field, Collection<Object>> collectionFields = detachCollectionFields( object );

        reattachFields( object, fields, currentUser );

        log.debug( "Trying to save new object => " + IdentifiableObjectUtils.getDisplayName( object ) + " (" + object.getClass().getSimpleName() + ")" +
            "" );

        updatePeriodTypes( object );
        objectBridge.saveObject( object, !options.isSharing() );

        reattachCollectionFields( object, collectionFields, currentUser );

        objectBridge.updateObject( object );

        if ( object instanceof User && !options.isDryRun() )
        {
            userCredentials.setUserInfo( (User) object );
            userCredentials.setId( object.getId() );

            if ( userCredentials.getPassword() != null )
            {
                userService.encodeAndSetPassword( userCredentials, userCredentials.getPassword() );
            }

            Map<Field, Collection<Object>> collectionFieldsUserCredentials = detachCollectionFields( userCredentials );

            sessionFactory.getCurrentSession().save( userCredentials );

            reattachCollectionFields( userCredentials, collectionFieldsUserCredentials, currentUser );

            sessionFactory.getCurrentSession().saveOrUpdate( userCredentials );

            ((User) object).setUserCredentials( userCredentials );

            objectBridge.updateObject( object );
        }

        if ( !options.isDryRun() )
        {
            nonIdentifiableObjects.save( object );
        }

        summaryType.setLastImported( object.getUid() );

        log.debug( "Save successful." );

        return true;
    }

    /**
     * Update idObject from old => new.
     *
     * @param currentUser     User to check for access.
     * @param object          Object to import
     * @param persistedObject The current version of the idObject
     * @return An ImportConflict instance if there was a conflict, otherwise null
     */
    protected boolean updateObject( User currentUser, T object, T persistedObject )
    {
        if ( !aclService.canUpdate( currentUser, persistedObject ) )
        {
            summaryType.getImportConflicts().add(
                new ImportConflict( IdentifiableObjectUtils.getDisplayName( persistedObject ), "Permission denied for update of object " +
                    persistedObject.getUid() ) );

            log.debug( "Permission denied for update of object " + persistedObject.getUid() );

            return false;
        }

        // for now, don't support ProgramStage, ProgramValidation and EventReport for dryRun
        if ( (ProgramStage.class.isAssignableFrom( persistedObject.getClass() )
            || ProgramValidation.class.isAssignableFrom( persistedObject.getClass() )
            || EventReport.class.isAssignableFrom( persistedObject.getClass() )) && options.isDryRun() )
        {
            return true;
        }

        List<ErrorReport> errorReports = schemaValidator.validate( object );

        if ( !errorReports.isEmpty() )
        {
            summaryType.getImportConflicts().add(
                new ImportConflict( IdentifiableObjectUtils.getDisplayName( object ), "Validation Violations: " + errorReports ) );

            return false;
        }

        if ( User.class.isInstance( object ) )
        {
            User user = (User) object;
            errorReports = userService.validateUser( user, currentUser );

            if ( !errorReports.isEmpty() )
            {
                summaryType.getImportConflicts().add(
                    new ImportConflict( IdentifiableObjectUtils.getDisplayName( object ), "Validation Violations: " + errorReports ) );

                return false;
            }
        }

        NonIdentifiableObjects nonIdentifiableObjects = new NonIdentifiableObjects( currentUser );
        errorReports = nonIdentifiableObjects.validate( persistedObject, object );

        if ( !errorReports.isEmpty() )
        {
            summaryType.getImportConflicts().add(
                new ImportConflict( IdentifiableObjectUtils.getDisplayName( object ), "Validation Violations: " + errorReports ) );

            return false;
        }

        nonIdentifiableObjects.extract( object );
        nonIdentifiableObjects.delete( persistedObject );

        UserCredentials userCredentials = null;

        if ( object instanceof User )
        {
            userCredentials = ((User) object).getUserCredentials();

            if ( userCredentials == null )
            {
                summaryType.getImportConflicts().add(
                    new ImportConflict( IdentifiableObjectUtils.getDisplayName( object ), "User is missing userCredentials part." ) );

                return false;
            }
        }

        Map<Field, Object> fields = detachFields( object );
        Map<Field, Collection<Object>> collectionFields = detachCollectionFields( object );

        reattachFields( object, fields, currentUser );

        if ( !options.isDryRun() )
        {
            if ( !options.isSharing() )
            {
                User persistedObjectUser = persistedObject.getUser();
                persistedObject.mergeWith( object, options.getMergeMode() );
                // mergeService.merge( persistedObject, object, options.getMergeStrategy() );
                persistedObject.setUser( persistedObjectUser );
            }
            else
            {
                persistedObject.mergeWith( object, options.getMergeMode() );
                // mergeService.merge( persistedObject, object, options.getMergeStrategy() );
                persistedObject.mergeSharingWith( object );
            }
        }

        updatePeriodTypes( persistedObject );

        reattachCollectionFields( persistedObject, collectionFields, currentUser );

        log.debug( "Starting update of object " + IdentifiableObjectUtils.getDisplayName( persistedObject ) + " (" + persistedObject.getClass()
            .getSimpleName() + ")" );

        objectBridge.updateObject( persistedObject );

        if ( !options.isDryRun() )
        {
            if ( object instanceof User )
            {
                Map<Field, Object> fieldsUserCredentials = detachFields( userCredentials );
                Map<Field, Collection<Object>> collectionFieldsUserCredentials = detachCollectionFields( userCredentials );

                if ( userCredentials.getPassword() != null )
                {
                    userService.encodeAndSetPassword( userCredentials, userCredentials.getPassword() );
                }

                ((User) persistedObject).getUserCredentials().mergeWith( userCredentials, options.getMergeMode() );
                // mergeService.merge( ((User) persistedObject).getUserCredentials(), userCredentials, options.getMergeStrategy() );
                reattachFields( ((User) persistedObject).getUserCredentials(), fieldsUserCredentials, currentUser );
                reattachCollectionFields( ((User) persistedObject).getUserCredentials(), collectionFieldsUserCredentials, currentUser );

                sessionFactory.getCurrentSession().saveOrUpdate( ((User) persistedObject).getUserCredentials() );
            }

            nonIdentifiableObjects.save( persistedObject );
        }

        summaryType.setLastImported( object.getUid() );

        log.debug( "Update successful." );

        return true;
    }

    private void updatePeriodTypes( T object )
    {
        for ( Field field : object.getClass().getDeclaredFields() )
        {
            if ( PeriodType.class.isAssignableFrom( field.getType() ) )
            {
                PeriodType periodType = ReflectionUtils.invokeGetterMethod( field.getName(), object );

                if ( periodType != null )
                {
                    periodType = objectBridge.getObject( periodType );
                    ReflectionUtils.invokeSetterMethod( field.getName(), object, periodType );
                }
            }
        }
    }

    //-------------------------------------------------------------------------------------------------------
    // Helpers
    //-------------------------------------------------------------------------------------------------------

    private void importObjectLocal( User user, T object )
    {
        if ( validateIdentifiableObject( object ) )
        {
            startImport( user, object );
        }
        else
        {
            summaryType.incrementIgnored();
        }
    }

    private void startImport( User user, T object )
    {
        T persistedObject = objectBridge.getObject( object );

        if ( options.getImportStrategy().isCreate() )
        {
            if ( newObject( user, object ) )
            {
                summaryType.incrementImported();
            }
        }
        else if ( options.getImportStrategy().isUpdate() )
        {
            if ( updateObject( user, object, persistedObject ) )
            {
                summaryType.incrementUpdated();
            }
            else
            {
                summaryType.incrementIgnored();
            }
        }
        else if ( options.getImportStrategy().isCreateAndUpdate() )
        {
            if ( persistedObject != null )
            {
                if ( updateObject( user, object, persistedObject ) )
                {
                    summaryType.incrementUpdated();
                }
                else
                {
                    summaryType.incrementIgnored();
                }
            }
            else
            {
                if ( newObject( user, object ) )
                {
                    summaryType.incrementImported();
                }
                else
                {
                    summaryType.incrementIgnored();
                }
            }
        }
        else if ( options.getImportStrategy().isDelete() )
        {
            if ( deleteObject( user, persistedObject ) )
            {
                summaryType.incrementDeleted();
            }
            else
            {
                summaryType.incrementIgnored();
            }
        }
    }

    private boolean validateIdentifiableObject( T object )
    {
        ImportConflict conflict = null;
        boolean success = true;

        if ( options.getImportStrategy().isDelete() )
        {
            success = validateForDeleteStrategy( object );
            return success;
        }

        if ( (object.getName() == null || object.getName().length() == 0)
            && !DashboardItem.class.isInstance( object ) && !Translation.class.isInstance( object )
            && !ProgramRuleAction.class.isAssignableFrom( object.getClass() )
            && !ProgramStageDataElement.class.isInstance( object ) )
        {
            conflict = new ImportConflict( IdentifiableObjectUtils.getDisplayName( object ), "Empty name for object " + object );
        }

        if ( NameableObject.class.isInstance( object ) )
        {
            NameableObject nameableObject = (NameableObject) object;

            if ( (nameableObject.getShortName() == null || nameableObject.getShortName().length() == 0)
                // this is nasty, but we have types in the system which have shortName, but which do -not- require not-null )
                && !TrackedEntityAttribute.class.isAssignableFrom( object.getClass() )
                && !TrackedEntity.class.isAssignableFrom( object.getClass() )
                && !DataElementCategoryOption.class.isAssignableFrom( object.getClass() )
                && !CategoryOptionGroupSet.class.isAssignableFrom( object.getClass() )
                && !DashboardItem.class.isAssignableFrom( object.getClass() )
                && !ProgramStageDataElement.class.isAssignableFrom( object.getClass() )
                && !ProgramRuleAction.class.isAssignableFrom( object.getClass() )
                && !Constant.class.isAssignableFrom( object.getClass() ) )
            {
                conflict = new ImportConflict( IdentifiableObjectUtils.getDisplayName( object ), "Empty shortName for object " + object );
            }
        }

        if ( conflict != null )
        {
            summaryType.getImportConflicts().add( conflict );
        }

        if ( options.getImportStrategy().isCreate() )
        {
            success = validateForNewStrategy( object );
        }
        else if ( options.getImportStrategy().isUpdate() )
        {
            success = validateForUpdatesStrategy( object );
        }
        else if ( options.getImportStrategy().isCreateAndUpdate() )
        {
            // if we have a match on at least one of the objects, then assume update
            if ( objectBridge.getObjects( object ).size() > 0 )
            {
                success = validateForUpdatesStrategy( object );
            }
            else
            {
                success = validateForNewStrategy( object );
            }
        }

        return success;
    }

    private boolean validateForUpdatesStrategy( T object )
    {
        ImportConflict conflict = null;
        Collection<T> objects = objectBridge.getObjects( object );

        if ( objects.isEmpty() )
        {
            conflict = reportLookupConflict( object );
        }
        else if ( objects.size() > 1 )
        {
            conflict = reportMoreThanOneConflict( object );
        }

        if ( conflict != null )
        {
            summaryType.getImportConflicts().add( conflict );

            return false;
        }

        return true;
    }

    private boolean validateForNewStrategy( T object )
    {
        ImportConflict conflict;
        Collection<T> objects = objectBridge.getObjects( object );

        if ( objects.size() > 0 )
        {
            conflict = reportConflict( object );
            summaryType.getImportConflicts().add( conflict );

            return false;
        }

        return true;
    }

    private boolean validateForDeleteStrategy( T object )
    {
        ImportConflict conflict = null;
        Collection<T> objects = objectBridge.getObjects( object );

        if ( objects.isEmpty() )
        {
            conflict = reportLookupConflict( object );
        }
        else if ( objects.size() > 1 )
        {
            conflict = reportMoreThanOneConflict( object );
        }

        if ( conflict != null )
        {
            summaryType.getImportConflicts().add( conflict );

            return false;
        }

        return true;
    }

    private IdentifiableObject findObjectByReference( IdentifiableObject identifiableObject )
    {
        if ( identifiableObject == null )
        {
            return null;
        }
        else if ( Period.class.isInstance( identifiableObject ) )
        {
            Period period = (Period) identifiableObject;

            if ( !options.isDryRun() )
            {
                period = periodService.reloadPeriod( period );
                sessionFactory.getCurrentSession().flush();
            }

            return period;
        }
        else if ( DataElementOperand.class.isInstance( identifiableObject ) )
        {
            return dataElementOperandService.getDataElementOperandByUid( identifiableObject.getUid() );
        }

        return objectBridge.getObject( identifiableObject );
    }

    private Map<Field, Object> detachFields( final Object object )
    {
        final Map<Field, Object> fieldMap = Maps.newHashMap();

        if ( object == null )
        {
            return fieldMap;
        }

        final Collection<Field> fieldCollection = ReflectionUtils.collectFields( object.getClass(), idObjects );

        for ( Field field : fieldCollection )
        {
            Object ref = ReflectionUtils.invokeGetterMethod( field.getName(), object );

            if ( ref != null )
            {
                fieldMap.put( field, ref );
                ReflectionUtils.invokeSetterMethod( field.getName(), object, new Object[]{ null } );
            }
        }

        return fieldMap;
    }

    private void reattachFields( Object object, Map<Field, Object> fields, User user )
    {
        for ( Field field : fields.keySet() )
        {
            IdentifiableObject idObject = (IdentifiableObject) fields.get( field );
            IdentifiableObject reference = findObjectByReference( idObject );

            if ( reference == null )
            {
                if ( schemaService.getSchema( idObject.getClass() ) != null )
                {
                    reportReferenceError( object, idObject );
                }
            }
            else
            {
                if ( !aclService.canRead( user, reference ) )
                {
                    reportReadAccessError( object, reference );
                    reference = null;
                }
            }

            if ( !options.isDryRun() )
            {
                ReflectionUtils.invokeSetterMethod( field.getName(), object, reference );
            }
        }
    }

    private Map<Field, Collection<Object>> detachCollectionFields( final Object object )
    {
        final Map<Field, Collection<Object>> collectionFields = Maps.newHashMap();
        final Collection<Field> fieldCollection = ReflectionUtils.collectFields( object.getClass(), idObjectCollectionsWithScanned );

        for ( Field field : fieldCollection )
        {
            Collection<Object> objects = ReflectionUtils.invokeGetterMethod( field.getName(), object );

            if ( objects != null && !objects.isEmpty() )
            {
                collectionFields.put( field, objects );
                Collection<Object> emptyCollection = ReflectionUtils.newCollectionInstance( field.getType() );
                ReflectionUtils.invokeSetterMethod( field.getName(), object, emptyCollection );
            }
        }

        return collectionFields;
    }

    private void reattachCollectionFields( final Object idObject, Map<Field, Collection<Object>> collectionFields, User user )
    {
        for ( Field field : collectionFields.keySet() )
        {
            Collection<Object> collection = collectionFields.get( field );
            final Collection<Object> objects = ReflectionUtils.newCollectionInstance( field.getType() );

            for ( Object object : collection )
            {
                IdentifiableObject reference = findObjectByReference( (IdentifiableObject) object );

                if ( reference != null )
                {
                    if ( !aclService.canRead( user, reference ) )
                    {
                        reportReadAccessError( idObject, reference );
                    }
                    else
                    {
                        objects.add( reference );
                    }
                }
                else
                {
                    if ( schemaService.getSchema( idObject.getClass() ) != null ||
                        UserCredentials.class.isAssignableFrom( idObject.getClass() ) )
                    {
                        reportReferenceError( idObject, object );
                    }
                }
            }

            if ( !options.isDryRun() )
            {
                ReflectionUtils.invokeSetterMethod( field.getName(), idObject, objects );
            }
        }
    }

    private ImportConflict reportLookupConflict( IdentifiableObject object )
    {
        return new ImportConflict( IdentifiableObjectUtils.getDisplayName( object ), "Object does not exist." );
    }

    private ImportConflict reportMoreThanOneConflict( IdentifiableObject object )
    {
        return new ImportConflict( IdentifiableObjectUtils.getDisplayName( object ), "More than one object matches identifiers." );
    }

    private ImportConflict reportConflict( IdentifiableObject object )
    {
        return new ImportConflict( IdentifiableObjectUtils.getDisplayName( object ), "Object already exists." );
    }

    public String identifiableObjectToString( Object object )
    {
        if ( IdentifiableObject.class.isInstance( object ) )
        {
            IdentifiableObject identifiableObject = (IdentifiableObject) object;

            return "IdentifiableObject{" +
                "id=" + identifiableObject.getId() +
                ", uid='" + identifiableObject.getUid() + '\'' +
                ", code='" + identifiableObject.getCode() + '\'' +
                ", name='" + identifiableObject.getName() + '\'' +
                ", created=" + identifiableObject.getCreated() +
                ", lastUpdated=" + identifiableObject.getLastUpdated() +
                '}';
        }

        return object != null ? object.toString() : "object is null";
    }

    private void reportReferenceError( Object object, Object reference )
    {
        if ( UserCredentials.class.isInstance( object ) || UserCredentials.class.isInstance( reference ) )
        {
            return;
        }

        String objectName = object != null ? object.getClass().getSimpleName() : "null";
        String referenceName = reference != null ? reference.getClass().getSimpleName() : "null";

        String logMsg = "Unknown reference to " + identifiableObjectToString( reference ) + " (" + referenceName + ")" +
            " on object " + identifiableObjectToString( object ) + " (" + objectName + ").";

        log.debug( logMsg );

        ImportConflict importConflict = new ImportConflict( IdentifiableObjectUtils.getDisplayName( object ), logMsg );
        summaryType.getImportConflicts().add( importConflict );
    }

    private void reportReadAccessError( Object object, Object reference )
    {
        String objectName = object != null ? object.getClass().getSimpleName() : "null";
        String referenceName = reference != null ? reference.getClass().getSimpleName() : "null";

        String logMsg = "User does not have read access to " + identifiableObjectToString( reference ) + " (" + referenceName + ")" +
            " on object " + identifiableObjectToString( object ) + " (" + objectName + ").";

        log.debug( logMsg );

        ImportConflict importConflict = new ImportConflict( IdentifiableObjectUtils.getDisplayName( object ), logMsg );
        summaryType.getImportConflicts().add( importConflict );
    }

    //-------------------------------------------------------------------------------------------------------
    // Internal state
    //-------------------------------------------------------------------------------------------------------

    protected ImportTypeSummary summaryType;

    protected ImportOptions options;

    // keeping this internal for now, might be split into several classes
    private class NonIdentifiableObjects
    {
        private Set<AttributeValue> attributeValues = new HashSet<>();
        private Set<UserGroupAccess> userGroupAccesses = new HashSet<>();

        private Expression leftSide;
        private Expression rightSide;
        private Expression sampleSkipTest;
        private Expression generator;

        private Set<DataElementOperand> compulsoryDataElementOperands = new HashSet<>();
        private Set<DataElementOperand> greyedFields = new HashSet<>();
        private List<DataElementOperand> dataElementOperands = new ArrayList<>();
        private List<DataElementCategoryDimension> categoryDimensions = new ArrayList<>();
        private List<DataDimensionItem> dataDimensionItems = new ArrayList<>();
        private List<ObjectTranslation> translations = new ArrayList<>();

        private User user;

        private Schema schema;

        private NonIdentifiableObjects( User user )
        {
            this.user = user;
        }

        public List<ErrorReport> validate( T persistedObject, T object )
        {
            schema = schemaService.getDynamicSchema( object.getClass() );
            List<ErrorReport> errorReports = new ArrayList<>();

            if ( schema.havePersistedProperty( "attributeValues" ) )
            {
                for ( AttributeValue attributeValue : object.getAttributeValues() )
                {
                    Attribute attribute = objectBridge.getObject( attributeValue.getAttribute() );

                    if ( attribute == null )
                    {
                        errorReports.add( new ErrorReport( Attribute.class, ErrorCode.E5002, attributeValue.getAttribute().getUid(),
                            object.getUid(), "attributeValues" ) );
                    }

                    attributeValue.setAttribute( attribute );
                }

                errorReports.addAll( attributeService.validateAttributeValues( persistedObject, object.getAttributeValues() ) );
            }

            return errorReports;
        }

        public void extract( T object )
        {
            schema = schemaService.getDynamicSchema( object.getClass() );
            attributeValues = extractAttributeValues( object );
            userGroupAccesses = extractUserGroupAccesses( object );
            leftSide = extractExpression( object, "leftSide" );
            rightSide = extractExpression( object, "rightSide" );
            sampleSkipTest = extractExpression( object, "sampleSkipTest" );
            generator = extractExpression( object, "generator" );
            compulsoryDataElementOperands = Sets.newHashSet( extractDataElementOperands( object, "compulsoryDataElementOperands" ) );
            greyedFields = Sets.newHashSet( extractDataElementOperands( object, "greyedFields" ) );
            dataElementOperands = Lists.newArrayList( extractDataElementOperands( object, "dataElementOperands" ) );
            categoryDimensions = extractCategoryDimensions( object );
            dataDimensionItems = extractDataDimensionItems( object );
            translations = extractTranslations( object );
        }

        private List<ObjectTranslation> extractTranslations( T object )
        {
            List<ObjectTranslation> translations = new ArrayList<>( object.getTranslations() );
            object.getTranslations().clear();

            return translations;
        }

        public void delete( T object )
        {
            schema = schemaService.getDynamicSchema( object.getClass() );

            if ( !options.isDryRun() )
            {
                deleteExpression( object, "leftSide" );
                deleteExpression( object, "rightSide" );
                deleteExpression( object, "sampleSkipTest" );
                deleteExpression( object, "generator" );

                if ( options.getImportStrategy().isDelete() )
                {
                    deleteDataElementOperands( object, "compulsoryDataElementOperands" );
                    deleteDataElementOperands( object, "dataElementOperands" );
                    deleteDataElementOperands( object, "greyedFields" );
                }
            }
        }

        public void save( T object )
        {
            schema = schemaService.getDynamicSchema( object.getClass() );

            saveAttributeValues( object, attributeValues );
            saveUserGroupAccess( object, userGroupAccesses );
            saveExpression( object, "leftSide", leftSide );
            saveExpression( object, "rightSide", rightSide );
            saveExpression( object, "sampleSkipTest", sampleSkipTest );
            saveExpression( object, "generator", generator );
            saveDataElementOperands( object, "compulsoryDataElementOperands", compulsoryDataElementOperands );
            saveDataElementOperands( object, "greyedFields", greyedFields );
            saveDataElementOperands( object, "dataElementOperands", dataElementOperands );
            saveCategoryDimensions( object, categoryDimensions );
            saveDataDimensionItems( object, dataDimensionItems );
            saveTranslations( object, translations );
        }

        private void saveTranslations( T object, List<ObjectTranslation> translations )
        {
            translations.forEach( objectTranslation ->
            {
                sessionFactory.getCurrentSession().saveOrUpdate( objectTranslation );
                object.getTranslations().add( objectTranslation );
            } );
        }

        private Expression extractExpression( T object, String fieldName )
        {
            if ( (!ValidationRule.class.isInstance( object )) &&
                (!Predictor.class.isInstance( object )) )
            {
                return null;
            }

            Expression expression = null;

            if ( ReflectionUtils.findGetterMethod( fieldName, object ) != null )
            {
                expression = ReflectionUtils.invokeGetterMethod( fieldName, object );

                if ( expression != null && Expression.class.isAssignableFrom( expression.getClass() ) )
                {
                    ReflectionUtils.invokeSetterMethod( fieldName, object, new Object[]{ null } );
                }
            }

            return expression;
        }

        private Collection<DataElementOperand> extractDataElementOperands( T object, String fieldName )
        {
            Collection<DataElementOperand> dataElementOperands = Sets.newHashSet();

            if ( ReflectionUtils.findGetterMethod( fieldName, object ) != null )
            {
                Collection<DataElementOperand> detachedDataElementOperands = ReflectionUtils.invokeGetterMethod( fieldName, object );

                dataElementOperands = ReflectionUtils.newCollectionInstance( detachedDataElementOperands.getClass() );
                dataElementOperands.addAll( detachedDataElementOperands );
                detachedDataElementOperands.clear();
            }

            return dataElementOperands;
        }

        private List<DataElementCategoryDimension> extractCategoryDimensions( T object )
        {
            List<DataElementCategoryDimension> dataElementCategoryDimensions = new ArrayList<>();
            Method getterMethod = ReflectionUtils.findGetterMethod( "categoryDimensions", object );

            if ( getterMethod != null )
            {
                List<DataElementCategoryDimension> detachedCategoryDimensions = ReflectionUtils.invokeMethod( object, getterMethod );
                dataElementCategoryDimensions.addAll( detachedCategoryDimensions );

                if ( !options.isDryRun() )
                {
                    detachedCategoryDimensions.clear();
                }
            }

            return dataElementCategoryDimensions;
        }

        private void saveCategoryDimensions( T object, List<DataElementCategoryDimension> categoryDimensions )
        {
            Method getterMethod = ReflectionUtils.findGetterMethod( "categoryDimensions", object );

            if ( categoryDimensions != null && !categoryDimensions.isEmpty() && getterMethod != null )
            {
                List<DataElementCategoryDimension> detachedCategoryDimensions = ReflectionUtils.invokeMethod( object, getterMethod );

                for ( DataElementCategoryDimension categoryDimension : categoryDimensions )
                {
                    Map<Field, Object> detachFields = detachFields( categoryDimension );
                    reattachFields( categoryDimension, detachFields, user );

                    Map<Field, Collection<Object>> detachCollectionFields = detachCollectionFields( categoryDimension );
                    reattachCollectionFields( categoryDimension, detachCollectionFields, user );

                    if ( !options.isDryRun() )
                    {
                        categoryDimension.setId( 0 );
                        detachedCategoryDimensions.add( categoryDimension );
                    }
                }

            }
        }

        private void saveExpression( T object, String fieldName, Expression expression )
        {
            if ( expression != null )
            {
                Map<Field, Collection<Object>> identifiableObjectCollections = detachCollectionFields( expression );
                reattachCollectionFields( expression, identifiableObjectCollections, user );

                expression.setId( 0 );
                expressionService.addExpression( expression );

                ReflectionUtils.invokeSetterMethod( fieldName, object, expression );
            }
        }

        private void saveDataElementOperands( T object, String fieldName, Collection<DataElementOperand> dataElementOperands )
        {
            Collection<DataElementOperand> detachedDataElementOperands = ReflectionUtils.invokeGetterMethod( fieldName, object );

            if ( detachedDataElementOperands == null )
            {
                return;
            }

            for ( DataElementOperand dataElementOperand : dataElementOperands )
            {
                Map<Field, Object> identifiableObjects = detachFields( dataElementOperand );
                reattachFields( dataElementOperand, identifiableObjects, user );

                dataElementOperand.setId( 0 );
                dataElementOperandService.addDataElementOperand( dataElementOperand );
            }

            detachedDataElementOperands.clear();
            detachedDataElementOperands.addAll( dataElementOperands );
            sessionFactory.getCurrentSession().flush();
        }

        private Set<AttributeValue> extractAttributeValues( T object )
        {
            Set<AttributeValue> attributeValues = new HashSet<>();

            if ( schema.havePersistedProperty( "attributeValues" ) )
            {
                attributeValues = object.getAttributeValues();
                object.setAttributeValues( new HashSet<>() );
            }

            return attributeValues;
        }

        private Set<UserGroupAccess> extractUserGroupAccesses( T object )
        {
            userGroupAccesses = object.getUserGroupAccesses();
            object.setUserGroupAccesses( new HashSet<>() );

            return userGroupAccesses;
        }

        private void saveAttributeValues( T object, Collection<AttributeValue> attributeValues )
        {
            for ( AttributeValue attributeValue : attributeValues )
            {
                Attribute attribute = objectBridge.getObject( attributeValue.getAttribute() );

                if ( attribute == null )
                {
                    log.debug( "Unknown reference to " + attributeValue.getAttribute() + " on object " + attributeValue );
                    return;
                }

                attributeValue.setAttribute( attribute );
            }

            try
            {
                attributeService.updateAttributeValues( object, new HashSet<>( attributeValues ) );
            }
            catch ( Exception ex )
            {
                log.info( ex.getMessage() );
            }
        }

        private void saveUserGroupAccess( T object, Set<UserGroupAccess> userGroupAccesses )
        {
            object.getUserGroupAccesses().clear();

            for ( UserGroupAccess uga : userGroupAccesses )
            {
                UserGroup userGroup = objectBridge.getObject( uga.getUserGroup() );

                if ( userGroup == null )
                {
                    log.info( "Unknown reference to " + uga.getUserGroup() + " on object " + uga );
                    return;
                }

                uga.setUserGroup( userGroup );
                userGroupAccessService.addUserGroupAccess( uga );
                object.getUserGroupAccesses().add( uga );
            }
        }

        private void deleteExpression( T object, String fieldName )
        {
            Expression expression = extractExpression( object, fieldName );

            if ( expression != null )
            {
                expressionService.deleteExpression( expression );
            }
        }

        private void deleteDataElementOperands( T object, String fieldName )
        {
            Collection<DataElementOperand> dataElementOperands = extractDataElementOperands( object, fieldName );
            dataElementOperands.forEach( dataElementOperandService::deleteDataElementOperand );
        }

        private List<DataDimensionItem> extractDataDimensionItems( T object )
        {
            List<DataDimensionItem> dataDimensionItems = new ArrayList<>();

            if ( BaseAnalyticalObject.class.isInstance( object ) )
            {
                BaseAnalyticalObject analyticalObject = (BaseAnalyticalObject) object;
                dataDimensionItems = new ArrayList<>( analyticalObject.getDataDimensionItems() );
                analyticalObject.getDataDimensionItems().clear();
            }

            return dataDimensionItems;
        }

        private void saveDataDimensionItems( T object, Collection<DataDimensionItem> dataDimensionItems )
        {
            if ( BaseAnalyticalObject.class.isInstance( object ) )
            {
                BaseAnalyticalObject analyticalObject = (BaseAnalyticalObject) object;

                for ( DataDimensionItem dataDimensionItem : dataDimensionItems )
                {
                    Map<Field, Object> identifiableObjects = detachFields( dataDimensionItem );
                    reattachFields( dataDimensionItem, identifiableObjects, user );

                    if ( dataDimensionItem.getDataElementOperand() != null )
                    {
                        dataDimensionItem.getDataElementOperand().setId( 0 );
                        dataElementOperandService.addDataElementOperand( dataDimensionItem.getDataElementOperand() );
                    }

                    analyticalObject.getDataDimensionItems().add( dataDimensionItem );
                }
            }
        }
    }
}
