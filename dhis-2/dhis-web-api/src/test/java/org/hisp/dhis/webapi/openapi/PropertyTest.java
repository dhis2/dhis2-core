/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.openapi;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.jsontree.Json;
import org.junit.jupiter.api.Test;

class PropertyTest {

  @OpenApi.Property(value = String.class)
  private static class StringInOpenAPI {}

  private static class RedefinedType {

    @JsonProperty private StringInOpenAPI text;
  }

  @Test
  void testGetPropertiesGivenOpenApiPropertyAnnotatedClassThatHasValueSet() {
    Collection<Property> properties = Property.getProperties(RedefinedType.class);
    assertEquals(1, properties.size());
    assertEquals(String.class, new ArrayList<>(properties).get(0).getType());
  }

  /**
   * When some properties have OpenAPI annotation but none has jackson annotations all properties
   * are included implicitly. This is because it is assumed that the OpenAPI annotation is used to
   * adjust the annotated property, not to select it.
   */
  private static class AnnotatedSetter {
    @OpenApi.Property
    public void setExplicit(String value) {}

    public void setImplicit(String value) {}
  }

  @Test
  void testGetPropertiesGivenOpenApiPropertyAnnotatedSetter() {
    Collection<Property> properties = Property.getProperties(AnnotatedSetter.class);
    assertEquals(2, properties.size());
    assertEquals(
        Set.of("explicit", "implicit"),
        properties.stream().map(Property::getName).collect(toSet()));
    assertEquals(Set.of(String.class), properties.stream().map(Property::getType).collect(toSet()));
  }

  /**
   * When a jackson annotated property is ignored using OpenAPI annotations that property is
   * excluded. Any accessor with a OpenAPI or jackson annotation is also included.
   */
  private static class IgnoredField {

    @OpenApi.Ignore @JsonProperty private String excluded;

    @JsonProperty
    public void setIncluded(String value) {}
  }

  @Test
  void testGetPropertiesGivenOpenApiIgnoreAndJsonPropertyAnnotatedField() {
    List<Property> properties = Property.getProperties(IgnoredField.class);
    assertEquals(1, properties.size());
    assertEquals("included", properties.get(0).getName());
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
    assertEquals(Json.of("hello"), properties.get("initial").getDefaultValue());
    assertEquals(Json.of(42), properties.get("annotated").getDefaultValue());
    assertEquals(Json.of(true), properties.get("annotatedOpenApi").getDefaultValue());
  }
}
