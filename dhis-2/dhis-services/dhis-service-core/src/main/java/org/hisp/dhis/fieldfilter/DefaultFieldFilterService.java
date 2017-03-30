package org.hisp.dhis.fieldfilter;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.node.AbstractNode;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.NodeTransformer;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.user.UserCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultFieldFilterService implements FieldFilterService
{
    private static final Log log = LogFactory.getLog( DefaultFieldFilterService.class );

    private final Pattern FIELD_PATTERN = Pattern.compile( "^(?<field>\\w+)" );

    private final Pattern TRANSFORMER_PATTERN = Pattern.compile( "(?<type>\\||::|~)(?<name>\\w+)(?:\\((?<args>[\\w;]+)\\))?" );

    @Autowired
    private FieldParser fieldParser;

    @Autowired
    private SchemaService schemaService;

    @Autowired( required = false )
    private Set<NodeTransformer> nodeTransformers = new HashSet<>();

    private ImmutableMap<String, Preset> presets = ImmutableMap.of();

    private ImmutableMap<String, NodeTransformer> transformers = ImmutableMap.of();

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
    }

    @Override
    public ComplexNode filter( Object object, List<String> fieldList )
    {
        Assert.notNull( object, "Object cannot be null" );

        CollectionNode collectionNode = filter( object.getClass(), Lists.newArrayList( object ), fieldList );

        if ( collectionNode.getChildren().size() > 0 )
        {
            return (ComplexNode) collectionNode.getChildren().get( 0 );
        }

        return null;
    }

    @Override
    public CollectionNode filter( Class<?> klass, List<?> objects, List<String> fieldList )
    {
        String fields = fieldList == null ? "" : Joiner.on( "," ).join( fieldList );

        Schema rootSchema = schemaService.getDynamicSchema( klass );

        CollectionNode collectionNode = new CollectionNode( rootSchema.getCollectionName() );
        collectionNode.setNamespace( rootSchema.getNamespace() );

        if ( objects == null || objects.isEmpty() )
        {
            return collectionNode;
        }

        FieldMap fieldMap = new FieldMap();
        Schema schema = schemaService.getDynamicSchema( objects.get( 0 ).getClass() );

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
        objects.forEach( object -> collectionNode.addChild( buildNode( finalFieldMap, klass, object ) ) );

        return collectionNode;
    }

    private AbstractNode buildNode( FieldMap fieldMap, Class<?> klass, Object object )
    {
        Schema schema = schemaService.getDynamicSchema( klass );
        return buildNode( fieldMap, klass, object, schema.getName() );
    }

    private AbstractNode buildNode( FieldMap fieldMap, Class<?> klass, Object object, String nodeName )
    {
        Schema schema = schemaService.getDynamicSchema( klass );

        ComplexNode complexNode = new ComplexNode( nodeName );
        complexNode.setNamespace( schema.getNamespace() );

        if ( object == null )
        {
            return new SimpleNode( schema.getName(), null );
        }

        updateFields( fieldMap, schema.getKlass() );

        for ( String fieldKey : fieldMap.keySet() )
        {
            AbstractNode child;
            Property property = schema.getProperty( fieldKey );

            if ( property == null || !property.isReadable() )
            {
                // throw new FieldFilterException( fieldKey, schema );
                log.debug( "Unknown field property `" + fieldKey + "`, available fields are " + schema.getPropertyMap().keySet() );
                continue;
            }

            Object returnValue = ReflectionUtils.invokeMethod( object, property.getGetterMethod() );
            Schema propertySchema = schemaService.getDynamicSchema( property.getKlass() );

            FieldMap fieldValue = fieldMap.get( fieldKey );

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
                updateFields( fieldValue, property.getKlass() );
            }

            if ( fieldValue.isEmpty() )
            {
                List<String> fields = Preset.defaultAssociationPreset().getFields();

                if ( property.isCollection() )
                {
                    Collection<?> collection = (Collection<?>) returnValue;

                    child = new CollectionNode( property.getCollectionName() );
                    child.setNamespace( property.getNamespace() );

                    if ( property.isIdentifiableObject() && isProperIdObject( property.getItemKlass() ) )
                    {
                        for ( Object collectionObject : collection )
                        {
                            child.addChild( getProperties( property, collectionObject, fields ) );
                        }
                    }
                    else if ( !property.isSimple() )
                    {
                        FieldMap map = getFullFieldMap( schemaService.getDynamicSchema( property.getItemKlass() ) );

                        for ( Object collectionObject : collection )
                        {
                            Node node = buildNode( map, property.getItemKlass(), collectionObject );

                            if ( !node.getChildren().isEmpty() )
                            {
                                child.addChild( node );
                            }
                        }
                    }
                    else
                    {
                        if ( collection != null )
                        {
                            for ( Object collectionObject : collection )
                            {
                                SimpleNode simpleNode = child.addChild( new SimpleNode( property.getName(), collectionObject ) );
                                simpleNode.setProperty( property );
                            }
                        }
                    }
                }
                else if ( property.isIdentifiableObject() && isProperIdObject( property.getKlass() ) )
                {
                    child = getProperties( property, returnValue, fields );
                }
                else
                {
                    if ( propertySchema.getProperties().isEmpty() )
                    {
                        SimpleNode simpleNode = new SimpleNode( fieldKey, returnValue );
                        simpleNode.setAttribute( property.isAttribute() );
                        simpleNode.setNamespace( property.getNamespace() );

                        child = simpleNode;
                    }
                    else
                    {
                        child = buildNode( getFullFieldMap( propertySchema ), property.getKlass(), returnValue );
                    }
                }
            }
            else
            {
                if ( property.isCollection() )
                {
                    child = new CollectionNode( property.getCollectionName() );
                    child.setNamespace( property.getNamespace() );

                    for ( Object collectionObject : (Collection<?>) returnValue )
                    {
                        Node node = buildNode( fieldValue, property.getItemKlass(), collectionObject, property.getName() );

                        if ( !node.getChildren().isEmpty() )
                        {
                            child.addChild( node );
                        }
                    }
                }
                else
                {
                    child = buildNode( fieldValue, property.getKlass(), returnValue );
                }
            }

            if ( child != null )
            {
                child.setName( fieldKey );
                child.setProperty( property );

                // TODO fix ugly hack, will be replaced by custom field serializer/deserializer
                if ( child.isSimple() && PeriodType.class.isInstance( (((SimpleNode) child).getValue()) ) )
                {
                    child = new SimpleNode( child.getName(), ((PeriodType) ((SimpleNode) child).getValue()).getName() );
                }

                complexNode.addChild( fieldValue.getPipeline().process( child ) );
            }
        }

        return complexNode;
    }

    private void updateFields( FieldMap fieldMap, Class<?> klass )
    {
        // we need two run this (at least) two times, since some of the presets might contain other presets
        updateFields( fieldMap, klass, true );
        updateFields( fieldMap, klass, false );
    }

    private void updateFields( FieldMap fieldMap, Class<?> klass, boolean expandOnly )
    {
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
                    .filter( property -> !fieldMap.containsKey( property.key() ) && property.isPersisted() && property.isOwner() )
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

                matcher = TRANSFORMER_PATTERN.matcher( fieldKey );

                while ( matcher.find() )
                {
                    String nameMatch = matcher.group( "name" );
                    String argsMatch = matcher.group( "args" );

                    if ( transformers.containsKey( nameMatch ) )
                    {
                        NodeTransformer transformer = transformers.get( nameMatch );
                        List<String> args = argsMatch == null ? new ArrayList<>() : Lists.newArrayList( argsMatch.split( ";" ) );
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

        ComplexNode complexNode = new ComplexNode( currentProperty.getName() );
        complexNode.setNamespace( currentProperty.getNamespace() );
        complexNode.setProperty( currentProperty );

        Schema schema;

        if ( currentProperty.isCollection() )
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

    private boolean isProperIdObject( Class<?> klass )
    {
        return !(UserCredentials.class.isAssignableFrom( klass ) || EmbeddedObject.class.isAssignableFrom( klass ));
    }
}
