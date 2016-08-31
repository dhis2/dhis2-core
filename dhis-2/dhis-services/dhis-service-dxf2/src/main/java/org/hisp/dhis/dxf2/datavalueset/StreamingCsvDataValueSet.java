package org.hisp.dhis.dxf2.datavalueset;

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

import java.io.IOException;

import org.hisp.dhis.dxf2.datavalue.DataValue;
import org.hisp.dhis.dxf2.datavalue.StreamingCsvDataValue;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

/**
 * @author Lars Helge Overland
 */
public class StreamingCsvDataValueSet
    extends DataValueSet
{
    private CsvWriter writer;
    
    private CsvReader reader;
    
    public StreamingCsvDataValueSet( CsvWriter writer )
    {
        this.writer = writer;
        
        try
        {
            this.writer.writeRecord( StreamingCsvDataValue.getHeaders() ); // Write headers
        }
        catch ( IOException ex )
        {
            throw new RuntimeException( "Failed to write CSV headers", ex );
        }
    }
    
    public StreamingCsvDataValueSet( CsvReader reader )
    {
        this.reader = reader;
    }
    
    @Override
    public boolean hasNextDataValue()
    {
        try
        {
            return reader.readRecord();
        }
        catch ( IOException ex )
        {
            throw new RuntimeException( "Failed to read record", ex );
        }
    }

    @Override
    public DataValue getNextDataValue()
    {
        try
        {
            return new StreamingCsvDataValue( reader.getValues() );
        }
        catch ( IOException ex )
        {
            throw new RuntimeException( "Failed to get CSV values", ex );
        }
    }

    @Override
    public DataValue getDataValueInstance()
    {
        return new StreamingCsvDataValue( writer );
    }

    @Override
    public void close()
    {
        if ( writer != null )
        {
            writer.close();
        }
        
        if ( reader != null )
        {
            reader.close();
        }
    }    
}
