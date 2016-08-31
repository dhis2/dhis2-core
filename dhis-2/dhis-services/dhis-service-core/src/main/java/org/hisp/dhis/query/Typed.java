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

import org.hisp.dhis.schema.Klass;

import com.google.common.collect.Iterators;

/**
 * Simple class for checking if an object is one of several allowed classes, mainly used in Operator where
 * a parameter can be type constrained.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class Typed
{
    private final Class<?>[] klasses;

    public Typed( Class<?>[] klasses )
    {
        this.klasses = klasses;
    }

    public Class<?>[] getKlasses()
    {
        return klasses;
    }

    public boolean isValid( Klass klass )
    {
        return klass == null || isValid( klass.getKlass() );
    }

    public boolean isValid( Class<?> klass )
    {
        if ( klasses.length == 0 || klass == null )
        {
            return true;
        }

        for ( Class<?> k : klasses )
        {
            if ( k != null && k.isAssignableFrom( klass ) )
            {
                return true;
            }
        }

        return false;
    }

    public static Typed from( Class<?>... klasses )
    {
        return new Typed( klasses );
    }

    public static Typed from( Iterable<? extends Class<?>> iterable )
    {
        return new Typed( Iterators.toArray( iterable.iterator(), Class.class ) );
    }
}
