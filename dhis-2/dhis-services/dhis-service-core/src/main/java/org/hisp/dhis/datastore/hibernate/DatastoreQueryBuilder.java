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
package org.hisp.dhis.datastore.hibernate;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

import org.hisp.dhis.datastore.DatastoreQuery;
import org.hisp.dhis.datastore.DatastoreQuery.Comparison;
import org.hisp.dhis.datastore.DatastoreQuery.Filter;
import org.hisp.dhis.datastore.DatastoreQuery.Order;
import org.hisp.dhis.system.util.SqlUtils;

/**
 * Creates the HQL from a {@link DatastoreQuery} using {@link #createFetchHQL()}
 * and can set parameters for the filter value placeholders in that query using
 * {@link #applyParameterValues(BiConsumer)}.
 *
 * @author Jan Bernitt
 */
@AllArgsConstructor
public class DatastoreQueryBuilder
{
    private final DatastoreQuery query;

    String createFetchHQL()
    {
        String fields = createFieldsHQL();
        String nonNullFilters = createHasNonNullFieldsFilters();
        String orders = createOrderHQL();
        String filters = createFilterHQL();

        return format(
            "select key %s from DatastoreEntry where namespace = :namespace and (%s) and (%s) order by %s",
            fields, nonNullFilters, filters, orders );
    }

    void applyParameterValues( BiConsumer<String, Object> setParameter )
    {
        int i = 0;
        for ( Filter f : query.getFilters() )
        {
            if ( !f.getOperator().isUnary() )
            {
                setParameter.accept( "f_" + i, toTypedFilterValue( f ) );
            }
            i++;
        }
    }

    private String createFieldsHQL()
    {
        return query.getFields().isEmpty()
            ? ""
            : "," + String.join( ",", createExtractFieldValueExpressions( true ) );
    }

    private List<String> createExtractFieldValueExpressions( boolean asFields )
    {
        return query.getFields().stream()
            .map( f -> toValueAtPathHQL( f.getPath(), asFields ) )
            .collect( toList() );
    }

    private String createOrderHQL()
    {
        Order order = query.getOrder();
        String dir = order.getDirection().name().toLowerCase();
        if ( order.isKeyPath() )
        {
            return "key " + dir;
        }
        if ( order.isValuePath() )
        {
            return "cast(jbvalue as text) " + dir;
        }
        return toValueAtPathHQL( order.getPath(), false ) + " " + dir;
    }

    private String createHasNonNullFieldsFilters()
    {
        return query.isIncludeAll() || query.getFields().isEmpty()
            ? "1=1"
            : createExtractFieldValueExpressions( false ).stream()
                .map( f -> f + " is not null" )
                .collect( joining( " or " ) );
    }

    private String createFilterHQL()
    {
        List<Filter> filters = query.getFilters();
        if ( filters.isEmpty() )
        {
            return "1=1";
        }
        AtomicInteger index = new AtomicInteger();
        return filters.stream()
            .map( f -> createFilterHQL( f, index.getAndIncrement() ) )
            .collect( joining( query.isAnyFilter() ? " or " : " and " ) );
    }

    private static String createFilterHQL( Filter filter, int id )
    {
        switch ( filter.getOperator() )
        {
        case EMPTY:
        case NOT_EMPTY:
            return createEmptinessFilterHQL( filter );
        case NULL:
        case NOT_NULL:
            return createNullnessFilterHQL( filter );
        case IN:
        case NOT_IN:
            return createInFilterHQL( filter, id );
        default:
            return createBinaryFilterHQL( filter, id );
        }
    }

    private static String createBinaryFilterHQL( Filter filter, int id )
    {
        Comparison cmp = filter.getOperator();
        String prop = toValueAtPathHQL( filter.getPath(), false );
        String placeholder = ":f_" + id;
        if ( filter.isKeyPath() )
        {
            return format( "key %s %s", createOperatorHQL( cmp ), placeholder );
        }
        String type = deriveNodeType( filter );
        String template = "(jsonb_typeof(%1$s) = '%2$s' and %1$s %3$s to_jsonb(%4$s))";
        boolean isTextFilter = "string".equals( type );
        if ( isTextFilter )
        {
            template = cmp.isCaseInsensitive()
                ? "(jsonb_typeof(%1$s) = 'string' and lower(%5$s) %3$s %4$s)"
                : "(jsonb_typeof(%1$s) = 'string' and %5$s %3$s %4$s)";
        }
        else if ( "object".equals( type ) || "array".equals( type ) )
        {
            template = "(jsonb_typeof(%1$s) = '%2$s' and %5$s %3$s %4$s)";
        }
        return format( template, prop, type, createOperatorHQL( cmp ), placeholder, toValueAtPathAsTextHQL( prop ) );
    }

    private static String createInFilterHQL( Filter filter, int id )
    {
        String prop = toValueAtPathHQL( filter.getPath(), false );
        String placeholder = ":f_" + id;
        return format( "(jsonb_typeof(%1$s) = 'string') and %1$s %2$s %3$s",
            prop, createOperatorHQL( filter.getOperator() ), placeholder );
    }

    private static String createNullnessFilterHQL( Filter filter )
    {
        String prop = toValueAtPathHQL( filter.getPath(), false );
        if ( filter.getOperator() == Comparison.NOT_NULL )
        {
            return prop + " is not null";
        }
        String template = ".".equals( filter.getPath() )
            ? "jsonb_typeof(%2$s) = 'null'"
            : "((jsonb_typeof(%1$s) is 'object' or jsonb_typeof(%1$s) is 'array') and %2$s is null)";
        return format( template, toValueAtParentPathHQL( filter.getPath() ), prop );
    }

    private static String createEmptinessFilterHQL( Filter filter )
    {
        String prop = toValueAtPathHQL( filter.getPath(), false );
        return format( "(jsonb_typeof(%1$s) = 'string' and %3$s %2$s '\"\"')"
            + " or (jsonb_typeof(%1$s) = 'array' and %3$s %2$s '[]')"
            + " or (jsonb_typeof(%1$s) = 'object' and %3$s %2$s '{}')",
            prop, filter.getOperator() == Comparison.EMPTY ? "=" : "!=", toValueAtPathAsTextHQL( prop ) );
    }

    private static String createOperatorHQL( Comparison op )
    {
        switch ( op )
        {
        case LESS_THAN:
            return "<";
        case LESS_THAN_OR_EQUAL:
            return "<=";
        case GREATER_THAN:
            return ">";
        case GREATER_THAN_OR_EQUAL:
            return ">=";
        case LIKE:
        case ILIKE:
        case STARTS_LIKE:
        case ENDS_LIKE:
        case STARTS_WITH:
        case ENDS_WITH:
            return "like";
        case NOT_LIKE:
        case NOT_ILIKE:
        case NOT_STARTS_LIKE:
        case NOT_ENDS_LIKE:
        case NOT_STARTS_WITH:
        case NOT_ENDS_WITH:
            return "not like";
        case IN:
            return "in";
        case NOT_IN:
            return "not in";
        case NOT_EQUAL:
            return "!=";
        default:
            return "=";
        }
    }

    private static String deriveNodeType( Filter filter )
    {
        String value = filter.getValue();
        switch ( value )
        {
        case "true":
        case "false":
            return "boolean";
        case "":
        case "null":
            return "";
        default:
            if ( value.startsWith( "{" ) && value.endsWith( "}" ) )
            {
                return "object";
            }
            else if ( value.startsWith( "[" ) && value.endsWith( "]" ) )
            {
                return "array";
            }
            return value.matches( "[0-9]+(\\.[0-9]*)?" ) ? "number" : "string";
        }
    }

    private static Object toTypedFilterValue( Filter filter )
    {
        String type = deriveNodeType( filter );
        String value = filter.getValue();
        if ( "boolean".equals( type ) )
        {
            return Boolean.parseBoolean( value );
        }
        else if ( "number".equals( type ) )
        {
            return Double.parseDouble( value );
        }
        String str = value.startsWith( "'" ) && value.endsWith( "'" )
            ? value.substring( 1, value.length() - 1 )
            : value;
        Comparison cmp = filter.getOperator();
        if ( cmp.isCaseInsensitive() )
        {
            str = str.toLowerCase();
        }
        if ( cmp.isStartFlexible() )
        {
            str = "%" + str;
        }
        if ( cmp.isEndFlexible() )
        {
            str += "%";
        }
        return cmp.isTextBased() && cmp != Comparison.IEQ ? str.replace( '*', '%' ) : str;
    }

    private static String toValueAtPathHQL( String path, boolean asField )
    {
        if ( ".".equals( path ) )
        {
            return asField ? "cast(jbvalue as text)" : "jbvalue";
        }
        if ( "_".equals( path ) )
        {
            return "key";
        }
        return "jsonb_extract_path(jbvalue, " + toPathSegments( path ) + " )";
    }

    private static String toValueAtParentPathHQL( String path )
    {
        return !path.contains( "." )
            ? toValueAtPathHQL( ".", false )
            : toValueAtPathHQL( path.substring( 0, path.lastIndexOf( '.' ) + 1 ), false );
    }

    private static String toValueAtPathAsTextHQL( String prop )
    {
        return "jbvalue".equals( prop )
            ? "cast(jbvalue as text)"
            : prop.replace( "jsonb_extract_path(", "jsonb_extract_path_text(" );
    }

    private static String toPathSegments( String path )
    {
        return Arrays.stream( path.split( "\\." ) )
            .map( SqlUtils::singleQuote )
            .collect( Collectors.joining( ", " ) );
    }
}
