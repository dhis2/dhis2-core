package org.hisp.dhis.common.adapter;

/*
 * Copyright (c) 2004-2019, University of Oslo
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
import org.hisp.dhis.render.DeviceRenderTypeMap;
import org.hisp.dhis.render.RenderDevice;
import org.hisp.dhis.render.type.RenderingObject;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Set;

public class DeviceRenderTypeMapSerializer
    extends JsonSerializer<DeviceRenderTypeMap<RenderingObject>>
{
    @Override
    public void serialize( DeviceRenderTypeMap<RenderingObject> value, JsonGenerator gen, SerializerProvider serializers )
        throws IOException
    {
        Set<RenderDevice> keys = value.keySet();
        if ( ToXmlGenerator.class.isAssignableFrom( gen.getClass() ) )
        {
            ToXmlGenerator xmlGenerator = (ToXmlGenerator) gen;
            try
            {
                XMLStreamWriter staxWriter = xmlGenerator.getStaxWriter();
                for ( RenderDevice key : keys )
                {
                    RenderingObject val =  value.get( key );
                    staxWriter.writeStartElement(  key.name() );
                    staxWriter.writeAttribute( RenderingObject._TYPE, val.getType().name() );
                    staxWriter.writeEndElement();
                }
            }
            catch ( XMLStreamException e )
            {
                throw new RuntimeException( e );
            }
        }
        else
        {
            gen.writeStartObject();

            for ( RenderDevice key : keys )
            {
                Object val =  value.get( key );
                String fieldValue = "";

                if ( LinkedHashMap.class.isAssignableFrom( val.getClass() ) )
                {
                    LinkedHashMap<String,String> map = ( LinkedHashMap<String, String> ) val;
                    fieldValue = map.get( RenderingObject._TYPE );
                }
                else if ( RenderingObject.class.isAssignableFrom( val.getClass()) )
                {
                    RenderingObject renderingObject = ( RenderingObject ) val;
                    fieldValue = renderingObject.getType().name();
                }
                else
                {
                    fieldValue = val.toString();
                }

                gen.writeObjectFieldStart( key.name() );
                gen.writeStringField( RenderingObject._TYPE,  fieldValue );
                gen.writeEndObject();
            }
            gen.writeEndObject();
        }
    }
}