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

import org.hisp.dhis.schema.Klass;

import java.util.Date;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public enum Operator
{
    EQ( Typed.from( String.class, Boolean.class, Number.class, Date.class ), 1 ),
    NE( Typed.from( String.class, Boolean.class, Number.class, Date.class ), 1 ),
    GT( Typed.from( String.class, Boolean.class, Number.class, Date.class ), 1 ),
    LT( Typed.from( String.class, Boolean.class, Number.class, Date.class ), 1 ),
    GE( Typed.from( String.class, Boolean.class, Number.class, Date.class ), 1 ),
    LE( Typed.from( String.class, Boolean.class, Number.class, Date.class ), 1 ),
    NULL( Typed.from( String.class, Boolean.class, Number.class, Date.class ) ),
    BETWEEN( Typed.from( String.class, Number.class, Date.class ), 2 ),
    LIKE( Typed.from( String.class ), 1 ),
    ILIKE( Typed.from( String.class ), 1 ),
    IN( 1, Integer.MAX_VALUE );

    Integer min;

    Integer max;

    // default is to allow all types
    Typed typed = Typed.from();

    Operator()
    {
        this.min = null;
        this.max = null;
    }

    Operator( Typed typed )
    {
        this.typed = typed;
        this.min = null;
        this.max = null;
    }

    Operator( int value )
    {
        this.min = value;
        this.max = value;
    }

    Operator( Typed typed, int value )
    {
        this.typed = typed;
        this.min = value;
        this.max = value;
    }

    Operator( int min, int max )
    {
        this.min = min;
        this.max = max;
    }

    Operator( Typed typed, int min, int max )
    {
        this.typed = typed;
        this.min = min;
        this.max = max;
    }

    public Integer getMin()
    {
        return min;
    }

    public Integer getMax()
    {
        return max;
    }

    public boolean isValid( Klass klass )
    {
        return typed.isValid( klass );
    }

    public boolean isValid( Class<?> klass )
    {
        return typed.isValid( klass );
    }
}
