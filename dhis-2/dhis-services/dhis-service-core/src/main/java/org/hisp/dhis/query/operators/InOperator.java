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
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.query.Type;
import org.hisp.dhis.query.Typed;
import org.hisp.dhis.schema.Property;

import java.util.Collection;
import java.util.Date;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class InOperator extends Operator
{
    public InOperator( Collection<?> arg )
    {
        super( Typed.from( Collection.class ), arg );
    }

    @Override
    public Criterion getHibernateCriterion( Property property )
    {
        return Restrictions.in( property.getFieldName(), getValue( Collection.class, property.getKlass(), args.get( 0 ) ) );
    }

    @Override
    public boolean test( Object value )
    {
        Collection<?> items = getValue( Collection.class );

        if ( items == null || value == null )
        {
            return false;
        }

        Type type = new Type( value );

        for ( Object item : items )
        {
            if ( compare( type, item, value ) )
            {
                return true;
            }
        }

        return false;
    }

    private boolean compare( Type type, Object lside, Object rside )
    {
        if ( type.isString() )
        {
            String s1 = getValue( String.class, lside );
            String s2 = (String) rside;

            return s1 != null && s2.equals( s1 );
        }
        else if ( type.isBoolean() )
        {
            Boolean s1 = getValue( Boolean.class, lside );
            Boolean s2 = (Boolean) rside;

            return s1 != null && s2.equals( s1 );
        }
        else if ( type.isInteger() )
        {
            Integer s1 = getValue( Integer.class, lside );
            Integer s2 = (Integer) rside;

            return s1 != null && s2.equals( s1 );
        }
        else if ( type.isFloat() )
        {
            Float s1 = getValue( Float.class, lside );
            Float s2 = (Float) rside;

            return s1 != null && s2.equals( s1 );
        }
        else if ( type.isDate() )
        {
            Date s1 = getValue( Date.class, lside );
            Date s2 = (Date) rside;

            return s1 != null && s2.equals( s1 );
        }
        else if ( type.isEnum() )
        {
            String s1 = String.valueOf( lside );
            String s2 = String.valueOf( rside );

            return s1 != null && s2.equals( s1 );
        }

        return false;
    }
}
