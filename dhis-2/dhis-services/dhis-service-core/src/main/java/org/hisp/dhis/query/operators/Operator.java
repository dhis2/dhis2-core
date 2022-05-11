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
package org.hisp.dhis.query.operators;

import java.util.Collection;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import org.hibernate.criterion.Criterion;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.query.QueryUtils;
import org.hisp.dhis.query.Type;
import org.hisp.dhis.query.Typed;
import org.hisp.dhis.query.planner.QueryPath;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@AllArgsConstructor( access = AccessLevel.PRIVATE )
public abstract class Operator<T extends Comparable<? super T>>
{
    protected final String name;

    protected final Typed typed;

    protected final Type argumentType;

    @Getter
    protected final List<T> args;

    @Getter
    protected final List<Collection<T>> collectionArgs;

    Operator( String name, Typed typed )
    {
        this( name, typed, null, List.of(), List.of() );
    }

    Operator( String name, Typed typed, Collection<T> collectionArg )
    {
        this( name, typed, new Type( collectionArg ), List.of(), List.of( collectionArg ) );
    }

    @SafeVarargs
    Operator( String name, Typed typed, Collection<T>... collectionArgs )
    {
        this( name, typed, new Type( collectionArgs[0] ), List.of(), List.of( collectionArgs ) );
    }

    Operator( String name, Typed typed, T arg )
    {
        this( name, typed, new Type( arg ), List.of( arg ), List.of() );
        validateArgs();
    }

    @SafeVarargs
    Operator( String name, Typed typed, T... args )
    {
        this( name, typed, new Type( args[0] ), List.of( args ), List.of() );
        validateArgs();
    }

    private void validateArgs()
    {
        for ( Object arg : args )
        {
            if ( !isValid( arg.getClass() ) )
            {
                throw new QueryParserException( "Value `" + arg + "` of type `"
                    + arg.getClass().getSimpleName() + "` is not supported by this operator." );
            }
        }
    }

    protected <S> S getValue( Class<S> klass, Class<?> secondaryClass, int idx )
    {
        if ( Collection.class.isAssignableFrom( klass ) )
        {
            return QueryUtils.parseValue( klass, secondaryClass, getCollectionArgs().get( idx ) );
        }

        return QueryUtils.parseValue( klass, secondaryClass, args.get( idx ) );
    }

    protected <S> S getValue( Class<S> klass, int idx )
    {
        if ( Collection.class.isAssignableFrom( klass ) )
        {
            return QueryUtils.parseValue( klass, null, getCollectionArgs().get( idx ) );
        }

        return QueryUtils.parseValue( klass, null, args.get( idx ) );
    }

    protected <S> S getValue( Class<S> klass )
    {
        if ( Collection.class.isAssignableFrom( klass ) )
        {
            return QueryUtils.parseValue( klass, null, getCollectionArgs().get( 0 ) );
        }

        return getValue( klass, 0 );
    }

    protected <S> T getValue( Class<S> klass, Class<?> secondaryClass, Object value )
    {
        return QueryUtils.parseValue( klass, secondaryClass, value );
    }

    protected <S> S getValue( Class<S> klass, Object value )
    {
        return QueryUtils.parseValue( klass, value );
    }

    public boolean isValid( Class<?> klass )
    {
        return typed.isValid( klass );
    }

    public abstract Criterion getHibernateCriterion( QueryPath queryPath );

    public abstract <Y> Predicate getPredicate( CriteriaBuilder builder, Root<Y> root, QueryPath queryPath );

    public abstract boolean test( Object value );

    org.hibernate.criterion.MatchMode getMatchMode( MatchMode matchMode )
    {
        switch ( matchMode )
        {
        case EXACT:
            return org.hibernate.criterion.MatchMode.EXACT;
        case START:
            return org.hibernate.criterion.MatchMode.START;
        case END:
            return org.hibernate.criterion.MatchMode.END;
        case ANYWHERE:
            return org.hibernate.criterion.MatchMode.ANYWHERE;
        default:
            return null;
        }
    }

    protected JpaQueryUtils.StringSearchMode getJpaMatchMode( MatchMode matchMode )
    {
        switch ( matchMode )
        {
        case EXACT:
            return JpaQueryUtils.StringSearchMode.EQUALS;
        case START:
            return JpaQueryUtils.StringSearchMode.STARTING_LIKE;
        case END:
            return JpaQueryUtils.StringSearchMode.ENDING_LIKE;
        case ANYWHERE:
            return JpaQueryUtils.StringSearchMode.ANYWHERE;
        default:
            return null;
        }
    }

    /**
     * Get JPA String search mode for NOT LIKE match mode.
     *
     * @param matchMode {@link MatchMode}
     * @return {@link JpaQueryUtils.StringSearchMode} used for generating JPA
     *         Api Query
     */
    protected JpaQueryUtils.StringSearchMode getNotLikeJpaMatchMode( MatchMode matchMode )
    {
        switch ( matchMode )
        {
        case EXACT:
            return JpaQueryUtils.StringSearchMode.NOT_EQUALS;
        case START:
            return JpaQueryUtils.StringSearchMode.NOT_STARTING_LIKE;
        case END:
            return JpaQueryUtils.StringSearchMode.NOT_ENDING_LIKE;
        case ANYWHERE:
            return JpaQueryUtils.StringSearchMode.NOT_ANYWHERE;
        default:
            return null;
        }
    }

    @Override
    public String toString()
    {
        return "[" + name + ", args: " + args + "]";
    }
}
