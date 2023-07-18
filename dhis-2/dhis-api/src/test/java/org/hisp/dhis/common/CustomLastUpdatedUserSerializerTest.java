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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import java.io.StringWriter;
import java.io.Writer;
import javax.xml.namespace.QName;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CustomLastUpdatedUserSerializer}.
 *
 * @author Volker Schmidt
 */
class CustomLastUpdatedUserSerializerTest {

  private User user;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setUid("jshfdkd323");
    user.setFirstName("Peter");
    user.setSurname("Brown");
  }

  @Test
  void serializeXml() throws Exception {
    Writer jsonWriter = new StringWriter();
    ToXmlGenerator jsonGenerator = new XmlFactory().createGenerator(jsonWriter);
    SerializerProvider serializerProvider = new ObjectMapper().getSerializerProvider();
    jsonGenerator.setNextName(new QName("urn:test", "lastUpdatedBy"));
    new CustomLastUpdatedUserSerializer().serialize(user, jsonGenerator, serializerProvider);
    jsonGenerator.flush();
    assertEquals(
        "<wstxns1:lastUpdatedBy xmlns:wstxns1=\"urn:test\" id=\"jshfdkd323\" name=\"Peter Brown\"/>",
        jsonWriter.toString());
  }

  @Test
  void serializeJson() throws Exception {
    Writer jsonWriter = new StringWriter();
    JsonGenerator jsonGenerator = new JsonFactory().createGenerator(jsonWriter);
    SerializerProvider serializerProvider = new ObjectMapper().getSerializerProvider();
    new CustomLastUpdatedUserSerializer().serialize(user, jsonGenerator, serializerProvider);
    jsonGenerator.flush();
    assertEquals("{\"id\":\"jshfdkd323\",\"name\":\"Peter Brown\"}", jsonWriter.toString());
  }
}
