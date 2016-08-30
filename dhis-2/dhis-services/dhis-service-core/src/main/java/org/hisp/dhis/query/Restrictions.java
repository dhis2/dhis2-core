package org.hisp.dhis.query;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.query.operators.BetweenOperator;
import org.hisp.dhis.query.operators.EqualOperator;
import org.hisp.dhis.query.operators.GreaterEqualOperator;
import org.hisp.dhis.query.operators.GreaterThanOperator;
import org.hisp.dhis.query.operators.InOperator;
import org.hisp.dhis.query.operators.LessEqualOperator;
import org.hisp.dhis.query.operators.LessThanOperator;
import org.hisp.dhis.query.operators.LikeOperator;
import org.hisp.dhis.query.operators.MatchMode;
import org.hisp.dhis.query.operators.NotEqualOperator;
import org.hisp.dhis.query.operators.NotInOperator;
import org.hisp.dhis.query.operators.NotLikeOperator;
import org.hisp.dhis.query.operators.NotNullOperator;
import org.hisp.dhis.query.operators.NullOperator;

import java.util.Collection;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public final class Restrictions
{
    public static Restriction eq( String path, Object value )
    {
        return new Restriction( path, new EqualOperator( value ) );
    }

    public static Restriction ne( String path, Object value )
    {
        return new Restriction( path, new NotEqualOperator( value ) );
    }

    public static Restriction gt( String path, Object value )
    {
        return new Restriction( path, new GreaterThanOperator( value ) );
    }

    public static Restriction lt( String path, Object value )
    {
        return new Restriction( path, new LessThanOperator( value ) );
    }

    public static Restriction ge( String path, Object value )
    {
        return new Restriction( path, new GreaterEqualOperator( value ) );
    }

    public static Restriction le( String path, Object value )
    {
        return new Restriction( path, new LessEqualOperator( value ) );
    }

    public static Restriction between( String path, Object lside, Object rside )
    {
        return new Restriction( path, new BetweenOperator( lside, rside ) );
    }

    public static Restriction like( String path, Object value, MatchMode matchMode )
    {
        return new Restriction( path, new LikeOperator( value, true, matchMode ) );
    }

    public static Restriction notLike( String path, Object value, MatchMode matchMode )
    {
        return new Restriction( path, new NotLikeOperator( value, true, matchMode ) );
    }

    public static Restriction ilike( String path, Object value, MatchMode matchMode )
    {
        return new Restriction( path, new LikeOperator( value, false, matchMode ) );
    }

    public static Restriction notIlike( String path, Object value, MatchMode matchMode )
    {
        return new Restriction( path, new NotLikeOperator( value, false, matchMode ) );
    }

    public static Restriction in( String path, Collection<?> values )
    {
        return new Restriction( path, new InOperator( values ) );
    }

    public static Restriction notIn( String path, Collection<?> values )
    {
        return new Restriction( path, new NotInOperator( values ) );
    }

    public static Restriction isNull( String path )
    {
        return new Restriction( path, new NullOperator() );
    }

    public static Restriction isNotNull( String path )
    {
        return new Restriction( path, new NotNullOperator() );
    }

    private Restrictions()
    {
    }
}
