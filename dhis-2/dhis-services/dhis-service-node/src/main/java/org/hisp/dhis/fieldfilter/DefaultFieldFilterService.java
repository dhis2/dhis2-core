/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.fieldfilter;

import static java.beans.Introspector.decapitalize;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.node.AbstractNode;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.NodeTransformer;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyTransformer;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserGroupAccess;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Component( "org.hisp.dhis.fieldfilter.FieldFilterService" )
public class DefaultFieldFilterService implements FieldFilterService
{
    private final static Pattern FIELD_PATTERN = Pattern.compile( "^(?<field>\\w+)" );

    private final static Pattern TRANSFORMER_PATTERN = Pattern
        .compile( "(?<type>\\||::|~)(?<name>\\w+)(?:\\((?<args>[\\w;]+)\\))?" );

    private final FieldParser fieldParser;

    private final SchemaService schemaService;

    private final AclService aclService;

    private final CurrentUserService currentUserService;

    private final AttributeService attributeService;

    private final Set<NodeTransformer> nodeTransformers;

    private ImmutableMap<String, Preset> presets = ImmutableMap.of();

    private ImmutableMap<String, NodeTransformer> transformers = ImmutableMap.of();

    private Property baseIdentifiableIdProperty;

    private final Cache<PropertyTransformer> transformerCache;

    private final UserGroupService userGroupService;

    private final UserService userService;

    public DefaultFieldFilterService(
        FieldParser fieldParser,
        SchemaService schemaService,
        AclService aclService,
        CurrentUserService currentUserService,
        AttributeService attributeService,
        CacheProvider cacheProvider,
        UserGroupService userGroupService,
        UserService userService,
        @Autowired( required = false ) Set<NodeTransformer> nodeTransformers )
    {
        this.fieldParser = fieldParser;
        this.schemaService = schemaService;
        this.aclService = aclService;
        this.currentUserService = currentUserService;
        this.userService = userService;
        this.attributeService = attributeService;
        this.userGroupService = userGroupService;
        this.nodeTransformers = nodeTransformers == null ? new HashSet<>() : nodeTransformers;
        this.transformerCache = cacheProvider.createPropertyTransformerCache();
    }

    @PostConstruct
    public void init()
    {
        ImmutableMap.Builder<String, Preset> presetBuilder = ImmutableMap.builder();

        for ( Preset preset : Preset.values() )
        {
            presetBuilder.put( preset.getName(), preset );
        }

        presets = presetBuilder.build();

        ImmutableMap.Builder<String, NodeTransformer> transformerBuilder = ImmutableMap.builder();

        for ( NodeTransformer transformer : nodeTransformers )
        {
            transformerBuilder.put( transformer.name(), transformer );
        }

        transformers = transformerBuilder.build();

        baseIdentifiableIdProperty = schemaService.getDynamicSchema( BaseIdentifiableObject.class ).getProperty( "id" );
    }

    @Override
    public ComplexNode toComplexNode( FieldFilterParams params )
    {
        if ( params.getObjects().isEmpty() )
        {
            return null;
        }

        Object object = params.getObjects().get( 0 );
        CollectionNode collectionNode = toCollectionNode( object.getClass(), params );

        if ( !collectionNode.getChildren().isEmpty() )
        {
            return (ComplexNode) collectionNode.getChildren().get( 0 );
        }

        return null;
    }

    @Override
    public CollectionNode toCollectionNode( Class<?> wrapper, FieldFilterParams params )
    {
        String fields = params.getFields() == null ? "" : Joiner.on( "," ).join( params.getFields() );

        Schema rootSchema = schemaService.getDynamicSchema( wrapper );

        CollectionNode collectionNode = new CollectionNode( rootSchema.getCollectionName() );
        collectionNode.setNamespace( rootSchema.getNamespace() );

        List<?> objects = params.getObjects();

        if ( params.getSkipSharing() )
        {
            final List<String> fieldList = CollectionUtils.isEmpty( params.getFields() )
                ? Collections.singletonList( "*" )
                : params.getFields();
            // excludes must be preserved (e.g. when field collections like
            // :owner are used, which is not expanded by modify filter)
            fields = Stream
                .concat( fieldParser.modifyFilter( fieldList, SHARING_FIELDS ).stream(), SHARING_FIELDS.stream() )
                .filter( org.apache.commons.lang3.StringUtils::isNotBlank ).distinct()
                .collect( Collectors.joining( "," ) );
        }

        if ( params.getObjects().isEmpty() || objects.stream().allMatch( Objects::isNull ) )
        {
            return collectionNode;
        }

        FieldMap fieldMap = new FieldMap();
        Schema schema = schemaService.getDynamicSchema( HibernateProxyUtils.getRealClass( objects.get( 0 ) ) );

        if ( StringUtils.isEmpty( fields ) )
        {
            for ( Property property : schema.getProperties() )
            {
                fieldMap.put( property.getName(), new FieldMap() );
            }
        }
        else
        {
            fieldMap = fieldParser.parse( fields );
        }

        final FieldMap finalFieldMap = fieldMap;

        if ( params.getUser() == null )
        {
            params.setUser( currentUserService.getCurrentUser() );
        }

        objects.forEach( object -> {
            AbstractNode node = buildNode( finalFieldMap, wrapper, object, params.getUser(), params.getDefaults() );

            if ( node != null )
            {
                collectionNode.addChild( node );
            }
        } );

        return collectionNode;
    }

    @Override
    public CollectionNode toConcreteClassCollectionNode( final Class<?> klass, final FieldFilterParams params,
        final String collectionName, final String namespace )
    {
        final String fields = params.getFields() == null ? "" : Joiner.on( "," ).join( params.getFields() );

        final CollectionNode collectionNode = new CollectionNode( collectionName );
        collectionNode.setNamespace( namespace );

        final List<?> objects = params.getObjects();

        if ( params.getObjects().isEmpty() )
        {
            return collectionNode;
        }

        FieldMap fieldMap = new FieldMap();

        // If fields not specified OR set as "*", bring all fields.
        if ( StringUtils.isBlank( fields )
            || "*".equals( StringUtils.trimToEmpty( fields ) ) )
        {
            for ( final Field property : klass.getDeclaredFields() )
            {
                fieldMap.put( property.getName(), new FieldMap() );
            }
        }
        else
        {
            fieldMap = fieldParser.parse( fields );
        }

        final FieldMap finalFieldMap = fieldMap;

        if ( params.getUser() == null )
        {
            params.setUser( currentUserService.getCurrentUser() );
        }

        objects.forEach( object -> {
            final AbstractNode node = buildNode( finalFieldMap, object, namespace );

            if ( node != null )
            {
                collectionNode.addChild( node );
            }
        } );

        return collectionNode;
    }

    private AbstractNode buildNode( FieldMap fieldMap, Class<?> klass, Object object, User user, Defaults defaults )
    {
        Schema schema = schemaService.getDynamicSchema( klass );
        return buildNode( fieldMap, klass, object, user, schema.getName(), defaults );
    }

    private boolean mayExclude( Class<?> klass, Defaults defaults )
    {
        return Defaults.EXCLUDE == defaults && IdentifiableObject.class.isAssignableFrom( klass ) &&
            (Preheat.isDefaultClass( klass ) || klass.isInterface() || (klass.getModifiers() & Modifier.ABSTRACT) != 0);
    }

    private boolean shouldExclude( Object object, Defaults defaults )
    {
        return Defaults.EXCLUDE == defaults && object instanceof IdentifiableObject &&
            Preheat.isDefaultObject( (IdentifiableObject) object )
            && "default".equals( ((IdentifiableObject) object).getName() );
    }

    private AbstractNode buildNode( final FieldMap fieldMap, final Object klassInstance, final String namespace )
    {
        final ComplexNode complexNode = new ComplexNode( decapitalize( klassInstance.getClass().getSimpleName() ) );
        complexNode.setNamespace( namespace );

        for ( final String fieldKey : fieldMap.keySet() )
        {
            try
            {
                final String originalName = org.apache.commons.lang3.StringUtils.substringBefore( fieldKey, "~" );
                final String rename = org.apache.commons.lang3.StringUtils.substringBetween( fieldKey, "(", ")" );

                final Field field = klassInstance.getClass().getDeclaredField( originalName );
                field.setAccessible( true ); // NOSONAR

                final Object value = ReflectionUtils.invokeGetterMethod( originalName, klassInstance );

                if ( org.apache.commons.lang3.StringUtils.isNotBlank( rename ) )
                {
                    complexNode.addChild( new SimpleNode( rename, value ) );
                }
                else
                {
                    complexNode.addChild( new SimpleNode( originalName, value ) );
                }
            }
            catch ( NoSuchFieldException e )
            {
                log.warn( "Error reading attribute", e );
            }
        }

        return complexNode;
    }

    private AbstractNode buildNode( FieldMap fieldMap, Class<?> klass, Object object, User user, String nodeName,
        Defaults defaults )
    {
        Schema schema = schemaService.getDynamicSchema( klass );

        ComplexNode complexNode = new ComplexNode( nodeName );
        complexNode.setNamespace( schema.getNamespace() );

        if ( object == null )
        {
            return new SimpleNode( schema.getName(), null );
        }

        if ( shouldExclude( object, defaults ) )
        {
            return null;
        }

        updateFields( fieldMap, schema.getKlass() );

        if ( fieldMap.containsKey( "access" ) && schema.isIdentifiableObject() )
        {
            ((BaseIdentifiableObject) object)
                .setAccess( aclService.getAccess( (IdentifiableObject) object, user ) );
        }

        if ( fieldMap.containsKey( "attribute" ) && AttributeValue.class.isAssignableFrom( object.getClass() ) )
        {
            AttributeValue attributeValue = (AttributeValue) object;
            attributeValue.setAttribute( attributeService.getAttribute( attributeValue.getAttribute().getUid() ) );
        }

        if ( UserGroupAccess.class.isAssignableFrom( object.getClass() ) )
        {
            UserGroupAccess userGroupAccess = (UserGroupAccess) object;
            userGroupAccess
                .setDisplayName( userGroupService.getDisplayName( userGroupAccess.getUserGroupUid() ) );
        }

        if ( UserAccess.class.isAssignableFrom( object.getClass() ) )
        {
            UserAccess userAccess = (UserAccess) object;
            userAccess.setDisplayName( userService.getDisplayName( userAccess.getUserUid() ) );
        }

        for ( String fieldKey : fieldMap.keySet() )
        {
            AbstractNode child = null;
            Property property = schema.getProperty( fieldKey );
            FieldMap fieldValue = fieldMap.get( fieldKey );

            if ( property == null || !property.isReadable() )
            {
                // throw new FieldFilterException( fieldKey, schema );
                log.debug( "Unknown field property `" + fieldKey + "`, available fields are "
                    + schema.getPropertyMap().keySet() );
                continue;
            }

            Object returnValue = ReflectionUtils.invokeMethod( object, property.getGetterMethod() );

            Class<?> propertyClass = property.getKlass();
            Schema propertySchema = schemaService.getDynamicSchema( propertyClass );

            if ( property.hasPropertyTransformer() )
            {
                Optional<PropertyTransformer> propertyTransformer = transformerCache
                    .get( property.getPropertyTransformer().getName(), s -> {
                        try
                        {
                            return property.getPropertyTransformer().newInstance();
                        }
                        catch ( InstantiationException | IllegalAccessException e )
                        {
                            throw new RuntimeException( e );
                        }
                    } );

                if ( propertyTransformer.isPresent() && returnValue != null )
                {
                    returnValue = propertyTransformer.get().transform( returnValue );
                    propertyClass = returnValue.getClass();
                    propertySchema = schemaService.getDynamicSchema( propertyClass );
                    updateFields( fieldValue, propertyTransformer.get().getKlass() );
                }
            }

            if ( returnValue != null
                && propertySchema.getProperties().isEmpty()
                && !property.isCollection()
                && property.getKlass().isInterface()
                && !property.isIdentifiableObject() )
            {
                // try to retrieve schema from concrete class
                propertyClass = returnValue.getClass();
                propertySchema = schemaService.getDynamicSchema( propertyClass );
            }

            if ( returnValue == null && property.isCollection() )
            {
                continue;
            }

            if ( property.isCollection() )
            {
                updateFields( fieldValue, property.getItemKlass() );
            }
            else
            {
                updateFields( fieldValue, propertyClass );
            }

            if ( fieldValue.isEmpty() )
            {
                List<String> fields = Preset.defaultAssociationPreset().getFields();

                if ( property.isCollection() )
                {
                    Collection<?> collection = (Collection<?>) returnValue;

                    child = new CollectionNode( property.getCollectionName(), collection.size() );
                    child.setNamespace( property.getNamespace() );

                    if ( property.isIdentifiableObject() && isProperIdObject( property.getItemKlass() ) )
                    {
                        final boolean mayExclude = collection.isEmpty()
                            || mayExclude( property.getItemKlass(), defaults );

                        for ( Object collectionObject : collection )
                        {
                            if ( !mayExclude || !shouldExclude( collectionObject, defaults ) )
                            {
                                child.addChild( getProperties( property, collectionObject, fields ) );
                            }
                        }
                    }
                    else if ( !property.isSimple() )
                    {
                        FieldMap map = getFullFieldMap( schemaService.getDynamicSchema( property.getItemKlass() ) );

                        for ( Object collectionObject : collection )
                        {
                            Node node = buildNode( map, property.getItemKlass(), collectionObject, user, defaults );

                            if ( node != null && !node.getChildren().isEmpty() )
                            {
                                child.addChild( node );
                            }
                        }
                    }
                    else
                    {
                        for ( Object collectionObject : collection )
                        {
                            SimpleNode simpleNode = child
                                .addChild( new SimpleNode( property.getName(), collectionObject ) );
                            simpleNode.setProperty( property );
                        }
                    }
                }
                else if ( property.isIdentifiableObject() && isProperIdObject( propertyClass ) )
                {
                    if ( !shouldExclude( returnValue, defaults ) )
                    {
                        child = getProperties( property, returnValue, fields );
                    }
                }
                else
                {
                    if ( property.getKlass().isAssignableFrom( Map.class ) )
                    {
                        child = handleMapProperty( returnValue, property );
                    }
                    else if ( propertySchema.getProperties().isEmpty() )
                    {
                        SimpleNode simpleNode = new SimpleNode( fieldKey, returnValue );
                        simpleNode.setAttribute( property.isAttribute() );
                        simpleNode.setNamespace( property.getNamespace() );

                        child = simpleNode;
                    }
                    else
                    {
                        child = buildNode( getFullFieldMap( propertySchema ), propertyClass, returnValue, user,
                            defaults );
                    }
                }
            }
            else
            {
                if ( property.isCollection() )
                {
                    child = new CollectionNode( property.getCollectionName() );
                    child.setNamespace( property.getNamespace() );

                    for ( Object collectionObject : (Collection<?>) Objects.requireNonNull( returnValue ) )
                    {
                        Node node;

                        if ( property.hasPropertyTransformer() )
                        {
                            // if it has a transformer, re-get the schema (the
                            // item klass has probably changed)
                            Schema sch = schemaService
                                .getDynamicSchema( HibernateProxyUtils.getRealClass( collectionObject ) );
                            node = buildNode( fieldValue, sch.getKlass(), collectionObject, user, property.getName(),
                                defaults );
                        }
                        else
                        {
                            node = buildNode( fieldValue, property.getItemKlass(), collectionObject, user,
                                property.getName(), defaults );
                        }

                        if ( !Objects.requireNonNull( node ).getChildren().isEmpty() )
                        {
                            child.addChild( node );
                        }
                    }
                }
                else
                {
                    returnValue = handleJsonbObjectProperties( klass, propertyClass, returnValue );
                    child = buildNode( fieldValue, propertyClass, returnValue, user, defaults );
                }
            }

            if ( child != null )
            {
                child.setName( fieldKey );
                child.setProperty( property );

                // TODO fix ugly hack, will be replaced by custom field
                // serializer/deserializer
                if ( child.isSimple() && (((SimpleNode) child).getValue()) instanceof PeriodType )
                {
                    child = new SimpleNode( child.getName(), ((PeriodType) ((SimpleNode) child).getValue()).getName() );
                }

                complexNode.addChild( fieldValue.getPipeline().process( child ) );
            }
        }

        return complexNode;
    }

    /**
     * Generate ComplexNode with Map structure based given inputMapObject. The
     * format of {@link org.hisp.dhis.user.sharing.Sharing#userGroups} is:
     *
     * <pre>
     * {@code
     * <userGroups>
     *  <B6JNeAQ6akX>
     *      <access>r-rw----</access>
     *      <id>B6JNeAQ6akX</id>
     *  </B6JNeAQ6akX>
     *  <GogLpGmkL0g>
     *      <access>r-rw----</access>
     *      <id>GogLpGmkL0g</id>
     *  </GogLpGmkL0g>
     * </userGroups>
     * }
     * </pre>
     *
     * @param inputMapObject
     *
     *        <pre>
     * {@code  Map<String, Object>}
     *        </pre>
     *
     * @param property {@link Property} of the given map object.
     * @return {@link ComplexNode}
     */
    private ComplexNode handleMapProperty( Object inputMapObject, Property property )
    {
        if ( inputMapObject == null
            || (!property.getKlass().isAssignableFrom( org.hisp.dhis.user.sharing.UserGroupAccess.class )
                && !property.getKlass().isAssignableFrom( org.hisp.dhis.user.sharing.UserAccess.class )) )
        {
            return null;
        }

        Map<String, Object> mapObject = (Map<String, Object>) inputMapObject;

        ComplexNode mapNode = new ComplexNode( property.getName() );

        if ( mapObject.entrySet().isEmpty() )
        {
            return null;
        }

        FieldMap fieldMap = null;

        for ( Entry<String, Object> item : mapObject.entrySet() )
        {
            if ( fieldMap == null )
            {
                fieldMap = getFullFieldMap(
                    schemaService.getDynamicSchema( item.getValue().getClass() ) );
            }

            ComplexNode mapItemNode = new ComplexNode( item.getKey() );

            for ( final String fieldName : fieldMap.keySet() )
            {
                final String originalName = org.apache.commons.lang3.StringUtils.substringBefore( fieldName, "~" );
                final String rename = org.apache.commons.lang3.StringUtils.substringBetween( fieldName, "(", ")" );
                final Object value = ReflectionUtils.invokeGetterMethod( originalName, item.getValue() );

                if ( org.apache.commons.lang3.StringUtils.isNotBlank( rename ) )
                {
                    mapItemNode.addChild( new SimpleNode( rename, value ) );
                }
                else
                {
                    mapItemNode.addChild( new SimpleNode( originalName, value ) );
                }
            }

            mapItemNode.addChild( mapItemNode );
        }
        return mapNode;
    }

    private void updateFields( FieldMap fieldMap, Class<?> klass )
    {
        if ( fieldMap.isEmpty() )
        {
            return;
        }

        // we need two run this (at least) two times, since some of the presets
        // might contain other presets
        updateFields( fieldMap, klass, true );
        updateFields( fieldMap, klass, false );
    }

    private void updateFields( FieldMap fieldMap, Class<?> klass, boolean expandOnly )
    {
        if ( fieldMap.isEmpty() )
        {
            return;
        }

        Schema schema = schemaService.getDynamicSchema( klass );
        List<String> cleanupFields = Lists.newArrayList();

        for ( String fieldKey : Sets.newHashSet( fieldMap.keySet() ) )
        {
            Collection<Property> properties = schema.getReadableProperties().values();

            if ( "*".equals( fieldKey ) )
            {
                properties.stream()
                    .filter( property -> !fieldMap.containsKey( property.key() ) )
                    .forEach( property -> fieldMap.put( property.key(), new FieldMap() ) );

                cleanupFields.add( fieldKey );
            }
            else if ( ":persisted".equals( fieldKey ) )
            {
                properties.stream()
                    .filter( property -> !fieldMap.containsKey( property.key() ) && property.isPersisted() )
                    .forEach( property -> fieldMap.put( property.key(), new FieldMap() ) );

                cleanupFields.add( fieldKey );
            }
            else if ( ":owner".equals( fieldKey ) )
            {
                properties.stream()
                    .filter( property -> !fieldMap.containsKey( property.key() ) && property.isPersisted()
                        && property.isOwner() )
                    .forEach( property -> fieldMap.put( property.key(), new FieldMap() ) );

                cleanupFields.add( fieldKey );
            }
            else if ( fieldKey.startsWith( ":" ) )
            {
                Preset preset = presets.get( fieldKey.substring( 1 ) );

                if ( preset == null )
                {
                    continue;
                }

                List<String> fields = preset.getFields();

                fields.stream()
                    .filter( field -> !fieldMap.containsKey( field ) )
                    .forEach( field -> fieldMap.put( field, new FieldMap() ) );

                cleanupFields.add( fieldKey );
            }
            else if ( fieldKey.startsWith( "!" ) && !expandOnly )
            {
                cleanupFields.add( fieldKey );
            }
            else if ( fieldKey.contains( "::" ) || fieldKey.contains( "|" ) || fieldKey.contains( "~" ) )
            {
                Matcher matcher = FIELD_PATTERN.matcher( fieldKey );

                if ( !matcher.find() )
                {
                    continue;
                }

                String fieldName = matcher.group( "field" );

                FieldMap value = new FieldMap();
                value.putAll( fieldMap.get( fieldKey ) );

                matcher = TRANSFORMER_PATTERN.matcher( fieldKey );

                while ( matcher.find() )
                {
                    String nameMatch = matcher.group( "name" );
                    String argsMatch = matcher.group( "args" );

                    if ( transformers.containsKey( nameMatch ) )
                    {
                        NodeTransformer transformer = transformers.get( nameMatch );
                        List<String> args = argsMatch == null ? new ArrayList<>()
                            : Lists.newArrayList( argsMatch.split( ";" ) );
                        value.getPipeline().addTransformer( transformer, args );
                    }
                }

                fieldMap.put( fieldName, value );

                cleanupFields.add( fieldKey );
            }
        }

        for ( String field : cleanupFields )
        {
            fieldMap.remove( field );

            if ( !expandOnly )
            {
                fieldMap.remove( field.substring( 1 ) );
            }
        }
    }

    private FieldMap getFullFieldMap( Schema schema )
    {
        FieldMap fieldMap = new FieldMap();

        for ( Property property : schema.getReadableProperties().values() )
        {
            fieldMap.put( property.getName(), new FieldMap() );
        }

        for ( String mapKey : schema.getPropertyMap().keySet() )
        {
            if ( schema.getProperty( mapKey ).isReadable() )
            {
                fieldMap.put( mapKey, new FieldMap() );
            }
        }

        return fieldMap;
    }

    private ComplexNode getProperties( Property currentProperty, Object object, List<String> fields )
    {
        if ( object == null )
        {
            return null;
        }

        // performance optimization for ID only queries on base identifiable
        // objects
        if ( isBaseIdentifiableObjectIdOnly( object, fields ) )
        {
            return createBaseIdentifiableObjectIdNode( currentProperty, object );
        }

        ComplexNode complexNode = new ComplexNode( currentProperty.getName() );
        complexNode.setNamespace( currentProperty.getNamespace() );
        complexNode.setProperty( currentProperty );

        Schema schema;

        if ( currentProperty.hasPropertyTransformer() )
        {
            schema = schemaService.getDynamicSchema( HibernateProxyUtils.getRealClass( object ) );
        }
        else if ( currentProperty.isCollection() )
        {
            schema = schemaService.getDynamicSchema( currentProperty.getItemKlass() );
        }
        else
        {
            schema = schemaService.getDynamicSchema( currentProperty.getKlass() );
        }

        for ( String field : fields )
        {
            Property property = schema.getProperty( field );

            if ( property == null )
            {
                continue;
            }

            Object returnValue = ReflectionUtils.invokeMethod( object, property.getGetterMethod() );

            SimpleNode simpleNode = new SimpleNode( field, returnValue );
            simpleNode.setAttribute( property.isAttribute() );
            simpleNode.setNamespace( property.getNamespace() );
            simpleNode.setProperty( property );

            complexNode.addChild( simpleNode );
        }

        return complexNode;
    }

    private boolean isBaseIdentifiableObjectIdOnly( @Nonnull Object object, @Nonnull List<String> fields )
    {
        return fields.size() == 1 && fields.get( 0 ).equals( "id" ) && object instanceof BaseIdentifiableObject;
    }

    private ComplexNode createBaseIdentifiableObjectIdNode( @Nonnull Property currentProperty, @Nonnull Object object )
    {
        return new ComplexNode( currentProperty, new SimpleNode(
            "id", baseIdentifiableIdProperty, ((BaseIdentifiableObject) object).getUid() ) );
    }

    private boolean isProperIdObject( Class<?> klass )
    {
        if ( UserCredentials.class.isAssignableFrom( klass ) || EmbeddedObject.class.isAssignableFrom( klass ) )
        {
            return false;
        }

        return IdentifiableObject.class.isAssignableFrom( klass );
    }

    /**
     * {@link AttributeValue} is saved as JSONB, and it contains only
     * Attribute's uid If fields parameter requires more than just Attribute's
     * uid then we need to get full {@link Attribute} object ( from cache ) e.g.
     * fields=id,name,attributeValues[value,attribute[id,name,description]]
     */
    private Object handleJsonbObjectProperties( Class<?> klass, Class<?> propertyClass, Object returnObject )
    {
        if ( AttributeValue.class.isAssignableFrom( klass ) && Attribute.class.isAssignableFrom( propertyClass ) )
        {
            returnObject = attributeService.getAttribute( ((Attribute) returnObject).getUid() );
        }

        return returnObject;
    }
}
