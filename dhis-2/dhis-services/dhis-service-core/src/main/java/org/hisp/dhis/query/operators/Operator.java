package org.hisp.dhis.query.operators;

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

import org.hibernate.criterion.Criterion;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.query.QueryUtils;
import org.hisp.dhis.query.Type;
import org.hisp.dhis.query.Typed;
import org.hisp.dhis.schema.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public abstract class Operator
{
    protected final List<Object> args = new ArrayList<>();

    protected final Typed typed;

    protected Type argumentType;

    public Operator( Typed typed )
    {
        this.typed = typed;
    }

    public Operator( Typed typed, Object arg )
    {
        this( typed );
        this.argumentType = new Type( arg );
        this.args.add( arg );
        validate();
    }

    private void validate()
    {
        for ( Object arg : args )
        {
            if ( !isValid( arg.getClass() ) )
            {
                throw new QueryParserException( "Value `" + arg + "` of type `" + arg.getClass().getSimpleName() + "` is not supported by this operator." );
            }
        }
    }

    public Operator( Typed typed, Object... args )
    {
        this( typed );
        this.argumentType = new Type( args[0] );
        Collections.addAll( this.args, args );
    }

    public List<Object> getArgs()
    {
        return args;
    }

    protected <T> T getValue( Class<T> klass, Class<?> secondaryClass, int idx )
    {
        return QueryUtils.parseValue( klass, secondaryClass, args.get( idx ) );
    }

    protected <T> T getValue( Class<T> klass, int idx )
    {
        return QueryUtils.parseValue( klass, null, args.get( idx ) );
    }

    protected <T> T getValue( Class<T> klass )
    {
        return getValue( klass, 0 );
    }

    protected <T> T getValue( Class<T> klass, Class<?> secondaryClass, Object value )
    {
        return QueryUtils.parseValue( klass, secondaryClass, value );
    }

    protected <T> T getValue( Class<T> klass, Object value )
    {
        return QueryUtils.parseValue( klass, value );
    }

    public boolean isValid( Class<?> klass )
    {
        return typed.isValid( klass );
    }

    public abstract Criterion getHibernateCriterion( Property property );

    public abstract boolean test( Object value );
}
