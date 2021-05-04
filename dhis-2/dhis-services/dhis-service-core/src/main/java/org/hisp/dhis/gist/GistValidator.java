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
package org.hisp.dhis.gist;

import static org.hisp.dhis.gist.GistLogic.isNonNestedPath;

import java.util.List;

import lombok.AllArgsConstructor;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.gist.GistQuery.Field;
import org.hisp.dhis.gist.GistQuery.Filter;
import org.hisp.dhis.gist.GistQuery.Owner;
import org.hisp.dhis.hibernate.exception.ReadAccessDeniedException;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.RelativePropertyContext;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Component;

/**
 * Validates a {@link GistQuery} for consistency and owner and type level
 * access.
 *
 * @author Jan Bernitt
 */
@Component
@AllArgsConstructor
final class GistValidator
{
    private final CurrentUserService currentUserService;

    private final AclService aclService;

    private final UserService userService;

    private final IdentifiableObjectManager objectManager;

    public void validateQuery( GistQuery query, RelativePropertyContext context )
    {
        Owner owner = query.getOwner();
        if ( owner != null )
        {
            validateAccess( owner );
            validateCollection(
                context.switchedTo( owner.getType() ).resolveMandatory( owner.getCollectionProperty() ) );
        }
        query.getFilters().forEach( filter -> validateFilter( filter, context ) );
        query.getOrders().forEach( order -> validateOrder( context.resolveMandatory( order.getPropertyPath() ) ) );
        query.getFields().forEach( field -> validateField( field, context ) );
    }

    private void validateAccess( Owner owner )
    {
        User currentUser = currentUserService.getCurrentUser();
        if ( !aclService.canRead( currentUser, owner.getType() ) )
        {
            throw createNoReadAccess( owner );
        }
        if ( !aclService.canRead( currentUser, objectManager.get( owner.getType(), owner.getId() ) ) )
        {
            throw createNoReadAccess( owner );
        }
    }

    private void validateCollection( Property collection )
    {
        if ( !collection.isCollection() || !collection.isPersisted() )
        {
            throw createIllegalProperty( collection, "Property `%s` is not a persisted collection member." );
        }
    }

    @SuppressWarnings( "unchecked" )
    private void validateField( Field f, RelativePropertyContext context )
    {
        String path = f.getPropertyPath();
        if ( Field.REFS_PATH.equals( path ) )
        {
            return;
        }
        Property field = context.resolveMandatory( path );
        if ( !field.isReadable() )
        {
            throw createNoReadAccess( field );
        }
        Class<?> itemType = GistLogic.getBaseType( field );
        if ( IdentifiableObject.class.isAssignableFrom( itemType ) && !aclService
            .canRead( currentUserService.getCurrentUser(), (Class<? extends IdentifiableObject>) itemType ) )
        {
            throw createNoReadAccess( field );
        }
        if ( !isNonNestedPath( path ) )
        {
            List<Property> pathElements = context.resolvePath( path );
            Property head = pathElements.get( 0 );
            if ( head.isCollection() && head.isPersisted() )
            {
                throw createIllegalProperty( field,
                    "Property `%s` computes to many values and therefore cannot be used as a field." );
            }
        }
    }

    private void validateFilter( Filter f, RelativePropertyContext context )
    {
        Property filter = context.resolveMandatory( f.getPropertyPath() );
        if ( !filter.isPersisted() )
        {
            throw createIllegalProperty( filter, "Property `%s` cannot be used as filter property." );
        }

        validateFilterArgument( f );
        validateFilterAccess( f );
    }

    private void validateFilterAccess( Filter f )
    {
        if ( f.getOperator().isAccessCompare() )
        {
            String[] ids = f.getValue();
            if ( ids.length != 1 )
            {
                throw createIllegalFilter( f, "Filter `%s` requires a single user ID as argument." );
            }
            User user = userService.getUser( ids[0] );
            if ( user == null )
            {
                throw createIllegalFilter( f, "User for filter `%s` does not exist." );
            }
            if ( !aclService.canRead( currentUserService.getCurrentUser(), user ) )
            {
                throw createIllegalFilter( f,
                    "Filtering by user access in filter `%s` requires read permissions to the user filter by." );
            }
        }
    }

    private void validateFilterArgument( Filter f )
    {
        if ( f.getOperator().isUnary() )
        {
            if ( f.getValue().length > 0 )
            {
                throw createIllegalFilter( f, "Filter `%s` uses an unary operator and does not need an argument." );
            }
        }
        else if ( f.getValue().length == 0 )
        {
            throw createIllegalFilter( f, "Filter `%s` uses a binary operator that does need an argument." );
        }
        if ( !f.getOperator().isMultiValue() && f.getValue().length > 1 )
        {
            throw createIllegalFilter( f, "Filter `%s` can only be used with a single argument." );
        }
    }

    private void validateOrder( Property order )
    {
        if ( !order.isPersisted() || !order.isSimple() )
        {
            throw createIllegalProperty( order, "Property `%s` cannot be used as order property." );
        }
    }

    private IllegalArgumentException createIllegalProperty( Property property, String message )
    {
        return new IllegalArgumentException( String.format( message, property.getName() ) );
    }

    private IllegalArgumentException createIllegalFilter( Filter filter, String message )
    {
        return new IllegalArgumentException( String.format( message, filter.toString() ) );
    }

    private ReadAccessDeniedException createNoReadAccess( Owner owner )
    {
        return new ReadAccessDeniedException(
            String.format( "User not allowed to view %s %s", owner.getType().getSimpleName(), owner.getId() ) );
    }

    private ReadAccessDeniedException createNoReadAccess( Property field )
    {
        if ( field.isReadable() )
        {
            return new ReadAccessDeniedException( String.format( "Property `%s` is not readable.", field.getName() ) );
        }
        return new ReadAccessDeniedException(
            String.format( "Property `%s` is not readable as user is not allowed to view objects of type %s",
                field.getName(), GistLogic.getBaseType( field ) ) );
    }
}
