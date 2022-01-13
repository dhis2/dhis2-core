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
        String fields = String.join( ",", createExtractFieldValueExpressions( true ) );
        String nonNullFilters = createNonNullFilters();
        String orders = toOrderHQL();
        String filters = toFilterHQL();

        return String.format(
            "select key, %s from DatastoreEntry where namespace = :namespace and (%s) and (%s) order by %s",
            fields, nonNullFilters, filters, orders );
    }

    private List<String> createExtractFieldValueExpressions( boolean asFields )
    {
        return query.getFields().stream()
            .map( f -> toValueAtPathHQL( f.getPath(), asFields ) )
            .collect( toList() );
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

    private String createNonNullFilters()
    {
        return query.isIncludeAll()
            ? "1=1"
            : createExtractFieldValueExpressions( false ).stream()
                .map( f -> f + " is not null" )
                .collect( joining( " or " ) );
    }

    private String toOrderHQL()
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

    private String toFilterHQL()
    {
        List<Filter> filters = query.getFilters();
        if ( filters.isEmpty() )
        {
            return "1=1";
        }
        AtomicInteger index = new AtomicInteger();
        return filters.stream()
            .map( f -> toFilterHQL( f, index.getAndIncrement() ) )
            .collect( joining( query.isAnyFilter() ? " or " : " and " ) );
    }

    private String toFilterHQL( Filter filter, int id )
    {
        String prop = toValueAtPathHQL( filter.getPath(), false );
        Comparison cmp = filter.getOperator();
        if ( cmp.isUnary() )
        {
            if ( cmp == Comparison.EMPTY || cmp == Comparison.NOT_EMPTY )
            {
                return String.format( "(jsonb_typeof(%1$s) = 'string' and length(cast(%1$s as text) %2$s 0)"
                    + " or (jsonb_typeof(%1$s) = 'array' and %1$s %2$s to_jsonb(cast('[]' as text)))"
                    + " or (jsonb_typeof(%1$s) = 'object' and %1$s %2$s to_jsonb(cast('{}' as text)))",
                    prop, cmp == Comparison.EMPTY ? "=" : "!=" );
            }
            return String.format( "jsonb_typeof(%1$s) %2$s 'null'", prop, cmp == Comparison.NULL ? "=" : "!=" );
        }
        String placeholder = ":f_" + id;
        String type = deriveNodeType( filter );
        String template = "(jsonb_typeof(%1$s) = '%2$s' and %1$s %3$s to_jsonb(%4$s))";
        if ( "string".equals( type ) )
        {
            template = "(jsonb_typeof(%1$s) = 'string' and cast(%1$s as text) %3$s '%4$s')";
        }
        else if ( "object".equals( type ) || "array".equals( type ) )
        {
            template = "(jsonb_typeof(%1$s) = '%2$s' and %1$s %3$s to_jsonb(cast('%4$s' as text)))";
        }
        return String.format( template, prop, type, toOperatorHQL( cmp ), placeholder );
    }

    private static String toOperatorHQL( Comparison op )
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
            return "like";
        case NOT_LIKE:
            return "not like";
        case NOT_EQUAL:
            return "!=";
        default:
            return "=";
        }
    }

    private String deriveNodeType( Filter filter )
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
            return value.matches( "[0-9]+(\\.[0-9]*)?" ) ? "number" : "string";
        }
    }

    private Object toTypedFilterValue( Filter filter )
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
        return value.startsWith( "'" ) && value.endsWith( "'" ) ? value.substring( 1, value.length() - 1 ) : value;
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

    private static String toPathSegments( String path )
    {
        return Arrays.stream( path.split( "\\." ) )
            .map( SqlUtils::singleQuote )
            .collect( Collectors.joining( ", " ) );
    }
}
