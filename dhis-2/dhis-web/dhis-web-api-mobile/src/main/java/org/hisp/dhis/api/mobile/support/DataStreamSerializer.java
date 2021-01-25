package org.hisp.dhis.api.mobile.support;



import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.hisp.dhis.api.mobile.model.DataStreamSerializable;

public class DataStreamSerializer
{
    public static DataStreamSerializable read( Class<? extends DataStreamSerializable> clazz, InputStream input )
        throws IOException
    {
        DataStreamSerializable t;
        try
        {
            t = clazz.newInstance();
            t.deSerialize( new DataInputStream( input ) );
            return t;
        }
        catch ( InstantiationException e )
        {
            e.printStackTrace();
            throw new IOException( "Can't instantiate class " + clazz.getName(), e );
        }
        catch ( IllegalAccessException e )
        {
            e.printStackTrace();
            throw new IOException( "Not allowed to instantiate class " + clazz.getName(), e );
        }
    }

    public static void write( DataStreamSerializable entity, OutputStream out )
        throws IOException
    {
        ByteArrayOutputStream baos = serializePersistent( entity );
        GZIPOutputStream gzip = new GZIPOutputStream( out );
        DataOutputStream dos = new DataOutputStream( gzip );

        try
        {
            byte[] res = baos.toByteArray();
            dos.write( res );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        finally
        {
            dos.flush();
            dos.close();

            //gzip.finish();
        }
    }

    private static ByteArrayOutputStream serializePersistent( DataStreamSerializable entity )
        throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream( baos );
        entity.serialize( out );
        out.flush();
        return baos;
    }
}
