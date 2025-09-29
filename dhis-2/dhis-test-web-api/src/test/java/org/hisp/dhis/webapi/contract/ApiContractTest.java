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
package org.hisp.dhis.webapi.contract;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonGenerator;
import org.hisp.dhis.test.webapi.json.domain.JsonSchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * This test class executes tests against all contracts from the dhis2-api-contracts repo, which are
 * copied to the test resources directory. The TestFactory iterates over all contracts found. <br>
 * Test steps are:
 *
 * <ul>
 *   <li>Read in contract
 *   <li>Create and save a type expected in the contract e.g. Category
 *   <li>Make the HTTP request from the contract e.g. GET, POST...
 *   <li>Assert HTTP response code
 *   <li>Assert response has no validation errors when the contract JSON schema is applied
 * </ul>
 */
@Slf4j
@Transactional
class ApiContractTest extends H2ControllerIntegrationTestBase {

  private static final ObjectMapper mapper = new ObjectMapper();

  @TestFactory
  @DisplayName("Test API contracts")
  Stream<DynamicTest> apiContractTest() {
    Set<ApiContract> contracts = getContracts();
    if (contracts.isEmpty()) {
      return Stream.of(
          DynamicTest.dynamicTest(
              "Problem reading API contracts",
              () -> Assertions.fail("Problem reading API contracts")));
    }

    return contracts.stream()
        .map(
            contract ->
                DynamicTest.dynamicTest(
                    "Testing contract: " + contract.name(),
                    () -> assertGetRequestContract(contract)));
  }

  private void assertGetRequestContract(ApiContract contract) throws JsonProcessingException {
    // Given an object exists
    String uid = createType(contract);
    assertNotNull(uid, "Created UID should not be null for type being tested");

    // When a GET call is made for that object
    HttpResponse response = GET(contract.requestUrl().replace("{id}", uid));

    // Then the HTTP status code should match
    assertEquals(contract.responseStatus(), response.status().code(), "HTTP status code mismatch");

    // And the response body should not have any JSON schema validation errors
    Set<ValidationMessage> errors =
        contract.jsonSchema().validate(mapper.readTree(response.content().toJson()));
    assertTrue(
        errors.isEmpty(),
        () -> String.format("Valid JSON should pass schema validation, errors: %s", errors));
  }

  /**
   * Create type to be tested using existing JsonSchema code
   *
   * @param contract contract which contains type
   * @return UID of created type
   */
  private String createType(ApiContract contract) {
    // get type from contract
    String type = contract.name();

    JsonSchema schema = GET("/schemas/" + type).content().as(JsonSchema.class);
    JsonGenerator generator = new JsonGenerator(schema);

    Map<String, String> objects = generator.generateObjects(schema);

    // create needed object(s)
    // last created is the one we want to test
    // those before might be objects it depends upon that need to be created first
    String uid = null;
    for (Map.Entry<String, String> entry : objects.entrySet()) {
      uid = assertStatus(HttpStatus.CREATED, POST(entry.getKey(), entry.getValue()));
    }
    return uid;
  }

  /**
   * Reads in JSON contracts from src/test/resources/api-contracts/contracts/. Returns a set of
   * instantiated {@link ApiContract}s.
   *
   * @return set of instantiated {@link ApiContract}s.
   */
  private static Set<ApiContract> getContracts() {
    Set<ApiContract> contracts = new HashSet<>();
    Path contractsDir = Path.of("src/test/resources/contracts/");

    try (Stream<Path> paths = Files.walk(contractsDir)) {
      paths
          .filter(path -> path.toString().endsWith("contract.json"))
          .forEach(
              filePath -> {
                try {
                  JsonNode jsonNode = mapper.readTree(Files.readString(filePath));
                  contracts.add(mapper.treeToValue(jsonNode, ApiContract.class));
                } catch (Exception e) {
                  System.err.printf(
                      "Failed to load contract from %s: %s%n", filePath, e.getMessage());
                }
              });
    } catch (IOException e) {
      System.out.printf("Error walking directory %s: %s%n", contractsDir, e.getMessage());
      return Set.of();
    }
    return contracts;
  }
}
