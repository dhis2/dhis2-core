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
package org.hisp.dhis.dxf2.webmessage.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import org.apache.commons.io.IOUtils;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.dxf2.webmessage.WebMessageParseException;

/** Created by vanyas on 5/4/17. */
public class WebMessageParseUtils {
  private static final ObjectMapper JSON_MAPPER = JacksonObjectMapperConfig.staticJsonMapper();

  public static <T> T fromWebMessageResponse(InputStream input, Class<T> klass)
      throws WebMessageParseException {
    StringWriter writer = new StringWriter();
    try {
      IOUtils.copy(input, writer, "UTF-8");
    } catch (IOException e) {
      throw new WebMessageParseException("Could not read the InputStream" + e.getMessage(), e);
    }
    return parseJson(writer.toString(), klass);
  }

  public static <T> T fromWebMessageResponse(String input, Class<T> klass)
      throws WebMessageParseException {
    return parseJson(input, klass);
  }

  private static <T> T parseJson(String input, Class<T> klass) throws WebMessageParseException {
    JsonNode objectNode = null;
    try {
      objectNode = JSON_MAPPER.readTree(input);
    } catch (IOException e) {
      throw new WebMessageParseException("Invalid JSON String. " + e.getMessage(), e);
    }

    JsonNode responseNode = null;

    if (objectNode != null) {
      responseNode = objectNode.get("response");
    } else {
      throw new WebMessageParseException("The object node is null. Could not parse the JSON.");
    }

    try {
      return JSON_MAPPER.readValue(responseNode.toString(), klass);
    } catch (IOException e) {
      throw new WebMessageParseException("Could not parse the JSON." + e.getMessage(), e);
    }
  }
}
