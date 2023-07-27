package org.hisp.dhis.jsonschema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class JsonSchemaValidatorTest {

  private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

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
        validationMessages, Set.of("$.description: is missing but it is required"));
  }

  @Test
  void validateCheck_CheckMissingName() {
    JsonNode jsonNode = getJsonNodeFromFile("check_without_name.yaml");
    Set<ValidationMessage> validationMessages =
        JsonSchemaValidator.validateDataIntegrityCheck(jsonNode);
    assertEquals(1, validationMessages.size());
    assertContainsValidationMessages(
        validationMessages, Set.of("$.name: is missing but it is required"));
  }

  @Test
  void validateCheck_CheckMissing10Fields() {
    JsonNode jsonNode = getJsonNodeFromFile("check_without_10_required_fields.yaml");
    Set<ValidationMessage> validationMessages =
        JsonSchemaValidator.validateDataIntegrityCheck(jsonNode);
    assertEquals(10, validationMessages.size());
    assertContainsValidationMessages(
        validationMessages,
        Set.of(
            "$.details_id_type: is missing but it is required",
            "$.details_sql: is missing but it is required",
            "$.details_uid: is missing but it is required",
            "$.introduction: is missing but it is required",
            "$.recommendation: is missing but it is required",
            "$.section: is missing but it is required",
            "$.section_order: is missing but it is required",
            "$.severity: is missing but it is required",
            "$.summary_sql: is missing but it is required",
            "$.summary_uid: is missing but it is required"));
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
    try {
      return mapper.readTree(
          new ClassPathResource("test-data-integrity-checks/" + file).getInputStream());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
