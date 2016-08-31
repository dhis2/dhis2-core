package org.hisp.dhis.preheat;

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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.dataelement.DataElementCategoryDimension;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeDimension;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.hisp.dhis.trackedentity.TrackedEntityProgramIndicatorDimension;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.validation.ValidationRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
@Transactional
public class DefaultPreheatService implements PreheatService
{
    private static final Log log = LogFactory.getLog( DefaultPreheatService.class );

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private QueryService queryService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private PeriodStore periodStore;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private AttributeService attributeService;

    @Override
    @SuppressWarnings( "unchecked" )
    public Preheat preheat( PreheatParams params )
    {
        Timer timer = new SystemTimer().start();

        Preheat preheat = new Preheat();
        preheat.setUser( params.getUser() );
        preheat.setDefaults( manager.getDefaults() );
        preheat.setUsernames( getUsernames() );

        if ( preheat.getUser() == null )
        {
            preheat.setUser( currentUserService.getCurrentUser() );
        }

        for ( Class<? extends IdentifiableObject> klass : params.getObjects().keySet() )
        {
            params.getObjects().get( klass ).stream()
                .filter( identifiableObject -> StringUtils.isEmpty( identifiableObject.getUid() ) )
                .forEach( identifiableObject -> ((BaseIdentifiableObject) identifiableObject).setUid( CodeGenerator.generateCode() ) );
        }

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> uniqueCollectionMap = new HashMap<>();
        Set<Class<? extends IdentifiableObject>> klasses = new HashSet<>( params.getObjects().keySet() );

        if ( PreheatMode.ALL == params.getPreheatMode() )
        {
            if ( params.getClasses().isEmpty() )
            {
                schemaService.getMetadataSchemas().stream().filter( Schema::isIdentifiableObject )
                    .forEach( schema -> params.getClasses().add( (Class<? extends IdentifiableObject>) schema.getKlass() ) );
            }

            for ( Class<? extends IdentifiableObject> klass : params.getClasses() )
            {
                Query query = Query.from( schemaService.getDynamicSchema( klass ) );
                query.setUser( preheat.getUser() );
                List<? extends IdentifiableObject> objects = queryService.query( query );

                if ( PreheatIdentifier.UID == params.getPreheatIdentifier() || PreheatIdentifier.AUTO == params.getPreheatIdentifier() )
                {
                    preheat.put( PreheatIdentifier.UID, objects );
                }

                if ( PreheatIdentifier.CODE == params.getPreheatIdentifier() || PreheatIdentifier.AUTO == params.getPreheatIdentifier() )
                {
                    preheat.put( PreheatIdentifier.CODE, objects );
                }

                if ( klasses.contains( klass ) && !objects.isEmpty() )
                {
                    uniqueCollectionMap.put( klass, new ArrayList<>( objects ) );
                }
            }
        }
        else if ( PreheatMode.REFERENCE == params.getPreheatMode() )
        {
            Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Set<String>>> references = collectReferences( params.getObjects() );

            Map<Class<? extends IdentifiableObject>, Set<String>> uidMap = references.get( PreheatIdentifier.UID );
            Map<Class<? extends IdentifiableObject>, Set<String>> codeMap = references.get( PreheatIdentifier.CODE );

            if ( uidMap != null && (PreheatIdentifier.UID == params.getPreheatIdentifier() || PreheatIdentifier.AUTO == params.getPreheatIdentifier()) )
            {
                for ( Class<? extends IdentifiableObject> klass : uidMap.keySet() )
                {
                    Collection<String> identifiers = uidMap.get( klass );

                    if ( !identifiers.isEmpty() )
                    {
                        Query query = Query.from( schemaService.getDynamicSchema( klass ) );
                        query.setUser( preheat.getUser() );
                        query.add( Restrictions.in( "id", identifiers ) );
                        List<? extends IdentifiableObject> objects = queryService.query( query );
                        preheat.put( PreheatIdentifier.UID, objects );
                    }
                }
            }

            if ( codeMap != null && (PreheatIdentifier.CODE == params.getPreheatIdentifier() || PreheatIdentifier.AUTO == params.getPreheatIdentifier()) )
            {
                for ( Class<? extends IdentifiableObject> klass : codeMap.keySet() )
                {
                    Collection<String> identifiers = codeMap.get( klass );

                    if ( !identifiers.isEmpty() )
                    {
                        Query query = Query.from( schemaService.getDynamicSchema( klass ) );
                        query.setUser( preheat.getUser() );
                        query.add( Restrictions.in( "code", identifiers ) );
                        List<? extends IdentifiableObject> objects = queryService.query( query );
                        preheat.put( PreheatIdentifier.CODE, objects );
                    }
                }
            }

            for ( Class<? extends IdentifiableObject> klass : klasses )
            {
                Query query = Query.from( schemaService.getDynamicSchema( klass ) );
                query.setUser( preheat.getUser() );
                List<? extends IdentifiableObject> objects = queryService.query( query );

                if ( !objects.isEmpty() )
                {
                    uniqueCollectionMap.put( klass, new ArrayList<>( objects ) );
                }
            }
        }

        if ( uniqueCollectionMap.containsKey( User.class ) )
        {
            List<IdentifiableObject> userCredentials = new ArrayList<>();

            for ( IdentifiableObject identifiableObject : uniqueCollectionMap.get( User.class ) )
            {
                User user = (User) identifiableObject;

                if ( user.getUserCredentials() != null )
                {
                    userCredentials.add( user.getUserCredentials() );
                }
            }

            uniqueCollectionMap.put( UserCredentials.class, userCredentials );
        }

        preheat.setUniquenessMap( collectUniqueness( uniqueCollectionMap ) );

        // add preheat placeholders for objects that will be created and set mandatory/unique attributes
        for ( Class<? extends IdentifiableObject> klass : params.getObjects().keySet() )
        {
            List<IdentifiableObject> objects = params.getObjects().get( klass );
            preheat.put( params.getPreheatIdentifier(), objects );
        }

        handleAttributes( params.getObjects(), preheat );

        periodStore.getAll().forEach( period -> preheat.getPeriodMap().put( period.getName(), period ) );
        periodStore.getAllPeriodTypes().forEach( periodType -> preheat.getPeriodTypeMap().put( periodType.getName(), periodType ) );

        log.info( "(" + preheat.getUsername() + ") Import:Preheat[" + params.getPreheatMode() + "] took " + timer.toString() );

        return preheat;
    }

    private void handleAttributes( Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects, Preheat preheat )
    {
        for ( Class<? extends IdentifiableObject> klass : objects.keySet() )
        {
            List<Attribute> mandatoryAttributes = attributeService.getMandatoryAttributes( klass );

            if ( !mandatoryAttributes.isEmpty() )
            {
                preheat.getMandatoryAttributes().put( klass, new HashSet<>() );
            }

            mandatoryAttributes.forEach( attribute -> preheat.getMandatoryAttributes().get( klass ).add( attribute.getUid() ) );

            List<Attribute> uniqueAttributes = attributeService.getUniqueAttributes( klass );

            if ( !uniqueAttributes.isEmpty() )
            {
                preheat.getUniqueAttributes().put( klass, new HashSet<>() );
            }

            uniqueAttributes.forEach( attribute -> preheat.getUniqueAttributes().get( klass ).add( attribute.getUid() ) );

            List<? extends IdentifiableObject> uniqueAttributeValues = manager.getAllByAttributes( klass, uniqueAttributes );
            handleUniqueAttributeValues( klass, uniqueAttributeValues, preheat );
        }

        if ( objects.containsKey( Attribute.class ) )
        {
            List<IdentifiableObject> attributes = objects.get( Attribute.class );

            for ( IdentifiableObject identifiableObject : attributes )
            {
                Attribute attribute = (Attribute) identifiableObject;

                if ( attribute.isMandatory() )
                {
                    attribute.getSupportedClasses().forEach( klass -> {
                        if ( !preheat.getMandatoryAttributes().containsKey( klass ) ) preheat.getMandatoryAttributes().put( klass, new HashSet<>() );
                        preheat.getMandatoryAttributes().get( klass ).add( attribute.getUid() );
                    } );
                }

                if ( attribute.isUnique() )
                {
                    attribute.getSupportedClasses().forEach( klass -> {
                        if ( !preheat.getUniqueAttributes().containsKey( klass ) ) preheat.getUniqueAttributes().put( klass, new HashSet<>() );
                        preheat.getUniqueAttributes().get( klass ).add( attribute.getUid() );
                    } );
                }
            }
        }
    }

    private void handleUniqueAttributeValues( Class<? extends IdentifiableObject> klass, List<? extends IdentifiableObject> objects, Preheat preheat )
    {
        if ( objects.isEmpty() )
        {
            return;
        }

        preheat.getUniqueAttributeValues().put( klass, new HashMap<>() );

        objects.forEach( object -> {
            object.getAttributeValues().forEach( attributeValue -> {
                Set<String> uids = preheat.getUniqueAttributes().get( klass );

                if ( uids != null && uids.contains( attributeValue.getAttribute().getUid() ) )
                {
                    if ( !preheat.getUniqueAttributeValues().get( klass ).containsKey( attributeValue.getAttribute().getUid() ) )
                    {
                        preheat.getUniqueAttributeValues().get( klass ).put( attributeValue.getAttribute().getUid(), new HashMap<>() );
                    }

                    preheat.getUniqueAttributeValues().get( klass ).get( attributeValue.getAttribute().getUid() )
                        .put( attributeValue.getValue(), object.getUid() );
                }
            } );
        } );
    }

    @Override
    public void validate( PreheatParams params ) throws PreheatException
    {
        if ( PreheatMode.ALL == params.getPreheatMode() || PreheatMode.NONE == params.getPreheatMode() )
        {
            // nothing to validate for now, if classes is empty it will get all metadata classes
        }
        else if ( PreheatMode.REFERENCE == params.getPreheatMode() )
        {
            if ( params.getObjects().isEmpty() )
            {
                throw new PreheatException( "PreheatMode.REFERENCE, but no objects were provided." );
            }
        }
        else
        {
            throw new PreheatException( "Invalid preheat mode." );
        }
    }

    @Override
    public Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Set<String>>> collectReferences( IdentifiableObject object )
    {
        if ( object == null )
        {
            return new HashMap<>();
        }

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> map = new HashMap<>();
        map.put( object.getClass(), Lists.newArrayList( (IdentifiableObject) object ) );

        return collectReferences( map );
    }

    @Override
    public Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Set<String>>> collectReferences( Collection<IdentifiableObject> objects )
    {
        if ( objects == null || objects.isEmpty() )
        {
            return new HashMap<>();
        }

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> map = new HashMap<>();
        map.put( objects.iterator().next().getClass(), Lists.newArrayList( objects ) );

        return collectReferences( map );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Set<String>>> collectReferences( Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects )
    {
        Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Set<String>>> map = new HashMap<>();

        map.put( PreheatIdentifier.UID, new HashMap<>() );
        map.put( PreheatIdentifier.CODE, new HashMap<>() );

        Map<Class<? extends IdentifiableObject>, Set<String>> uidMap = map.get( PreheatIdentifier.UID );
        Map<Class<? extends IdentifiableObject>, Set<String>> codeMap = map.get( PreheatIdentifier.CODE );

        if ( objects.isEmpty() )
        {
            return map;
        }

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> scanObjects = new HashMap<>();
        scanObjects.putAll( objects ); // clone objects list, we don't want to modify it

        if ( scanObjects.containsKey( User.class ) )
        {
            List<IdentifiableObject> users = scanObjects.get( User.class );
            List<IdentifiableObject> userCredentials = new ArrayList<>();

            for ( IdentifiableObject identifiableObject : users )
            {
                User user = (User) identifiableObject;

                if ( user.getUserCredentials() != null )
                {
                    userCredentials.add( user.getUserCredentials() );
                }
            }

            scanObjects.put( UserCredentials.class, userCredentials );
        }

        for ( Class<? extends IdentifiableObject> objectClass : scanObjects.keySet() )
        {
            Schema schema = schemaService.getDynamicSchema( objectClass );
            List<Property> identifiableProperties = schema.getProperties().stream()
                .filter( p -> p.isPersisted() && p.isOwner() && (PropertyType.REFERENCE == p.getPropertyType() || PropertyType.REFERENCE == p.getItemPropertyType()) )
                .collect( Collectors.toList() );

            List<IdentifiableObject> identifiableObjects = scanObjects.get( objectClass );

            if ( !uidMap.containsKey( objectClass ) ) uidMap.put( objectClass, new HashSet<>() );
            if ( !codeMap.containsKey( objectClass ) ) codeMap.put( objectClass, new HashSet<>() );

            for ( IdentifiableObject object : identifiableObjects )
            {
                identifiableProperties.forEach( p -> {
                    if ( !p.isCollection() )
                    {
                        Class<? extends IdentifiableObject> klass = (Class<? extends IdentifiableObject>) p.getKlass();

                        if ( !uidMap.containsKey( klass ) ) uidMap.put( klass, new HashSet<>() );
                        if ( !codeMap.containsKey( klass ) ) codeMap.put( klass, new HashSet<>() );

                        Object reference = ReflectionUtils.invokeMethod( object, p.getGetterMethod() );

                        if ( reference != null )
                        {
                            IdentifiableObject identifiableObject = (IdentifiableObject) reference;
                            addIdentifiers( map, identifiableObject );
                        }
                    }
                    else
                    {
                        Collection<IdentifiableObject> reference = ReflectionUtils.invokeMethod( object, p.getGetterMethod() );
                        reference.forEach( identifiableObject -> addIdentifiers( map, identifiableObject ) );
                    }
                } );

                if ( AnalyticalObject.class.isInstance( object ) )
                {
                    BaseAnalyticalObject analyticalObject = (BaseAnalyticalObject) object;
                    List<DataDimensionItem> dataDimensionItems = analyticalObject.getDataDimensionItems();
                    List<DataElementCategoryDimension> categoryDimensions = analyticalObject.getCategoryDimensions();
                    List<TrackedEntityDataElementDimension> dataElementDimensions = analyticalObject.getDataElementDimensions();
                    List<TrackedEntityAttributeDimension> attributeDimensions = analyticalObject.getAttributeDimensions();
                    List<TrackedEntityProgramIndicatorDimension> programIndicatorDimensions = analyticalObject.getProgramIndicatorDimensions();

                    dataDimensionItems.forEach( dataDimensionItem -> {
                        addIdentifiers( map, dataDimensionItem.getIndicator() );
                        addIdentifiers( map, dataDimensionItem.getDataElement() );
                        addIdentifiers( map, dataDimensionItem.getReportingRate() );
                        addIdentifiers( map, dataDimensionItem.getProgramDataElement() );
                        addIdentifiers( map, dataDimensionItem.getProgramAttribute() );

                        if ( dataDimensionItem.getDataElementOperand() != null )
                        {
                            addIdentifiers( map, dataDimensionItem.getDataElementOperand().getDataElement() );
                            addIdentifiers( map, dataDimensionItem.getDataElementOperand().getCategoryOptionCombo() );
                        }
                    } );

                    categoryDimensions.forEach( categoryDimension -> {
                        addIdentifiers( map, categoryDimension.getDimension() );
                        categoryDimension.getItems().forEach( item -> addIdentifiers( map, item ) );
                    } );

                    dataElementDimensions.forEach( trackedEntityDataElementDimension -> {
                        addIdentifiers( map, trackedEntityDataElementDimension.getDataElement() );
                        addIdentifiers( map, trackedEntityDataElementDimension.getLegendSet() );
                    } );

                    attributeDimensions.forEach( trackedEntityAttributeDimension -> {
                        addIdentifiers( map, trackedEntityAttributeDimension.getAttribute() );
                        addIdentifiers( map, trackedEntityAttributeDimension.getLegendSet() );
                    } );

                    programIndicatorDimensions.forEach( programIndicatorDimension -> {
                        addIdentifiers( map, programIndicatorDimension.getProgramIndicator() );
                        addIdentifiers( map, programIndicatorDimension.getLegendSet() );
                    } );
                }

                if ( ValidationRule.class.isInstance( object ) )
                {
                    ValidationRule validationRule = (ValidationRule) object;

                    if ( validationRule.getLeftSide() != null && !validationRule.getLeftSide().getDataElementsInExpression().isEmpty() )
                    {
                        validationRule.getLeftSide().getDataElementsInExpression().forEach( de -> addIdentifiers( map, de ) );
                    }

                    if ( validationRule.getRightSide() != null && !validationRule.getRightSide().getDataElementsInExpression().isEmpty() )
                    {
                        validationRule.getRightSide().getDataElementsInExpression().forEach( de -> addIdentifiers( map, de ) );
                    }
                }

                object.getAttributeValues().forEach( av -> addIdentifiers( map, av.getAttribute() ) );
                object.getUserGroupAccesses().forEach( uga -> addIdentifiers( map, uga.getUserGroup() ) );

                addIdentifiers( map, object );
            }
        }

        cleanEmptyEntries( uidMap );
        cleanEmptyEntries( codeMap );

        return map;
    }

    @Override
    public Map<Class<? extends IdentifiableObject>, Map<String, Map<Object, String>>> collectUniqueness( Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects )
    {
        Map<Class<? extends IdentifiableObject>, Map<String, Map<Object, String>>> uniqueMap = new HashMap<>();

        if ( objects.isEmpty() )
        {
            return uniqueMap;
        }

        for ( Class<? extends IdentifiableObject> objectClass : objects.keySet() )
        {
            Schema schema = schemaService.getDynamicSchema( objectClass );
            List<IdentifiableObject> identifiableObjects = objects.get( objectClass );
            uniqueMap.put( objectClass, handleUniqueProperties( schema, identifiableObjects ) );
        }

        return uniqueMap;
    }

    @Override
    public Map<Class<?>, Map<String, Map<String, Object>>> collectObjectReferences( Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects )
    {
        Map<Class<?>, Map<String, Map<String, Object>>> refs = new HashMap<>();

        if ( objects.isEmpty() )
        {
            return refs;
        }

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> scanObjects = new HashMap<>();
        scanObjects.putAll( objects ); // clone objects list, we don't want to modify it

        if ( scanObjects.containsKey( User.class ) )
        {
            List<IdentifiableObject> users = scanObjects.get( User.class );
            List<IdentifiableObject> userCredentials = new ArrayList<>();

            for ( IdentifiableObject identifiableObject : users )
            {
                User user = (User) identifiableObject;

                if ( user.getUserCredentials() != null )
                {
                    userCredentials.add( user.getUserCredentials() );
                }
            }

            scanObjects.put( UserCredentials.class, userCredentials );
        }

        for ( Class<? extends IdentifiableObject> objectClass : scanObjects.keySet() )
        {
            Schema schema = schemaService.getDynamicSchema( objectClass );
            List<Property> properties = schema.getProperties().stream()
                .filter( p -> p.isPersisted() && p.isOwner() && (PropertyType.REFERENCE == p.getPropertyType() || PropertyType.REFERENCE == p.getItemPropertyType()) )
                .collect( Collectors.toList() );

            List<IdentifiableObject> identifiableObjects = scanObjects.get( objectClass );
            Map<String, Map<String, Object>> objectReferenceMap = new HashMap<>();
            refs.put( objectClass, objectReferenceMap );

            for ( IdentifiableObject object : identifiableObjects )
            {
                objectReferenceMap.put( object.getUid(), new HashMap<>() );

                properties.forEach( p -> {
                    if ( !p.isCollection() )
                    {
                        IdentifiableObject reference = ReflectionUtils.invokeMethod( object, p.getGetterMethod() );

                        if ( reference != null )
                        {
                            try
                            {
                                IdentifiableObject identifiableObject = (IdentifiableObject) p.getKlass().newInstance();
                                identifiableObject.mergeWith( reference, MergeMode.REPLACE );
                                objectReferenceMap.get( object.getUid() ).put( p.getName(), identifiableObject );
                            }
                            catch ( InstantiationException | IllegalAccessException ignored )
                            {
                            }
                        }
                    }
                    else
                    {
                        Collection<IdentifiableObject> refObjects = ReflectionUtils.newCollectionInstance( p.getKlass() );
                        Collection<IdentifiableObject> references = ReflectionUtils.invokeMethod( object, p.getGetterMethod() );

                        if ( references != null )
                        {
                            for ( IdentifiableObject reference : references )
                            {
                                try
                                {
                                    IdentifiableObject identifiableObject = (IdentifiableObject) p.getItemKlass().newInstance();
                                    identifiableObject.mergeWith( reference, MergeMode.REPLACE );
                                    refObjects.add( identifiableObject );
                                }
                                catch ( InstantiationException | IllegalAccessException ignored )
                                {
                                }
                            }
                        }

                        objectReferenceMap.get( object.getUid() ).put( p.getCollectionName(), refObjects );
                    }
                } );
            }
        }

        return refs;
    }

    @Override
    public TypeReport checkReferences( Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, Preheat preheat, PreheatIdentifier identifier )
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

    @Override
    public List<PreheatErrorReport> checkReferences( Class<? extends IdentifiableObject> klass, IdentifiableObject object, Preheat preheat, PreheatIdentifier identifier )
    {
        List<PreheatErrorReport> preheatErrorReports = new ArrayList<>();

        if ( object == null )
        {
            return preheatErrorReports;
        }

        Schema schema = schemaService.getDynamicSchema( object.getClass() );
        schema.getProperties().stream()
            .filter( p -> p.isPersisted() && p.isOwner() && (PropertyType.REFERENCE == p.getPropertyType() || PropertyType.REFERENCE == p.getItemPropertyType()) )
            .forEach( p -> {
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

    @Override
    public TypeReport checkUniqueness( Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, Preheat preheat, PreheatIdentifier identifier )
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
                objectReport.addErrorReports( errorReports );
                typeReport.addObjectReport( objectReport );
                typeReport.getStats().incIgnored();

                iterator.remove();
            }

            idx++;
        }

        return typeReport;
    }

    @Override
    public List<ErrorReport> checkUniqueness( Class<? extends IdentifiableObject> klass, IdentifiableObject object, Preheat preheat, PreheatIdentifier identifier )
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

        uniqueProperties.forEach( property -> {
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

    @Override
    public TypeReport checkMandatoryAttributes( Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, Preheat preheat, PreheatIdentifier identifier )
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
                objectReport.addErrorReports( errorReports );
                typeReport.addObjectReport( objectReport );
                typeReport.getStats().incIgnored();

                iterator.remove();
            }

            idx++;
        }

        return typeReport;
    }

    @Override
    public List<ErrorReport> checkMandatoryAttributes( Class<? extends IdentifiableObject> klass, IdentifiableObject object, Preheat preheat, PreheatIdentifier identifier )
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

    @Override
    public TypeReport checkUniqueAttributes( Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, Preheat preheat, PreheatIdentifier identifier )
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
                objectReport.addErrorReports( errorReports );
                typeReport.addObjectReport( objectReport );
                typeReport.getStats().incIgnored();

                iterator.remove();
            }

            idx++;
        }

        return typeReport;
    }

    @Override
    public List<ErrorReport> checkUniqueAttributes( Class<? extends IdentifiableObject> klass, IdentifiableObject object, Preheat preheat, PreheatIdentifier identifier )
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

        attributeValues.forEach( attributeValue -> {
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

    @Override
    public void connectReferences( Object object, Preheat preheat, PreheatIdentifier identifier )
    {
        if ( object == null )
        {
            return;
        }

        Map<Class<? extends IdentifiableObject>, IdentifiableObject> defaults = preheat.getDefaults();

        Schema schema = schemaService.getDynamicSchema( object.getClass() );
        schema.getProperties().stream()
            .filter( p -> p.isPersisted() && p.isOwner() && (PropertyType.REFERENCE == p.getPropertyType() || PropertyType.REFERENCE == p.getItemPropertyType()) )
            .forEach( p -> {
                if ( skipConnect( p.getKlass() ) || skipConnect( p.getItemKlass() ) )
                {
                    return;
                }

                if ( !p.isCollection() )
                {
                    IdentifiableObject refObject = ReflectionUtils.invokeMethod( object, p.getGetterMethod() );
                    IdentifiableObject ref = getPersistedObject( preheat, identifier, refObject );

                    if ( Preheat.isDefaultClass( refObject ) && (ref == null || "default".equals( refObject.getName() )) )
                    {
                        ref = defaults.get( refObject.getClass() );
                    }

                    if ( ref != null && ref.getId() == 0 )
                    {
                        ReflectionUtils.invokeMethod( object, p.getSetterMethod(), (Object) null );
                    }
                    else
                    {
                        ReflectionUtils.invokeMethod( object, p.getSetterMethod(), ref );
                    }
                }
                else
                {
                    Collection<IdentifiableObject> objects = ReflectionUtils.newCollectionInstance( p.getKlass() );
                    Collection<IdentifiableObject> refObjects = ReflectionUtils.invokeMethod( object, p.getGetterMethod() );

                    for ( IdentifiableObject refObject : refObjects )
                    {
                        IdentifiableObject ref = getPersistedObject( preheat, identifier, refObject );

                        if ( Preheat.isDefaultClass( refObject ) && (ref == null || "default".equals( refObject.getName() )) )
                        {
                            ref = defaults.get( refObject.getClass() );
                        }

                        if ( ref != null && ref.getId() != 0 ) objects.add( ref );
                    }

                    ReflectionUtils.invokeMethod( object, p.getSetterMethod(), objects );
                }
            } );
    }

    //-----------------------------------------------------------------------------------
    // Utility Methods
    //-----------------------------------------------------------------------------------

    private void cleanEmptyEntries( Map<Class<? extends IdentifiableObject>, Set<String>> map )
    {
        Set<Class<? extends IdentifiableObject>> classes = new HashSet<>( map.keySet() );
        classes.stream().filter( klass -> map.get( klass ).isEmpty() ).forEach( map::remove );
    }

    private void addIdentifiers( Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Set<String>>> map, IdentifiableObject identifiableObject )
    {
        if ( identifiableObject == null ) return;

        Map<Class<? extends IdentifiableObject>, Set<String>> uidMap = map.get( PreheatIdentifier.UID );
        Map<Class<? extends IdentifiableObject>, Set<String>> codeMap = map.get( PreheatIdentifier.CODE );

        if ( !uidMap.containsKey( identifiableObject.getClass() ) ) uidMap.put( identifiableObject.getClass(), new HashSet<>() );
        if ( !codeMap.containsKey( identifiableObject.getClass() ) ) codeMap.put( identifiableObject.getClass(), new HashSet<>() );

        if ( !StringUtils.isEmpty( identifiableObject.getUid() ) ) uidMap.get( identifiableObject.getClass() ).add( identifiableObject.getUid() );
        if ( !StringUtils.isEmpty( identifiableObject.getCode() ) ) codeMap.get( identifiableObject.getClass() ).add( identifiableObject.getCode() );
    }

    private Map<String, Map<Object, String>> handleUniqueProperties( Schema schema, List<IdentifiableObject> objects )
    {
        List<Property> uniqueProperties = schema.getProperties().stream()
            .filter( p -> p.isPersisted() && p.isOwner() && p.isUnique() && p.isSimple() )
            .collect( Collectors.toList() );

        Map<String, Map<Object, String>> map = new HashMap<>();

        for ( IdentifiableObject object : objects )
        {
            uniqueProperties.forEach( property -> {
                if ( !map.containsKey( property.getName() ) ) map.put( property.getName(), new HashMap<>() );
                Object value = ReflectionUtils.invokeMethod( object, property.getGetterMethod() );
                if ( value != null ) map.get( property.getName() ).put( value, object.getUid() );
            } );
        }

        return map;
    }

    private IdentifiableObject getPersistedObject( Preheat preheat, PreheatIdentifier identifier, IdentifiableObject ref )
    {
        if ( Period.class.isInstance( ref ) )
        {
            IdentifiableObject period = preheat.getPeriodMap().get( ref.getName() );

            if ( period == null )
            {
                period = periodService.reloadIsoPeriod( ref.getName() );
            }

            if ( period != null )
            {
                preheat.getPeriodMap().put( period.getName(), (Period) period );
            }

            return period;
        }

        return preheat.get( identifier, ref );
    }

    @SuppressWarnings( "unchecked" )
    private Map<String, UserCredentials> getUsernames()
    {
        Map<String, UserCredentials> userCredentialsMap = new HashMap<>();
        Query query = Query.from( schemaService.getDynamicSchema( UserCredentials.class ) );
        List<UserCredentials> userCredentials = (List<UserCredentials>) queryService.query( query );

        for ( UserCredentials uc : userCredentials )
        {
            userCredentialsMap.put( uc.getUsername(), uc );
        }

        return userCredentialsMap;
    }

    private boolean skipCheck( Class<?> klass )
    {
        return klass != null && (
            UserCredentials.class.isAssignableFrom( klass ) || DataElementOperand.class.isAssignableFrom( klass )
                || Period.class.isAssignableFrom( klass ) || PeriodType.class.isAssignableFrom( klass ));
    }

    private boolean skipConnect( Class<?> klass )
    {
        return klass != null && (UserCredentials.class.isAssignableFrom( klass ) || DataElementOperand.class.isAssignableFrom( klass ));
    }
}
