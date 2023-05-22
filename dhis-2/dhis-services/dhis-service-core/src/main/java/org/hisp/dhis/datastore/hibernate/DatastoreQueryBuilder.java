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

import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static java.lang.String.format;
import static java.util.Arrays.asList;
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
            if ( !f.getOperator().isUnary() && !f.isNullValue() )
            {
                setParameter.accept( "f_" + i, toTypedFilterArgument( f ) );
            }
            i++;
        }
    }

    private String createFieldsHQL()
    {
        return query.getFields().isEmpty()
            ? ""
            : "," + String.join( ",", createExtractFieldValueExpressions() );
    }

    private List<String> createExtractFieldValueExpressions()
    {
        return query.getFields().stream()
            .map( f -> toValueAtPathHQL( f.getPath() ) )
            .collect( toList() );
    }

    private String createOrderHQL()
    {
        Order order = query.getOrder();
        String dir = order.getDirection().toString().replace( "n", "" );
        if ( "desc".equals( dir ) )
        {
            dir += " nulls last";
        }
        if ( order.isKeyPath() )
        {
            return "key " + dir;
        }
        if ( order.isValuePath() )
        {
            return order.getDirection().isNumeric()
                ? "cast(cast(jbPlainValue as text) as double) " + dir
                : "cast(jbPlainValue as text) " + dir;
        }
        String path = toValueAtPathHQL( order.getPath() );
        return order.getDirection().isNumeric()
            ? "cast(cast(" + path + " as text) as double) " + dir
            : path + " " + dir;
    }

    private String createHasNonNullFieldsFilters()
    {
        return query.isIncludeAll() || query.getFields().isEmpty()
            ? "1=1"
            : createExtractFieldValueExpressions().stream()
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
                return filter.isNullValue()
                    ? createNullnessFilterHQL( filter )
                    : createBinaryFilterHQL( filter, id );
        }
    }

    private static String createBinaryFilterHQL( Filter filter, int id )
    {
        String prop = toValueAtPathHQL( filter.getPath() );
        String placeholder = ":f_" + id;
        String template = createFilterTemplateHQL( filter );
        return format( template, prop, deriveNodeType( filter ), createOperatorHQL( filter.getOperator() ), placeholder,
            toValueAtPathAsTextHQL( prop ) );
    }

    private static String createFilterTemplateHQL( Filter filter )
    {
        if ( filter.isKeyPath() )
        {
            return filter.getOperator().isCaseInsensitive() ? "lower(%5$s) %3$s %4$s" : "%5$s %3$s %4$s";
        }
        switch ( deriveNodeType( filter ) )
        {
            case "string":
                return filter.getOperator().isCaseInsensitive()
                    ? "(jsonb_typeof(%1$s) = 'string' and lower(%5$s) %3$s %4$s)"
                    : "(jsonb_typeof(%1$s) = 'string' and %5$s %3$s %4$s)";
            case "":
                return "%1$s %3$s to_jsonb(%4$s)";
            default:
                return "(jsonb_typeof(%1$s) = '%2$s' and %1$s %3$s to_jsonb(%4$s))";
        }
    }

    private static String createInFilterHQL( Filter filter, int id )
    {
        String prop = toValueAtPathHQL( filter.getPath() );
        String propAsText = toValueAtPathAsTextHQL( prop );
        String placeholder = ":f_" + id;
        String template = filter.isKeyPath()
            ? "%1$s %2$s (%3$s)"
            : "(jsonb_typeof(%1$s) = 'string') and %4$s %2$s (%3$s)";
        return format( template,
            prop, createOperatorHQL( filter.getOperator() ), placeholder, propAsText );
    }

    private static String createNullnessFilterHQL( Filter filter )
    {
        return format( "jsonb_typeof(%1$s) %2$s 'null'", toValueAtPathHQL( filter.getPath() ),
            createOperatorHQL( filter.getOperator() ) );
    }

    private static String createEmptinessFilterHQL( Filter filter )
    {
        String prop = toValueAtPathHQL( filter.getPath() );
        return format( "(jsonb_typeof(%1$s) = 'string' and %3$s %2$s '')"
            + " or (jsonb_typeof(%1$s) = 'array' and jsonb_array_length(%1$s) %2$s 0)"
            + " or (jsonb_typeof(%1$s) = 'object' and %1$s %2$s jsonb_object('{}'))",
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
            case NOT_NULL:
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

    private static Object toTypedFilterArgument( Filter filter )
    {
        String value = filter.getValue();
        switch ( deriveNodeType( filter ) )
        {
            case "boolean":
                return parseBoolean( value );
            case "number":
                double doubleValue = parseDouble( value );
                return doubleValue % 1 == 0d ? (int) doubleValue : doubleValue;
            case "array":
                return filter.getOperator().isIn()
                    ? asList( value.substring( 1, value.length() - 1 ).split( "," ) )
                    : value;
            case "object":
                return value;
            default:
                return toTextPatternArgument( filter, value );
        }
    }

    private static String toTextPatternArgument( Filter filter, String value )
    {
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

    /**
     * @param path path to extract
     * @return the expression to use to get the JSONB or key value at the
     *         provided path
     */
    private static String toValueAtPathHQL( String path )
    {
        if ( ".".equals( path ) )
        {
            return "jbPlainValue";
        }
        if ( "_".equals( path ) )
        {
            return "key";
        }
        return "jsonb_extract_path(jbPlainValue, " + toPathSegments( path ) + " )";
    }

    /**
     * @param prop the path property as returned by
     *        {@link #toValueAtPathHQL(String)}
     * @return the expression to use to get the text value of the JSONB node or
     *         key value at the provided path.
     */
    private static String toValueAtPathAsTextHQL( String prop )
    {
        return "jbPlainValue".equals( prop )
            // a string node as " quotes in text form which we have to strip
            ? "trim(both '\"' from cast(jbPlainValue as text))"
            : prop.replace( "jsonb_extract_path(", "jsonb_extract_path_text(" );
    }

    /**
     * <pre>
     *     a => 'a'
     * a.b.c => 'a','b','c'
     * </pre>
     */
    private static String toPathSegments( String path )
    {
        return Arrays.stream( path.split( "\\." ) )
            .map( SqlUtils::singleQuote )
            .collect( Collectors.joining( ", " ) );
    }
}
