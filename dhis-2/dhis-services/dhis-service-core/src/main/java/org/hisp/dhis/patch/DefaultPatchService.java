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
package org.hisp.dhis.patch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
@RequiredArgsConstructor
@Transactional // TODO not sure if this can be completely readonly
public class DefaultPatchService implements PatchService
{
    private final SchemaService schemaService;

    private final QueryService queryService;

    @Override
    public Patch diff( PatchParams params )
    {
        if ( !params.haveJsonNode() )
        {
            return diff( params.getSource(), params.getTarget(), params.isIgnoreTransient() );
        }

        return diff( params.getJsonNode() );
    }

    @Override
    public void apply( Patch patch, Object target )
    {
        if ( target == null )
        {
            return;
        }

        Schema schema = schemaService.getDynamicSchema( HibernateProxyUtils.getRealClass( target ) );

        if ( schema == null )
        {
            return;
        }

        patch.getMutations().forEach( mutation -> applyMutation( mutation, schema, target ) );
    }

    private Patch diff( Object source, Object target, boolean ignoreTransient )
    {
        Patch patch = new Patch();

        if ( source == null || !HibernateProxyUtils.getRealClass( source ).isInstance( target ) )
        {
            return patch;
        }

        Schema schema = schemaService.getDynamicSchema( HibernateProxyUtils.getRealClass( target ) );

        if ( schema == null )
        {
            return patch;
        }

        patch.setMutations( calculateMutations( schema, source, target, ignoreTransient ) );

        return patch;
    }

    private Patch diff( JsonNode jsonNode )
    {
        Patch patch = new Patch();
        patch.setMutations( calculateMutations( jsonNode ) );

        return patch;
    }

    private List<Mutation> calculateMutations( Schema schema, Object source, Object target, boolean ignoreTransient )
    {
        List<Mutation> mutations = new ArrayList<>();

        for ( Property property : schema.getProperties() )
        {
            if ( ignoreTransient && !property.isOwner() )
            {
                continue;
            }
            mutations.addAll( calculateMutation( property.key(), property, source, target ) );
        }

        return mutations;
    }

    @SuppressWarnings( "unchecked" )
    private List<Mutation> calculateMutation( String path, Property property, Object source, Object target )
    {
        Object sourceValue = ReflectionUtils.invokeMethod( source, property.getGetterMethod() );
        Object targetValue = ReflectionUtils.invokeMethod( target, property.getGetterMethod() );
        List<Mutation> mutations = new ArrayList<>();

        if ( sourceValue == null && targetValue == null )
        {
            return mutations;
        }

        if ( targetValue == null || sourceValue == null )
        {
            return Lists.newArrayList( new Mutation( path, targetValue ) );
        }

        if ( property.isCollection() && property.isIdentifiableObject() && !property.isEmbeddedObject() )
        {
            Collection<Object> addCollection = ReflectionUtils.newCollectionInstance( property.getKlass() );

            Collection<Object> sourceCollection = ((Collection<Object>) sourceValue).stream()
                .filter( Objects::nonNull )
                .map( o -> ((IdentifiableObject) o).getUid() ).collect( Collectors.toList() );

            Collection<Object> targetCollection = ((Collection<Object>) targetValue).stream()
                .filter( Objects::nonNull )
                .map( o -> ((IdentifiableObject) o).getUid() ).collect( Collectors.toList() );

            for ( Object o : targetCollection )
            {
                if ( !sourceCollection.contains( o ) )
                {
                    addCollection.add( o );
                }
                else
                {
                    sourceCollection.remove( o );
                }
            }

            if ( !addCollection.isEmpty() )
            {
                mutations.add( new Mutation( path, addCollection ) );
            }

            Collection<Object> delCollection = ReflectionUtils.newCollectionInstance( property.getKlass() );
            delCollection.addAll( sourceCollection );

            if ( !delCollection.isEmpty() )
            {
                mutations.add( new Mutation( path, delCollection, Mutation.Operation.DELETION ) );
            }
        }
        else if ( property.isCollection() && !property.isEmbeddedObject() && !property.isIdentifiableObject() )
        {
            List<Object> sourceCollection = new ArrayList<>( (Collection<Object>) sourceValue );
            Collection<Object> targetCollection = (Collection<Object>) targetValue;

            Collection<Object> addCollection = ReflectionUtils.newCollectionInstance( property.getKlass() );

            for ( Object o : targetCollection )
            {
                if ( !sourceCollection.contains( o ) )
                {
                    addCollection.add( o );
                }
                else
                {
                    sourceCollection.remove( o );
                }
            }

            if ( !addCollection.isEmpty() )
            {
                mutations.add( new Mutation( path, addCollection ) );
            }

            Collection<Object> delCollection = ReflectionUtils.newCollectionInstance( property.getKlass() );
            delCollection.addAll( sourceCollection );

            if ( !delCollection.isEmpty() )
            {
                mutations.add( new Mutation( path, delCollection, Mutation.Operation.DELETION ) );
            }
        }
        else if ( property.isSimple() || property.isEmbeddedObject() )
        {
            if ( !targetValue.equals( sourceValue ) )
            {
                return Lists.newArrayList( new Mutation( path, targetValue ) );
            }
        }

        return mutations;
    }

    private List<Mutation> calculateMutations( JsonNode rootNode )
    {
        List<Mutation> mutations = new ArrayList<>();
        List<String> fieldNames = Lists.newArrayList( rootNode.fieldNames() );

        for ( String fieldName : fieldNames )
        {
            JsonNode node = rootNode.get( fieldName );
            mutations.addAll( calculateMutations( fieldName, node ) );
        }

        return mutations;
    }

    private List<Mutation> calculateMutations( String path, JsonNode node )
    {
        List<Mutation> mutations = new ArrayList<>();

        switch ( node.getNodeType() )
        {
        case OBJECT:
            List<String> fieldNames = Lists.newArrayList( node.fieldNames() );

            for ( String fieldName : fieldNames )
            {
                mutations.addAll( calculateMutations( path + "." + fieldName, node.get( fieldName ) ) );
            }

            break;
        case ARRAY:
            Collection<Object> identifiers = new ArrayList<>();

            for ( JsonNode jsonNode : node )
            {
                identifiers.add( getValue( jsonNode ) );
            }

            mutations.add( new Mutation( path, identifiers ) );
            break;
        default:
            mutations.add( new Mutation( path, getValue( node ) ) );
            break;
        }

        return mutations;
    }

    private Object getValue( JsonNode node )
    {
        switch ( node.getNodeType() )
        {
        case BOOLEAN:
            return node.booleanValue();
        case NUMBER:
            return node.numberValue();
        case STRING:
            return node.textValue();
        case NULL:
            return null;
        }

        return null;
    }

    private void applyMutation( Mutation mutation, Schema schema, Object target )
    {
        String path = mutation.getPath();
        String[] paths = path.split( "\\." );

        Schema currentSchema = schema;
        Property currentProperty = null;
        Object currentTarget = target;

        for ( int i = 0; i < paths.length; i++ )
        {
            if ( !currentSchema.hasProperty( paths[i] ) )
            {
                return;
            }

            currentProperty = currentSchema.getProperty( paths[i] );

            if ( currentProperty == null )
            {
                return;
            }

            if ( (currentProperty.isSimple() && !currentProperty.isCollection()) && i != (paths.length - 1) )
            {
                return;
            }

            if ( currentProperty.isCollection() )
            {
                currentSchema = schemaService.getDynamicSchema( currentProperty.getItemKlass() );
            }
            else
            {
                currentSchema = schemaService.getDynamicSchema( currentProperty.getKlass() );
            }

            if ( i < (paths.length - 1) )
            {
                currentTarget = ReflectionUtils.invokeMethod( currentTarget, currentProperty.getGetterMethod() );
            }
        }

        if ( currentSchema != null && currentProperty != null )
        {
            applyMutation( mutation, currentProperty, currentTarget );
        }
    }

    // TODO fix type cast from object to T
    @SuppressWarnings( "unchecked" )
    private <T extends Comparable<? super T>> void applyMutation( Mutation mutation, Property property, Object target )
    {
        Object value = mutation.getValue();

        if ( property.isCollection() )
        {
            Collection<Object> collection = ReflectionUtils.invokeMethod( target, property.getGetterMethod() );
            Collection<Object> sourceCollection = Collection.class.isInstance( value ) ? (Collection<Object>) value
                : Lists.newArrayList( value );

            if ( collection == null )
            {
                collection = ReflectionUtils.newCollectionInstance( property.getKlass() );
            }

            for ( Object o : sourceCollection )
            {
                Object object = o;

                if ( property.isIdentifiableObject() && !property.isEmbeddedObject() )
                {
                    if ( !(object instanceof String) )
                    {
                        return;
                    }

                    Schema schema = schemaService.getDynamicSchema( property.getItemKlass() );

                    Query query = Query.from( schema );

                    query.add( Restrictions.eq( "id", (T) object ) ); // optimize
                                                                     // by
                                                                     // using
                                                                     // .in(..)
                                                                     // query

                    List<? extends IdentifiableObject> objects = queryService.query( query );

                    if ( objects.size() != 1 )
                    {
                        return;
                    }

                    object = objects.get( 0 );
                }

                // validate type
                if ( !property.getItemKlass().isInstance( object ) )
                {
                    return;
                }

                if ( Mutation.Operation.ADDITION == mutation.getOperation() )
                {
                    if ( !collection.contains( object ) )
                    {
                        collection.add( object );
                    }
                }
                else if ( Mutation.Operation.DELETION == mutation.getOperation() )
                {
                    if ( collection.contains( object ) )
                    {
                        collection.remove( object );
                    }
                }
            }

            ReflectionUtils.invokeMethod( target, property.getSetterMethod(), collection );
        }
        else if ( property.isIdentifiableObject() && !property.isEmbeddedObject() )
        {
            if ( !String.class.isInstance( value ) )
            {
                return;
            }

            Schema schema = schemaService.getDynamicSchema( property.getKlass() );

            Query query = Query.from( schema );
            query.add( Restrictions.eq( "id", (T) value ) );

            List<? extends IdentifiableObject> objects = queryService.query( query );

            if ( objects.size() != 1 )
            {
                return;
            }

            value = objects.get( 0 );

            // validate type
            if ( !property.getKlass().isInstance( value ) )
            {
                return;
            }

            ReflectionUtils.invokeMethod( target, property.getSetterMethod(), value );
        }
        else
        {
            value = parseValue( value, property.getKlass() );

            // validate type
            if ( !property.getKlass().isInstance( value ) )
            {
                return;
            }

            ReflectionUtils.invokeMethod( target, property.getSetterMethod(), value );
        }
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private Object parseValue( Object value, Class<?> klass )
    {
        if ( klass.isInstance( value ) || !String.class.isInstance( value ) )
        {
            return value;
        }

        String stringValue = (String) value;

        if ( Integer.class.isAssignableFrom( klass ) )
        {
            try
            {
                return Integer.valueOf( stringValue );
            }
            catch ( Exception ex )
            {
            }
        }
        else if ( Boolean.class.isAssignableFrom( klass ) )
        {
            try
            {
                return Boolean.valueOf( stringValue );
            }
            catch ( Exception ex )
            {
            }
        }
        else if ( Float.class.isAssignableFrom( klass ) )
        {
            try
            {
                return Float.valueOf( stringValue );
            }
            catch ( Exception ex )
            {
            }
        }
        else if ( Double.class.isAssignableFrom( klass ) )
        {
            try
            {
                return Double.valueOf( stringValue );
            }
            catch ( Exception ex )
            {
            }
        }
        else if ( Date.class.isAssignableFrom( klass ) )
        {
            try
            {
                return DateUtils.parseDate( stringValue );
            }
            catch ( Exception ex )
            {
            }
        }
        if ( Enum.class.isAssignableFrom( klass ) )
        {
            Optional<? extends Enum<?>> enumValue = Enums.getIfPresent( (Class<? extends Enum>) klass, stringValue );

            if ( enumValue.isPresent() )
            {
                return enumValue.get();
            }
        }

        return null;
    }
}
