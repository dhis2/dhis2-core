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
package org.hisp.dhis.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Data;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen
 */
class SchemaFieldIntrospectorTest extends SingleSetupIntegrationTestBase {

  @Autowired private SchemaService schemaService;

  @Test
  void testMethodScan() {
    Schema schema = schemaService.getDynamicSchema(SimpleWithMethods.class);
    assertNotNull(schema.getProperty("x"));
    assertNotNull(schema.getProperty("y"));
  }

  @Test
  void testFieldScan() {
    Schema schema = schemaService.getDynamicSchema(SimpleWithFields.class);
    assertNotNull(schema.getProperty("x"));
    assertNotNull(schema.getProperty("y"));
  }

  @Test
  void testFieldMethodScan() {
    Schema schema = schemaService.getDynamicSchema(SimpleWithFieldAndMethod.class);
    assertNotNull(schema.getProperty("x"));
    assertNotNull(schema.getProperty("y"));
  }

  @Test
  void testNamespaces() {
    Schema schema = schemaService.getDynamicSchema(SimpleWithFieldAndMethodWithNamespace.class);
    assertEquals("simple", schema.getName());
    assertEquals("https://simple.com", schema.getNamespace());
    assertNotNull(schema.getProperty("x"));
    assertNotNull(schema.getProperty("y"));
    assertEquals("https://x.simple.com", schema.getProperty("x").getNamespace());
    assertEquals("https://y.simple.com", schema.getProperty("y").getNamespace());
  }
}

@Data
class SimpleWithMethods {

  private String x;

  private String y;

  @JsonProperty
  public String getX() {
    return x;
  }

  @JsonProperty
  public String getY() {
    return y;
  }
}

@Data
class SimpleWithFields {

  @JsonProperty private String x;

  @JsonProperty private String y;
}

@Data
class SimpleWithFieldAndMethod {

  @JsonProperty private String x;

  private String y;

  @JsonProperty
  public String getY() {
    return y;
  }
}

@Data
@JsonRootName(value = "simple", namespace = "https://simple.com")
class SimpleWithFieldAndMethodWithNamespace {

  @JsonProperty(namespace = "https://x.simple.com")
  private String x;

  private String y;

  @JsonProperty(namespace = "https://y.simple.com")
  public String getY() {
    return y;
  }
}
