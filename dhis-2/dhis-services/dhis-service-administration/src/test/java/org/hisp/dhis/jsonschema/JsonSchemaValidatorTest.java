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
package org.hisp.dhis.jsonschema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class JsonSchemaValidatorTest {

  private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

  @BeforeAll()
  static void setup() {
    Locale.setDefault(new Locale("en", "US"));
  }

  @Test
  void validateCheck_ValidFile() {
    JsonNode jsonNode = getJsonNodeFromFile("check_with_all_required_fields.yaml");
    Set<ValidationMessage> validationMessages =
        JsonSchemaValidator.validateDataIntegrityCheck(jsonNode);
    assertTrue(validationMessages.isEmpty());
  }

  @Test
  void validateCheck_CheckMissingDescription() {
    JsonNode jsonNode = getJsonNodeFromFile("check_without_description.yaml");
    Set<ValidationMessage> validationMessages =
        JsonSchemaValidator.validateDataIntegrityCheck(jsonNode);
    assertEquals(1, validationMessages.size());
    assertContainsValidationMessages(
        validationMessages, Set.of("$: required property 'description' not found"));
  }

  @Test
  void validateCheck_CheckMissingName() {
    JsonNode jsonNode = getJsonNodeFromFile("check_without_name.yaml");
    Set<ValidationMessage> validationMessages =
        JsonSchemaValidator.validateDataIntegrityCheck(jsonNode);
    assertEquals(1, validationMessages.size());
    assertContainsValidationMessages(
        validationMessages, Set.of("$: required property 'name' not found"));
  }

  @Test
  void validateCheck_CheckMissing10Fields() {
    JsonNode jsonNode = getJsonNodeFromFile("check_without_10_required_fields.yaml");
    Set<ValidationMessage> validationMessages =
        JsonSchemaValidator.validateDataIntegrityCheck(jsonNode);
    assertEquals(8, validationMessages.size());
    assertContainsValidationMessages(
        validationMessages,
        Set.of(
            "$: required property 'details_id_type' not found",
            "$: required property 'details_sql' not found",
            "$: required property 'introduction' not found",
            "$: required property 'recommendation' not found",
            "$: required property 'section' not found",
            "$: required property 'section_order' not found",
            "$: required property 'severity' not found",
            "$: required property 'summary_sql' not found"));
  }

  private void assertContainsValidationMessages(
      Set<ValidationMessage> validations, Set<String> expected) {
    assertTrue(
        validations.stream()
            .map(ValidationMessage::getMessage)
            .collect(Collectors.toSet())
            .containsAll(expected));
  }

  private JsonNode getJsonNodeFromFile(String file) {
    try (InputStream is =
        new ClassPathResource("test-data-integrity-checks/" + file).getInputStream()) {
      return mapper.readTree(is);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
