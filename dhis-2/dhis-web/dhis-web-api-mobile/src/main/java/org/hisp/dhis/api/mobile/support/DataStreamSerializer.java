package org.hisp.dhis.api.mobile.support;

<<<<<<< HEAD
/*
 * Copyright (c) 2004-2020, University of Oslo
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
=======

>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

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
