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
package org.hisp.dhis.preheat;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.category.CategoryDimension;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.schema.MergeParams;
import org.hisp.dhis.schema.MergeService;
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
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserRole;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@RequiredArgsConstructor
@Service( "org.hisp.dhis.preheat.PreheatService" )
@Scope( value = "prototype", proxyMode = ScopedProxyMode.INTERFACES )
public class DefaultPreheatService implements PreheatService
{
    private final SchemaService schemaService;

    private final QueryService queryService;

    private final IdentifiableObjectManager manager;

    private final CurrentUserService currentUserService;

    private final PeriodStore periodStore;

    private final PeriodService periodService;

    private final AttributeService attributeService;

    private final MergeService mergeService;

    private final SchemaToDataFetcher schemaToDataFetcher;

    @Override
    @Transactional( readOnly = true )
    public Preheat preheat( PreheatParams params )
    {
        Timer timer = new SystemTimer().start();

        Preheat preheat = new Preheat();
        preheat.setUser( params.getUser() );
        preheat.setDefaults( manager.getDefaults() );

        if ( preheat.getUser() == null )
        {
            preheat.setUser( currentUserService.getCurrentUser() );
        }

        preheat.put( PreheatIdentifier.UID, preheat.getUser() );
        preheat.put( PreheatIdentifier.CODE, preheat.getUser() );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> uniqueCollectionMap = new HashMap<>();
        Set<Class<? extends IdentifiableObject>> klasses = new HashSet<>( params.getObjects().keySet() );

        Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Set<String>>> references = collectReferences(
            params.getObjects() );

        Map<Class<? extends IdentifiableObject>, Set<String>> uidMap = references.get( PreheatIdentifier.UID );
        Map<Class<? extends IdentifiableObject>, Set<String>> codeMap = references.get( PreheatIdentifier.CODE );

        boolean hasOnlyUIDClasses = uidMap.keySet().stream().anyMatch( this::isOnlyUID );

        if ( PreheatIdentifier.UID == params.getPreheatIdentifier() || hasOnlyUIDClasses )
        {
            for ( Class<? extends IdentifiableObject> klass : uidMap.keySet() )
            {
                List<List<String>> identifiers = Lists.partition( Lists.newArrayList( uidMap.get( klass ) ),
                    20000 );

                if ( !identifiers.isEmpty() )
                {
                    for ( List<String> ids : identifiers )
                    {
                        Query query = Query.from( schemaService.getDynamicSchema( klass ) );
                        query.setUser( preheat.getUser() );
                        query.setSkipSharing( true );
                        query.add( Restrictions.in( "id", ids ) );
                        List<? extends IdentifiableObject> objects = queryService.query( query );
                        preheat.put( PreheatIdentifier.UID, objects );
                    }
                }
            }
        }

        if ( codeMap != null && (PreheatIdentifier.CODE == params.getPreheatIdentifier()) )
        {
            for ( Class<? extends IdentifiableObject> klass : codeMap.keySet() )
            {
                List<List<String>> identifiers = Lists.partition( Lists.newArrayList( codeMap.get( klass ) ),
                    20000 );

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

            if ( uidMap.containsKey( User.class ) && !uidMap.get( User.class ).isEmpty() )
            {
                List<List<String>> identifiers = Lists.partition( Lists.newArrayList( uidMap.get( User.class ) ),
                    20000 );

                for ( List<String> ids : identifiers )
                {
                    Query query = Query.from( schemaService.getDynamicSchema( User.class ) );
                    query.setUser( preheat.getUser() );
                    query.add( Restrictions.in( "id", ids ) );
                    List<? extends IdentifiableObject> objects = queryService.query( query );
                    preheat.put( PreheatIdentifier.UID, objects );
                }
            }

            if ( uidMap.containsKey( UserRole.class )
                && !uidMap.get( UserRole.class ).isEmpty() )
            {
                List<List<String>> identifiers = Lists
                    .partition( Lists.newArrayList( uidMap.get( UserRole.class ) ), 20000 );

                for ( List<String> ids : identifiers )
                {
                    Query query = Query.from( schemaService.getDynamicSchema( UserRole.class ) );
                    query.setUser( preheat.getUser() );
                    query.add( Restrictions.in( "id", ids ) );
                    List<? extends IdentifiableObject> objects = queryService.query( query );
                    preheat.put( PreheatIdentifier.UID, objects );
                }
            }
        }

        for ( Class<? extends IdentifiableObject> klass : klasses )
        {
            List<? extends IdentifiableObject> objects = schemaToDataFetcher
                .fetch( schemaService.getDynamicSchema( klass ) );
            if ( !objects.isEmpty() )
            {
                uniqueCollectionMap.put( klass, new ArrayList<>( objects ) );
            }
        }

        // assign an uid to objects without an UID, if they don't have UID but
        // an existing object exists then reuse the UID
        for ( Class<? extends IdentifiableObject> klass : params.getObjects().keySet() )
        {
            params.getObjects().get( klass ).forEach( o -> {
                IdentifiableObject object = preheat.get( params.getPreheatIdentifier(), o );

                if ( object != null )
                {
                    ((BaseIdentifiableObject) o).setUid( object.getUid() );
                }

                if ( StringUtils.isEmpty( o.getUid() ) )
                {
                    ((BaseIdentifiableObject) o).setUid( CodeGenerator.generateUid() );
                }
            } );
        }

        preheat.setUniquenessMap( collectUniqueness( params.getPreheatIdentifier(), uniqueCollectionMap ) );

        // add preheat placeholders for objects that will be created and set
        // mandatory/unique attributes
        for ( Class<? extends IdentifiableObject> klass : params.getObjects().keySet() )
        {
            List<IdentifiableObject> objects = params.getObjects().get( klass );
            preheat.put( params.getPreheatIdentifier(), objects );
        }

        handleAttributes( params.getObjects(), preheat );
        handleSharing( params, preheat );

        periodStore.getAll().forEach( period -> preheat.getPeriodMap().put( period.getName(), period ) );
        periodStore.getAllPeriodTypes()
            .forEach( periodType -> preheat.getPeriodTypeMap().put( periodType.getName(), periodType ) );

        log.info( "(" + preheat.getUsername() + ") Import:Preheat[" + params.getPreheatMode() + "] took "
            + timer.toString() );

        return preheat;
    }

    private void handleSharing( PreheatParams params, Preheat preheat )
    {
        params.getObjects().forEach( ( klass, list ) -> list.forEach( object -> {

            Schema schema = schemaService.getDynamicSchema( klass );

            if ( schema == null || !schema.isShareable() )
            {
                return;
            }

            preheat.put( params.getPreheatIdentifier(), object );
        } ) );
    }

    private void handleAttributes( Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects,
        Preheat preheat )
    {
        Map<Class<? extends IdentifiableObject>, List<Attribute>> attributesByObjectType = new HashMap<>();
        for ( Attribute a : attributeService.getAllAttributes() )
        {
            a.getSupportedClasses().forEach(
                type -> attributesByObjectType.computeIfAbsent( type, key -> new ArrayList<>() ).add( a ) );
        }
        for ( Class<? extends IdentifiableObject> klass : objects.keySet() )
        {
            List<Attribute> mandatoryAttributes = attributesByObjectType.getOrDefault( klass, List.of() ).stream()
                .filter( Attribute::isMandatory )
                .collect( toUnmodifiableList() );

            mandatoryAttributes.forEach( attribute -> preheat.getMandatoryAttributes()
                .computeIfAbsent( klass, key -> new HashSet<>() ).add( attribute.getUid() ) );

            List<Attribute> uniqueAttributes = attributesByObjectType.getOrDefault( klass, List.of() ).stream()
                .filter( Attribute::isUnique )
                .collect( toUnmodifiableList() );

            uniqueAttributes.forEach( attribute -> preheat.getUniqueAttributes()
                .computeIfAbsent( klass, key -> new HashSet<>() ).add( attribute.getUid() ) );

            List<? extends IdentifiableObject> uniqueAttributeValues = manager.getAllByAttributes( klass,
                uniqueAttributes );
            handleUniqueAttributeValues( klass, uniqueAttributeValues, preheat );

            addAllClassesAttributes( klass, preheat, attributesByObjectType.getOrDefault( klass, List.of() ) );
        }

        if ( objects.containsKey( Attribute.class ) )
        {
            List<IdentifiableObject> attributes = objects.get( Attribute.class );

            for ( IdentifiableObject identifiableObject : attributes )
            {
                Attribute attribute = (Attribute) identifiableObject;

                if ( attribute.isMandatory() )
                {
                    attribute.getSupportedClasses().forEach( klass -> preheat.getMandatoryAttributes()
                        .computeIfAbsent( klass, key -> new HashSet<>() ).add( attribute.getUid() ) );
                }

                if ( attribute.isUnique() )
                {
                    attribute.getSupportedClasses().forEach( klass -> preheat.getUniqueAttributes()
                        .computeIfAbsent( klass, key -> new HashSet<>() ).add( attribute.getUid() ) );
                }

                attribute.getSupportedClasses().forEach( klass -> preheat.addClassAttribute( klass, attribute ) );
            }
        }
    }

    /**
     * Get all metadata attributes of the given klass from database and put them
     * to preheat.attributesByTargetObjectType
     *
     * @param klass Class used for querying {@link Attribute}
     * @param preheat {@link Preheat} to store all queried attributes
     */
    private void addAllClassesAttributes( Class<? extends IdentifiableObject> klass, Preheat preheat,
        List<Attribute> attributes )
    {
        if ( CollectionUtils.isEmpty( attributes )
            || !MapUtils.isEmpty( preheat.getAttributesByClass( klass ) ) )
        {
            return;
        }

        preheat.addClassAttributes( klass, Sets.newHashSet( attributes ) );
    }

    private void handleUniqueAttributeValues( Class<? extends IdentifiableObject> klass,
        List<? extends IdentifiableObject> objects, Preheat preheat )
    {
        if ( objects.isEmpty() )
        {
            return;
        }

        preheat.getUniqueAttributeValues().put( klass, new HashMap<>() );

        objects.forEach( object -> object.getAttributeValues().forEach( attributeValue -> {
            Set<String> uids = preheat.getUniqueAttributes().get( klass );

            if ( uids != null && uids.contains( attributeValue.getAttribute().getUid() ) )
            {
                if ( !preheat.getUniqueAttributeValues().get( klass )
                    .containsKey( attributeValue.getAttribute().getUid() ) )
                {
                    preheat.getUniqueAttributeValues().get( klass ).put( attributeValue.getAttribute().getUid(),
                        new HashMap<>() );
                }

                preheat.getUniqueAttributeValues().get( klass ).get( attributeValue.getAttribute().getUid() )
                    .put( attributeValue.getValue(), object.getUid() );
            }
        } ) );
    }

    @Override
    public void validate( PreheatParams params )
        throws PreheatException
    {
        if ( params.getObjects().isEmpty() )
        {
            throw new PreheatException( "PreheatMode.REFERENCE, but no objects were provided." );
        }
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Set<String>>> collectReferences(
        Object object )
    {
        if ( object == null )
        {
            return new HashMap<>();
        }

        if ( object instanceof Collection )
        {
            return collectReferences( (Collection<?>) object );
        }
        else if ( object instanceof Map )
        {
            return collectReferences( (Map<Class<?>, List<?>>) object );
        }

        Map<Class<?>, List<?>> map = new HashMap<>();
        map.put( HibernateProxyUtils.getRealClass( object ), Lists.newArrayList( object ) );

        return collectReferences( map );
    }

    private Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Set<String>>> collectReferences(
        Collection<?> objects )
    {
        if ( objects == null || objects.isEmpty() )
        {
            return new HashMap<>();
        }

        Map<Class<?>, List<?>> map = new HashMap<>();
        map.put( HibernateProxyUtils.getRealClass( objects.iterator().next() ), Lists.newArrayList( objects ) );

        return collectReferences( map );
    }

    @SuppressWarnings( "unchecked" )
    private Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Set<String>>> collectReferences(
        Map<Class<?>, List<?>> objects )
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

        // Clone objects list, we don't want to modify it
        Map<Class<?>, List<?>> targets = new HashMap<>( objects );

        collectScanTargets( targets );

        for ( Class<?> klass : targets.keySet() )
        {
            Schema schema = schemaService.getDynamicSchema( klass );

            List<Property> referenceProperties = schema.getProperties().stream()
                .filter( p -> p.isPersisted() && p.isOwner()
                    && (PropertyType.REFERENCE == p.getPropertyType()
                        || PropertyType.REFERENCE == p.getItemPropertyType()) )
                .collect( toList() );

            for ( Object object : targets.get( klass ) )
            {
                handleLegacyUserCredentials( klass, object );

                if ( schema.isIdentifiableObject() )
                {
                    IdentifiableObject identifiableObject = (IdentifiableObject) object;
                    identifiableObject.getAttributeValues().forEach( av -> addIdentifiers( map, av.getAttribute() ) );
                    identifiableObject.getSharing().getUserGroups().values()
                        .forEach( uga -> addIdentifiers( map, uga.getUserGroup() ) );
                    identifiableObject.getSharing().getUsers().values()
                        .forEach( ua -> addIdentifiers( map, ua.getUser() ) );

                    if ( identifiableObject.getCreatedBy() != null )
                    {
                        addIdentifiers( map, identifiableObject.getCreatedBy() );
                    }

                    addIdentifiers( map, identifiableObject );
                }

                referenceProperties.forEach( p -> {
                    if ( !p.isCollection() )
                    {
                        Class<? extends IdentifiableObject> itemKlass = (Class<? extends IdentifiableObject>) p
                            .getKlass();

                        if ( !uidMap.containsKey( itemKlass ) )
                            uidMap.put( itemKlass, new HashSet<>() );
                        if ( !codeMap.containsKey( itemKlass ) )
                            codeMap.put( itemKlass, new HashSet<>() );

                        Object reference = ReflectionUtils.invokeMethod( object, p.getGetterMethod() );

                        if ( reference != null )
                        {
                            IdentifiableObject identifiableObject = (IdentifiableObject) reference;
                            addIdentifiers( map, identifiableObject );
                        }
                    }
                    else
                    {
                        Collection<IdentifiableObject> reference = ReflectionUtils.invokeMethod( object,
                            p.getGetterMethod() );

                        if ( reference != null )
                        {
                            reference.forEach( identifiableObject -> addIdentifiers( map, identifiableObject ) );

                            if ( DataElementOperand.class.isAssignableFrom( p.getItemKlass() ) )
                            {
                                CollectionUtils.nullSafeForEach( reference, identifiableObject -> {
                                    DataElementOperand dataElementOperand = (DataElementOperand) identifiableObject;
                                    addIdentifiers( map, dataElementOperand.getDataElement() );
                                    addIdentifiers( map, dataElementOperand.getCategoryOptionCombo() );
                                } );
                            }
                        }
                    }
                } );

                collectAnalyticalObjectReferences( map, object );
            }
        }

        cleanEmptyEntries( uidMap );
        cleanEmptyEntries( codeMap );

        return map;
    }

    /**
     * Collect references for {@link AnalyticalObject}.
     *
     * @param map the mapping between {@link PreheatIdentifier} and object
     *        identifiers.
     * @param object the object.
     */
    private void collectAnalyticalObjectReferences(
        Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Set<String>>> map, Object object )
    {
        if ( object instanceof AnalyticalObject )
        {
            BaseAnalyticalObject analyticalObject = (BaseAnalyticalObject) object;
            List<DataDimensionItem> dataDimensionItems = analyticalObject.getDataDimensionItems();
            List<CategoryDimension> categoryDimensions = analyticalObject.getCategoryDimensions();
            List<TrackedEntityDataElementDimension> trackedEntityDataElementDimensions = analyticalObject
                .getDataElementDimensions();
            List<TrackedEntityAttributeDimension> attributeDimensions = analyticalObject.getAttributeDimensions();
            List<TrackedEntityProgramIndicatorDimension> programIndicatorDimensions = analyticalObject
                .getProgramIndicatorDimensions();

            CollectionUtils.nullSafeForEach( dataDimensionItems, dataDimensionItem -> {
                addIdentifiers( map, dataDimensionItem.getDimensionalItemObject() );

                if ( dataDimensionItem.getDataElementOperand() != null )
                {
                    addIdentifiers( map, dataDimensionItem.getDataElementOperand().getDataElement() );
                    addIdentifiers( map, dataDimensionItem.getDataElementOperand().getCategoryOptionCombo() );
                }

                if ( dataDimensionItem.getReportingRate() != null )
                {
                    addIdentifiers( map, dataDimensionItem.getReportingRate().getDataSet() );
                }

                if ( dataDimensionItem.getProgramDataElement() != null )
                {
                    addIdentifiers( map, dataDimensionItem.getProgramDataElement().getDataElement() );
                    addIdentifiers( map, dataDimensionItem.getProgramDataElement().getProgram() );
                }

                if ( dataDimensionItem.getProgramAttribute() != null )
                {
                    addIdentifiers( map, dataDimensionItem.getProgramAttribute().getAttribute() );
                    addIdentifiers( map, dataDimensionItem.getProgramAttribute().getProgram() );
                }
            } );

            CollectionUtils.nullSafeForEach( categoryDimensions, categoryDimension -> {
                addIdentifiers( map, categoryDimension.getDimension() );
                categoryDimension.getItems().forEach( item -> addIdentifiers( map, item ) );
            } );

            CollectionUtils.nullSafeForEach( trackedEntityDataElementDimensions, trackedEntityDataElementDimension -> {
                addIdentifiers( map, trackedEntityDataElementDimension.getDataElement() );
                addIdentifiers( map, trackedEntityDataElementDimension.getLegendSet() );
                addIdentifiers( map, trackedEntityDataElementDimension.getProgramStage() );
            } );

            CollectionUtils.nullSafeForEach( attributeDimensions, trackedEntityAttributeDimension -> {
                addIdentifiers( map, trackedEntityAttributeDimension.getAttribute() );
                addIdentifiers( map, trackedEntityAttributeDimension.getLegendSet() );
            } );

            CollectionUtils.nullSafeForEach( programIndicatorDimensions, programIndicatorDimension -> {
                addIdentifiers( map, programIndicatorDimension.getProgramIndicator() );
                addIdentifiers( map, programIndicatorDimension.getLegendSet() );
            } );
        }
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public Map<Class<?>, Map<String, Map<String, Object>>> collectObjectReferences( Object object )
    {
        if ( object == null )
        {
            return new HashMap<>();
        }

        if ( object instanceof Collection )
        {
            return collectObjectReferences( (Collection<?>) object );
        }
        else if ( object instanceof Map )
        {
            return collectObjectReferences( (Map<Class<?>, List<?>>) object );
        }

        Map<Class<?>, List<?>> map = new HashMap<>();
        map.put( HibernateProxyUtils.getRealClass( object ), Lists.newArrayList( object ) );

        return collectObjectReferences( map );
    }

    private Map<Class<?>, Map<String, Map<String, Object>>> collectObjectReferences( Collection<?> objects )
    {
        if ( objects == null || objects.isEmpty() )
        {
            return new HashMap<>();
        }

        Map<Class<?>, List<?>> map = new HashMap<>();
        map.put( HibernateProxyUtils.getRealClass( objects.iterator().next() ), Lists.newArrayList( objects ) );

        return collectObjectReferences( map );
    }

    @SuppressWarnings( "unchecked" )
    private Map<Class<?>, Map<String, Map<String, Object>>> collectObjectReferences( Map<Class<?>, List<?>> objects )
    {
        Map<Class<?>, Map<String, Map<String, Object>>> map = new HashMap<>();

        if ( objects.isEmpty() )
        {
            return map;
        }

        // clone objects list, we don't want to modify it
        Map<Class<?>, List<?>> targets = new HashMap<>( objects );
        collectScanTargets( targets );

        for ( Class<?> objectClass : targets.keySet() )
        {
            Schema schema = schemaService.getDynamicSchema( objectClass );

            if ( !schema.isIdentifiableObject() )
            {
                continue;
            }

            List<Property> properties = schema.getProperties().stream()
                .filter( p -> p.isPersisted() && p.isOwner()
                    && (PropertyType.REFERENCE == p.getPropertyType()
                        || PropertyType.REFERENCE == p.getItemPropertyType()) )
                .collect( toList() );

            List<IdentifiableObject> identifiableObjects = (List<IdentifiableObject>) targets.get( objectClass );
            Map<String, Map<String, Object>> refMap = new HashMap<>();
            map.put( objectClass, refMap );

            for ( IdentifiableObject object : identifiableObjects )
            {
                refMap.put( object.getUid(), new HashMap<>() );

                properties.forEach( p -> {
                    if ( !p.isCollection() )
                    {
                        IdentifiableObject reference = ReflectionUtils.invokeMethod( object, p.getGetterMethod() );

                        if ( reference != null )
                        {
                            try
                            {
                                IdentifiableObject identifiableObject = (IdentifiableObject) p.getKlass().newInstance();
                                mergeService.merge( new MergeParams<>( reference, identifiableObject ) );
                                refMap.get( object.getUid() ).put( p.getName(), identifiableObject );
                            }
                            catch ( InstantiationException | IllegalAccessException ignored )
                            {
                            }
                        }
                    }
                    else
                    {
                        Collection<IdentifiableObject> refObjects = ReflectionUtils
                            .newCollectionInstance( p.getKlass() );
                        Collection<IdentifiableObject> references = ReflectionUtils.invokeMethod( object,
                            p.getGetterMethod() );

                        if ( references != null )
                        {
                            for ( IdentifiableObject reference : references )
                            {
                                if ( reference == null )
                                {
                                    continue;
                                }

                                try
                                {
                                    IdentifiableObject identifiableObject = (IdentifiableObject) p.getItemKlass()
                                        .newInstance();
                                    mergeService.merge( new MergeParams<>( reference, identifiableObject ) );
                                    refObjects.add( identifiableObject );
                                }
                                catch ( InstantiationException | IllegalAccessException ignored )
                                {
                                }
                            }
                        }

                        refMap.get( object.getUid() ).put( p.getCollectionName(), refObjects );
                    }
                } );
            }
        }

        return map;
    }

    private void collectScanTargets( Map<Class<?>, List<?>> targets )
    {
        for ( Map.Entry<Class<?>, List<?>> entry : new HashMap<>( targets ).entrySet() )
        {
            Class<?> klass = entry.getKey();
            List<?> objects = entry.getValue();

            Schema schema = schemaService.getDynamicSchema( klass );
            Map<String, Property> properties = schema.getEmbeddedObjectProperties();

            if ( properties.isEmpty() )
            {
                return;
            }

            for ( Property property : properties.values() )
            {
                if ( property.isCollection() )
                {
                    List<Object> list = new ArrayList<>();

                    if ( targets.containsKey( property.getItemKlass() ) )
                    {
                        list.addAll( targets.get( property.getItemKlass() ) );
                    }

                    objects
                        .forEach( o -> list.addAll( ReflectionUtils.invokeMethod( o, property.getGetterMethod() ) ) );
                    targets.put( property.getItemKlass(), list );
                }
                else
                {
                    List<Object> list = new ArrayList<>();

                    if ( targets.containsKey( property.getKlass() ) )
                    {
                        list.addAll( targets.get( property.getKlass() ) );
                    }

                    objects.forEach( o -> list.add( ReflectionUtils.invokeMethod( o, property.getGetterMethod() ) ) );
                    targets.put( property.getKlass(), list );
                }
            }
        }
    }

    @Override
    public Map<Class<? extends IdentifiableObject>, Map<String, Map<Object, String>>> collectUniqueness(
        PreheatIdentifier identifier, Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects )
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
            Map<String, Map<Object, String>> value = handleUniqueProperties( schema, identifier, identifiableObjects );
            uniqueMap.put( objectClass, value );
        }

        return uniqueMap;
    }

    @Override
    public void connectReferences( Object object, Preheat preheat, PreheatIdentifier identifier )
    {
        if ( object == null )
        {
            return;
        }

        Schema schema = schemaService.getDynamicSchema( HibernateProxyUtils.getRealClass( object ) );

        List<Property> properties = schema.getProperties().stream()
            .filter( p -> p.isPersisted() && p.isOwner()
                && (PropertyType.REFERENCE == p.getPropertyType()
                    || PropertyType.REFERENCE == p.getItemPropertyType()) )
            .collect( toList() );

        for ( Property property : properties )
        {
            if ( skipConnect( property.getKlass() ) || skipConnect( property.getItemKlass() ) )
            {
                continue;
            }

            if ( !property.isCollection() )
            {
                IdentifiableObject refObject = ReflectionUtils.invokeMethod( object, property.getGetterMethod() );
                IdentifiableObject ref = getPersistedObject( preheat, identifier, refObject );

                ref = connectDefaults( preheat, property, object, refObject, ref );

                if ( ref != null && ref.getId() == 0 )
                {
                    ReflectionUtils.invokeMethod( object, property.getSetterMethod(), (Object) null );
                }
                else
                {
                    ReflectionUtils.invokeMethod( object, property.getSetterMethod(), ref );
                }
            }
            else
            {
                Collection<IdentifiableObject> objects = ReflectionUtils.newCollectionInstance( property.getKlass() );
                Collection<IdentifiableObject> refObjects = ReflectionUtils.invokeMethod( object,
                    property.getGetterMethod() );

                for ( IdentifiableObject refObject : refObjects )
                {
                    IdentifiableObject ref = getPersistedObject( preheat, identifier, refObject );
                    if ( ref != null && ref.getId() != 0 )
                        objects.add( ref );
                }

                ReflectionUtils.invokeMethod( object, property.getSetterMethod(), objects );
            }
        }
    }

    @Override
    @Transactional( readOnly = true )
    public void refresh( IdentifiableObject object )
    {
        PreheatParams preheatParams = new PreheatParams();
        preheatParams.setUser( currentUserService.getCurrentUser() );
        preheatParams.addObject( object );

        Preheat preheat = preheat( preheatParams );
        connectReferences( object, preheat, PreheatIdentifier.UID );
    }

    // -----------------------------------------------------------------------------------
    // Utility Methods
    // -----------------------------------------------------------------------------------

    private IdentifiableObject connectDefaults( Preheat preheat, Property property, Object object,
        IdentifiableObject refObject, IdentifiableObject ref )
    {
        Map<Class<? extends IdentifiableObject>, IdentifiableObject> defaults = preheat.getDefaults();

        if ( refObject == null && DataSetElement.class.isInstance( object ) )
        {
            return null;
        }

        IdentifiableObject defaultObject = defaults.get( property.getKlass() );

        if ( Preheat.isDefaultClass( property.getKlass() ) )
        {
            if ( refObject == null
                || (refObject.getUid() != null && refObject.getUid().equals( defaultObject.getUid() )) )
            {
                ref = defaultObject;
            }
        }

        return ref;
    }

    private void cleanEmptyEntries( Map<Class<? extends IdentifiableObject>, Set<String>> map )
    {
        Set<Class<? extends IdentifiableObject>> classes = new HashSet<>( map.keySet() );
        classes.stream().filter( klass -> map.get( klass ).isEmpty() ).forEach( map::remove );
    }

    @SuppressWarnings( "unchecked" )
    private void addIdentifiers( Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Set<String>>> map,
        IdentifiableObject identifiableObject )
    {
        if ( identifiableObject == null )
            return;

        Map<Class<? extends IdentifiableObject>, Set<String>> uidMap = map.get( PreheatIdentifier.UID );
        Map<Class<? extends IdentifiableObject>, Set<String>> codeMap = map.get( PreheatIdentifier.CODE );

        Class<? extends IdentifiableObject> klass = HibernateProxyUtils.getRealClass( identifiableObject );

        if ( !uidMap.containsKey( klass ) )
            uidMap.put( klass, new HashSet<>() );
        if ( !codeMap.containsKey( klass ) )
            codeMap.put( klass, new HashSet<>() );

        if ( !StringUtils.isEmpty( identifiableObject.getUid() ) )
            uidMap.get( klass ).add( identifiableObject.getUid() );
        if ( !StringUtils.isEmpty( identifiableObject.getCode() ) )
            codeMap.get( klass ).add( identifiableObject.getCode() );
    }

    private Map<String, Map<Object, String>> handleUniqueProperties( Schema schema, PreheatIdentifier identifier,
        List<IdentifiableObject> objects )
    {
        List<Property> uniqueProperties = schema.getProperties().stream()
            .filter( p -> p.isPersisted() && p.isOwner() && p.isUnique() && p.isSimple() )
            .collect( toList() );

        Map<String, Map<Object, String>> map = new HashMap<>();

        for ( IdentifiableObject object : objects )
        {
            uniqueProperties.forEach( property -> {
                if ( !map.containsKey( property.getName() ) )
                    map.put( property.getName(), new HashMap<>() );
                Object value = ReflectionUtils.invokeMethod( object, property.getGetterMethod() );
                if ( value != null )
                    map.get( property.getName() ).put( value, identifier.getIdentifier( object ) );
            } );
        }

        return map;
    }

    private IdentifiableObject getPersistedObject( Preheat preheat, PreheatIdentifier identifier,
        IdentifiableObject ref )
    {
        if ( ref instanceof Period )
        {
            Period period = preheat.getPeriodMap().get( ref.getName() );

            if ( period == null )
            {
                period = periodService.reloadIsoPeriod( ref.getName() );
            }

            if ( period != null )
            {
                preheat.getPeriodMap().put( period.getName(), period );
            }

            return period;
        }

        return preheat.get( identifier, ref );
    }

    private boolean skipConnect( Class<?> klass )
    {
        return klass != null && EmbeddedObject.class.isAssignableFrom( klass );
    }

    private boolean isOnlyUID( Class<?> klass )
    {
        return UserGroup.class.isAssignableFrom( klass ) || User.class.isAssignableFrom( klass );
    }

    // TODO: To remove when we remove old UserCredentials compatibility layer
    /**
     * This is a temporary fix to maintain backwards compatibility with the old
     * UserCredentials class
     */
    private void handleLegacyUserCredentials( Class<?> klass, Object object )
    {
        if ( !User.class.isAssignableFrom( klass ) || object == null )
        {
            return;
        }

        User.populateUserCredentialsDtoFields( (User) object );
    }
}
