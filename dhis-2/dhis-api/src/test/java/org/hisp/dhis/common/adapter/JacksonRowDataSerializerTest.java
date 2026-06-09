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
package org.hisp.dhis.common.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.StringWriter;
import java.util.List;
import org.hisp.dhis.common.RawJsonValue;
import org.junit.jupiter.api.Test;

class JacksonRowDataSerializerTest {

  private final JacksonRowDataSerializer serializer = new JacksonRowDataSerializer();

  @Test
  void regularStringValuesAreWrappedInJsonStrings() throws Exception {
    List<List<Object>> rows = List.of(List.of("hello", "world"));

    assertEquals("[[\"hello\",\"world\"]]", serialize(rows));
  }

  @Test
  void rawJsonValueIsEmittedAsInlineJson() throws Exception {
    List<List<Object>> rows = List.of(List.of("Alice", new RawJsonValue("{\"key\":\"value\"}")));

    assertEquals("[[\"Alice\",{\"key\":\"value\"}]]", serialize(rows));
  }

  @Test
  void rawJsonArrayIsEmittedAsInlineJsonArray() throws Exception {
    List<List<Object>> rows = List.of(List.of(new RawJsonValue("[1,2,3]")));

    assertEquals("[[[1,2,3]]]", serialize(rows));
  }

  @Test
  void nullRawJsonValueIsEmittedAsEmptyString() throws Exception {
    List<Object> row = new java.util.ArrayList<>();
    row.add(null);
    List<List<Object>> rows = List.of(row);

    assertEquals("[[\"\"]]", serialize(rows));
  }

  private String serialize(List<List<Object>> rows) throws Exception {
    StringWriter writer = new StringWriter();
    JsonGenerator generator = new JsonFactory().createGenerator(writer);
    serializer.serialize(rows, generator, new ObjectMapper().getSerializerProvider());
    generator.flush();
    return writer.toString();
  }
}
