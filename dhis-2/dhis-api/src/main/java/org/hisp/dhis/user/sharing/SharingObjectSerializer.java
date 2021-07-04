/*
 * Copyright (c) 2004-2004-2021, University of Oslo
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
package org.hisp.dhis.user.sharing;

import java.io.IOException;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

/**
 * Custom serializer for {@link UserAccess} and {@link UserGroupAccess}.
 * <p>
 * Currently this class doesn't work with XMLMapper from
 * JacksonObjectMapperConfig.
 * <p>
 * It only works with StAXNodeSerializer, which is used by NodeService.
 */
public class SharingObjectSerializer
    extends JsonSerializer<Map<String, AccessObject>>
{
    @Override
    public void serialize( Map<String, AccessObject> map, JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider )
        throws IOException
    {
        if ( ToXmlGenerator.class.isAssignableFrom( jsonGenerator.getClass() ) )
        {
            ToXmlGenerator xmlGenerator = (ToXmlGenerator) jsonGenerator;
            XMLStreamWriter staxWriter = xmlGenerator.getStaxWriter();
            map.values().forEach( accessObject -> writeAccessObject( staxWriter, accessObject ) );
        }
        else
        {
            jsonGenerator.writeStartObject();
            map.values().forEach( accessObject -> writeAccessObject( jsonGenerator, accessObject ) );
            jsonGenerator.writeEndObject();
        }
    }

    private void writeAccessObject( JsonGenerator jsonGenerator, AccessObject accessObject )
    {
        try
        {
            jsonGenerator.writeFieldName( accessObject.getId() );
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField( "id", accessObject.getId() );
            jsonGenerator.writeStringField( "access", accessObject.getAccess() );
            jsonGenerator.writeEndObject();
        }
        catch ( IOException e )
        {
            throw new IllegalQueryException( new ErrorMessage( ErrorCode.E1106, accessObject.getId() ) );
        }
    }

    private void writeAccessObject( XMLStreamWriter staxWriter, AccessObject accessObject )
    {
        try
        {
            staxWriter.writeStartElement( accessObject.getId() );
            staxWriter.writeStartElement( "id" );
            staxWriter.writeCharacters( accessObject.getId() );
            staxWriter.writeEndElement();

            staxWriter.writeStartElement( "access" );
            staxWriter.writeCharacters( accessObject.getAccess() );
            staxWriter.writeEndElement();
            staxWriter.writeEndElement();
        }
        catch ( XMLStreamException e )
        {
            throw new IllegalQueryException( new ErrorMessage( ErrorCode.E1106, accessObject.getId() ) );
        }
    }
}