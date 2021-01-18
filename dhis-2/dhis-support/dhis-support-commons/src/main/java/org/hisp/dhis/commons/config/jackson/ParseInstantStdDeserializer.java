package org.hisp.dhis.commons.config.jackson;

/*
 *  Copyright (c) 2004-2020, University of Oslo
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
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonParseException;
import org.hisp.dhis.util.DateUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class ParseInstantStdDeserializer extends StdDeserializer<Instant>
{
    public ParseInstantStdDeserializer()
    {
        super( Instant.class );
    }

    @Override
    public Instant deserialize( JsonParser parser, DeserializationContext context )
        throws IOException
    {
        String valueAsString = tryGetValueAsString( parser );

        if ( Objects.nonNull( valueAsString ) )
        {
            try
            {
                return DateUtils.instantFromDateAsString( valueAsString );
            }
            catch ( Exception e )
            {
                throw new JsonParseException( parser,
                    String.format(
                        "Invalid date format '%s', only '" + DateUtils.ISO8601_NO_TZ_PATTERN + "' format is supported.",
                        valueAsString ) );
            }
        }
        return null;
    }

    private String tryGetValueAsString( JsonParser parser )
    {
        try
        {
            return parser.getValueAsString();
        }
        catch ( Exception e )
        {
            // intentionally empty
        }
        return null;
    }

}
