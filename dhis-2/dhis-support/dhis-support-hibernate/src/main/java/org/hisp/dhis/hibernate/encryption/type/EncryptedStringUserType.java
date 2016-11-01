package org.hisp.dhis.hibernate.encryption.type;

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

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.hisp.dhis.hibernate.encryption.HibernateEncryptorRegistry;
import org.jasypt.encryption.pbe.PBEStringEncryptor;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

/**
 * Hibernate {@link UserType} implementation which employs a {@link PBEStringEncryptor} to
 * perform transparent encryption/decryption of {@link String} properties.
 *
 * The employed encryptor is resolved from the {@link HibernateEncryptorRegistry}, which must be
 * set up with a named encryptor. The encryptor is resolved through the 'encryptor' parameter,
 * which looks up the given name in the registry.
 *
 * If no 'encryptor' parameter is given, or the given name does not resolve to a
 * {@link PBEStringEncryptor} in the {@link HibernateEncryptorRegistry}, an
 * {@link IllegalArgumentException} is thrown at initialization.
 *
 * This class implements a similar pattern to the encrypted types provided by the
 * org.jasypt.hibernate4 package, but serves to avoid this dependency (which breaks on Hibernate > 5.1.x).
 *
 * @author Halvdan Hoem Grelland
 */
public class EncryptedStringUserType
    implements UserType, ParameterizedType
{
    public static final String PARAMETER_ENCRYPTOR = "encryptor";

    private static final int[] sqlTypes = new int[] { Types.VARCHAR };

    private String encryptorName = null;

    private PBEStringEncryptor encryptor = null;

    @Override
    public int[] sqlTypes()
    {
        return sqlTypes.clone();
    }

    @Override
    public Class returnedClass()
    {
        return String.class;
    }

    @Override
    public boolean equals( Object x, Object y )
        throws HibernateException
    {
        return x == y || ( x != null && y != null && x.equals( y ) );
    }

    @Override
    public int hashCode( final Object x )
        throws HibernateException
    {
        return x.hashCode();
    }

    @Override
    public Object nullSafeGet( ResultSet rs, String[] names, SessionImplementor session, Object owner )
        throws HibernateException, SQLException
    {
        ensureEncryptorInit();

        String value = rs.getString( names[0] );

        return rs.wasNull() ? null : encryptor.decrypt( value );
    }

    @Override
    public void nullSafeSet( PreparedStatement st, Object value, int index, SessionImplementor session )
        throws HibernateException, SQLException
    {
        ensureEncryptorInit();

        if ( value == null )
        {
            st.setNull( index, Types.VARCHAR );
        }
        else
        {
            st.setString( index, encryptor.encrypt( value.toString() ) );
        }
    }

    @Override
    public Object deepCopy( final Object value )
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
    public Serializable disassemble( Object value ) throws HibernateException
    {
        return value == null ? null : (Serializable) deepCopy( value );
    }

    @Override
    public Object assemble( Serializable cached, Object owner ) throws HibernateException
    {
        return cached == null ? null : deepCopy( cached );
    }

    @Override
    public Object replace( Object original, Object target, Object owner ) throws HibernateException
    {
        return original;
    }

    @Override
    public void setParameterValues( Properties parameters )
    {
        this.encryptorName = parameters.getProperty( PARAMETER_ENCRYPTOR );

        if ( encryptorName == null )
        {
            throw new IllegalArgumentException(
                String.format( "Required parameter '%s' is not configured", PARAMETER_ENCRYPTOR ) );
        }
    }

    // Private methods

    private synchronized void ensureEncryptorInit()
    {
        if ( encryptor == null )
        {
            encryptor = HibernateEncryptorRegistry.getInstance().getEncryptor( encryptorName );
        }
    }
}
