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
package org.hisp.dhis.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper methods to deserialise JSON back to POJOs using jackson's {@link
 * com.fasterxml.jackson.databind.ObjectMapper}.
 *
 * @author Jan Bernitt
 */
@Slf4j
public class JsonUtils {
  private JsonUtils() {
    throw new UnsupportedOperationException("util");
  }

  public static <T, E extends Exception> T jsonToObject(
      JsonNode value,
      Class<T> type,
      ObjectMapper mapper,
      Function<JsonProcessingException, E> handler)
      throws E {
    try {
      return mapper.treeToValue(value, type);
    } catch (JsonProcessingException ex) {
      throw handler.apply(ex);
    }
  }

  public static <T> T jsonToObject(
      JsonNode value, Class<T> type, T defaultValue, ObjectMapper mapper) {
    try {
      return mapper.treeToValue(value, type);
    } catch (JsonProcessingException ex) {
      if (log.isDebugEnabled()) {
        log.debug(ex.getMessage());
      }
      return defaultValue;
    }
  }
}
