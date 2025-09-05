package org.hisp.dhis.webapi.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * This test class executes tests against all contracts from the dhis2-api-contracts repo. There is
 * 1 parameterized test which iterates over all contracts found. <br>
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

  @Autowired private IdentifiableObjectManager manager;
  @Autowired private SchemaService schemaService;

  @ParameterizedTest(name = "{0} test")
  @MethodSource("getContracts")
  @DisplayName("Test API contracts")
  void apiContractTest(ApiContract contract)
      throws InvocationTargetException,
          NoSuchMethodException,
          IllegalAccessException,
          JsonProcessingException {
    assertGetRequestContract(contract);
  }

  private <T extends IdentifiableObject> void assertGetRequestContract(ApiContract contract)
      throws InvocationTargetException,
          NoSuchMethodException,
          IllegalAccessException,
          JsonProcessingException {
    // Given a object exists
    T identifiableObject = createTypeAndSave(contract);

    // When a GET call is made for that object
    HttpResponse response = GET(contract.requestUrl().replace("{id}", identifiableObject.getUid()));

    // Then the HTTP status code should match
    assertEquals(contract.responseStatus(), response.status().code(), "HTTP status code mismatch");

    // And the response body should not have any JSON schema validation errors
    Set<ValidationMessage> errors =
        contract.jsonSchema().validate(mapper.readTree(response.content().toJson()));
    assertTrue(
        errors.isEmpty(),
        () -> String.format("Valid JSON should pass schema validation, errors: %s", errors));
  }

  @SuppressWarnings("unchecked")
  private <T extends IdentifiableObject> T createTypeAndSave(ApiContract contract)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    // get type from contract
    String type = contract.name();

    // change 1st char to lowercase for get schema use
    String typeLowerCase = Character.toLowerCase(type.charAt(0)) + type.substring(1);
    Schema schema = schemaService.getSchemaBySingularName(typeLowerCase);

    // get class from schema so we can cast a created object to that type
    Class<?> klass = schema.getKlass();

    // invoke 'create' test method in TestBase to create an object to save and test against
    // it is expected that the create+'type' method exists. Create one if not.
    Method method = TestBase.class.getMethod("create" + type, char.class);
    Object createdType = method.invoke(null, 'a');
    T identifiableObject = (T) klass.cast(createdType);
    manager.save(identifiableObject);
    return identifiableObject;
  }

  /**
   * Reads in JSON contracts from a jar at classpath /contracts. Returns a set of instantiated
   * {@link ApiContract}s.
   *
   * @return set of instantiated {@link ApiContract}s.
   * @throws URISyntaxException URISyntaxException
   */
  private static Set<ApiContract> getContracts() throws URISyntaxException {
    Set<ApiContract> contracts = new HashSet<>();

    URI uri =
        Objects.requireNonNull(
                Thread.currentThread().getContextClassLoader().getResource("contracts"))
            .toURI();

    // impl for jar
    try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
      Path contractsPath = fileSystem.getPath("/contracts");

      try (Stream<Path> paths = Files.walk(contractsPath)) {
        paths
            .filter(Files::isRegularFile)
            .filter(
                path -> path.toString().endsWith(".json") && path.toString().contains("-contract"))
            .forEach(
                filePath -> {
                  try {
                    JsonNode jsonNode = mapper.readTree(Files.readString(filePath));
                    contracts.add(mapper.treeToValue(jsonNode, ApiContract.class));
                  } catch (Exception e) {
                    log.error(e.getMessage());
                  }
                });
      }
    } catch (IOException e) {
      log.error(e.getMessage());
    }
    return contracts;
  }
}
