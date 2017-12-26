package org.hisp.dhis.hibernate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.hisp.dhis.interpretation.Mention;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MentionType
    implements
    UserType
{

    private final int[] arrayTypes = new int[] { Types.ARRAY };
    
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public int[] sqlTypes()
    {
        return arrayTypes;
    }

    @Override
    public Class<List> returnedClass()
    {
        return List.class;
    }

    @Override
    public Object nullSafeGet( ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner )
        throws HibernateException,
        SQLException
    {
        if ( names != null && names.length > 0 && rs != null && rs.getArray( names[0] ) != null )
        {
            // weirdness causing either hibernate or postgres jdbc driver to
            // cause both short and
            // integer types to return.. no idea. Even odder after changing a
            // smallint array from
            // {0,1,2} to {0,1,2,4,5} it switch from Integer to Short.
            Object array = rs.getArray( names[0] ).getArray();
            List<Mention> mentionList = new ArrayList<Mention>();
            for ( int i = 0; i < ((String[])array).length; i++ ) {
                try
                {
                    mentionList.add( mapper.readValue( ((String[])array)[i].getBytes( "UTF-8" ), Mention.class ));
                }
                catch ( IOException e )
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
            
            return mentionList;

        }

        return null;
    }

    private String[] convertToText( Mention[] mentions )
    {
        String[] mentionsToString = new String[mentions.length];
        for ( int i = 0; i < mentions.length; i++ )
        {
            try
            {
                
                final StringWriter w = new StringWriter();
                mapper.writeValue( w, mentions[i] );
                w.flush();
                mentionsToString[i] = w.toString();
            }
            catch ( final Exception ex )
            {
                throw new RuntimeException( "Failed to convert Invoice to String: " + ex.getMessage(), ex );
            }
        }

        return mentionsToString;
    }

    @Override
    public void nullSafeSet( PreparedStatement st, Object value, int index, SharedSessionContractImplementor session )
        throws HibernateException,
        SQLException
    {
        if ( value != null && st != null )
        {
            List<Mention> list = (List<Mention>) value;
            Mention[] castObject = list.toArray( new Mention[list.size()] );
            Array array = session.connection().createArrayOf( "jsonb", this.convertToText( castObject ));
            st.setArray( index, array );
        }
        else
        {
            st.setNull( index, arrayTypes[0] );
        }

    }

    @Override
    public Object deepCopy( final Object value )
        throws HibernateException
    {
        if ( value == null )
            return null;

        List<Mention> list = (List<Mention>) value;
        ArrayList<Mention> clone = new ArrayList<Mention>();
        for ( Object intOn : list )
            clone.add( (Mention) intOn );

        return clone;
    }

    @Override
    public boolean isMutable()
    {
        return true;
    }

    @Override
    public Serializable disassemble( final Object value )
        throws HibernateException
    {
        return (Serializable) this.deepCopy( value );
    }

    @Override
    public Object assemble( final Serializable cached, final Object owner )
        throws HibernateException
    {
        return this.deepCopy( cached );
    }

    @Override
    public Object replace( final Object original, final Object target, final Object owner )
        throws HibernateException
    {
        return this.deepCopy( original );
    }

    @Override
    public boolean equals( final Object obj1, final Object obj2 )
        throws HibernateException
    {
        if ( obj1 == null )
        {
            return obj2 == null;
        }
        return obj1.equals( obj2 );
    }

    @Override
    public int hashCode( final Object obj )
        throws HibernateException
    {
        return obj.hashCode();
    }

}
