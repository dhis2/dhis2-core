package org.hisp.dhis.common;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import org.hisp.dhis.user.User;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.NotSerializableException;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public class CustomLastUpdatedUserSerializer extends JsonSerializer<User>
{
    @Override
    public void serialize( User user, JsonGenerator jsonGenerator, SerializerProvider serializerProvider ) throws  IOException
    {
        if ( ToXmlGenerator.class.isAssignableFrom( jsonGenerator.getClass() ) )
        {
            ToXmlGenerator xmlGenerator = ( ToXmlGenerator ) jsonGenerator;
            try
            {
                XMLStreamWriter staxWriter = xmlGenerator.getStaxWriter();
                staxWriter.writeStartElement( "lastUpdatedBy" );
                staxWriter.writeAttribute( "id", user.getUid() );
                staxWriter.writeAttribute( "name", user.getDisplayName() );
                staxWriter.writeEndElement();
            }
            catch ( XMLStreamException e )
            {
                throw new NotSerializableException( "Failed to serialize User object:" + user );
            }
        }
        else
        {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField( "id", user.getUid() );
            jsonGenerator.writeStringField( "name", user.getDisplayName() );
            jsonGenerator.writeEndObject();
        }
    }
}