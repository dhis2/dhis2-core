package org.hisp.dhis.query;

/*
 *
 *  Copyright (c) 2004-2018, University of Oslo
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *  Neither the name of the HISP project nor the names of its contributors may
 *  be used to endorse or promote products derived from this software without
 *  specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

import org.hisp.dhis.schema.Property;
import org.springframework.context.i18n.LocaleContextHolder;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.function.Function;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public class JpaQueryUtils
{
    public static final String HIBERNATE_CACHEABLE_HINT = "org.hibernate.cacheable";

    public static Function<Root<?>, Order> getOrders( CriteriaBuilder builder, String field )
    {
        Function<Root<?>, Order> order = root -> builder.asc( root.get( field ) );

        return order;
    }

    /**
     * Generate a String comparison Predicate base on input parameters.
     *
     * Example:  JpaUtils.stringPredicateCaseSensitive( builder, root.get( "name" ),key , JpaUtils.StringSearchMode.ANYWHERE ) )
     *
     * @param builder CriteriaBuilder
     * @param path Property Path for query
     * @param attrValue Value to check
     * @param searchMode JpaQueryUtils.StringSearchMode
     * @return
     */
    public static Predicate stringPredicateCaseSensitive( CriteriaBuilder builder, Expression<String> expressionPath, Object objectValue, StringSearchMode searchMode )
    {
        return  stringPredicate(  builder,  expressionPath, objectValue, searchMode, true );
    }

    /**
     * Generate a String comparison Predicate base on input parameters.
     *
     * Example:  JpaUtils.stringPredicateIgnoreCase( builder, root.get( "name" ),key , JpaUtils.StringSearchMode.ANYWHERE ) )
     *
     * @param builder CriteriaBuilder
     * @param path Property Path for query
     * @param attrValue Value to check
     * @param searchMode JpaQueryUtils.StringSearchMode
     * @return
     */
    public static Predicate stringPredicateIgnoreCase( CriteriaBuilder builder, Expression<String> expressionPath, Object objectValue, StringSearchMode searchMode )
    {
        return  stringPredicate(  builder,  expressionPath, objectValue, searchMode, false );
    }

    /**
     * Generate a String comparison Predicate base on input parameters.
     *
     * Example:  JpaUtils.stringPredicate( builder, root.get( "name" ), "%" + key + "%", JpaUtils.StringSearchMode.LIKE, false ) )
     *
     * @param builder CriteriaBuilder
     * @param path Property Path for query
     * @param attrValue Value to check
     * @param searchMode JpaQueryUtils.StringSearchMode
     * @param caseSesnitive is case sensitive
     * @return
     */
    private static Predicate stringPredicate( CriteriaBuilder builder, Expression<String> expressionPath, Object objectValue, StringSearchMode searchMode, boolean caseSesnitive )
    {
        Expression<String> path = expressionPath;
        Object attrValue = objectValue;

        if ( !caseSesnitive )
        {
            path = builder.lower( path );
            attrValue = ((String) attrValue).toLowerCase( LocaleContextHolder.getLocale() );
        }

        switch ( searchMode )
        {
            case EQUALS:
                return builder.equal( path, attrValue );
            case ENDING_LIKE:
                return builder.like( path, "%" + attrValue );
            case STARTING_LIKE:
                return builder.like( path, attrValue + "%" );
            case ANYWHERE:
                return builder.like( path, "%" + attrValue + "%" );
            case LIKE:
                return builder.like( path, (String) attrValue ); // assume user provide the wild cards
            default:
                throw new IllegalStateException( "expecting a search mode!" );
        }
    }

    /**
     * Use for generating search String predicate in JPA criteria query
     */
    public enum StringSearchMode
    {

        EQUALS( "eq" ), // Match exactly
        ANYWHERE( "any" ), // Like search with '%' prefix and suffix
        STARTING_LIKE( "sl" ), // Like search and add a '%' prefix before searching
        LIKE( "li" ), // User provides the wild card
        ENDING_LIKE( "el" ); // LIKE search and add a '%' suffix before searching

        private final String code;

        StringSearchMode( String code )
        {
            this.code = code;
        }

        public String getCode()
        {
            return code;
        }

        public static final StringSearchMode convert( String code )
        {
            for ( StringSearchMode searchMode : StringSearchMode.values() )
            {
                if ( searchMode.getCode().equals( code ) )
                {
                    return searchMode;
                }
            }

            return EQUALS;
        }
    }

    /**
     * Use for parsing filter parameter for Object which doesn't extend IdentifiableObject.
     */
    public static Predicate getPredicate( CriteriaBuilder builder, Property property, Path path, String operator, String value )
    {
        switch ( operator )
        {
            case "in" :
                return path.in( QueryUtils.parseValue( Collection.class, property.getKlass(), value ) );
            case "eq" :
                return  builder.equal( path, QueryUtils.parseValue( property.getKlass(), value )  );
            default:
                throw new QueryParserException( "Query operator is not supported : " + operator );
        }
    }
}
