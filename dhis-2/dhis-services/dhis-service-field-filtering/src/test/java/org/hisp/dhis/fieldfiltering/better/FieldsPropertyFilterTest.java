/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.fieldfiltering.better;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FieldsPropertyFilterTest {

  private final FieldsPropertyFilter filter = new FieldsPropertyFilter();

  @ParameterizedTest
  @MethodSource("ifEmptyTransformation")
  void testIfEmptyTransformation(Object value, boolean expected, String description) {
    boolean result = filter.checkIfEmpty(value);
    assertEquals(expected, result, description);
  }

  static Stream<Arguments> ifEmptyTransformation() {
    return Stream.of(
        Arguments.of(List.of(), true, "Empty collection should be empty"),
        Arguments.of(List.of("item"), false, "Non-empty collection should not be empty"),
        Arguments.of("", true, "Empty string should be empty"),
        Arguments.of("hello", false, "Non-empty string should not be empty"),
        Arguments.of(new String[0], true, "Empty array should be empty"),
        Arguments.of(new String[] {"item"}, false, "Non-empty array should not be empty"),
        Arguments.of(null, true, "Null value should be empty"),
        Arguments.of(42, false, "Non-collection object should not be empty"));
  }

  // TODO(ivo) fail validation in FieldsParser as this should be invalid
  @Test
  void testApplyUnknownTransformation() {
    Fields.Transformation transformation = new Fields.Transformation("unknown");
    Object originalValue = "test";
    FieldsPropertyFilter.TransformationResult result =
        filter.applyTransformation(transformation, originalValue, "field");

    assertEquals(originalValue, result.value());
    assertEquals("field", result.fieldName());
  }

  @Test
  void testApplyRenameTransformation() {
    Fields.Transformation transformation = new Fields.Transformation("rename", "newFieldName");
    Object originalValue = "test";
    FieldsPropertyFilter.TransformationResult result =
        filter.applyTransformation(transformation, originalValue, "oldFieldName");

    assertEquals(originalValue, result.value());
    assertEquals("newFieldName", result.fieldName());
  }

  // TODO(ivo) fail validation in FieldsParser as this should be invalid
  @Test
  void testApplyRenameTransformationWithoutArgument() {
    Fields.Transformation transformation = new Fields.Transformation("rename");
    Object originalValue = "test";
    FieldsPropertyFilter.TransformationResult result =
        filter.applyTransformation(transformation, originalValue, "fieldName");

    assertEquals(originalValue, result.value());
    assertEquals("fieldName", result.fieldName());
  }

  @Test
  void testApplyIsEmptyWithListTransformation() {
    FieldsPropertyFilter.TransformationResult result =
        filter.applyIsEmpty(List.of("item"), "fieldName");

    assertEquals(false, result.value());
    assertEquals("fieldName", result.fieldName());
  }

  @Test
  void testTransformationPipelineHandlesUnknownTransformations() {
    List<Fields.Transformation> transformations =
        List.of(
            new Fields.Transformation("isEmpty"),
            new Fields.Transformation("nonExistentTransform"),
            new Fields.Transformation("rename", "hasItems"));

    Object currentValue = List.of("item");
    String currentFieldName = "items";

    // Simulate the transformation pipeline manually
    for (Fields.Transformation transformation : transformations) {
      FieldsPropertyFilter.TransformationResult result =
          filter.applyTransformation(transformation, currentValue, currentFieldName);
      currentValue = result.value();
      currentFieldName = result.fieldName();
    }

    // Verify that the pipeline continues after unknown transformation:
    // 1. isEmpty: List.of("item") -> false
    // 2. nonExistentTransform: false -> false (unchanged)
    // 3. rename: "items" -> "hasItems"
    assertEquals(false, currentValue);
    assertEquals("hasItems", currentFieldName);
  }

  public record TestObject(String id, List<String> items) {}
}
