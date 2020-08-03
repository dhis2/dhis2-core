package org.hisp.dhis.schema.transformer;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.Builder;
import lombok.Data;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.schema.AbstractPropertyTransformer;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.UUID;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class UserPropertyTransformer
    extends AbstractPropertyTransformer<User>
{
    @Override
    public Object transform( Object o )
    {
        if ( !(o instanceof User) )
        {
            return o;
        }

        User user = (User) o;
        UserCredentials userCredentials = user.getUserCredentials();

        Assert.notNull( userCredentials, "UserCredentials should never be null." );

        return UserDto.builder()
            .id( userCredentials.getUuid().toString() )
            .username( userCredentials.getUsername() )
            .build();
    }

    @Data
    @Builder
    public static class UserDto
    {
        private String id;

        private String username;

        @JsonProperty
        public String getId()
        {
            return id;
        }

        @JsonProperty
        public String getUsername()
        {
            return username;
        }
    }

    public static final class JacksonSerialize extends StdSerializer<User>
    {
        public JacksonSerialize()
        {
            super( User.class );
        }

        @Override
        public void serialize( User user, JsonGenerator gen, SerializerProvider provider ) throws IOException
        {
            UserCredentials userCredentials = user.getUserCredentials();
            Assert.notNull( userCredentials, "UserCredentials should never be null." );

            gen.writeStartObject();
            gen.writeStringField( "id", userCredentials.getUuid().toString() );
            gen.writeStringField( "username", userCredentials.getUsername() );
            gen.writeEndObject();
        }
    }

    public static final class JacksonDeserialize extends StdDeserializer<User>
    {
        public JacksonDeserialize()
        {
            super( User.class );
        }

        @Override
        public User deserialize( JsonParser jp, DeserializationContext ctxt ) throws IOException, JsonProcessingException
        {
            User user = new User();
            UserCredentials userCredentials = new UserCredentials();
            user.setUserCredentials( userCredentials );

            JsonNode node = jp.getCodec().readTree( jp );

            if ( node.has( "id" ) )
            {
                String identifier = node.get( "id" ).asText();

                if ( CodeGenerator.isValidUid( identifier ) )
                {
                    user.setUid( identifier );
                    userCredentials.setUid( identifier );
                }
                else
                {
                    userCredentials.setUuid( UUID.fromString( identifier ) );
                }
            }

            if ( node.has( "code" ) )
            {
                String code = node.get( "code" ).asText();

                user.setCode( code );
                userCredentials.setCode( code );
            }

            if ( node.has( "username" ) )
            {
                userCredentials.setUsername( node.get( "username" ).asText() );
            }

            return user;
        }
    }
}
