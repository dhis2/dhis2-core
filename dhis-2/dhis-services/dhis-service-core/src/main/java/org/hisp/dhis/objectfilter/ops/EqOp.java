package org.hisp.dhis.objectfilter.ops;

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
import java.util.Date;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class EqOp extends Op
{
    @Override
    public OpStatus evaluate( Object object )
    {
        if ( getValue() == null || object == null )
        {
            return OpStatus.EXCLUDE;
        }

        if ( String.class.isInstance( object ) )
        {
            String s1 = getValue( String.class );
            String s2 = (String) object;

            return (s1 != null && s2.equals( s1 )) ? OpStatus.INCLUDE : OpStatus.EXCLUDE;
        }
        else if ( Boolean.class.isInstance( object ) )
        {
            Boolean s1 = getValue( Boolean.class );
            Boolean s2 = (Boolean) object;

            return (s1 != null && s2.equals( s1 )) ? OpStatus.INCLUDE : OpStatus.EXCLUDE;
        }
        else if ( Integer.class.isInstance( object ) )
        {
            Integer s1 = getValue( Integer.class );
            Integer s2 = (Integer) object;

            return (s1 != null && s2.equals( s1 )) ? OpStatus.INCLUDE : OpStatus.EXCLUDE;
        }
        else if ( Float.class.isInstance( object ) )
        {
            Float s1 = getValue( Float.class );
            Float s2 = (Float) object;

            return (s1 != null && s2.equals( s1 )) ? OpStatus.INCLUDE : OpStatus.EXCLUDE;
        }
        else if ( Collection.class.isInstance( object ) )
        {
            Collection<?> collection = (Collection<?>) object;
            Integer size = getValue( Integer.class );

            if ( size != null && collection.size() == size )
            {
                return OpStatus.INCLUDE;
            }
            else
            {
                return OpStatus.EXCLUDE;
            }
        }
        else if ( Date.class.isInstance( object ) )
        {
            Date s1 = getValue( Date.class );
            Date s2 = (Date) object;

            return (s1 != null && s2.equals( s1 )) ? OpStatus.INCLUDE : OpStatus.EXCLUDE;
        }
        else if ( Enum.class.isInstance( object ) )
        {
            String s1 = getValue();
            String s2 = String.valueOf( object );

            return (s1 != null && s2.equals( s1 )) ? OpStatus.INCLUDE : OpStatus.EXCLUDE;
        }

        return OpStatus.EXCLUDE;
    }
}
