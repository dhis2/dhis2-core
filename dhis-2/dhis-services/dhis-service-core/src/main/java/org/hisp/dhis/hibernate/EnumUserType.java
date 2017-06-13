package org.hisp.dhis.hibernate;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

/**
 * Template for storing enums. Borrowed from http://community.jboss.org/wiki/
 * UserTypeforpersistinganEnumwithaVARCHARcolumn
 */
public class EnumUserType<E extends Enum<E>>
    implements UserType
{
    private Class<E> clazz = null;

    protected EnumUserType( Class<E> c )
    {
        this.clazz = c;
    }

    private static final int[] SQL_TYPES = { Types.VARCHAR };

    @Override
    public int[] sqlTypes()
    {
        return SQL_TYPES;
    }

    @Override
    public Class<?> returnedClass()
    {
        return clazz;
    }

    @Override
    public Object nullSafeGet( ResultSet resultSet, String[] names, SessionImplementor impl, Object owner )
        throws HibernateException, SQLException
    {
        String name = resultSet.getString( names[0] );
        E result = null;
        if ( !resultSet.wasNull() )
        {
            result = Enum.valueOf( clazz, name );
        }
        return result;
    }

    @Override
    public void nullSafeSet( PreparedStatement preparedStatement, Object value, int index, SessionImplementor impl )
        throws HibernateException, SQLException
    {
        if ( null == value )
        {
            preparedStatement.setNull( index, Types.VARCHAR );
        }
        else
        {
            preparedStatement.setString( index, ((Enum<?>) value).name() );
        }
    }

    @Override
    public Object deepCopy( Object value )
        throws HibernateException
    {
        return value;
    }

    @Override
    public boolean isMutable()
    {
        return false;
    }

    @Override
    public Object assemble( Serializable cached, Object owner )
        throws HibernateException
    {
        return cached;
    }

    @Override
    public Serializable disassemble( Object value )
        throws HibernateException
    {
        return (Serializable) value;
    }

    @Override
    public Object replace( Object original, Object target, Object owner )
        throws HibernateException
    {
        return original;
    }

    @Override
    public int hashCode( Object x )
        throws HibernateException
    {
        return x.hashCode();
    }

    @Override
    public boolean equals( Object x, Object y )
        throws HibernateException
    {
        if ( x == y )
            return true;
        if ( null == x || null == y )
            return false;
        return x.equals( y );
    }
}