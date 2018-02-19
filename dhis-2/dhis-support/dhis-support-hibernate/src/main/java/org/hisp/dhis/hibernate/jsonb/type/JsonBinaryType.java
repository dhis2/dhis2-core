package org.hisp.dhis.hibernate.jsonb.type;

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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.postgresql.util.PGobject;

import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author Stian Sandvold <stian@dhis2.org>
 */
@SuppressWarnings("rawtypes")
public class JsonBinaryType implements UserType, ParameterizedType
{
    public static final ObjectMapper MAPPER = new ObjectMapper();

    static
    {
        MAPPER.setSerializationInclusion( JsonInclude.Include.NON_NULL );
    }

    private ObjectWriter writer;

    private ObjectReader reader;

    private Class returnedClass;

    @Override
    public int[] sqlTypes()
    {
        return new int[]{ Types.JAVA_OBJECT };
    }

    @Override
    public Class returnedClass()
    {
        return returnedClass;
    }

    @Override
    public boolean equals( Object x, Object y ) throws HibernateException
    {
        return x == y || !(x == null || y == null) && x.equals( y );
    }

    @Override
    public int hashCode( Object x ) throws HibernateException
    {
        return null == x ? 0 : x.hashCode();
    }

    @Override
    public Object nullSafeGet( ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner ) throws HibernateException, SQLException
    {
        final Object result = rs.getObject( names[0] );

        if ( !rs.wasNull() )
        {
            String content = null;

            if ( result instanceof String )
            {
                content = (String) result;
            }
            else if ( result instanceof PGobject )
            {
                content = ((PGobject) result).getValue();
            }
            
            // Other types currently ignored
            
            if ( content != null )
            {
                return convertJsonToObject( content );
            }
        }

        return null;
    }
    
    @Override
    public void nullSafeSet( PreparedStatement ps, Object value, int idx, SharedSessionContractImplementor session ) throws HibernateException, SQLException
    {
        if ( value == null )
        {
            ps.setObject( idx, null );
            return;
        }

        PGobject pg = new PGobject();
        pg.setType( "jsonb" );
        pg.setValue( convertObjectToJson( value ) );

        ps.setObject( idx, pg );
    }

    @Override
    public Object deepCopy( Object value ) throws HibernateException
    {
        String json = convertObjectToJson( value );
        return convertJsonToObject( json );
    }

    @Override
    public boolean isMutable()
    {
        return true;
    }

    @Override
    public Serializable disassemble( Object value ) throws HibernateException
    {
        return (Serializable) this.deepCopy( value );
    }

    @Override
    public Object assemble( Serializable cached, Object owner ) throws HibernateException
    {
        return this.deepCopy( cached );
    }

    @Override
    public Object replace( Object original, Object target, Object owner ) throws HibernateException
    {
        return this.deepCopy( original );
    }

    @Override
    public void setParameterValues( Properties parameters )
    {
        final String clazz = (String) parameters.get( "clazz" );

        if ( clazz == null )
        {
            throw new IllegalArgumentException(
                String.format( "Required parameter '%s' is not configured", "clazz" ) );
        }

        try
        {
            init( classForName( clazz ) );
        }
        catch ( ClassNotFoundException e )
        {
            throw new IllegalArgumentException( "Class: " + clazz + " is not a known class type." );
        }
    }

    private void init( Class klass )
    {
        returnedClass = klass;
        reader = MAPPER.readerFor( klass );
        writer = MAPPER.writerFor( klass );
    }

    private static Class classForName( String name ) throws ClassNotFoundException
    {
        try
        {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            
            if ( classLoader != null )
            {
                return classLoader.loadClass( name );
            }
        }
        catch ( Throwable ignore )
        {
        }

        return Class.forName( name );
    }

    /**
     * Serializes an object to JSON.
     * 
     * @param object the object to convert.
     * @return JSON content.
     */
    protected String convertObjectToJson( Object object )
    {
        try
        {
            return writer.writeValueAsString( object );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    /**
     * Deserializes JSON content to an object.
     * 
     * @param content the JSON content.
     * @return an object.
     */
    protected Object convertJsonToObject( String content )
    {
        try
        {
            return reader.readValue( content );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
}
