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
package org.hisp.dhis.fieldfiltering;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.fieldfiltering.transformers.IsEmptyFieldTransformer;
import org.hisp.dhis.fieldfiltering.transformers.IsNotEmptyFieldTransformer;
import org.hisp.dhis.fieldfiltering.transformers.KeyByFieldTransformer;
import org.hisp.dhis.fieldfiltering.transformers.PluckFieldTransformer;
import org.hisp.dhis.fieldfiltering.transformers.RenameFieldTransformer;
import org.hisp.dhis.fieldfiltering.transformers.SizeFieldTransformer;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.OrderComparator;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * @author Morten Olav Hansen
 */
@Service
public class FieldFilterService
{
    private final FieldPathHelper fieldPathHelper;

    @Qualifier( "jsonMapper" )
    private final ObjectMapper jsonMapper;

    private final SchemaService schemaService;

    private final AclService aclService;

    private final CurrentUserService currentUserService;

    private final UserGroupService userGroupService;

    private final UserService userService;

    private final AttributeService attributeService;

    public FieldFilterService(
        FieldPathHelper fieldPathHelper,
        ObjectMapper jsonMapper,
        SchemaService schemaService,
        AclService aclService,
        CurrentUserService currentUserService,
        UserGroupService userGroupService,
        UserService userService,
        AttributeService attributeService )
    {
        this.fieldPathHelper = fieldPathHelper;
        this.jsonMapper = configureFieldFilterObjectMapper( jsonMapper );
        this.schemaService = schemaService;
        this.aclService = aclService;
        this.currentUserService = currentUserService;
        this.userGroupService = userGroupService;
        this.userService = userService;
        this.attributeService = attributeService;
    }

    private ObjectMapper configureFieldFilterObjectMapper( ObjectMapper objectMapper )
    {
        objectMapper = objectMapper.copy();

        SimpleModule module = new SimpleModule();
        module.setMixInAnnotation( Object.class, FieldFilterMixin.class );

        objectMapper.registerModule( module );
        objectMapper.setAnnotationIntrospector( new IgnoreJsonSerializerRefinementAnnotationInspector() );

        return objectMapper;
    }

    /**
     * Determines whether given path is included in the resulting ObjectNodes
     * after applying {@link #toObjectNode(Object, List)}. This obviously
     * requires that the actual data contains such path when filtered.
     * <p>
     * For example given a structure like
     * <p>
     * <code>{"event": "relationships": [] }</code>
     * </p>
     * and a
     * <p>
     * <code>filter="*,first[second[!third]]"</code>
     * </p>
     * <p>
     * both paths<code>first</code> and <code>first.second</code> will result in
     * true as they will be included in the filtered result. While
     * <code>first.second.third</code> will result in false.
     * </p>
     *
     * @param rootClass class the filter will be applied on
     * @param filter field paths to be applied on the class
     * @param path path to check for inclusion in the filter
     * @return true if path is included in filter
     */
    public boolean filterIncludes( Class<?> rootClass, List<FieldPath> filter, String path )
    {
        return fieldPathHelper.apply( filter, rootClass ).stream()
            .anyMatch( f -> f.toFullPath().equals( path ) );
    }

    private static class IgnoreJsonSerializerRefinementAnnotationInspector extends JacksonAnnotationIntrospector
    {
        /**
         * Since the field filter will handle type refinement itself (to avoid
         * recursive loops), we want to ignore any type refinement happening
         * with @JsonSerialize(...). In the future we would want to remove
         * all @JsonSerialize annotations and just use the field filters, but
         * since we still have object mappers without field filtering we can't
         * do this just yet.
         */
        @Override
        public JavaType refineSerializationType( MapperConfig<?> config, Annotated a, JavaType baseType )
        {
            return baseType;
        }
    }

    public <T> ObjectNode toObjectNode( T object, String filters )
    {
        List<FieldPath> fieldPaths = FieldFilterParser.parse( filters );
        return toObjectNode( object, fieldPaths );
    }

    public <T> ObjectNode toObjectNode( T object, List<FieldPath> filters )
    {
        List<ObjectNode> objectNodes = toObjectNodes( List.of( object ), filters, null, false );

        if ( objectNodes.isEmpty() )
        {
            return null;
        }

        return objectNodes.get( 0 );
    }

    public List<ObjectNode> toObjectNodes( FieldFilterParams<?> params )
    {
        List<ObjectNode> objectNodes = new ArrayList<>();

        if ( params.getObjects().isEmpty() )
        {
            return objectNodes;
        }

        List<FieldPath> fieldPaths = FieldFilterParser.parse( params.getFilters() );
        return toObjectNodes( params.getObjects(), fieldPaths, params.getUser(), params.isSkipSharing() );
    }

    public <T> List<ObjectNode> toObjectNodes( List<T> objects, List<FieldPath> fieldPaths )
    {
        return toObjectNodes( objects, fieldPaths, null, false );
    }

    private <T> List<ObjectNode> toObjectNodes( List<T> objects, List<FieldPath> fieldPaths, User user,
        boolean isSkipSharing )
    {
        List<ObjectNode> objectNodes = new ArrayList<>();

        if ( objects.isEmpty() )
        {
            return objectNodes;
        }

        toObjectNodes( objects, fieldPaths, user, isSkipSharing, objectNodes::add );

        return objectNodes;
    }

    private <T> void toObjectNodes( List<T> objects, List<FieldPath> filter, User user, boolean isSkipSharing,
        Consumer<ObjectNode> consumer )
    {
        if ( user == null )
        {
            user = currentUserService.getCurrentUser();
        }

        // In case we get a proxied object in we can't just use o.getClass(), we
        // need to figure out the real class name by using HibernateProxyUtils.
        Object firstObject = objects.iterator().next();
        List<FieldPath> paths = fieldPathHelper.apply( filter, HibernateProxyUtils.getRealClass( firstObject ) );

        SimpleFilterProvider filterProvider = getSimpleFilterProvider( paths, isSkipSharing );

        // only set filter provider on a local copy so that we don't affect
        // other object mappers (running across other threads)
        ObjectMapper objectMapper = jsonMapper.copy().setFilterProvider( filterProvider );

        Map<String, List<FieldTransformer>> fieldTransformers = getTransformers( paths );

        for ( Object object : objects )
        {
            applyAccess( object, paths, isSkipSharing, user );
            applySharingDisplayNames( object, paths, isSkipSharing );
            applyAttributeValuesAttribute( object, paths, isSkipSharing );

            ObjectNode objectNode = objectMapper.valueToTree( object );
            applyAttributeValueFields( object, objectNode, paths );
            applyTransformers( objectNode, null, "", fieldTransformers );

            consumer.accept( objectNode );
        }
    }

    /**
     * Streams filtered object nodes using given JsonGenerator.
     *
     * @param params Filter params to apply
     * @param generator Pre-created json generator
     * @throws IOException
     */
    public void toObjectNodesStream( FieldFilterParams<?> params, JsonGenerator generator )
        throws IOException
    {
        if ( params.getObjects().isEmpty() )
        {
            return;
        }

        if ( params.getUser() == null )
        {
            params.setUser( currentUserService.getCurrentUser() );
        }

        List<FieldPath> fieldPaths = FieldFilterParser.parse( params.getFilters() );

        // In case we get a proxied object in we can't just use o.getClass(), we
        // need to figure out the real class name by using HibernateProxyUtils.
        Object firstObject = params.getObjects().iterator().next();
        fieldPathHelper.apply( fieldPaths, HibernateProxyUtils.getRealClass( firstObject ) );

        SimpleFilterProvider filterProvider = getSimpleFilterProvider( fieldPaths, params.isSkipSharing() );

        // only set filter provider on a local copy so that we don't affect
        // other object mappers (running across other threads)
        ObjectMapper objectMapper = jsonMapper.copy().setFilterProvider( filterProvider );

        Map<String, List<FieldTransformer>> fieldTransformers = getTransformers( fieldPaths );

        for ( Object object : params.getObjects() )
        {
            applyAccess( object, fieldPaths, params.isSkipSharing(), params.getUser() );
            applySharingDisplayNames( object, fieldPaths, params.isSkipSharing() );
            applyAttributeValuesAttribute( object, fieldPaths, params.isSkipSharing() );

            ObjectNode objectNode = objectMapper.valueToTree( object );
            applyAttributeValueFields( object, objectNode, fieldPaths );
            applyTransformers( objectNode, null, "", fieldTransformers );

            generator.writeObject( objectNode );
        }
    }

    private void applyAttributeValueFields( Object object, ObjectNode objectNode, List<FieldPath> fieldPaths )
    {
        if ( !(object instanceof BaseIdentifiableObject) )
        {
            return;
        }
        for ( FieldPath path : fieldPaths )
        {
            if ( path.getProperty() == null && CodeGenerator.isValidUid( path.getFullPath() ) )
            {
                AttributeValue value = ((BaseIdentifiableObject) object).getAttributeValue( path.getFullPath() );
                if ( value != null )
                {
                    String v = value.getValue();
                    Attribute attribute = attributeService.getAttribute( value.getAttribute().getUid() );
                    if ( v != null && !v.isBlank() && attribute.getValueType().isJson() )
                    {
                        try
                        {
                            objectNode.set( path.getFullPath(), jsonMapper.readTree( v ) );
                        }
                        catch ( JsonProcessingException e )
                        {
                            objectNode.put( path.getFullPath(), v );
                        }
                    }
                    else
                    {
                        objectNode.put( path.getFullPath(), v );
                    }
                }
            }
        }
    }

    private void applyFieldPathVisitor( Object object, List<FieldPath> fieldPaths,
        boolean isSkipSharing, Predicate<String> filter, Consumer<Object> consumer )
    {
        if ( object == null || isSkipSharing )
        {
            return;
        }

        Schema schema = schemaService.getDynamicSchema( HibernateProxyUtils.getRealClass( object ) );

        if ( !schema.isIdentifiableObject() )
        {
            return;
        }

        fieldPaths.forEach( fp -> {
            if ( filter.test( fp.toFullPath() ) )
            {
                fieldPathHelper.visitFieldPaths( object, List.of( fp ), consumer );
            }
        } );
    }

    public ObjectNode createObjectNode()
    {
        return jsonMapper.createObjectNode();
    }

    public ArrayNode createArrayNode()
    {
        return jsonMapper.createArrayNode();
    }

    /**
     * Recursively applies FieldTransformers to a Json node.
     */
    private void applyTransformers( JsonNode node, JsonNode parent, String path,
        Map<String, List<FieldTransformer>> fieldTransformers )
    {
        if ( parent != null && !parent.isArray() && !path.isEmpty() )
        {
            List<FieldTransformer> transformers = fieldTransformers.get( path.substring( 1 ) );

            if ( transformers != null )
            {
                transformers.forEach( tf -> tf.apply( path.substring( 1 ), node, parent ) );
            }
        }

        if ( node.isObject() )
        {
            ObjectNode objectNode = (ObjectNode) node;

            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining( fieldNames::add );

            for ( String fieldName : fieldNames )
            {
                applyTransformers( objectNode.get( fieldName ), objectNode, path + "." + fieldName, fieldTransformers );
            }
        }
        else if ( node.isArray() )
        {
            ArrayNode arrayNode = (ArrayNode) node;

            for ( JsonNode item : arrayNode )
            {
                applyTransformers( item, arrayNode, path, fieldTransformers );
            }
        }
    }

    private SimpleFilterProvider getSimpleFilterProvider( List<FieldPath> fieldPaths, boolean skipSharing )
    {
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter( "field-filter", new FieldFilterSimpleBeanPropertyFilter( fieldPaths, skipSharing ) );

        return filterProvider;
    }

    private Map<String, List<FieldTransformer>> getTransformers( List<FieldPath> fieldPaths )
    {
        Map<String, List<FieldTransformer>> transformerMap = new HashMap<>();

        for ( FieldPath fieldPath : fieldPaths )
        {
            List<FieldTransformer> fieldTransformers = new ArrayList<>();
            String fullPath = fieldPath.toFullPath();

            transformerMap.put( fullPath, fieldTransformers );

            for ( FieldPathTransformer fieldPathTransformer : fieldPath.getTransformers() )
            {
                switch ( fieldPathTransformer.getName() )
                {
                case "rename":
                    fieldTransformers.add( new RenameFieldTransformer( fieldPathTransformer ) );
                    break;
                case "size":
                    fieldTransformers.add( SizeFieldTransformer.INSTANCE );
                    break;
                case "isEmpty":
                    fieldTransformers.add( IsEmptyFieldTransformer.INSTANCE );
                    break;
                case "isNotEmpty":
                    fieldTransformers.add( IsNotEmptyFieldTransformer.INSTANCE );
                    break;
                case "pluck":
                    fieldTransformers.add( new PluckFieldTransformer( fieldPathTransformer ) );
                    break;
                case "keyBy":
                    fieldTransformers.add( new KeyByFieldTransformer( fieldPathTransformer ) );
                    break;
                default:
                    // invalid transformer
                    break;
                }
            }

            fieldTransformers.sort( OrderComparator.INSTANCE );
        }

        return transformerMap;
    }

    private void applyAttributeValuesAttribute( Object object, List<FieldPath> fieldPaths, boolean isSkipSharing )
    {
        applyFieldPathVisitor( object, fieldPaths, isSkipSharing,
            s -> s.equals( "attributeValues.attribute" ) || s.endsWith( ".attributeValues.attribute" ),
            o -> {
                if ( o instanceof AttributeValue )
                {
                    ((AttributeValue) o).setAttribute(
                        attributeService.getAttribute( ((AttributeValue) o).getAttribute().getUid() ) );
                }
            } );
    }

    private void applySharingDisplayNames( Object root, List<FieldPath> fieldPaths,
        boolean isSkipSharing )
    {
        applyFieldPathVisitor( root, fieldPaths, isSkipSharing,
            s -> s.contains( "sharing" )
                || s.equals( "userGroupAccesses" ) || s.endsWith( ".userGroupAccesses" )
                || s.equals( "userGroupAccesses.displayName" ) || s.endsWith( ".userGroupAccesses.displayName" )
                || s.equals( "userAccesses" ) || s.endsWith( ".userAccesses" )
                || s.equals( "userAccesses.displayName" ) || s.endsWith( ".userAccesses.displayName" ),
            o -> {
                if ( root instanceof BaseIdentifiableObject )
                {
                    ((BaseIdentifiableObject) root).getSharing().getUserGroups().values()
                        .forEach( uga -> uga.setDisplayName( userGroupService.getDisplayName( uga.getId() ) ) );
                    ((BaseIdentifiableObject) root).getSharing().getUsers().values()
                        .forEach( ua -> ua.setDisplayName( userService.getDisplayName( ua.getId() ) ) );
                }

                if ( o instanceof BaseIdentifiableObject )
                {
                    ((BaseIdentifiableObject) o).getSharing().getUserGroups().values()
                        .forEach( uga -> uga.setDisplayName( userGroupService.getDisplayName( uga.getId() ) ) );
                    ((BaseIdentifiableObject) o).getSharing().getUsers().values()
                        .forEach( ua -> ua.setDisplayName( userService.getDisplayName( ua.getId() ) ) );
                }
            } );
    }

    private void applyAccess( Object object, List<FieldPath> fieldPaths, boolean isSkipSharing, User user )
    {
        applyFieldPathVisitor( object, fieldPaths, isSkipSharing, s -> s.equals( "access" ) || s.endsWith( ".access" ),
            o -> {
                if ( o instanceof BaseIdentifiableObject )
                {
                    ((BaseIdentifiableObject) o)
                        .setAccess( aclService.getAccess( ((IdentifiableObject) o), user ) );
                }
            } );
    }

}
