/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dxf2.datavalueset;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hisp.dhis.dxf2.datavalue.DataValue;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Reads {@link DataValueSet} from JSON input using jackson's streaming API.
 *
 * @author Jan Bernitt
 */
final class JsonDataValueSetStreamReader implements DataValueSetReader
{
    private static final Map<String, BiConsumer<DataValueSet, String>> HEADER_SETTERS_BY_MEMBER_NAME = new HashMap<>();

    private static final Map<String, BiConsumer<DataValue, String>> VALUE_SETTERS_BY_MEMBER_NAME = new HashMap<>();

    static
    {
        HEADER_SETTERS_BY_MEMBER_NAME.put( "idScheme", DataValueSet::setIdScheme );
        HEADER_SETTERS_BY_MEMBER_NAME.put( "dataElementIdScheme", DataValueSet::setDataElementIdScheme );
        HEADER_SETTERS_BY_MEMBER_NAME.put( "orgUnitIdScheme", DataValueSet::setOrgUnitIdScheme );
        HEADER_SETTERS_BY_MEMBER_NAME.put( "categoryOptionComboIdScheme",
            DataValueSet::setCategoryOptionComboIdScheme );
        HEADER_SETTERS_BY_MEMBER_NAME.put( "dataSetIdScheme", DataValueSet::setDataSetIdScheme );
        HEADER_SETTERS_BY_MEMBER_NAME.put( "strategy", DataValueSet::setStrategy );
        HEADER_SETTERS_BY_MEMBER_NAME.put( "dataSet", DataValueSet::setDataSet );
        HEADER_SETTERS_BY_MEMBER_NAME.put( "completeDate", DataValueSet::setCompleteDate );
        HEADER_SETTERS_BY_MEMBER_NAME.put( "period", DataValueSet::setPeriod );
        HEADER_SETTERS_BY_MEMBER_NAME.put( "orgUnit", DataValueSet::setOrgUnit );
        HEADER_SETTERS_BY_MEMBER_NAME.put( "attributeOptionCombo", DataValueSet::setAttributeOptionCombo );

        VALUE_SETTERS_BY_MEMBER_NAME.put( "dataElement", DataValue::setDataElement );
        VALUE_SETTERS_BY_MEMBER_NAME.put( "period", DataValue::setPeriod );
        VALUE_SETTERS_BY_MEMBER_NAME.put( "orgUnit", DataValue::setOrgUnit );
        VALUE_SETTERS_BY_MEMBER_NAME.put( "categoryOptionCombo", DataValue::setCategoryOptionCombo );
        VALUE_SETTERS_BY_MEMBER_NAME.put( "attributeOptionCombo", DataValue::setAttributeOptionCombo );
        VALUE_SETTERS_BY_MEMBER_NAME.put( "value", DataValue::setValue );
        VALUE_SETTERS_BY_MEMBER_NAME.put( "storedBy", DataValue::setStoredBy );
        VALUE_SETTERS_BY_MEMBER_NAME.put( "created", DataValue::setCreated );
        VALUE_SETTERS_BY_MEMBER_NAME.put( "lastUpdated", DataValue::setLastUpdated );
        VALUE_SETTERS_BY_MEMBER_NAME.put( "comment", DataValue::setComment );
    }

    private final JsonParser parser;

    public JsonDataValueSetStreamReader( InputStream in )
    {
        try
        {
            this.parser = new JsonFactory().createParser( in );
        }
        catch ( IOException ex )
        {
            throw new UncheckedIOException( ex );
        }
    }

    @Override
    public DataValueSet readHeader()
    {
        try
        {
            if ( parser.nextToken() != JsonToken.START_OBJECT )
            {
                throw new IllegalArgumentException( "Expected data set root object" );
            }
            DataValueSet header = new DataValueSet();
            while ( parser.nextToken() == JsonToken.FIELD_NAME )
            {
                String member = parser.getCurrentName();
                JsonToken valueToken = parser.nextToken();
                if ( valueToken == JsonToken.VALUE_STRING )
                {
                    String value = parser.getText();
                    BiConsumer<DataValueSet, String> setter = HEADER_SETTERS_BY_MEMBER_NAME.get( member );
                    if ( setter != null && value != null )
                    {
                        setter.accept( header, value );
                    }
                }
                else if ( valueToken.isBoolean() )
                {
                    if ( "dryRun".equals( member ) )
                    {
                        header.setDryRun( parser.getBooleanValue() );
                    }
                }
                else if ( valueToken == JsonToken.START_ARRAY )
                {
                    return header;
                }
                else
                {
                    // ignore
                    parser.skipChildren();
                }

            }
            skipUntilToken( JsonToken.START_ARRAY );
            return header;
        }
        catch ( IOException ex )
        {
            throw new UncheckedIOException( ex );
        }
    }

    @Override
    public DataValueEntry readNext()
    {
        try
        {
            if ( parser.nextToken() == JsonToken.END_ARRAY )
            {
                return null;
            }
            skipUntilToken( JsonToken.START_OBJECT );
            DataValue next = new DataValue();
            while ( parser.nextToken() == JsonToken.FIELD_NAME )
            {
                String member = parser.getCurrentName();
                JsonToken valueToken = parser.nextToken();
                if ( valueToken == JsonToken.VALUE_STRING )
                {
                    String value = parser.getText();
                    BiConsumer<DataValue, String> setter = VALUE_SETTERS_BY_MEMBER_NAME.get( member );
                    if ( setter != null && value != null )
                    {
                        setter.accept( next, value );
                    }
                }
                else if ( valueToken == JsonToken.END_OBJECT )
                {
                    return next;
                }
                else if ( valueToken.isBoolean() )
                {
                    if ( "followup".equals( member ) )
                    {
                        next.setFollowup( parser.getBooleanValue() );
                    }
                    else if ( "deleted".equals( member ) )
                    {
                        next.setDeleted( parser.getBooleanValue() );
                    }
                }
                else
                {
                    // ignore
                    parser.skipChildren();
                }
            }
            return next;
        }
        catch ( IOException ex )
        {
            throw new UncheckedIOException( ex );
        }
    }

    @Override
    public void close()
    {
        try
        {
            parser.close();
        }
        catch ( IOException ex )
        {
            throw new UncheckedIOException( ex );
        }
    }

    private void skipUntilToken( JsonToken token )
        throws IOException
    {

        while ( parser.currentToken() != token )
        {
            parser.nextToken();
        }
    }
}
