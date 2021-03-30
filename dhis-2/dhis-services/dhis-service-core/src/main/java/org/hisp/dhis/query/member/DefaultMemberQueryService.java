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
import static org.hisp.dhis.query.member.MemberQueryLogic.getDefaultFields;
import static org.hisp.dhis.query.member.MemberQueryLogic.isIdProperty;
import static org.hisp.dhis.query.member.MemberQueryLogic.isLocalProperty;
import static org.hisp.dhis.query.member.MemberQueryLogic.isRelationField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.hibernate.exception.ReadAccessDeniedException;
import org.hisp.dhis.query.member.MemberQuery.Comparison;
import org.hisp.dhis.query.member.MemberQuery.Field;
import org.hisp.dhis.query.member.MemberQuery.Filter;
import org.hisp.dhis.query.member.MemberQuery.Owner;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.RelationViewType;
import org.hisp.dhis.schema.RelativePropertyContext;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jan Bernitt
 */
@Slf4j
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
        RelativePropertyContext context = createPropertyContext( query );
        return withFlatObjectFields( context, query.getFields().isEmpty()
            ? query.withFields( getDefaultFields( context.getHome() ) )
            : query );
    }

    private static <T extends IdentifiableObject> MemberQuery<T> withFlatObjectFields( RelativePropertyContext context,
        MemberQuery<T> query )
    {
        List<Field> fields = new ArrayList<>();
        for ( Field f : query.getFields() )
        {
            String path = f.getPropertyPath();
            if ( "*".equals( path ) )
            {
                fields.addAll( getDefaultFields( context.getHome() ) );
                continue;
            }
            Property field = context.resolveMandatory( path );
            if ( isLocalProperty( path ) && isRelationField( field ) && !field.isCollection() )
            {
                Class<?> fieldType = field.getKlass();
                RelativePropertyContext fieldContext = context.switchedTo( fieldType );
                if ( !field.getRelationViewFields().isEmpty() )
                {
                    field.getRelationViewFields().stream()
                        .map( fieldContext::resolveMandatory )
                        .forEach( p -> fields.add( nestedField( path, p ) ) );
                }
                else
                {
                    fieldContext.getHome().getProperties().stream()
                        .filter( MemberQueryLogic::isNestedField )
                        .forEach( p -> fields.add( nestedField( path, p ) ) );
                }
            }
            else
            {
                fields.add( f );
            }
        }
        return query.withFields( fields );
    }

    private static Field nestedField( String parentPath, Property field )
    {
        return new Field( parentPath + "." + field.key(), field.getRelationViewDisplayAs() );
    }

    @Override
    public <T extends IdentifiableObject> List<T> queryMemberItemsAsObjects( MemberQuery<T> query )
    {
        RelativePropertyContext context = createPropertyContext( query );
        validateQuery( query, context );
        return listWithParameters( query, context,
            getSession().createQuery( createHqlBuilder( query, context ).buildHQL(), query.getElementType() ) );
    }

    @Override
    public List<?> queryMemberItems( MemberQuery<?> query )
    {
        RelativePropertyContext context = createPropertyContext( query );
        validateQuery( query, context );
        HqlBuilder hqlBuilder = createHqlBuilder( extendedWithRequiredFields( query, context ), context );
        return wrapCollectionFields( query, context, hqlBuilder, listWithParameters( query, context,
            getSession().createQuery( hqlBuilder.buildHQL(), Object[].class ) ) );
    }

    /**
     * When a query asks for fields which are collections the query needs to
     * include UID column so that each match row can be identified as the root
     * or owner of the collection property.
     */
    private <T extends IdentifiableObject> MemberQuery<T> extendedWithRequiredFields( MemberQuery<T> query,
        RelativePropertyContext context )
    {
        // ID column already present?
        if ( hasLocalField( query, context, MemberQueryLogic::isIdProperty ) )
        {
            return query;
        }
        // ID column required?
        if ( hasLocalField( query, context, Property::isCollection ) )
        {
            return query.withField( new Field( "id", RelationViewType.AUTO ) );
        }
        return query;
    }

    static boolean hasLocalField( MemberQuery<?> query, RelativePropertyContext context, Predicate<Property> filter )
    {
        return query.getFields().stream().anyMatch( f -> isLocalProperty( f.getPropertyPath() )
            && filter.test( context.resolveMandatory( f.getPropertyPath() ) ) );
    }

    private List<Object[]> wrapCollectionFields( MemberQuery<?> query, RelativePropertyContext context,
        HqlBuilder hqlBuilder, List<Object[]> rows )
    {
        Map<Integer, RelationViewType> postProcessedFields = hqlBuilder.getPostProcessedColumns();
        if ( !postProcessedFields.isEmpty() )
        {
            Integer idColumnIndex = hqlBuilder.getIdColumnIndex();
            if ( idColumnIndex == null )
            {
                throw new IllegalStateException(
                    "Query has collection fields but did not include required ID column." );
            }
            String endpoint = query.getContextRoot() + context.getHome().getRelativeApiEndpoint();
            for ( Entry<Integer, RelationViewType> entry : postProcessedFields.entrySet() )
            {
                int index = entry.getKey();
                Field field = query.getFields().get( index );
                Property property = context.resolveMandatory( field.getPropertyPath() );
                switch ( entry.getValue() )
                {
                case ID_OBJECTS:
                    rows.forEach( row -> row[index] = stream( (String[]) row[index] ).map( IdObject::new ).toArray() );
                    break;
                case REF:
                case COUNT:
                    String prop = property.key();
                    rows.forEach( row -> row[index] = new EndpointDescriptor(
                        endpoint + '/' + row[idColumnIndex] + '/' + prop + "/items", getCount( row[index] ) ) );
                    break;
                default: // NOOP
                }
            }
        }
        return rows;
    }

    private static Number getCount( Object value )
    {
        return !(value instanceof Number) || ((Number) value).intValue() == -1 ? null : (Number) value;
    }

    private RelativePropertyContext createPropertyContext( MemberQuery<?> query )
    {
        return new RelativePropertyContext( query.getElementType(), schemaService::getDynamicSchema );
    }

    private <T extends IdentifiableObject> HqlBuilder createHqlBuilder( MemberQuery<T> query,
        RelativePropertyContext context )
    {
        return new HqlBuilder( query, context.switchedTo( query.getElementType() ) );
    }

    private <T> List<T> listWithParameters( MemberQuery<?> query, RelativePropertyContext context,
        Query<T> dbQuery )
    {
        dbQuery.setParameter( "OwnerId", query.getOwner().getId() );
        for ( Filter filter : query.getFilters() )
        {
            Property property = context.resolveMandatory( filter.getPropertyPath() );
            dbQuery.setParameter( getVarName( filter.getPropertyPath() ), getParameterValue( property, filter ) );
        }
        dbQuery.setMaxResults( query.getPageSize() );
        dbQuery.setFirstResult( query.getPageOffset() );
        dbQuery.setCacheable( false );
        return dbQuery.list();
    }

    private Object getParameterValue( Property property, Filter filter )
    {
        String[] value = filter.getValue();
        if ( value.length == 0 )
        {
            return "";
        }
        if ( value.length == 1 )
        {
            return queryTypedValue( property, filter, value[0] );
        }
        return stream( value ).map( e -> queryTypedValue( property, filter, e ) ).toArray();
    }

    private Object queryTypedValue( Property property, Filter filter, String value )
    {
        if ( value == null || property.getKlass() == String.class )
        {
            return value;
        }
        if ( MemberQueryLogic.isCollectionSizeFilter( filter, property ) )
        {
            // TODO parse error handling
            return Integer.parseInt( value );
        }
        String valueAsJson = value;
        Class<?> itemType = MemberQueryLogic.getSimpleField( property );
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
        query.getFilters().forEach( filter -> validateFilter( context.resolveMandatory( filter.getPropertyPath() ) ) );
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
        Property field = context.resolveMandatory( path );
        if ( !field.isReadable() )
        {
            throw createNoReadAccess( field );
        }
        Class<?> itemType = MemberQueryLogic.getSimpleField( field );
        if ( IdentifiableObject.class.isAssignableFrom( itemType ) && !aclService
            .canRead( currentUserService.getCurrentUser(), (Class<? extends IdentifiableObject>) itemType ) )
        {
            throw createNoReadAccess( field );
        }
        if ( !isLocalProperty( path ) )
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

    private void validateFilter( Property filter )
    {
        if ( !filter.isPersisted() )
        {
            throw createIllegalProperty( filter, "Property `%s` cannot be used as filter property." );
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
                field.getName(), MemberQueryLogic.getSimpleField( field ) ) );
    }

    static String getVarName( String property )
    {
        return property.replace( '.', '_' );
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
    @RequiredArgsConstructor
    private static final class HqlBuilder
    {
        private final MemberQuery<?> query;

        private final RelativePropertyContext context;

        private final Map<Integer, RelationViewType> postProcessedFields = new HashMap<>();

        private Integer idColumnIndex;

        static RelationViewType getEffectiveViewType( Field field, Property property )
        {
            RelationViewType targetType = field.getRelations();
            if ( targetType == RelationViewType.AUTO )
            {
                targetType = RelationViewType.COUNT;
            }
            if ( !property.isRelationViewDisplayOption( targetType ) )
            {
                return property.getRelationViewDisplayAs();
            }
            if ( property.isEmbeddedObject() && !property.isIdentifiableObject() )
            {
                return targetType == RelationViewType.IDS || targetType == RelationViewType.ID_OBJECTS
                    ? RelationViewType.COUNT
                    : targetType;
            }
            return targetType;
        }

        public Map<Integer, RelationViewType> getPostProcessedColumns()
        {
            return postProcessedFields;
        }

        public Integer getIdColumnIndex()
        {
            return idColumnIndex;
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
            String filterBy = createFiltersHQL();
            String orderBy = createOrdersHQL();
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
            AtomicInteger index = new AtomicInteger( 0 );
            return join( query.getFields(), ", ", "e",
                ( str, field ) -> str.append( createFieldHQL( field, index.getAndIncrement() ) ) );
        }

        private String createFieldHQL( Field field, int index )
        {
            String property = field.getPropertyPath();
            if ( isLocalProperty( property ) )
            {
                Property p = context.resolveMandatory( property );
                if ( isIdProperty( p ) )
                {
                    idColumnIndex = index;
                }
                if ( p.isCollection() && isRelationField( p ) )
                {
                    RelationViewType type = getEffectiveViewType( field, p );
                    if ( type != RelationViewType.IDS )
                    {
                        postProcessedFields.put( index, type );
                    }
                    switch ( type )
                    {
                    default:
                    case REF:
                        return "-1"; // indicates unknown count
                    case COUNT:
                        return "size(e." + getMemberPath( property ) + ")";
                    case ID_OBJECTS:
                    case IDS:
                        String tableName = "t_" + getVarName( property );
                        return "(select array_agg(" + tableName + ".uid) from "
                            + p.getItemKlass().getSimpleName() + " " + tableName + " where " + tableName
                            + " in elements(e." + getMemberPath( property ) + "))";
                    }
                }
            }
            String memberPath = getMemberPath( property );
            return "e." + memberPath;
        }

        private String createFiltersHQL()
        {
            return join( query.getFilters(), " and ", "1=1", this::createFiltersHQL );
        }

        private void createFiltersHQL( StringBuilder str, Filter filter )
        {
            boolean sizeOp = MemberQueryLogic.isCollectionSizeFilter( filter,
                context.resolveMandatory( filter.getPropertyPath() ) );
            if ( sizeOp )
            {
                str.append( "size(" );
            }
            str.append( "e." ).append( getMemberPath( filter.getPropertyPath() ) );
            if ( sizeOp )
            {
                str.append( ")" );
            }
            str.append( " " ).append( createOperatorLeftSideHQL( filter.getOperator() ) )
                .append( " :" ).append( getVarName( filter.getPropertyPath() ) )
                .append( createOperatorRightSideHQL( filter.getOperator() ) );
        }

        private String createOrdersHQL()
        {
            return join( query.getOrders(), ",", "e.id asc",
                ( str, order ) -> str
                    .append( " e." )
                    .append( getMemberPath( order.getPropertyPath() ) )
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

    @AllArgsConstructor
    public static final class IdObject
    {
        @JsonProperty
        final String id;
    }

    @AllArgsConstructor
    @JsonInclude( Include.NON_NULL )
    public static final class EndpointDescriptor
    {

        @JsonProperty
        final String apiEndpoint;

        @JsonProperty
        final Number count;

    }
}
