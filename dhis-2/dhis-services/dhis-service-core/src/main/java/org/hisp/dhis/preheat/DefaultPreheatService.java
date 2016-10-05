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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.dataelement.DataElementCategoryDimension;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodStore;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
                    List<List<String>> identifiers = Lists.partition( Lists.newArrayList( uidMap.get( klass ) ), 20000 );

                    if ( !identifiers.isEmpty() )
                    {
                        for ( List<String> ids : identifiers )
                        {
                            Query query = Query.from( schemaService.getDynamicSchema( klass ) );
                            query.setUser( preheat.getUser() );
                            query.add( Restrictions.in( "id", ids ) );
                            List<? extends IdentifiableObject> objects = queryService.query( query );
                            preheat.put( PreheatIdentifier.UID, objects );
                        }
                    }
                }
            }

            if ( codeMap != null && (PreheatIdentifier.CODE == params.getPreheatIdentifier() || PreheatIdentifier.AUTO == params.getPreheatIdentifier()) )
            {
                for ( Class<? extends IdentifiableObject> klass : codeMap.keySet() )
                {
                    List<List<String>> identifiers = Lists.partition( Lists.newArrayList( codeMap.get( klass ) ), 20000 );

                    if ( !identifiers.isEmpty() )
                    {
                        for ( List<String> ids : identifiers )
                        {
                            Query query = Query.from( schemaService.getDynamicSchema( klass ) );
                            query.setUser( preheat.getUser() );
                            query.add( Restrictions.in( "code", ids ) );
                            List<? extends IdentifiableObject> objects = queryService.query( query );
                            preheat.put( PreheatIdentifier.CODE, objects );
                        }
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
                    attribute.getSupportedClasses().forEach( klass ->
                    {
                        if ( !preheat.getMandatoryAttributes().containsKey( klass ) ) preheat.getMandatoryAttributes().put( klass, new HashSet<>() );
                        preheat.getMandatoryAttributes().get( klass ).add( attribute.getUid() );
                    } );
                }

                if ( attribute.isUnique() )
                {
                    attribute.getSupportedClasses().forEach( klass ->
                    {
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

        objects.forEach( object ->
        {
            object.getAttributeValues().forEach( attributeValue ->
            {
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
                identifiableProperties.forEach( p ->
                {
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

                        if ( DataElementOperand.class.isAssignableFrom( p.getItemKlass() ) )
                        {
                            reference.forEach( identifiableObject ->
                            {
                                DataElementOperand dataElementOperand = (DataElementOperand) identifiableObject;
                                addIdentifiers( map, dataElementOperand.getDataElement() );
                                addIdentifiers( map, dataElementOperand.getCategoryOptionCombo() );
                            } );
                        }
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

                    dataDimensionItems.forEach( dataDimensionItem ->
                    {
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

                    categoryDimensions.forEach( categoryDimension ->
                    {
                        addIdentifiers( map, categoryDimension.getDimension() );
                        categoryDimension.getItems().forEach( item -> addIdentifiers( map, item ) );
                    } );

                    dataElementDimensions.forEach( trackedEntityDataElementDimension ->
                    {
                        addIdentifiers( map, trackedEntityDataElementDimension.getDataElement() );
                        addIdentifiers( map, trackedEntityDataElementDimension.getLegendSet() );
                    } );

                    attributeDimensions.forEach( trackedEntityAttributeDimension ->
                    {
                        addIdentifiers( map, trackedEntityAttributeDimension.getAttribute() );
                        addIdentifiers( map, trackedEntityAttributeDimension.getLegendSet() );
                    } );

                    programIndicatorDimensions.forEach( programIndicatorDimension ->
                    {
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

                properties.forEach( p ->
                {
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
            .forEach( p ->
            {
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

    @Override
    public void refresh( IdentifiableObject object )
    {
        PreheatParams preheatParams = new PreheatParams();
        preheatParams.setUser( currentUserService.getCurrentUser() );
        preheatParams.addObject( object );

        Preheat preheat = preheat( preheatParams );
        connectReferences( object, preheat, PreheatIdentifier.UID );
    }

    //-----------------------------------------------------------------------------------
    // Utility Methods
    //-----------------------------------------------------------------------------------

    private void cleanEmptyEntries( Map<Class<? extends IdentifiableObject>, Set<String>> map )
    {
        Set<Class<? extends IdentifiableObject>> classes = new HashSet<>( map.keySet() );
        classes.stream().filter( klass -> map.get( klass ).isEmpty() ).forEach( map::remove );
    }

    @SuppressWarnings( "unchecked" )
    private void addIdentifiers( Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Set<String>>> map, IdentifiableObject identifiableObject )
    {
        if ( identifiableObject == null ) return;

        Map<Class<? extends IdentifiableObject>, Set<String>> uidMap = map.get( PreheatIdentifier.UID );
        Map<Class<? extends IdentifiableObject>, Set<String>> codeMap = map.get( PreheatIdentifier.CODE );

        Class<? extends IdentifiableObject> klass = (Class<? extends IdentifiableObject>) ReflectionUtils.getRealClass( identifiableObject.getClass() );

        if ( !uidMap.containsKey( klass ) ) uidMap.put( klass, new HashSet<>() );
        if ( !codeMap.containsKey( klass ) ) codeMap.put( klass, new HashSet<>() );

        if ( !StringUtils.isEmpty( identifiableObject.getUid() ) ) uidMap.get( klass ).add( identifiableObject.getUid() );
        if ( !StringUtils.isEmpty( identifiableObject.getCode() ) ) codeMap.get( klass ).add( identifiableObject.getCode() );
    }

    private Map<String, Map<Object, String>> handleUniqueProperties( Schema schema, List<IdentifiableObject> objects )
    {
        List<Property> uniqueProperties = schema.getProperties().stream()
            .filter( p -> p.isPersisted() && p.isOwner() && p.isUnique() && p.isSimple() )
            .collect( Collectors.toList() );

        Map<String, Map<Object, String>> map = new HashMap<>();

        for ( IdentifiableObject object : objects )
        {
            uniqueProperties.forEach( property ->
            {
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

    private boolean skipConnect( Class<?> klass )
    {
        return klass != null && (UserCredentials.class.isAssignableFrom( klass ) || DataElementOperand.class.isAssignableFrom( klass ));
    }
}
