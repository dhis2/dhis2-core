/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.openapi;

import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.common.OpenApi;
import org.junit.jupiter.api.Test;

class PropertyTest {
  @OpenApi.Property(value = String.class)
  private static class AProperty {}

  private static class AnotherProperty {
    @OpenApi.Property
    public void setOpenApiProperty(String openApiProperty) {}

    public void setProperty(String property) {}
  }

  private static class IgnoredProperty {

    @OpenApi.Ignore @JsonProperty private String openApiProperty;

    @JsonProperty
    public void setOpenApiProperty(String openApiProperty) {}
  }

  @JsonProperty private AProperty aProperty;

  @Test
  void testGetPropertiesGivenOpenApiPropertyAnnotatedClassThatHasValueSet() {
    Collection<Property> properties = Property.getProperties(PropertyTest.class);
    assertEquals(String.class, new ArrayList<>(properties).get(0).getType());
  }

  @Test
  void testGetPropertiesGivenOpenApiPropertyAnnotatedSetter() {
    Collection<Property> properties = Property.getProperties(AnotherProperty.class);
    assertEquals(1, properties.size());
    Property property = new ArrayList<>(properties).get(0);
    assertEquals("openApiProperty", property.getName());
    assertEquals(String.class, property.getType());
  }

  @Test
  void testGetPropertiesGivenOpenApiIgnoreAndJsonPropertyAnnotatedField() {
    Collection<Property> properties = Property.getProperties(IgnoredProperty.class);
    assertEquals(0, properties.size());
  }

  @Getter
  @Setter
  public static class DefaultProperty {
    private String initial = "hello";

    @JsonProperty(defaultValue = "42")
    private int annotated;

    @OpenApi.Property(defaultValue = "true")
    private boolean annotatedOpenApi;

    @JsonProperty
    public String getInitial() {
      return initial;
    }
  }

  @Test
  void testGetPropertiesDefaultValues() {
    Map<String, Property> properties =
        Property.getProperties(DefaultProperty.class).stream()
            .collect(toMap(Property::getName, Function.identity()));
    assertEquals(3, properties.size());
    assertEquals("hello", properties.get("initial").getDefaultValue());
    assertEquals("42", properties.get("annotated").getDefaultValue());
    assertEquals("true", properties.get("annotatedOpenApi").getDefaultValue());
  }
}
