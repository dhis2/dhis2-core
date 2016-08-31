package org.hisp.dhis.common.adapter;

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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class JacksonOrganisationUnitGroupSymbolSerializer extends JsonSerializer<OrganisationUnitGroup>
{
    private static DateFormat DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssZ" );

    @Override
    public void serialize( OrganisationUnitGroup value, JsonGenerator jgen, SerializerProvider provider ) throws IOException
    {
        if ( ToXmlGenerator.class.isAssignableFrom( jgen.getClass() ) )
        {
            ToXmlGenerator xmlGenerator = (ToXmlGenerator) jgen;

            try
            {
                XMLStreamWriter staxWriter = xmlGenerator.getStaxWriter();

                staxWriter.writeStartElement( DxfNamespaces.DXF_2_0, "organisationUnitGroup" );
                staxWriter.writeAttribute( "id", value.getUid() );
                staxWriter.writeAttribute( "name", value.getName() );
                staxWriter.writeAttribute( "created", DATE_FORMAT.format( value.getCreated() ) );
                staxWriter.writeAttribute( "lastUpdated", DATE_FORMAT.format( value.getLastUpdated() ) );

                if ( value.getHref() != null )
                {
                    staxWriter.writeAttribute( "href", value.getHref() );
                }

                staxWriter.writeAttribute( "symbol", String.valueOf( value.getSymbol() ) );
                staxWriter.writeEndElement();
            }
            catch ( XMLStreamException e )
            {
                e.printStackTrace(); //TODO fix
            }
        }
        else
        {
            jgen.writeStartObject();

            jgen.writeStringField( "id", value.getUid() );
            jgen.writeStringField( "name", value.getName() );
            jgen.writeFieldName( "created" );
            provider.defaultSerializeDateValue( value.getCreated(), jgen );

            jgen.writeFieldName( "lastUpdated" );
            provider.defaultSerializeDateValue( value.getLastUpdated(), jgen );

            if ( value.getHref() != null )
            {
                jgen.writeStringField( "href", value.getHref() );
            }

            jgen.writeStringField( "symbol", value.getSymbol() );

            jgen.writeEndObject();
        }
    }
}
