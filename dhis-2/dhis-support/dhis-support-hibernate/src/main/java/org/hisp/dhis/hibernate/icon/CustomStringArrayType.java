/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.hibernate.icon;

import java.io.Serializable;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

public class CustomStringArrayType implements UserType
{
    @Override
    public int[] sqlTypes()
    {
        return new int[] { Types.ARRAY };
    }

    @Override
    public Class returnedClass()
    {
        return String[].class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        if (x instanceof String[] xString && y instanceof String[] yString) {
            return Arrays.deepEquals(xString, yString);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode( Object x )
        throws HibernateException
    {
        return Arrays.hashCode( (String[]) x );
    }

    @Override
    public Object nullSafeGet( ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner )
        throws HibernateException,
        SQLException
    {
        Array array = rs.getArray( names[0] );
        return array != null ? array.getArray() : null;
    }

    @Override
    public void nullSafeSet( PreparedStatement st, Object value, int index, SharedSessionContractImplementor session )
        throws HibernateException,
        SQLException
    {
        if ( value != null && st != null )
        {
            Array array = session.connection().createArrayOf( "text", (String[]) value );
            st.setArray( index, array );
        }
        else
        {
            if ( st != null )
            {
                st.setNull( index, sqlTypes()[0] );
            }
        }
    }

    @Override
    public Object deepCopy( Object value )
        throws HibernateException
    {
        String[] a = (String[]) value;
        return Arrays.copyOf( a, a.length );
    }

    @Override
    public boolean isMutable()
    {
        return false;
    }

    @Override
    public Serializable disassemble( Object value )
        throws HibernateException
    {
        return (Serializable) value;
    }

    @Override
    public Object assemble( Serializable cached, Object owner )
        throws HibernateException
    {
        return cached;
    }

    @Override
    public Object replace( Object original, Object target, Object owner )
        throws HibernateException
    {
        return original;
    }
}
