package org.hisp.dhis.query;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.util.Collection;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public final class Restrictions
{
    public static Restriction eq( String path, Object value )
    {
        return new Restriction( path, Operator.EQ, value );
    }

    public static Restriction ne( String path, Object value )
    {
        return new Restriction( path, Operator.NE, value );
    }

    public static Restriction gt( String path, Object value )
    {
        return new Restriction( path, Operator.GT, value );
    }

    public static Restriction lt( String path, Object value )
    {
        return new Restriction( path, Operator.LT, value );
    }

    public static Restriction ge( String path, Object value )
    {
        return new Restriction( path, Operator.GE, value );
    }

    public static Restriction le( String path, Object value )
    {
        return new Restriction( path, Operator.LE, value );
    }

    public static Restriction between( String path, Object lside, Object rside )
    {
        return new Restriction( path, Operator.BETWEEN, lside, rside );
    }

    // Map like to ilike for the moment, since like in the web-api is actually ilike..
    public static Restriction like( String path, Object value )
    {
        return new Restriction( path, Operator.ILIKE, value );
    }

    public static Restriction ilike( String path, Object value )
    {
        return new Restriction( path, Operator.ILIKE, value );
    }

    public static Restriction in( String path, Object... values )
    {
        return new Restriction( path, Operator.IN, values );
    }

    public static Restriction in( String path, Collection<?> values )
    {
        return new Restriction( path, Operator.IN, values );
    }

    public static Restriction isNull( String path )
    {
        return new Restriction( path, Operator.NULL );
    }

    private Restrictions()
    {
    }
}
