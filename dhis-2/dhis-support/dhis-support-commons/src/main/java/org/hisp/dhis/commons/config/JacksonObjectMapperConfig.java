package org.hisp.dhis.commons.config;

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

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import org.hisp.dhis.commons.config.jackson.EmptyStringToNullStdDeserializer;
import org.hisp.dhis.commons.config.jackson.ParseDateStdDeserializer;
import org.hisp.dhis.commons.config.jackson.WriteDateStdSerializer;
import org.hisp.dhis.commons.config.jackson.geometry.JtsXmlModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Date;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Configuration
public class JacksonObjectMapperConfig
{
    public static final ObjectMapper jsonMapper = new ObjectMapper();
    public static final ObjectMapper xmlMapper = new XmlMapper();

    @Primary
    @Bean( "jsonMapper" )
    public ObjectMapper jsonMapper()
    {
        return jsonMapper;
    }

    @Bean( "xmlMapper" )
    public ObjectMapper xmlMapper()
    {
        return xmlMapper;
    }

    public static ObjectMapper staticJsonMapper()
    {
        return jsonMapper;
    }

    public static ObjectMapper staticXmlMapper()
    {
        return xmlMapper;
    }

    static
    {
        SimpleModule module = new SimpleModule();
        module.addDeserializer( String.class, new EmptyStringToNullStdDeserializer() );
        module.addDeserializer( Date.class, new ParseDateStdDeserializer() );
        module.addSerializer( Date.class, new WriteDateStdSerializer() );

        ObjectMapper[] objectMappers = new ObjectMapper[]{ jsonMapper, xmlMapper };

        for ( ObjectMapper objectMapper : objectMappers )
        {
            objectMapper.registerModules( module,
                new JavaTimeModule(),
                new Jdk8Module(),
                new Hibernate5Module()
            );

            objectMapper.setSerializationInclusion( JsonInclude.Include.NON_NULL );
            objectMapper.disable( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS );
            objectMapper.disable( SerializationFeature.FAIL_ON_EMPTY_BEANS );
            objectMapper.enable( SerializationFeature.WRAP_EXCEPTIONS );

            objectMapper.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
            objectMapper.disable( DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY );
            objectMapper.enable( DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES );
            objectMapper.enable( DeserializationFeature.WRAP_EXCEPTIONS );

            objectMapper.disable( MapperFeature.AUTO_DETECT_FIELDS );
            objectMapper.disable( MapperFeature.AUTO_DETECT_CREATORS );
            objectMapper.disable( MapperFeature.AUTO_DETECT_GETTERS );
            objectMapper.disable( MapperFeature.AUTO_DETECT_SETTERS );
            objectMapper.disable( MapperFeature.AUTO_DETECT_IS_GETTERS );
        }

        jsonMapper.registerModule( new JtsModule( new GeometryFactory( new PrecisionModel(), 4326 ) ) );
        xmlMapper.registerModule( new JtsXmlModule() );
    }
}
