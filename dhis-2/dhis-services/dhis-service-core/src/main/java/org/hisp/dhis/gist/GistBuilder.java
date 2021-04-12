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

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.hisp.dhis.gist.GistLogic.getBaseType;
import static org.hisp.dhis.gist.GistLogic.isCollectionSizeFilter;
import static org.hisp.dhis.gist.GistLogic.isHrefProperty;
import static org.hisp.dhis.gist.GistLogic.isNonNestedPath;
import static org.hisp.dhis.gist.GistLogic.isPersistentCollectionField;
import static org.hisp.dhis.gist.GistLogic.isPersistentReferenceField;
import static org.hisp.dhis.gist.GistLogic.parentPath;
import static org.hisp.dhis.gist.GistLogic.pathOnSameParent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.gist.GistQuery.Comparison;
import org.hisp.dhis.gist.GistQuery.Field;
import org.hisp.dhis.gist.GistQuery.Filter;
import org.hisp.dhis.gist.GistQuery.Owner;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.schema.GistTransform;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.RelativePropertyContext;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.translation.Translation;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Purpose of this helper is to avoid passing around same state while building
 * the HQL query and to setup post processing of results.
 *
 * Usage:
 * <ol>
 * <li>Use {@link #buildFetchHQL()} to create the HQL query</li>
 * <li>Use {@link #transform(List)} on the result rows when querying selected
 * columns</li>
 * </ol>
 * <p>
 * Within the HQL naming conventions are:
 *
 * <pre>
 *   o => owner table
 *   e => member collection element table
 * </pre>
 *
 * @author Jan Bernitt
 */
@RequiredArgsConstructor
final class GistBuilder
{

    private static final String GIST_PATH = "/gist";

    /**
     * HQL does not allow plain "null" in select columns list as the type is
     * unknown. Therefore we just cast it to some simple type. Which is not
     * important as the value will be {@code null} anyway.
     */
    public static final String HQL_NULL = "cast(null as char)";

    static GistBuilder createFetchBuilder( GistQuery query, RelativePropertyContext context )
    {
        return new GistBuilder( addSupportFields( query, context ), context );
    }

    static GistBuilder createCountBuilder( GistQuery query, RelativePropertyContext context )
    {
        return new GistBuilder( query, context );
    }

    private final GistQuery query;

    private final RelativePropertyContext context;

    private final List<Consumer<Object[]>> fieldResultTransformers = new ArrayList<>();

    private final Map<String, Integer> fieldIndexByPath = new HashMap<>();

    /**
     * Depending on what fields should be listed other fields are needed to
     * fully compute the requested fields. Such fields are added should they not
     * be present already. This is done only within the builder. While the
     * fields are fetched from the database the caller does not include the
     * added fields as it is still working with the original field list.
     */
    private static GistQuery addSupportFields( GistQuery query,
        RelativePropertyContext context )
    {
        GistQuery extended = query;
        for ( Field f : query.getFields() )
        {
            if ( Field.REFS_PATH.equals( f.getPropertyPath() ) )
            {
                continue;
            }
            Property p = context.resolveMandatory( f.getPropertyPath() );
            // ID column not present but ID column required?
            if ( (isPersistentCollectionField( p ) || isHrefProperty( p ))
                && !hasSameParentField( extended, f, "id" ) )
            {
                extended = extended.withField( pathOnSameParent( f.getPropertyPath(), "id" ) );
            }
            // translatable fields? => make sure we have translations
            if ( (query.isTranslate() || f.isTranslate()) && p.isTranslatable()
                && !hasSameParentField( extended, f, "translations" ) )
            {
                extended = extended.withField( pathOnSameParent( f.getPropertyPath(), "translations" ) );
            }
        }
        return extended;
    }

    private static boolean hasSameParentField( GistQuery query, Field field, String property )
    {
        String parentPath = parentPath( field.getPropertyPath() );
        String requiredPath = parentPath.isEmpty() ? property : parentPath + "." + property;
        return query.getFields().stream().anyMatch( f -> f.getPropertyPath().equals( requiredPath ) );
    }

    private String getMemberPath( String property )
    {
        List<Property> path = context.resolvePath( property );
        return path.size() == 1 ? path.get( 0 ).getFieldName()
            : path.stream().map( Property::getFieldName ).collect( joining( "." ) );
    }

    /*
     * SQL response post processing...
     */

    @AllArgsConstructor( access = AccessLevel.PRIVATE )
    public static final class IdObject
    {
        @JsonProperty
        final String id;
    }

    public void transform( List<Object[]> rows )
    {
        if ( fieldResultTransformers.isEmpty() )
        {
            return;
        }
        for ( Object[] row : rows )
        {
            for ( Consumer<Object[]> transformer : fieldResultTransformers )
            {
                transformer.accept( row );
            }
        }
    }

    private void addTransformer( Consumer<Object[]> transformer )
    {
        fieldResultTransformers.add( transformer );
    }

    private Object translate( Object value, String property, Object translations )
    {
        @SuppressWarnings( "unchecked" )
        Set<Translation> list = (Set<Translation>) translations;
        if ( list == null || list.isEmpty() )
        {
            return value;
        }
        String locale = query.getTranslationLocale().toString();
        for ( Translation t : list )
        {
            if ( t.getLocale().equalsIgnoreCase( locale ) && t.getProperty().equalsIgnoreCase( property )
                && !t.getValue().isEmpty() )
                return t.getValue();
        }
        String lang = query.getTranslationLocale().getLanguage();
        for ( Translation t : list )
        {
            if ( t.getLocale().startsWith( lang ) && t.getProperty().equalsIgnoreCase( property )
                && !t.getValue().isEmpty() )
                return t.getValue();
        }
        return value;
    }

    /*
     * HQL query building...
     */

    public String buildFetchHQL()
    {
        String fields = createFieldsHQL();
        String orders = createOrdersHQL();
        String filters = createFiltersHQL();
        String elementTable = query.getElementType().getSimpleName();
        Owner owner = query.getOwner();
        if ( owner == null )
        {
            return String.format( "select %s from %s e where %s order by %s", fields, elementTable, filters, orders );
        }
        String op = query.isInverse() ? "not in" : "in";
        String ownerTable = owner.getType().getSimpleName();
        String collectionName = context.switchedTo( owner.getType() )
            .resolveMandatory( owner.getCollectionProperty() ).getFieldName();
        return String.format(
            "select %s from %s o, %s e where o.uid = :OwnerId and e %s elements(o.%s) and (%s) order by %s", fields,
            ownerTable, elementTable, op, collectionName, filters, orders );
    }

    public String buildCountHQL()
    {
        String filters = createFiltersHQL();
        String elementTable = query.getElementType().getSimpleName();
        Owner owner = query.getOwner();
        if ( owner == null )
        {
            return String.format( "select count(*) from %s where %s", elementTable, filters );
        }
        String op = query.isInverse() ? "not in" : "in";
        String ownerTable = owner.getType().getSimpleName();
        String collectionName = context.switchedTo( owner.getType() )
            .resolveMandatory( owner.getCollectionProperty() ).getFieldName();
        return String.format( "select count(*) from %s o, %s e where o.uid = :OwnerId and e %s elements(o.%s) and (%s)",
            ownerTable, elementTable, op, collectionName, filters );
    }

    private String createFieldsHQL()
    {
        // TODO when fields contains a property that is not persisted we
        // must use "e" (all)
        int i = 0;
        for ( Field f : query.getFields() )
        {
            fieldIndexByPath.put( f.getPropertyPath(), i++ );
        }
        return join( query.getFields(), ", ", "e", this::createFieldHQL );
    }

    private String createFieldHQL( int index, Field field )
    {
        String path = field.getPropertyPath();
        if ( Field.REFS_PATH.equals( path ) )
        {
            return HQL_NULL;
        }
        Property property = context.resolveMandatory( path );
        if ( query.isTranslate() && property.isTranslatable() && query.getTranslationLocale() != null )
        {
            int translationsFieldIndex = getSameParentFieldIndex( path, "translations" );
            addTransformer( row -> row[index] = translate( row[index], property.getTranslationKey(),
                row[translationsFieldIndex] ) );
        }
        if ( isHrefProperty( property ) )
        {
            String endpointRoot = getSameParentEndpointRoot( path );
            Integer idFieldIndex = getSameParentFieldIndex( path, "id" );
            if ( idFieldIndex != null )
            {
                addTransformer( row -> row[index] = toEndpointURL( endpointRoot, row[idFieldIndex] ) );
            }
            return "''"; // use empty string to mark a non DB value
        }
        if ( isPersistentReferenceField( property ) )
        {
            return createReferenceFieldHQL( index, field );
        }
        if ( isPersistentCollectionField( property ) )
        {
            return createCollectionFieldHQL( index, field );
        }
        String memberPath = getMemberPath( path );
        return "e." + memberPath;
    }

    private String createReferenceFieldHQL( int index, Field field )
    {
        String path = field.getPropertyPath();
        Property property = context.resolveMandatory( path );
        String idProperty = "uid";
        if ( property.getKlass() == PeriodType.class )
        {
            idProperty = "class";
        }
        else if ( !property.isIdentifiableObject() )
        {
            Schema propertySchema = context.switchedTo( property.getKlass() ).getHome();
            if ( propertySchema.getRelativeApiEndpoint() == null )
            {
                // TODO for now...
                return "e." + getMemberPath( path );
            }
        }

        if ( property.isIdentifiableObject() )
        {
            String endpointRoot = getEndpointRoot( property );
            int refIndex = fieldIndexByPath.get( Field.REFS_PATH );
            addTransformer( row -> addEndpointURL( row, refIndex, field, toEndpointURL( endpointRoot, row[index] ) ) );
        }

        if ( getFieldTransform( field ) == GistTransform.ID_OBJECTS )
        {
            addTransformer( row -> row[index] = toIdObject( row[index] ) );
        }
        if ( property.isRequired() )
        {
            return "e." + getMemberPath( path ) + "." + idProperty;
        }
        String tableName = "t_" + index;
        return "(select " + tableName + "." + idProperty + " from " + property.getKlass().getSimpleName() + " "
            + tableName
            + " where " + tableName + " = e." + getMemberPath( path ) + ")";
    }

    private String createCollectionFieldHQL( int index, Field field )
    {
        String path = field.getPropertyPath();
        Property property = context.resolveMandatory( path );
        String endpointRoot = getSameParentEndpointRoot( path );
        int idFieldIndex = getSameParentFieldIndex( path, "id" );
        int refIndex = fieldIndexByPath.get( Field.REFS_PATH );
        addTransformer(
            row -> addEndpointURL( row, refIndex, field, toEndpointURL( endpointRoot, row[idFieldIndex], property ) ) );

        GistTransform transform = getFieldTransform( field );
        switch ( transform )
        {
        default:
        case AUTO:
        case NONE:
            return HQL_NULL;
        case SIZE:
            return "size(e." + getMemberPath( path ) + ")";
        case IS_EMPTY:
            addTransformer( row -> row[index] = ((Number) row[index]).intValue() == 0 );
            return createIsEmptyTransformerHQL( path );
        case IS_NOT_EMPTY:
            addTransformer( row -> row[index] = ((Number) row[index]).intValue() > 0 );
            return createIsEmptyTransformerHQL( path );
        case NOT_MEMBER:
            addTransformer( row -> row[index] = ((Number) row[index]).intValue() == 0 );
            return createHasMemberTransformerHQL( index, field, property );
        case MEMBER:
            addTransformer( row -> row[index] = ((Number) row[index]).intValue() > 0 );
            return createHasMemberTransformerHQL( index, field, property );
        case ID_OBJECTS:
            addTransformer( row -> row[index] = toIdObjects( row[index] ) );
            return createIdsTransformerHQL( index, field, property );
        case IDS:
            return createIdsTransformerHQL( index, field, property );
        case PLUCK:
            return createPluckTransformerHQL( index, field, property );
        }
    }

    private String createIsEmptyTransformerHQL( String path )
    {
        // TODO use "exists" subquery for performance?
        return "size(e." + getMemberPath( path ) + ")";
    }

    private String createIdsTransformerHQL( int index, Field field, Property property )
    {
        return createPluckTransformerHQL( index, field, property );
    }

    private String createPluckTransformerHQL( int index, Field field, Property property )
    {
        String plucked = "uid";
        if ( field.getTransformationArgument() != null )
        {
            plucked = field.getTransformationArgument();
            Property pluckedProperty = context.switchedTo( property.getItemKlass() ).resolveMandatory( plucked );
            if ( pluckedProperty.getKlass() != String.class )
            {
                throw new UnsupportedOperationException( "Only textual properties can be plucked, but " + plucked
                    + " is a: " + pluckedProperty.getKlass() );
            }
        }
        String tableName = "t_" + index;
        return "(select array_agg(" + tableName + "." + plucked + ") from " + property.getItemKlass().getSimpleName()
            + " " + tableName + " where " + tableName + " in elements(e." + getMemberPath( field.getPropertyPath() )
            + "))";
    }

    private String createHasMemberTransformerHQL( int index, Field field, Property property )
    {
        String tableName = "t_" + index;
        return "(select count(*) from " + property.getItemKlass().getSimpleName() + " " + tableName + " where "
            + tableName + " in elements(e." + getMemberPath( field.getPropertyPath() ) + ") and " + tableName
            + ".uid = :p_"
            + field.getPropertyPath() + ")";
    }

    private void addEndpointURL( Object[] row, int refIndex, Field field, String url )
    {
        if ( row[refIndex] == null )
        {
            row[refIndex] = new TreeMap<>();
        }
        ((Map<String, String>) row[refIndex]).put( field.getName(), url );
    }

    private static String toEndpointURL( String endpointRoot, Object id )
    {
        return id == null ? null : endpointRoot + '/' + id + GIST_PATH;
    }

    private static String toEndpointURL( String endpointRoot, Object id, Property property )
    {
        return endpointRoot + '/' + id + '/' + property.key() + GIST_PATH;
    }

    private static IdObject toIdObject( Object id )
    {
        return id == null ? null : new IdObject( (String) id );
    }

    private static Object[] toIdObjects( Object ids )
    {
        return ids == null || ((Object[]) ids).length == 0
            ? null
            : Arrays.stream( ((String[]) ids) ).map( IdObject::new ).toArray();
    }

    private GistTransform getFieldTransform( Field field )
    {
        return !isNonNestedPath( field.getPropertyPath() ) ? GistTransform.NONE : field.getTransformation();
    }

    private Integer getSameParentFieldIndex( String path, String translations )
    {
        return fieldIndexByPath.get( pathOnSameParent( path, translations ) );
    }

    private String getSameParentEndpointRoot( String path )
    {
        return query.getEndpointRoot()
            + context.switchedTo( path ).getHome().getRelativeApiEndpoint();
    }

    private String getEndpointRoot( Property property )
    {
        return query.getEndpointRoot()
            + context.switchedTo( property.getKlass() ).getHome().getRelativeApiEndpoint();
    }

    private String createFiltersHQL()
    {
        String rootJunction = query.isAnyFilter() ? " or " : " and ";
        return join( query.getFilters(), rootJunction, "1=1", this::createFiltersHQL );
    }

    private String createFiltersHQL( int index, Filter filter )
    {
        StringBuilder str = new StringBuilder();
        boolean sizeOp = isCollectionSizeFilter( filter,
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
        Comparison operator = filter.getOperator();
        str.append( " " ).append( createOperatorLeftSideHQL( operator ) );
        if ( !operator.isUnary() )
        {
            str.append( " :f_" + index ).append( createOperatorRightSideHQL( operator ) );
        }
        return str.toString();
    }

    private String createOrdersHQL()
    {
        return join( query.getOrders(), ",", "e.id asc",
            ( index, order ) -> " e." + getMemberPath( order.getPropertyPath() ) + " "
                + order.getDirection().name().toLowerCase() );
    }

    private String createOperatorLeftSideHQL( Comparison operator )
    {
        switch ( operator )
        {
        case NULL:
            return "is null";
        case NOT_NULL:
            return "is not null";
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
        case EMPTY:
            return "== 0";
        case NOT_EMPTY:
            return "> 0";
        case LIKE:
        case STARTS_LIKE:
        case ENDS_LIKE:
            return "like";
        case NOT_LIKE:
        case NOT_STARTS_LIKE:
        case NOT_ENDS_LIKE:
            return "not like";
        case ILIKE:
        case STARTS_WITH:
        case ENDS_WITH:
            return "ilike";
        case NOT_ILIKE:
        case NOT_STARTS_WITH:
        case NOT_ENDS_WITH:
            return "not ilike";
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
        BiFunction<Integer, T, String> elementFactory )
    {
        if ( elements == null || elements.isEmpty() )
        {
            return empty;
        }
        StringBuilder str = new StringBuilder();
        int i = 0;
        for ( T e : elements )
        {
            if ( str.length() > 0 )
            {
                str.append( delimiter );
            }
            str.append( elementFactory.apply( i++, e ) );
        }
        return str.toString();
    }

    /*
     * HQL query parameter mapping...
     */

    public void addFetchParameters( BiConsumer<String, Object> dest,
        BiFunction<String, Class<?>, Object> argumentParser )
    {
        for ( Field field : query.getFields() )
        {
            if ( field.getTransformationArgument() != null && field.getTransformation() != GistTransform.PLUCK )
            {
                dest.accept( "p_" + field.getPropertyPath(), field.getTransformationArgument() );
            }
        }
        addCountParameters( dest, argumentParser );
    }

    public void addCountParameters( BiConsumer<String, Object> dest,
        BiFunction<String, Class<?>, Object> argumentParser )
    {
        Owner owner = query.getOwner();
        if ( owner != null )
        {
            dest.accept( "OwnerId", owner.getId() );
        }
        int i = 0;
        for ( Filter filter : query.getFilters() )
        {
            Comparison operator = filter.getOperator();
            if ( !operator.isUnary() )
            {
                Property property = context.resolveMandatory( filter.getPropertyPath() );
                Object value = getParameterValue( property, filter, argumentParser );
                dest.accept( "f_" + (i++), operator.isStringCompare()
                    ? completeLikeExpression( operator, (String) value )
                    : value );
            }
        }
    }

    private Object getParameterValue( Property property, Filter filter,
        BiFunction<String, Class<?>, Object> argumentParser )
    {
        String[] value = filter.getValue();
        if ( value.length == 0 )
        {
            return "";
        }
        if ( value.length == 1 )
        {
            return queryTypedValue( property, filter, value[0], argumentParser );
        }
        return stream( value ).map( e -> queryTypedValue( property, filter, e, argumentParser ) ).toArray();
    }

    private Object queryTypedValue( Property property, Filter filter, String value,
        BiFunction<String, Class<?>, Object> argumentParser )
    {
        if ( value == null || property.getKlass() == String.class )
        {
            return value;
        }
        if ( isCollectionSizeFilter( filter, property ) )
        {
            // TODO parse error handling
            return Integer.parseInt( value );
        }
        String valueAsJson = value;
        Class<?> itemType = getBaseType( property );
        if ( !(Number.class.isAssignableFrom( itemType ) || itemType == Boolean.class || itemType == boolean.class) )
        {
            valueAsJson = '"' + value + '"';
        }
        return argumentParser.apply( valueAsJson, itemType );
    }

    private static Object completeLikeExpression( Comparison operator, String value )
    {
        switch ( operator )
        {
        case LIKE:
        case NOT_LIKE:
            return value.contains( "*" ) || value.contains( "?" )
                ? value.replace( "*", "%" ).replace( "?", "_" )
                : "%" + value + "%";
        case STARTS_LIKE:
        case STARTS_WITH:
        case NOT_STARTS_LIKE:
        case NOT_STARTS_WITH:
            return "%" + value;
        case ENDS_LIKE:
        case ENDS_WITH:
        case NOT_ENDS_LIKE:
        case NOT_ENDS_WITH:
            return value + "%";
        default:
            return value;
        }
    }
}
