/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.commons.jackson.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.Date;
import org.hisp.dhis.util.DateUtils;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ParseDateStdDeserializer extends JsonDeserializer<Date> {
  @Override
  public Date deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    JsonToken currentToken = parser.getCurrentToken();
    if (currentToken == JsonToken.VALUE_STRING) {
      String dateString = parser.getValueAsString();
      try {
        return DateUtils.parseDate(dateString);
      } catch (Exception ignored) {
        if (dateString.matches("[0-9]+") && dateString.length() > 12) {
          return new Date(parser.getValueAsLong());
        }
        throw createInvalidDateException(parser);
      }
    }
    if (currentToken == JsonToken.VALUE_NUMBER_INT) {
      try {
        return new Date(parser.getValueAsLong());
      } catch (Exception ignored) {
        throw createInvalidDateException(parser);
      }
    }
    throw createInvalidDateException(parser);
  }

  private IOException createInvalidDateException(JsonParser parser) throws IOException {
    return new IOException(
        String.format(
            "Invalid date format '%s', only ISO format or UNIX Epoch timestamp is supported.",
            parser.getValueAsString()));
  }
}
