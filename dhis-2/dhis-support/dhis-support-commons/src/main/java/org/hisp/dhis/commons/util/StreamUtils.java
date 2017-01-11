package org.hisp.dhis.commons.util;

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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utility class with methods for streaming input and output.
 * 
 * @author Lars Helge Overland
 */
public class StreamUtils
{
    public static final String LINE_BREAK = "\n";
    public static final String ENCODING_UTF8 = "UTF-8";

    /**
     * Reads the content of the file to a StringBuffer. Each line is compared to
     * the keys of the argument map. If a line is matched, the line is replaced
     * with the keys corresponding value. Passing null as replace map argument skips
     * value replacement. The reading will stop at the first match for a single
     * line.
     *
     * @param file       the file to read from.
     * @param replaceMap a map containing keys to be matched and values with replacements.
     * @return a StringBuffer with the content of the file replaced according to the Map.
     * @throws IOException if operation failed.
     */
    public static StringBuffer readContent( File file, Map<String[], String> replaceMap )
        throws IOException
    {
        StringBuffer content = new StringBuffer();

        BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( file ), ENCODING_UTF8 ) );

        String line = null;

        String currentEndString = null;

        try
        {
            while ( (line = reader.readLine()) != null )
            {
                if ( currentEndString != null )
                {
                    if ( line.contains( currentEndString ) )
                    {
                        currentEndString = null;
                    }

                    continue;
                }

                if ( replaceMap != null )
                {
                    for ( Entry<String[], String> entry : replaceMap.entrySet() )
                    {
                        if ( line.contains( entry.getKey()[0] ) )
                        {
                            currentEndString = (entry.getKey()[1] != null && !line.contains( entry.getKey()[1] )) ? entry.getKey()[1] : null;

                            line = entry.getValue();

                            break;
                        }
                    }
                }

                content.append( line + LINE_BREAK );
            }
        }
        finally
        {
            try
            {
                reader.close();
            }
            catch ( Exception ex )
            {
            }
        }

        return content;
    }

    /**
     * Test for ZIP/GZIP stream signature. Wraps the input stream in a
     * BufferedInputStream. If ZIP/GZIP test is true wraps again in ZipInputStream/GZIPInputStream.
     *
     * @param in the InputStream.
     * @return the wrapped InputStream.
     * @throws IOException if operation failed.
     */
    public static InputStream wrapAndCheckCompressionFormat( InputStream in )
        throws IOException
    {
        BufferedInputStream bufferedIn = new BufferedInputStream( in );

        if ( isZip( bufferedIn ) )
        {
            ZipInputStream zipIn = new ZipInputStream( bufferedIn );
            zipIn.getNextEntry();
            return zipIn;
        }
        else if ( isGZip( bufferedIn ) )
        {
            GZIPInputStream gzipIn = new GZIPInputStream( bufferedIn );
            return gzipIn;
        }

        return bufferedIn;
    }

    /**
     * Test for ZIP stream signature.
     *
     * @param in the BufferedInputStream to test.
     * @return true if input stream is zip.
     */
    public static boolean isZip( BufferedInputStream in )
    {
        in.mark( 4 );
        byte[] b = new byte[4];
        byte[] zipSig = new byte[4];
        zipSig[0] = 0x50;
        zipSig[1] = 0x4b;
        zipSig[2] = 0x03;
        zipSig[3] = 0x04;

        try
        {
            in.read( b, 0, 4 );
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( "Couldn't read header from stream ", ex );
        }
        try
        {
            in.reset();
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( "Couldn't reset stream ", ex );
        }

        return Arrays.equals( b, zipSig );
    }

    /**
     * Test for GZIP stream signature.
     *
     * @param instream the BufferedInputStream to test.
     * @return true if input stream is gzip.
     */
    public static boolean isGZip( BufferedInputStream instream )
    {
        instream.mark( 2 );
        byte[] b = new byte[2];

        try
        {
            instream.read( b, 0, 2 );
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( "Couldn't read header from stream ", ex );
        }
        
        try
        {
            instream.reset();
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( "Couldn't reset stream ", ex );
        }

        return (b[0] == 31 && b[1] == -117);
    }

    /**
     * Reads the next ZIP file entry from the ZipInputStream and positions the
     * stream at the beginning of the entry data.
     *
     * @param in the ZipInputStream to read from.
     * @return a ZipEntry.
     */
    public static ZipEntry getNextZipEntry( ZipInputStream in )
    {
        try
        {
            return in.getNextEntry();
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( "Failed to get next entry in ZIP-file", ex );
        }
    }

    /**
     * Closes the current ZipEntry and positions the stream for writing the next entry.
     *
     * @param out the ZipOutputStream.
     */
    public static void closeZipEntry( ZipOutputStream out )
    {
        try
        {
            out.closeEntry();
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( "Failed to close the current ZipEntry", ex );
        }
    }

    /**
     * Finishes writing the contents of the ZIP output stream without closing the underlying stream.
     *
     * @param out the ZipOutputStream.
     */
    public static void finishZipEntry( ZipOutputStream out )
    {
        try
        {
            out.finish();
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( "Failed to finish the content of the ZipOutputStream", ex );
        }
    }
}
