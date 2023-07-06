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
package org.hisp.dhis.schema.validation;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Setter;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.schema.DefaultPropertyIntrospectorService;
import org.hisp.dhis.schema.PropertyIntrospectorService;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.Property.Value;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.schema.introspection.JacksonPropertyIntrospector;
import org.hisp.dhis.schema.introspection.PropertyPropertyIntrospector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This is a unit test for the {@link DefaultSchemaValidator}.
 *
 * @author Jan Bernitt
 */
class DefaultSchemaValidatorTest {

  @Builder
  // avoid having to write the setters here
  @Setter
  public static class SimpleFields {

    String optional;

    String string;

    String email;

    String url;

    String password;

    String color;

    Integer integer;

    Float aFloat;

    Double aDouble;

    List<Integer> list;

    @JsonProperty
    public String getOptional() {
      return optional;
    }

    @JsonProperty
    @Property(required = Value.TRUE)
    @PropertyRange(min = 5, max = 25)
    public String getString() {
      return string;
    }

    @JsonProperty
    @Property(PropertyType.EMAIL)
    public String getEmail() {
      return email;
    }

    @JsonProperty
    @Property(PropertyType.URL)
    public String getUrl() {
      return url;
    }

    @JsonProperty
    @Property(PropertyType.PASSWORD)
    public String getPassword() {
      return password;
    }

    @JsonProperty
    @Property(PropertyType.COLOR)
    public String getColor() {
      return color;
    }

    @JsonProperty
    @PropertyRange(min = 13, max = 42)
    public Integer getInteger() {
      return integer;
    }

    @JsonProperty
    @PropertyRange(min = 13, max = 42)
    public Float getAFloat() {
      return aFloat;
    }

    @JsonProperty
    @PropertyRange(min = 13, max = 42)
    public Double getADouble() {
      return aDouble;
    }

    @JsonProperty
    @PropertyRange(min = 2, max = 4)
    public List<Integer> getList() {
      return list;
    }
  }

  private final SchemaService schemaService = mock(SchemaService.class);

  private final DefaultSchemaValidator validator = new DefaultSchemaValidator(schemaService);

  private final PropertyIntrospectorService introspectorService =
      new DefaultPropertyIntrospectorService(
          new JacksonPropertyIntrospector().then(new PropertyPropertyIntrospector()));

  private final Schema schema = new Schema(SimpleFields.class, "singular", "plural");

  @BeforeEach
  void setUpSchema() {
    schema.setPropertyMap(introspectorService.getPropertiesMap(SimpleFields.class));
    when(schemaService.getDynamicSchema(SimpleFields.class)).thenReturn(schema);
  }

  @Test
  void testRequiredPropertyIsNull() {
    assertError(
        ErrorCode.E4000, SimpleFields.builder().build(), "Missing required property `string`.");
  }

  @Test
  void testStringPropertyTooLong() {
    // fake column length limitation
    schema.getProperty("string").setLength(20);
    assertError(
        ErrorCode.E4001,
        SimpleFields.builder().string("123456789012345678901").build(),
        "Maximum length of property `string`is 20, but given length was 21.");
  }

  @Test
  void testStringPropertyShorterThanMinLength() {
    assertError(
        ErrorCode.E4002,
        SimpleFields.builder().string("Hey").build(),
        "Allowed length range for property `string` is [5 to 25], but given length was 3.");
  }

  @Test
  void testStringPropertyLongerThanMaxLength() {
    assertError(
        ErrorCode.E4002,
        SimpleFields.builder().string("12345678901234567890123456").build(),
        "Allowed length range for property `string` is [5 to 25], but given length was 26.");
  }

  @Test
  void testEmailPropertyValid() {
    assertNoError(SimpleFields.builder().string("valid").email("test@exmaple.com").build());
  }

  @Test
  void testEmailPropertyInvalid() {
    assertError(
        ErrorCode.E4003,
        SimpleFields.builder().string("valid").email("notAnEmail").build(),
        "Property `email` requires a valid email address, was given `notAnEmail`.");
  }

  @Test
  void testUrlPropertyValid() {
    assertNoError(SimpleFields.builder().string("valid").password("veryGoodS3cret").build());
  }

  @Test
  void testUrlPropertyInvalid() {
    assertError(
        ErrorCode.E4004,
        SimpleFields.builder().string("valid").url("notAnURL").build(),
        "Property `url` requires a valid URL, was given `notAnURL`.");
  }

  @Test
  void testPasswordPropertyValid() {
    assertNoError(SimpleFields.builder().string("valid").password("veryGoodS3cret").build());
  }

  @Test
  void testPasswordPropertyInvalid() {
    assertError(
        ErrorCode.E4005,
        SimpleFields.builder().string("valid").password("tooShort").build(),
        "Property `password` requires a valid password, was given `tooShort`.");
  }

  @Test
  void testColorPropertyValid() {
    assertNoError(SimpleFields.builder().string("valid").color("#445566").build());
  }

  @Test
  void testColorPropertyInvalid() {
    assertError(
        ErrorCode.E4006,
        SimpleFields.builder().string("valid").color("notAColor").build(),
        "Property `color` requires a valid HEX color, was given `notAColor`.");
  }

  @Test
  void testIntegerPropertySmallerThanMinValue() {
    assertError(
        ErrorCode.E4008,
        SimpleFields.builder().string("valid").integer(7).build(),
        "Allowed range for numeric property `integer` is [13 to 42], but number given was 7.");
  }

  @Test
  void testIntegerPropertyLargerThanMaxValue() {
    assertError(
        ErrorCode.E4008,
        SimpleFields.builder().string("valid").integer(78).build(),
        "Allowed range for numeric property `integer` is [13 to 42], but number given was 78.");
  }

  @Test
  void testIntegerPropertyBetweenMinMax() {
    assertNoError(SimpleFields.builder().string("valid").integer(20).build());
  }

  @Test
  void testFloatPropertySmallerThanMinValue() {
    assertError(
        ErrorCode.E4008,
        SimpleFields.builder().string("valid").aFloat(7f).build(),
        "Allowed range for numeric property `aFloat` is [13 to 42], but number given was 7.");
  }

  @Test
  void testFloatPropertyLargerThanMaxValue() {
    assertError(
        ErrorCode.E4008,
        SimpleFields.builder().string("valid").aFloat(78f).build(),
        "Allowed range for numeric property `aFloat` is [13 to 42], but number given was 78.");
  }

  @Test
  void testFloatPropertyBetweenMinMax() {
    assertNoError(SimpleFields.builder().string("valid").aFloat(20f).build());
  }

  @Test
  void testDoublePropertySmallerThanMinValue() {
    assertError(
        ErrorCode.E4008,
        SimpleFields.builder().string("valid").aDouble(7d).build(),
        "Allowed range for numeric property `aDouble` is [13 to 42], but number given was 7.");
  }

  @Test
  void testDoublePropertyLargerThanMaxValue() {
    assertError(
        ErrorCode.E4008,
        SimpleFields.builder().string("valid").aDouble(78d).build(),
        "Allowed range for numeric property `aDouble` is [13 to 42], but number given was 78.");
  }

  @Test
  void testDoublePropertyBetweenMinMax() {
    assertNoError(SimpleFields.builder().string("valid").aDouble(20d).build());
  }

  @Test
  void testCollectionPropertySizeSmallerThanMinSize() {
    assertError(
        ErrorCode.E4007,
        SimpleFields.builder().string("valid").list(emptyList()).build(),
        "Allowed size range for collection property `list` is [2 to 4], but size given was 0.");
  }

  @Test
  void testCollectionPropertySizeLargerThanMaxSize() {
    assertError(
        ErrorCode.E4007,
        SimpleFields.builder().string("valid").list(asList(1, 2, 3, 4, 5)).build(),
        "Allowed size range for collection property `list` is [2 to 4], but size given was 5.");
  }

  @Test
  void testCollectionPropertySizeBetweenMinMaxSize() {
    assertNoError(SimpleFields.builder().string("valid").list(asList(1, 2, 3)).build());
  }

  private void assertError(ErrorCode expected, SimpleFields actual, String expectedMessage) {
    List<ErrorReport> reports = validator.validate(actual, false);
    assertEquals(1, reports.size(), "expected 1 report");
    ErrorReport report = reports.get(0);
    assertEquals(expected, report.getErrorCode());
    assertEquals(expectedMessage, report.getMessage());
  }

  private void assertNoError(SimpleFields actual) {
    List<ErrorReport> reports = validator.validate(actual, false);
    assertEquals(0, reports.size());
  }
}
