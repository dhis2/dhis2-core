package org.hisp.dhis.schema.patch;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.amqp.AmqpService;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.audit.MetadataAudit;
import org.hisp.dhis.schema.audit.MetadataAuditService;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.user.CurrentUserService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultPatchService implements PatchService
{
    private static final Log log = LogFactory.getLog( DefaultPatchService.class );

    private final SchemaService schemaService;

    private final QueryService queryService;

    private final AmqpService amqpService;

    private final MetadataAuditService metadataAuditService;

    private final CurrentUserService currentUserService;

    private final RenderService renderService;

    private final SystemService systemService;

    public DefaultPatchService( SchemaService schemaService, QueryService queryService, AmqpService amqpService,
        MetadataAuditService metadataAuditService, CurrentUserService currentUserService, RenderService renderService, SystemService systemService )
    {
        this.schemaService = schemaService;
        this.queryService = queryService;
        this.amqpService = amqpService;
        this.metadataAuditService = metadataAuditService;
        this.currentUserService = currentUserService;
        this.renderService = renderService;
        this.systemService = systemService;
    }

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

        Schema schema = schemaService.getDynamicSchema( target.getClass() );

        if ( schema == null )
        {
            return;
        }

        patch.getMutations().forEach( mutation -> applyMutation( mutation, schema, target ) );

        if ( !patch.getMutations().isEmpty() )
        {
            logAudit( patch, target, schema );
        }
    }

    private Patch diff( Object source, Object target, boolean ignoreTransient )
    {
        Patch patch = new Patch();

        if ( source == null || target == null || !source.getClass().isInstance( target ) )
        {
            return patch;
        }

        Schema schema = schemaService.getDynamicSchema( target.getClass() );

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

            if ( property.isCollection() )
            {
                mutations.addAll( calculateMutation( property.getCollectionName(), property, source, target ) );
            }
            else
            {
                mutations.addAll( calculateMutation( property.getName(), property, source, target ) );
            }
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
            Collection addCollection = ReflectionUtils.newCollectionInstance( property.getKlass() );

            Collection sourceCollection = (Collection) ((Collection) sourceValue).stream()
                .map( o -> ((IdentifiableObject) o).getUid() ).collect( Collectors.toList() );

            Collection targetCollection = (Collection) ((Collection) targetValue).stream()
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

            Collection delCollection = ReflectionUtils.newCollectionInstance( property.getKlass() );
            delCollection.addAll( sourceCollection );

            if ( !delCollection.isEmpty() )
            {
                mutations.add( new Mutation( path, delCollection, Mutation.Operation.DELETION ) );
            }
        }
        else if ( property.isCollection() && !property.isEmbeddedObject() && !property.isIdentifiableObject() )
        {
            List sourceCollection = new ArrayList( (Collection) sourceValue );
            Collection targetCollection = (Collection) targetValue;

            Collection addCollection = ReflectionUtils.newCollectionInstance( property.getKlass() );

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

            Collection delCollection = ReflectionUtils.newCollectionInstance( property.getKlass() );
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

    @SuppressWarnings( "unchecked" )
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
                Collection identifiers = new ArrayList<>();

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
            if ( !currentSchema.haveProperty( paths[i] ) )
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

    @SuppressWarnings( "unchecked" )
    private void applyMutation( Mutation mutation, Property property, Object target )
    {
        Object value = mutation.getValue();

        if ( property.isCollection() )
        {
            Collection collection = ReflectionUtils.invokeMethod( target, property.getGetterMethod() );
            Collection sourceCollection = Collection.class.isInstance( value ) ? (Collection) value : Lists.newArrayList( value );

            if ( collection == null )
            {
                collection = ReflectionUtils.newCollectionInstance( property.getKlass() );
            }

            for ( Object o : sourceCollection )
            {
                Object object = o;

                if ( property.isIdentifiableObject() && !property.isEmbeddedObject() )
                {
                    if ( !String.class.isInstance( object ) )
                    {
                        return;
                    }

                    Schema schema = schemaService.getDynamicSchema( property.getItemKlass() );

                    Query query = Query.from( schema );
                    query.add( Restrictions.eq( "id", object ) ); // optimize by using .in(..) query

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
            query.add( Restrictions.eq( "id", value ) );

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

    private void logAudit( Patch patch, Object target, Schema schema )
    {
        SystemInfo systemInfo = systemService.getSystemInfo();

        MetadataAudit audit = new MetadataAudit();
        audit.setCreatedAt( new Date() );
        audit.setCreatedBy( currentUserService.getCurrentUsername() );
        audit.setKlass( schema.getKlass().getName() );

        if ( IdentifiableObject.class.isInstance( target ) )
        {
            audit.setUid( ((IdentifiableObject) target).getUid() );
            audit.setCode( ((IdentifiableObject) target).getCode() );
        }

        audit.setType( AuditType.UPDATE );

        if ( amqpService.isEnabled() )
        {
            audit.setValue( renderService.toJsonAsString( patch ) );
            amqpService.publish( audit );
        }

        if ( systemInfo.getMetadataAudit().isAudit() )
        {
            if ( audit.getValue() == null )
            {
                audit.setValue( renderService.toJsonAsString( patch ) );
            }

            String auditJson = renderService.toJsonAsString( audit );

            if ( systemInfo.getMetadataAudit().isLog() )
            {
                log.info( "MetadataAuditEvent: " + auditJson );
            }

            if ( systemInfo.getMetadataAudit().isPersist() )
            {
                metadataAuditService.addMetadataAudit( audit );
            }
        }
    }
}
