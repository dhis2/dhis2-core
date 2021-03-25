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
package org.hisp.dhis.query.member;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import lombok.AllArgsConstructor;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.hibernate.exception.ReadAccessDeniedException;
import org.hisp.dhis.query.member.MemberQuery.Comparison;
import org.hisp.dhis.query.member.MemberQuery.Filter;
import org.hisp.dhis.query.member.MemberQuery.Owner;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.RelativePropertyContext;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Jan Bernitt
 */
@Service
@AllArgsConstructor
public class DefaultMemberQueryService implements MemberQueryService
{

    private final SessionFactory sessionFactory;

    private final SchemaService schemaService;

    private final CurrentUserService currentUserService;

    private final AclService aclService;

    private final IdentifiableObjectManager objectManager;

    private final ObjectMapper jsonMapper;

    private Session getSession()
    {
        return sessionFactory.getCurrentSession();
    }

    @Override
    public <T extends IdentifiableObject> MemberQuery<T> rectifyQuery( MemberQuery<T> query )
    {
        if ( query.getFields().isEmpty() )
        {
            return query.toBuilder().fields( getDefaultFields( query ) ).build();
        }
        return query;
    }

    private List<String> getDefaultFields( MemberQuery<?> query )
    {
        return schemaService.getDynamicSchema( query.getElementType() ).getProperties().stream()
            .filter( DefaultMemberQueryService::isDefaultField ).map( Property::getName ).collect( toList() );
    }

    private static boolean isDefaultField( Property p )
    {
        return p.isPersisted() && !p.isCollection() && p.isReadable() && (p.isSimple() || p.isEmbeddedObject());
    }

    @Override
    public <T extends IdentifiableObject> List<T> queryMemberItemsAsObjects( MemberQuery<T> query )
    {
        RelativePropertyContext context = createPropertyContext( query );
        validateQuery( query, context );
        return listWithParameters( query, context,
            getSession().createQuery( buildHQL( query, context ), query.getElementType() ) );
    }

    @Override
    public <T extends IdentifiableObject> List<Object[]> queryMemberItems( MemberQuery<T> query )
    {
        RelativePropertyContext context = createPropertyContext( query );
        validateQuery( query, context );
        // TODO when only 1 field is queried no array wrapper is used
        return listWithParameters( query, context,
            getSession().createQuery( buildHQL( query, context ), Object[].class ) );
    }

    private RelativePropertyContext createPropertyContext( MemberQuery<?> query )
    {
        return new RelativePropertyContext( query.getElementType(), schemaService::getDynamicSchema );
    }

    private <T extends IdentifiableObject> String buildHQL( MemberQuery<T> query,
        RelativePropertyContext context )
    {
        return new HqlBuilder( query, context.switchedTo( query.getElementType() ) ).buildHQL();
    }

    private <T> List<T> listWithParameters( MemberQuery<?> query, RelativePropertyContext context,
        Query<T> dbQuery )
    {
        dbQuery.setParameter( "OwnerId", query.getOwner().getId() );
        for ( Filter filter : query.getFilters() )
        {
            Property property = context.resolveMandatory( filter.getProperty() );
            dbQuery.setParameter( HqlBuilder.getVarName( filter.getProperty() ), queryValue( property, filter ) );
        }
        dbQuery.setMaxResults( query.getPageSize() );
        dbQuery.setFirstResult( query.getPageOffset() );
        dbQuery.setCacheable( false );
        return dbQuery.list();
    }

    private Object queryValue( Property property, Filter filter )
    {
        String[] value = filter.getValue();
        if ( value.length == 0 )
        {
            return "";
        }
        if ( value.length == 1 )
        {
            return queryTypedValue( property, value[0] );
        }
        return stream( value ).map( e -> queryTypedValue( property, e ) ).toArray();
    }

    private Object queryTypedValue( Property property, String value )
    {
        if ( value == null || property.getKlass() == String.class )
        {
            return value;
        }
        String valueAsJson = value;
        Class<?> itemType = getSimpleType( property );
        if ( !(Number.class.isAssignableFrom( itemType ) || itemType == Boolean.class || itemType == boolean.class) )
        {
            valueAsJson = '"' + value + '"';
        }
        try
        {
            return jsonMapper.readValue( valueAsJson, itemType );
        }
        catch ( JsonProcessingException e )
        {
            throw new IllegalArgumentException( String
                .format( "Property `%s` of type %s is not compatible with provided filter value: `%s`",
                    property.getName(), itemType, value ) );
        }
    }

    private void validateQuery( MemberQuery<?> query, RelativePropertyContext context )
    {
        Owner owner = query.getOwner();
        validateAccess( owner );
        validateCollection( context.switchedTo( owner.getType() ).resolveMandatory( owner.getCollectionProperty() ) );
        query.getFilters().forEach( filter -> validateFilter( context.resolveMandatory( filter.getProperty() ) ) );
        query.getOrders().forEach( order -> validateOrder( context.resolveMandatory( order.getProperty() ) ) );
        query.getFields().forEach( field -> validateField( context.resolveMandatory( field ) ) );
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
        if ( !collection.isCollection() || !collection.isPersisted()
            || !IdentifiableObject.class.isAssignableFrom( collection.getItemKlass() ) )
        {
            throw createIllegalProperty( collection, "Property `%s` is not a persisted collection member." );
        }
    }

    @SuppressWarnings( "unchecked" )
    private void validateField( Property field )
    {
        if ( !field.isReadable() )
        {
            throw createNoReadAccess( field );
        }
        Class<?> itemType = getSimpleType( field );
        if ( IdentifiableObject.class.isAssignableFrom( itemType ) && !aclService
            .canRead( currentUserService.getCurrentUser(), (Class<? extends IdentifiableObject>) itemType ) )
        {
            throw createNoReadAccess( field );
        }
    }

    private void validateFilter( Property filter )
    {
        if ( !filter.isPersisted() )
        {
            throw createIllegalProperty( filter, "Property `%s` cannot be used as filter property." );
        }
    }

    private void validateOrder( Property order )
    {
        if ( !order.isPersisted() || !order.isOrdered() )
        {
            throw createIllegalProperty( order, "Property `%s` cannot be used as order property." );
        }
    }

    private IllegalArgumentException createIllegalProperty( Property property, String message )
    {
        return new IllegalArgumentException( String.format( message, property.getName() ) );
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
                field.getName(), getSimpleType( field ) ) );
    }

    private Class<?> getSimpleType( Property p )
    {
        return p.isCollection() ? p.getItemKlass() : p.getKlass();
    }

    /**
     * Purpose of this helper is to avoid passing around same state while
     * building the HQL query. Instead this state is shared in form of fields.
     * <p>
     * Within the HQL naming conventions are:
     *
     * <pre>
     *   o => owner table
     *   e => member collection element table
     * </pre>
     */
    @AllArgsConstructor
    private static final class HqlBuilder
    {
        private final MemberQuery<?> query;

        private final RelativePropertyContext context;

        static String getVarName( String property )
        {
            return property.replace( '.', '_' );
        }

        private String getMemberPath( String property )
        {
            List<Property> path = context.resolvePath( property );
            return path.size() == 1
                ? path.get( 0 ).getFieldName()
                : path.stream().map( Property::getFieldName ).collect( joining( "." ) );
        }

        private String buildHQL()
        {
            Owner owner = query.getOwner();
            String fields = createFieldsHQL();
            String filterBy = createFilterByHQL();
            String orderBy = createOrderByHQL();
            String elementTable = query.getElementType().getSimpleName();
            String ownerTable = owner.getType().getSimpleName();
            String op = query.isInverse() ? "not in" : "in";
            String collectionName = context.switchedTo( owner.getType() )
                .resolveMandatory( owner.getCollectionProperty() ).getFieldName();
            return String.format(
                "select %s from %s o, %s e where o.uid = :OwnerId and e %s elements(o.%s) and %s order by %s", fields,
                ownerTable, elementTable, op, collectionName, filterBy, orderBy );
        }

        private String createFieldsHQL()
        {
            // TODO when fields contains a property that is not persisted we
            // must use "e" (all)
            return join( query.getFields(), ", ", "e",
                ( str, field ) -> str.append( "e." ).append( getMemberPath( field ) ) );
        }

        private String createFilterByHQL()
        {
            return join( query.getFilters(), "and ", "1=1",
                ( str, filter ) -> str
                    .append( "e." )
                    .append( getMemberPath( filter.getProperty() ) )
                    .append( " " )
                    .append( createOperatorLeftSideHQL( filter.getOperator() ) ).append( " :" )
                    .append( getVarName( filter.getProperty() ) )
                    .append( createOperatorRightSideHQL( filter.getOperator() ) ) );
        }

        private String createOrderByHQL()
        {
            return join( query.getOrders(), ",", "e.id asc",
                ( str, order ) -> str
                    .append( " e." )
                    .append( getMemberPath( order.getProperty() ) )
                    .append( " " )
                    .append( order.getDirection().name().toLowerCase() ) );
        }

        private String createOperatorLeftSideHQL( Comparison operator )
        {
            switch ( operator )
            {
            case EQ:
                return "=";
            case NE:
                return "!=";
            case LT:
                return "<";
            case GT:
                return ">";
            case LE:
                return "<=";
            case GE:
                return ">=";
            case IN:
                return "in (";
            case NOT_IN:
                return "not in (";
            default:
                return "";
            }
        }

        private String createOperatorRightSideHQL( Comparison operator )
        {
            switch ( operator )
            {
            case NOT_IN:
            case IN:
                return ")";
            default:
                return "";
            }
        }

        private <T> String join( Collection<T> elements, String delimiter, String empty,
            BiConsumer<StringBuilder, T> append )
        {
            if ( elements == null || elements.isEmpty() )
            {
                return empty;
            }
            StringBuilder str = new StringBuilder();
            for ( T e : elements )
            {
                if ( str.length() > 0 )
                {
                    str.append( delimiter );
                }
                append.accept( str, e );
            }
            return str.toString();
        }
    }
}
