package org.hisp.dhis.schema.patch;

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
 *
 */

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.util.ReflectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultPatchService implements PatchService
{
    private final SchemaService schemaService;
    private final QueryService queryService;

    public DefaultPatchService( SchemaService schemaService, QueryService queryService )
    {
        this.schemaService = schemaService;
        this.queryService = queryService;
    }

    @Override
    public Patch diff( Object source, Object target )
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

        patch.setMutations( calculateMutations( schema, source, target ) );

        return patch;
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
    }

    private List<Mutation> calculateMutations( Schema schema, Object source, Object target )
    {
        List<Mutation> mutations = new ArrayList<>();

        for ( Property property : schema.getProperties() )
        {
            Mutation mutation = calculateMutation( property.getName(), property, source, target );

            if ( mutation != null )
            {
                mutations.add( mutation );
            }
        }

        return mutations;
    }

    private Mutation calculateMutation( String path, Property property, Object source, Object target )
    {
        if ( property.isSimple() )
        {
            Object sourceValue = ReflectionUtils.invokeMethod( source, property.getGetterMethod() );
            Object targetValue = ReflectionUtils.invokeMethod( target, property.getGetterMethod() );

            if ( sourceValue == null && targetValue == null )
            {
                return null;
            }

            if ( targetValue == null || !targetValue.equals( sourceValue ) )
            {
                return new Mutation( path, targetValue );
            }
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
            Object object = value;

            if ( property.isIdentifiableObject() )
            {
                if ( !String.class.isInstance( value ) )
                {
                    return;
                }

                Schema schema = schemaService.getDynamicSchema( property.getItemKlass() );

                Query query = Query.from( schema );
                query.add( Restrictions.eq( "id", value ) );

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

            Collection collection = ReflectionUtils.invokeMethod( target, property.getGetterMethod() );

            if ( collection == null )
            {
                collection = ReflectionUtils.newCollectionInstance( property.getKlass() );
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

            ReflectionUtils.invokeMethod( target, property.getSetterMethod(), collection );
        }
        else
        {
            // validate type
            if ( !property.getKlass().isInstance( value ) )
            {
                return;
            }

            ReflectionUtils.invokeMethod( target, property.getSetterMethod(), value );
        }
    }
}
